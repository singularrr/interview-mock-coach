package com.example.interviewmockcoach.service.ai;

import com.example.interviewmockcoach.config.AiProperties;
import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.CategoryCountDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.enums.DifficultyLevel;
import com.example.interviewmockcoach.enums.QuestionCategory;
import com.example.interviewmockcoach.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.mode", havingValue = "openai")
public class OpenAiInterviewAiService implements InterviewAiService {

    private static final TypeReference<List<InterviewQuestionDto>> QUESTION_LIST_TYPE = new TypeReference<>() {
    };

    private final AiProperties aiProperties;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Override
    public List<InterviewQuestionDto> generateQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
        if (!isOpenAiReady()) {
            return fallbackGenerateQuestions(profile, contexts, questionCount);
        }

        try {
            String content = createChatClient().prompt()
                    .system(buildQuestionSystemPrompt())
                    .user(buildQuestionUserPrompt(profile, contexts, questionCount))
                    .call()
                    .content();
            List<InterviewQuestionDto> parsed = JsonUtils.fromJson(extractJson(content), QUESTION_LIST_TYPE);
            return normalizeQuestions(parsed, profile, contexts, questionCount);
        } catch (Exception ex) {
            log.warn("OpenAI generateQuestions failed, fallback to rule engine: {}", ex.getMessage());
            return fallbackGenerateQuestions(profile, contexts, questionCount);
        }
    }

    @Override
    public AnswerEvaluationDto evaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts) {
        if (!isOpenAiReady()) {
            return fallbackEvaluateAnswer(profile, question, answerText, contexts);
        }

        try {
            String content = createChatClient().prompt()
                    .system(buildEvaluationSystemPrompt())
                    .user(buildEvaluationUserPrompt(profile, question, answerText, contexts))
                    .call()
                    .content();
            AnswerEvaluationDto parsed = JsonUtils.fromJson(extractJson(content), AnswerEvaluationDto.class);
            return normalizeEvaluation(parsed, question, answerText, contexts);
        } catch (Exception ex) {
            log.warn("OpenAI evaluateAnswer failed, fallback to rule engine: {}", ex.getMessage());
            return fallbackEvaluateAnswer(profile, question, answerText, contexts);
        }
    }

    @Override
    public InterviewSummaryDto generateSummary(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations, List<RetrievedContextDto> contexts) {
        if (!isOpenAiReady()) {
            return fallbackGenerateSummary(profile, questions, evaluations, contexts);
        }

        try {
            String content = createChatClient().prompt()
                    .system(buildSummarySystemPrompt())
                    .user(buildSummaryUserPrompt(profile, questions, evaluations, contexts))
                    .call()
                    .content();
            InterviewSummaryDto parsed = JsonUtils.fromJson(extractJson(content), InterviewSummaryDto.class);
            return normalizeSummary(parsed, profile, questions, evaluations, contexts);
        } catch (Exception ex) {
            log.warn("OpenAI generateSummary failed, fallback to rule engine: {}", ex.getMessage());
            return fallbackGenerateSummary(profile, questions, evaluations, contexts);
        }
    }

    private boolean isOpenAiReady() {
        return aiProperties.getOpenai() != null
                && aiProperties.getOpenai().getApiKey() != null
                && !aiProperties.getOpenai().getApiKey().isBlank()
                && chatClientBuilderProvider.getIfAvailable() != null;
    }

    private ChatClient createChatClient() {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("ChatClient.Builder is not available");
        }
        return builder.build();
    }

    private String buildQuestionSystemPrompt() {
        return "你是研究生复试模拟官。请严格只返回合法 JSON 数组，不要输出 markdown、解释或代码块。"
                + "数组中的每个对象必须包含 questionId, category, difficulty, content, shouldFollowUp, orderIndex。"
                + "category 只能是 SELF_INTRO, PROFESSIONAL_BASE, PROJECT_EXPERIENCE, ADMISSION_MOTIVE, RESEARCH_DIRECTION, FOLLOW_UP。"
                + "difficulty 只能是 EASY, MEDIUM, HARD。";
    }

    private String buildQuestionUserPrompt(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
        return "请根据以下候选人信息和检索资料，生成 " + questionCount + " 道复试题目。\n"
                + "候选人信息：\n" + renderProfile(profile) + "\n"
                + "检索资料：\n" + renderContexts(contexts) + "\n"
                + "要求：题目要覆盖自我介绍、专业基础、项目经历、报考动机、研究方向等类别，题目内容要结合候选人背景和资料。\n"
                + "请只输出 JSON 数组。";
    }

    private String buildEvaluationSystemPrompt() {
        return "你是严谨的复试面试官。请严格只返回合法 JSON 对象，不要输出 markdown、解释或代码块。"
                + "对象必须包含 evaluationId, questionId, score, strengths, weaknesses, suggestions, followUpPoints, shouldFollowUp, answerText。"
                + "strengths、weaknesses、suggestions、followUpPoints 都必须是字符串数组。score 必须是 0 到 100 的整数。";
    }

    private String buildEvaluationUserPrompt(CandidateProfileDto profile, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts) {
        return "请对候选人的回答进行评分与点评。\n"
                + "候选人信息：\n" + renderProfile(profile) + "\n"
                + "题目：\n" + renderQuestion(question) + "\n"
                + "参考资料：\n" + renderContexts(contexts) + "\n"
                + "用户回答：\n" + safe(answerText) + "\n"
                + "请只输出 JSON 对象。";
    }

    private String buildSummarySystemPrompt() {
        return "你是复试模拟官的总结助手。请严格只返回合法 JSON 对象，不要输出 markdown、解释或代码块。"
                + "对象必须包含 summaryId, sessionId, overallScore, weakAreas, frequentQuestionCategories, summaryAdvice, markdownContent。"
                + "weakAreas 和 frequentQuestionCategories 都必须是包含 category 和 count 的数组。";
    }

    private String buildSummaryUserPrompt(CandidateProfileDto profile,
                                          List<InterviewQuestionDto> questions,
                                          List<AnswerEvaluationDto> evaluations,
                                          List<RetrievedContextDto> contexts) {
        return "请根据候选人、问题和评分结果生成总结。\n"
                + "候选人信息：\n" + renderProfile(profile) + "\n"
                + "问题列表：\n" + renderQuestions(questions) + "\n"
                + "评分列表：\n" + renderEvaluations(evaluations) + "\n"
                + "参考资料：\n" + renderContexts(contexts) + "\n"
                + "请只输出 JSON 对象。";
    }

    private List<InterviewQuestionDto> normalizeQuestions(List<InterviewQuestionDto> parsed,
                                                          CandidateProfileDto profile,
                                                          List<RetrievedContextDto> contexts,
                                                          int questionCount) {
        if (parsed == null || parsed.isEmpty()) {
            return fallbackGenerateQuestions(profile, contexts, questionCount);
        }

        List<InterviewQuestionDto> normalized = new ArrayList<>();
        for (int i = 0; i < parsed.size(); i++) {
            InterviewQuestionDto item = parsed.get(i);
            if (item == null || safe(item.getContent()).isBlank()) {
                continue;
            }
            normalized.add(new InterviewQuestionDto(
                    safeId(item.getQuestionId()),
                    item.getCategory() == null ? QuestionCategory.FOLLOW_UP : item.getCategory(),
                    item.getDifficulty() == null ? DifficultyLevel.MEDIUM : item.getDifficulty(),
                    safe(item.getContent()),
                    item.isShouldFollowUp(),
                    item.getOrderIndex() > 0 ? item.getOrderIndex() : i + 1
            ));
        }

        if (normalized.isEmpty()) {
            return fallbackGenerateQuestions(profile, contexts, questionCount);
        }
        if (normalized.size() > questionCount) {
            return normalized.subList(0, questionCount);
        }

        List<InterviewQuestionDto> result = new ArrayList<>(normalized);
        while (result.size() < questionCount) {
            int sourceIndex = result.size() % normalized.size();
            InterviewQuestionDto template = normalized.get(sourceIndex);
            result.add(new InterviewQuestionDto(
                    UUID.randomUUID().toString(),
                    template.getCategory(),
                    template.getDifficulty(),
                    template.getContent() + contextSuffix(contexts, result.size()),
                    template.isShouldFollowUp(),
                    result.size() + 1
            ));
        }
        return result;
    }

    private AnswerEvaluationDto normalizeEvaluation(AnswerEvaluationDto parsed,
                                                     InterviewQuestionDto question,
                                                     String answerText,
                                                     List<RetrievedContextDto> contexts) {
        if (parsed == null) {
            return fallbackEvaluateAnswer(null, question, answerText, contexts);
        }
        return new AnswerEvaluationDto(
                safeId(parsed.getEvaluationId()),
                safe(parsed.getQuestionId()).isBlank() ? question.getQuestionId() : parsed.getQuestionId(),
                clampScore(parsed.getScore()),
                safeList(parsed.getStrengths()),
                safeList(parsed.getWeaknesses()),
                safeList(parsed.getSuggestions()),
                safeList(parsed.getFollowUpPoints()),
                parsed.isShouldFollowUp(),
                safe(answerText)
        );
    }

    private InterviewSummaryDto normalizeSummary(InterviewSummaryDto parsed,
                                                 CandidateProfileDto profile,
                                                 List<InterviewQuestionDto> questions,
                                                 List<AnswerEvaluationDto> evaluations,
                                                 List<RetrievedContextDto> contexts) {
        if (parsed == null) {
            return fallbackGenerateSummary(profile, questions, evaluations, contexts);
        }
        InterviewSummaryDto dto = new InterviewSummaryDto(
                safeId(parsed.getSummaryId()),
                safe(parsed.getSessionId()),
                clampScore(parsed.getOverallScore()),
                parsed.getWeakAreas() == null ? List.of() : parsed.getWeakAreas(),
                parsed.getFrequentQuestionCategories() == null ? List.of() : parsed.getFrequentQuestionCategories(),
                safe(parsed.getSummaryAdvice()),
                safe(parsed.getMarkdownContent())
        );
        if (dto.getMarkdownContent().isBlank()) {
            return fallbackGenerateSummary(profile, questions, evaluations, contexts);
        }
        return dto;
    }

    private List<InterviewQuestionDto> fallbackGenerateQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
        List<InterviewQuestionDto> questions = baseQuestions(profile, contexts);
        if (questionCount <= questions.size()) {
            return questions.subList(0, questionCount);
        }

        List<InterviewQuestionDto> result = new ArrayList<>(questions);
        while (result.size() < questionCount) {
            InterviewQuestionDto template = questions.get(result.size() % questions.size());
            result.add(new InterviewQuestionDto(
                    UUID.randomUUID().toString(),
                    template.getCategory(),
                    template.getDifficulty(),
                    template.getContent() + contextSuffix(contexts, result.size()),
                    template.isShouldFollowUp(),
                    result.size() + 1
            ));
        }
        return result;
    }

    private AnswerEvaluationDto fallbackEvaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts) {
        String normalized = answerText == null ? "" : answerText.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        int score = 60;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> followUpPoints = new ArrayList<>();
        String cue = contextCue(contexts);

        if (normalized.length() >= 50) {
            score += 8;
            strengths.add("回答内容较完整");
        } else {
            score -= 5;
            weaknesses.add("回答偏短");
            suggestions.add("建议补充过程和结论");
        }

        if (containsAny(lower, "首先", "其次", "最后", "一方面", "另一方面")) {
            score += 8;
            strengths.add("表达结构清晰");
        }
        if (containsAny(lower, "项目", "实验", "成果", "优化", "实习")) {
            score += 8;
            strengths.add("有案例支撑");
        }
        if (containsAny(lower, safeLower(profile.getMajor()), safeLower(profile.getResearchDirection()), safeLower(profile.getSchool()))) {
            score += 6;
            strengths.add("与报考方向匹配");
        }
        if (contexts != null && !contexts.isEmpty() && containsAny(lower, "资料", "文档", "培养方案", "导师", "论文", "研究方向")) {
            score += 6;
            strengths.add("引用了检索资料");
        }
        if (containsAny(lower, "不知道", "不太清楚", "没想过")) {
            score -= 15;
            weaknesses.add("不确定表达偏多");
            suggestions.add("不会时先说思路，再尝试给出方向");
        }

        switch (question.getCategory()) {
            case SELF_INTRO -> followUpPoints.add("为什么选择这所学校和这个专业");
            case PROJECT_EXPERIENCE -> followUpPoints.add("你在项目里具体负责什么");
            case RESEARCH_DIRECTION -> followUpPoints.add("你目前最关注的研究问题是什么");
            case ADMISSION_MOTIVE -> followUpPoints.add("你对这所学校的了解有哪些");
            case PROFESSIONAL_BASE -> followUpPoints.add("这个概念的原理是什么");
            case FOLLOW_UP -> followUpPoints.add("请继续补充一个细节或例子");
        }

        if (!cue.isBlank()) {
            followUpPoints.add("可以结合 " + cue + " 进一步展开");
        }

        score = Math.max(0, Math.min(100, score));
        if (strengths.isEmpty()) {
            strengths.add("整体表达较稳定");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("暂时没有明显硬伤");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("建议增加一个更具体的细节");
        }

        return new AnswerEvaluationDto(UUID.randomUUID().toString(), question.getQuestionId(), score, strengths, weaknesses, suggestions, followUpPoints, question.isShouldFollowUp() || score < 75, answerText);
    }

    private InterviewSummaryDto fallbackGenerateSummary(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations, List<RetrievedContextDto> contexts) {
        int overallScore = evaluations == null || evaluations.isEmpty() ? 0 : (int) Math.round(evaluations.stream().mapToInt(AnswerEvaluationDto::getScore).average().orElse(0));

        Map<String, Long> weakCounts = new LinkedHashMap<>();
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        if (questions != null) {
            categoryCounts = questions.stream().collect(Collectors.groupingBy(q -> q.getCategory().name(), LinkedHashMap::new, Collectors.counting()));
        }
        if (evaluations != null && questions != null) {
            Map<String, QuestionCategory> questionCategoryMap = questions.stream().collect(Collectors.toMap(InterviewQuestionDto::getQuestionId, InterviewQuestionDto::getCategory, (a, b) -> a));
            for (AnswerEvaluationDto evaluation : evaluations) {
                if (evaluation.getScore() < 75) {
                    QuestionCategory category = questionCategoryMap.get(evaluation.getQuestionId());
                    if (category != null) {
                        weakCounts.merge(category.name(), 1L, Long::sum);
                    }
                }
            }
        }

        List<CategoryCountDto> weakAreas = weakCounts.entrySet().stream().map(entry -> new CategoryCountDto(entry.getKey(), entry.getValue())).toList();
        List<CategoryCountDto> frequentCategories = categoryCounts.entrySet().stream().map(entry -> new CategoryCountDto(entry.getKey(), entry.getValue())).toList();

        String advice = overallScore >= 85 ? "继续保持，重点打磨追问和研究规划。" : "建议重点补强专业基础、项目表达和研究方向阐述。";
        StringBuilder markdown = new StringBuilder();
        markdown.append("# 复试模拟总结\n\n");
        markdown.append("- 学校：").append(safe(profile == null ? null : profile.getSchool())).append("\n");
        markdown.append("- 专业：").append(safe(profile == null ? null : profile.getMajor())).append("\n");
        markdown.append("- 研究方向：").append(safe(profile == null ? null : profile.getResearchDirection())).append("\n");
        markdown.append("- 综合得分：").append(overallScore).append("\n\n");
        markdown.append("## 薄弱项\n");
        markdown.append(renderCategoryLines(weakAreas));
        markdown.append("\n## 高频题型\n");
        markdown.append(renderCategoryLines(frequentCategories));
        markdown.append("\n## 改进建议\n- ").append(advice).append("\n\n");
        markdown.append("## 检索参考\n").append(renderContextMarkdown(contexts));

        return new InterviewSummaryDto(UUID.randomUUID().toString(), null, overallScore, weakAreas, frequentCategories, advice, markdown.toString());
    }

    private List<InterviewQuestionDto> baseQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts) {
        String school = safe(profile == null ? null : profile.getSchool());
        String major = safe(profile == null ? null : profile.getMajor());
        String direction = safe(profile == null ? null : profile.getResearchDirection());
        String cue = contextCue(contexts);

        return List.of(
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, appendContext("请先做一个简短的自我介绍。", cue), true, 1),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.MEDIUM, appendContext("结合你的学习经历，谈谈你最熟悉的一个专业基础概念。", cue), true, 2),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.MEDIUM, appendContext("请介绍一个你印象最深的项目，说明你的贡献和结果。", cue), true, 3),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.ADMISSION_MOTIVE, DifficultyLevel.EASY, appendContext("为什么选择报考 " + school + " 的 " + major + "？", cue), true, 4),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.RESEARCH_DIRECTION, DifficultyLevel.MEDIUM, appendContext("你对 " + direction + " 方向的理解是什么？", cue), true, 5),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.HARD, appendContext("如果老师追问这个知识点的原理，你会怎么展开？", cue), true, 6),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.HARD, appendContext("如果让你重做这个项目，你最想优化哪一部分？", cue), true, 7),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, appendContext("请用 30 秒总结一下自己。", cue), false, 8)
        );
    }

    private String appendContext(String question, String cue) {
        if (cue == null || cue.isBlank()) {
            return question;
        }
        return question + " 结合资料中的 " + cue + " 会更好。";
    }

    private String contextSuffix(List<RetrievedContextDto> contexts, int index) {
        String cue = contextCue(contexts);
        if (cue.isBlank()) {
            return index % 2 == 0 ? "（追问版）" : "（补充版）";
        }
        return index % 2 == 0 ? " 结合 " + cue + " 再展开一点。" : " 如果老师继续追问，可围绕 " + cue + " 回答。";
    }

    private String contextCue(List<RetrievedContextDto> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "";
        }
        return contexts.stream()
                .filter(item -> item != null)
                .max(Comparator.comparingDouble(RetrievedContextDto::getScore))
                .map(item -> {
                    String title = safe(item.getDocumentTitle());
                    if (!title.isBlank()) {
                        return title;
                    }
                    String content = safe(item.getContent());
                    return content.length() <= 18 ? content : content.substring(0, 18);
                })
                .orElse("");
    }

    private String renderContexts(List<RetrievedContextDto> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "[]";
        }
        return JsonUtils.toJson(contexts);
    }

    private String renderProfile(CandidateProfileDto profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("school", safe(profile == null ? null : profile.getSchool()));
        map.put("major", safe(profile == null ? null : profile.getMajor()));
        map.put("researchDirection", safe(profile == null ? null : profile.getResearchDirection()));
        map.put("resumePoints", safe(profile == null ? null : profile.getResumePoints()));
        return JsonUtils.toJson(map);
    }

    private String renderQuestion(InterviewQuestionDto question) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("questionId", safe(question == null ? null : question.getQuestionId()));
        map.put("category", question == null || question.getCategory() == null ? null : question.getCategory().name());
        map.put("difficulty", question == null || question.getDifficulty() == null ? null : question.getDifficulty().name());
        map.put("content", safe(question == null ? null : question.getContent()));
        map.put("shouldFollowUp", question != null && question.isShouldFollowUp());
        map.put("orderIndex", question == null ? 0 : question.getOrderIndex());
        return JsonUtils.toJson(map);
    }

    private String renderQuestions(List<InterviewQuestionDto> questions) {
        return JsonUtils.toJson(questions == null ? List.of() : questions);
    }

    private String renderEvaluations(List<AnswerEvaluationDto> evaluations) {
        return JsonUtils.toJson(evaluations == null ? List.of() : evaluations);
    }

    private String renderCategoryLines(List<CategoryCountDto> items) {
        if (items == null || items.isEmpty()) {
            return "- 暂无\n";
        }
        StringBuilder builder = new StringBuilder();
        for (CategoryCountDto item : items) {
            builder.append("- ").append(item.getCategory()).append(": ").append(item.getCount()).append("\n");
        }
        return builder.toString();
    }

    private String renderContextMarkdown(List<RetrievedContextDto> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "- 本次没有额外检索到资料。\n";
        }
        StringBuilder builder = new StringBuilder();
        for (RetrievedContextDto context : contexts) {
            builder.append("- ")
                    .append(safe(context.getDocumentTitle()).isBlank() ? context.getDocumentId() : context.getDocumentTitle())
                    .append("（")
                    .append(safe(context.getSourceType()))
                    .append("）\n");
        }
        return builder.toString();
    }

    private String extractJson(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            if (firstLineBreak >= 0) {
                trimmed = trimmed.substring(firstLineBreak + 1).trim();
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start = -1;
        if (objectStart >= 0 && arrayStart >= 0) {
            start = Math.min(objectStart, arrayStart);
        } else if (objectStart >= 0) {
            start = objectStart;
        } else if (arrayStart >= 0) {
            start = arrayStart;
        }
        if (start >= 0) {
            trimmed = trimmed.substring(start);
        }
        int objectEnd = trimmed.lastIndexOf('}');
        int arrayEnd = trimmed.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);
        if (end >= 0) {
            trimmed = trimmed.substring(0, end + 1);
        }
        return trimmed.trim();
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String safeId(String value) {
        return safe(value).isBlank() ? UUID.randomUUID().toString() : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeLower(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String source, String... keywords) {
        if (source == null || source.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}