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
import java.util.HashMap;
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

    private static final TypeReference<List<InterviewQuestionDto>> QUESTION_LIST_TYPE = new TypeReference<>() { };

    private final AiProperties aiProperties;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Override
    public List<InterviewQuestionDto> generateQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
        if (!isOpenAiReady()) {
            return fallbackGenerateQuestions(profile, contexts, questionCount);
        }
        try {
            String content = createChatClient().prompt()
                    .system("请只输出 JSON 数组，每个对象包含 questionId, category, difficulty, content, shouldFollowUp, orderIndex。category 只能是 SELF_INTRO, PROFESSIONAL_BASE, PROJECT_EXPERIENCE, ADMISSION_MOTIVE, RESEARCH_DIRECTION, FOLLOW_UP。difficulty 只能是 EASY, MEDIUM, HARD。")
                    .user(buildQuestionUserPrompt(profile, contexts, questionCount))
                    .call()
                    .content();
            List<InterviewQuestionDto> parsed = JsonUtils.fromJson(extractJson(content), QUESTION_LIST_TYPE);
            return normalizeQuestions(parsed, profile, contexts, questionCount);
        } catch (Exception ex) {
            log.warn("OpenAI generateQuestions failed, fallback to rules: {}", ex.getMessage());
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
                    .system("请只输出 JSON 对象，包含 evaluationId, questionId, score, strengths, weaknesses, suggestions, followUpPoints, shouldFollowUp, answerText。score 必须是 0 到 100 的整数。")
                    .user(buildEvaluationUserPrompt(profile, question, answerText, contexts))
                    .call()
                    .content();
            AnswerEvaluationDto parsed = JsonUtils.fromJson(extractJson(content), AnswerEvaluationDto.class);
            return normalizeEvaluation(parsed, question, answerText, contexts);
        } catch (Exception ex) {
            log.warn("OpenAI evaluateAnswer failed, fallback to rules: {}", ex.getMessage());
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
                    .system("请只输出 JSON 对象，包含 summaryId, sessionId, overallScore, weakAreas, frequentQuestionCategories, summaryAdvice, markdownContent。weakAreas 和 frequentQuestionCategories 都必须是包含 category 和 count 的数组。")
                    .user(buildSummaryUserPrompt(profile, questions, evaluations, contexts))
                    .call()
                    .content();
            InterviewSummaryDto parsed = JsonUtils.fromJson(extractJson(content), InterviewSummaryDto.class);
            return normalizeSummary(parsed, profile, questions, evaluations, contexts);
        } catch (Exception ex) {
            log.warn("OpenAI generateSummary failed, fallback to rules: {}", ex.getMessage());
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

    private String buildQuestionUserPrompt(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
        return "候选人信息：" + renderProfile(profile) + "\n"
                + "参考资料：" + renderContexts(contexts) + "\n"
                + "请生成 " + questionCount + " 道复试问题。";
    }

    private String buildEvaluationUserPrompt(CandidateProfileDto profile, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts) {
        return "候选人信息：" + renderProfile(profile) + "\n"
                + "题目：" + renderQuestion(question) + "\n"
                + "参考资料：" + renderContexts(contexts) + "\n"
                + "回答：" + safe(answerText) + "\n"
                + "请输出回答评价。";
    }

    private String buildSummaryUserPrompt(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations, List<RetrievedContextDto> contexts) {
        return "候选人信息：" + renderProfile(profile) + "\n"
                + "题目列表：" + renderQuestions(questions) + "\n"
                + "评分列表：" + renderEvaluations(evaluations) + "\n"
                + "参考资料：" + renderContexts(contexts) + "\n"
                + "请输出总结。";
    }

    private List<InterviewQuestionDto> normalizeQuestions(List<InterviewQuestionDto> parsed, CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
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
            InterviewQuestionDto template = normalized.get(result.size() % normalized.size());
            result.add(new InterviewQuestionDto(
                    UUID.randomUUID().toString(),
                    template.getCategory(),
                    template.getDifficulty(),
                    template.getContent() + "（补充追问）",
                    template.isShouldFollowUp(),
                    result.size() + 1
            ));
        }
        return result;
    }

    private AnswerEvaluationDto normalizeEvaluation(AnswerEvaluationDto parsed, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts) {
        if (parsed == null) {
            return fallbackEvaluateAnswer(null, question, answerText, contexts);
        }
        return new AnswerEvaluationDto(
                safeId(parsed.getEvaluationId()),
                safe(parsed.getQuestionId()).isBlank() ? (question == null ? null : question.getQuestionId()) : parsed.getQuestionId(),
                clampScore(parsed.getScore()),
                safeList(parsed.getStrengths()),
                safeList(parsed.getWeaknesses()),
                safeList(parsed.getSuggestions()),
                safeList(parsed.getFollowUpPoints()),
                parsed.isShouldFollowUp(),
                safe(answerText)
        );
    }

    private InterviewSummaryDto normalizeSummary(InterviewSummaryDto parsed, CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations, List<RetrievedContextDto> contexts) {
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
        String school = safe(profile == null ? null : profile.getSchool());
        String major = safe(profile == null ? null : profile.getMajor());
        String direction = safe(profile == null ? null : profile.getResearchDirection());
        String cue = pickContextCue(contexts);

        List<InterviewQuestionDto> questions = List.of(
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, "请做一个简短的自我介绍，重点说明你为什么适合报考" + school + "的" + major + "方向。" + appendCue(cue), true, 1),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.MEDIUM, "结合你的本科学习经历，谈一谈你最熟悉的一个专业基础知识点。" + appendCue(cue), true, 2),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.MEDIUM, "请介绍一个你参与过的项目，并说明你在其中承担的职责。" + appendCue(cue), true, 3),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.ADMISSION_MOTIVE, DifficultyLevel.EASY, "为什么选择报考" + school + "的" + major + "专业？", true, 4),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.RESEARCH_DIRECTION, DifficultyLevel.MEDIUM, "你对" + direction + "方向的理解是什么，未来有什么研究规划？", true, 5),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.HARD, "如果老师继续追问这个知识点的原理，你会怎么展开说明？", true, 6),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.HARD, "如果让你重新做这个项目，你最想优化哪一部分，为什么？", true, 7),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, "请用 30 秒总结一下你自己。", false, 8)
        );

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
                    template.getContent() + "（补充题）",
                    template.isShouldFollowUp(),
                    result.size() + 1
            ));
        }
        return result;
    }

    private AnswerEvaluationDto fallbackEvaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts) {
        String answer = safe(answerText);
        String lower = answer.toLowerCase(Locale.ROOT);
        int score = 60;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> followUpPoints = new ArrayList<>();

        if (answer.length() >= 40) {
            score += 8;
            strengths.add("回答内容较完整");
        } else {
            score -= 5;
            weaknesses.add("回答偏短，信息量不足");
            suggestions.add("建议补充背景、过程和结果三个部分");
        }

        if (containsAny(lower, "首先", "其次", "最后", "一方面", "另一方面")) {
            score += 8;
            strengths.add("回答结构比较清晰");
        } else {
            weaknesses.add("结构感还不够明显");
            suggestions.add("可以按结论、理由、案例的顺序组织回答");
        }

        if (containsAny(lower, "项目", "实验", "实践", "结果", "优化", "代码")) {
            score += 8;
            strengths.add("有具体案例支撑");
        } else {
            weaknesses.add("缺少具体案例支撑");
            suggestions.add("建议加入一个项目或课程案例来证明自己的理解");
        }

        String school = safe(profile == null ? null : profile.getSchool()).toLowerCase(Locale.ROOT);
        String major = safe(profile == null ? null : profile.getMajor()).toLowerCase(Locale.ROOT);
        String direction = safe(profile == null ? null : profile.getResearchDirection()).toLowerCase(Locale.ROOT);
        if (containsAny(lower, school, major, direction)) {
            score += 6;
            strengths.add("和报考方向关联较强");
        }

        if (contexts != null && !contexts.isEmpty()) {
            String cue = pickContextCue(contexts);
            if (!cue.isBlank() && containsAny(lower, "资料", "文档", "培养方案", "导师", "研究方向", "官网")) {
                score += 6;
                strengths.add("结合了检索资料进行回答");
            }
            if (!cue.isBlank()) {
                followUpPoints.add("可以结合资料中的 " + cue + " 继续展开");
            }
        }

        if (containsAny(lower, "不知道", "不太清楚", "没想过", "忘了")) {
            score -= 15;
            weaknesses.add("表达中存在明显不确定性");
            suggestions.add("遇到不会的问题时，先说思路再给出有限结论");
        }

        if (question != null) {
            switch (question.getCategory()) {
                case SELF_INTRO -> followUpPoints.add("老师可能会继续问你为什么选择这个学校和专业");
                case PROJECT_EXPERIENCE -> followUpPoints.add("老师可能会继续追问你在项目中的具体职责");
                case RESEARCH_DIRECTION -> followUpPoints.add("老师可能会让你说明研究方向的具体问题意识");
                case ADMISSION_MOTIVE -> followUpPoints.add("老师可能会追问你对学校平台和资源的了解");
                case PROFESSIONAL_BASE -> followUpPoints.add("老师可能会继续问原理、推导或应用场景");
                case FOLLOW_UP -> followUpPoints.add("请补充一个更具体的例子或细节");
            }
        }

        if (strengths.isEmpty()) {
            strengths.add("整体表达较平稳");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("暂时没有明显短板");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("建议再补充一条更具体的事实或数据");
        }
        if (followUpPoints.isEmpty()) {
            followUpPoints.add("建议准备一条更具体的追问回答");
        }

        score = Math.max(0, Math.min(100, score));
        return new AnswerEvaluationDto(
                UUID.randomUUID().toString(),
                question == null ? null : question.getQuestionId(),
                score,
                strengths,
                weaknesses,
                suggestions,
                followUpPoints,
                score < 75 || (question != null && question.isShouldFollowUp()),
                answerText
        );
    }

    private InterviewSummaryDto fallbackGenerateSummary(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations, List<RetrievedContextDto> contexts) {
        int overallScore = (evaluations == null || evaluations.isEmpty())
                ? 0
                : (int) Math.round(evaluations.stream().mapToInt(AnswerEvaluationDto::getScore).average().orElse(0));

        Map<String, Long> weakCounts = new HashMap<>();
        Map<String, Long> categoryCounts = new HashMap<>();
        if (questions != null) {
            categoryCounts = questions.stream().collect(Collectors.groupingBy(
                    q -> q.getCategory().name(),
                    java.util.LinkedHashMap::new,
                    Collectors.counting()));
        }
        if (questions != null && evaluations != null) {
            Map<String, QuestionCategory> questionCategoryMap = questions.stream().collect(Collectors.toMap(
                    InterviewQuestionDto::getQuestionId,
                    InterviewQuestionDto::getCategory,
                    (a, b) -> a));
            for (AnswerEvaluationDto evaluation : evaluations) {
                if (evaluation != null && evaluation.getScore() < 75) {
                    QuestionCategory category = questionCategoryMap.get(evaluation.getQuestionId());
                    if (category != null) {
                        weakCounts.merge(category.name(), 1L, Long::sum);
                    }
                }
            }
        }

        List<CategoryCountDto> weakAreas = weakCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new CategoryCountDto(entry.getKey(), entry.getValue()))
                .toList();
        List<CategoryCountDto> frequentCategories = categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new CategoryCountDto(entry.getKey(), entry.getValue()))
                .toList();

        String advice = overallScore >= 85
                ? "整体表现稳定，建议继续强化追问应对和研究规划表述。"
                : "建议重点补强专业基础、项目表达和研究方向阐述。";

        StringBuilder markdown = new StringBuilder();
        markdown.append("# 复试模拟总结\n\n");
        markdown.append("## 基本信息\n");
        markdown.append("- 学校：").append(safe(profile == null ? null : profile.getSchool())).append("\n");
        markdown.append("- 专业：").append(safe(profile == null ? null : profile.getMajor())).append("\n");
        markdown.append("- 研究方向：").append(safe(profile == null ? null : profile.getResearchDirection())).append("\n\n");
        markdown.append("## 综合得分\n");
        markdown.append("- ").append(overallScore).append("\n\n");
        markdown.append("## 薄弱项\n");
        if (weakAreas.isEmpty()) {
            markdown.append("- 暂无明显薄弱项\n");
        } else {
            for (CategoryCountDto item : weakAreas) {
                markdown.append("- ").append(item.getCategory()).append(": ").append(item.getCount()).append("\n");
            }
        }
        markdown.append("\n## 高频题型\n");
        if (frequentCategories.isEmpty()) {
            markdown.append("- 暂无数据\n");
        } else {
            for (CategoryCountDto item : frequentCategories) {
                markdown.append("- ").append(item.getCategory()).append(": ").append(item.getCount()).append("\n");
            }
        }
        markdown.append("\n## 改进建议\n");
        markdown.append("- ").append(advice).append("\n\n");
        markdown.append("## 参考资料\n");
        if (contexts == null || contexts.isEmpty()) {
            markdown.append("- 本次未检索到额外资料\n");
        } else {
            for (RetrievedContextDto context : contexts) {
                if (context != null) {
                    markdown.append("- ").append(safe(context.getDocumentTitle()).isBlank() ? context.getDocumentId() : context.getDocumentTitle()).append("\n");
                }
            }
        }

        return new InterviewSummaryDto(
                UUID.randomUUID().toString(),
                null,
                overallScore,
                weakAreas,
                frequentCategories,
                advice,
                markdown.toString()
        );
    }

    private String renderProfile(CandidateProfileDto profile) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("school", safe(profile == null ? null : profile.getSchool()));
        map.put("major", safe(profile == null ? null : profile.getMajor()));
        map.put("researchDirection", safe(profile == null ? null : profile.getResearchDirection()));
        map.put("resumePoints", safe(profile == null ? null : profile.getResumePoints()));
        return JsonUtils.toJson(map);
    }

    private String renderQuestion(InterviewQuestionDto question) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("questionId", question == null ? "" : safe(question.getQuestionId()));
        map.put("category", question == null || question.getCategory() == null ? "" : question.getCategory().name());
        map.put("difficulty", question == null || question.getDifficulty() == null ? "" : question.getDifficulty().name());
        map.put("content", question == null ? "" : safe(question.getContent()));
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

    private String renderContexts(List<RetrievedContextDto> contexts) {
        return JsonUtils.toJson(contexts == null ? List.of() : contexts);
    }

    private String extractJson(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            trimmed = trimmed.substring(3, trimmed.length() - 3).trim();
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

    private String pickContextCue(List<RetrievedContextDto> contexts) {
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
                    return content.length() <= 20 ? content : content.substring(0, 20);
                })
                .orElse("");
    }

    private String appendCue(String cue) {
        return cue.isBlank() ? "" : " 可结合资料中的 " + cue + " 回答。";
    }
}