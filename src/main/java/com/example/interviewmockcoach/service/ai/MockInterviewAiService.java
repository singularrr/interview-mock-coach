package com.example.interviewmockcoach.service.ai;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.CategoryCountDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.enums.DifficultyLevel;
import com.example.interviewmockcoach.enums.QuestionCategory;
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

@Service
@ConditionalOnProperty(name = "ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockInterviewAiService implements InterviewAiService {

    @Override
    public List<InterviewQuestionDto> generateQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
        List<InterviewQuestionDto> templates = baseQuestions(profile, contexts);
        if (questionCount <= templates.size()) {
            return templates.subList(0, questionCount);
        }

        List<InterviewQuestionDto> questions = new ArrayList<>(templates);
        int index = 0;
        while (questions.size() < questionCount) {
            InterviewQuestionDto template = templates.get(index % templates.size());
            questions.add(new InterviewQuestionDto(
                    UUID.randomUUID().toString(),
                    template.getCategory(),
                    template.getDifficulty(),
                    template.getContent() + contextSuffix(contexts, index),
                    template.isShouldFollowUp(),
                    questions.size() + 1
            ));
            index++;
        }
        return questions;
    }

    @Override
    public AnswerEvaluationDto evaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts) {
        String normalized = answerText == null ? "" : answerText.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        int score = 60;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> followUpPoints = new ArrayList<>();
        String contextCue = contextCue(contexts);

        if (normalized.length() >= 40) {
            score += 8;
            strengths.add("回答内容比较完整");
        } else {
            score -= 5;
            weaknesses.add("回答偏短，信息量不足");
            suggestions.add("建议补充背景、过程和结果三个层次");
        }

        if (normalized.length() >= 120) {
            score += 6;
            strengths.add("展开充分，表达细节较多");
        }

        if (containsAny(lower, "首先", "其次", "最后", "一方面", "另一方面")) {
            score += 8;
            strengths.add("回答结构清晰");
        } else {
            weaknesses.add("结构感还不够强");
            suggestions.add("可以按结论、理由、案例的顺序组织回答");
        }

        if (containsAny(lower, "项目", "实验", "实习", "成果", "代码", "优化")) {
            score += 8;
            strengths.add("回答里有具体经历支撑");
        } else {
            weaknesses.add("缺少具体案例支撑");
            suggestions.add("建议加入一个项目或经历来佐证观点");
        }

        if (containsAny(lower, safeLower(profile.getMajor()), safeLower(profile.getResearchDirection()), safeLower(profile.getSchool()))) {
            score += 6;
            strengths.add("回答与报考方向相关");
        } else {
            weaknesses.add("和报考方向的贴合度还不够");
            suggestions.add("建议结合专业方向和研究兴趣做针对性表达");
        }

        if (contexts != null && !contexts.isEmpty() && containsAny(lower, "资料", "文档", "培养方案", "导师", "官网", "论文", "研究方向")) {
            score += 6;
            strengths.add("有结合检索资料进行回答");
        } else if (contexts != null && !contexts.isEmpty()) {
            suggestions.add("可以主动引用检索到的资料中的关键词或结论");
        }

        switch (question.getCategory()) {
            case SELF_INTRO -> {
                if (containsAny(lower, "本科", "项目", "实习", "竞赛")) {
                    score += 4;
                    strengths.add("自我介绍抓住了核心经历");
                } else {
                    followUpPoints.add("为什么选择当前学校和专业");
                }
            }
            case PROJECT_EXPERIENCE -> {
                if (containsAny(lower, "负责", "难点", "优化", "结果", "指标")) {
                    score += 5;
                    strengths.add("项目回答比较像复试表达");
                } else {
                    followUpPoints.add("这个项目里你具体负责什么");
                }
            }
            case RESEARCH_DIRECTION -> {
                if (containsAny(lower, "问题", "方法", "模型", "实验", "场景")) {
                    score += 6;
                    strengths.add("研究方向表达较具体");
                } else {
                    followUpPoints.add("你目前对这个方向的理解是什么");
                }
            }
            case ADMISSION_MOTIVE -> {
                if (containsAny(lower, "老师", "平台", "资源", "方向", "机会")) {
                    score += 5;
                    strengths.add("报考动机比较明确");
                } else {
                    followUpPoints.add("为什么选择这所学校而不是其他学校");
                }
            }
            case PROFESSIONAL_BASE -> {
                if (containsAny(lower, "原理", "定义", "流程", "算法", "公式")) {
                    score += 5;
                    strengths.add("专业基础表述较准确");
                } else {
                    followUpPoints.add("请你再展开一下这个概念的原理");
                }
            }
            case FOLLOW_UP -> score += 4;
        }

        if (containsAny(lower, "不知道", "不太清楚", "没想过", "忘了")) {
            score -= 15;
            weaknesses.add("表达里存在较明显的不确定感");
            suggestions.add("遇到不会的问题时，先说明思路再尝试回答");
            followUpPoints.add("如果老师继续追问，你打算从哪里切入");
        }

        if (!contextCue.isBlank()) {
            followUpPoints.add("可结合检索资料中的 " + contextCue + " 继续展开");
        }

        score = Math.max(0, Math.min(100, score));
        if (strengths.isEmpty()) {
            strengths.add("态度比较稳定");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("当前回答暂时没有明显硬伤");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("可以继续补充一个更具体的例子");
        }
        if (followUpPoints.isEmpty() && (question.isShouldFollowUp() || score < 75)) {
            followUpPoints.add("建议再补充一个细节或结果数据");
        }

        return new AnswerEvaluationDto(
                UUID.randomUUID().toString(),
                question.getQuestionId(),
                score,
                strengths,
                weaknesses,
                suggestions,
                followUpPoints,
                question.isShouldFollowUp() || score < 75,
                answerText
        );
    }

    @Override
    public InterviewSummaryDto generateSummary(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations, List<RetrievedContextDto> contexts) {
        int overallScore = evaluations == null || evaluations.isEmpty()
                ? 0
                : (int) Math.round(evaluations.stream().mapToInt(AnswerEvaluationDto::getScore).average().orElse(0));

        Map<String, Long> weakCounts = new LinkedHashMap<>();
        Map<String, Long> categoryCounts = new LinkedHashMap<>();

        if (questions != null) {
            categoryCounts = questions.stream().collect(Collectors.groupingBy(q -> q.getCategory().name(), LinkedHashMap::new, Collectors.counting()));
        }

        if (evaluations != null && questions != null && !questions.isEmpty()) {
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

        String advice = overallScore >= 85
                ? "整体表现稳定，建议继续强化项目细节、研究规划和追问应对。"
                : "建议重点补强项目表达、专业基础和研究方向阐述。";

        StringBuilder markdown = new StringBuilder();
        markdown.append("# 复试模拟总结\n\n");
        markdown.append("## 基本信息\n");
        markdown.append("- 学校：").append(safe(profile.getSchool())).append("\n");
        markdown.append("- 专业：").append(safe(profile.getMajor())).append("\n");
        markdown.append("- 研究方向：").append(safe(profile.getResearchDirection())).append("\n\n");
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
        markdown.append("## 检索参考\n");
        markdown.append(renderContextMarkdown(contexts));

        return new InterviewSummaryDto(UUID.randomUUID().toString(), null, overallScore, weakAreas, frequentCategories, advice, markdown.toString());
    }

    private List<InterviewQuestionDto> baseQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts) {
        String school = safe(profile.getSchool());
        String major = safe(profile.getMajor());
        String direction = safe(profile.getResearchDirection());
        String contextCue = contextCue(contexts);

        return List.of(
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, appendContext("请做一个简短的自我介绍，重点说明你为什么适合报考 " + school + " 的 " + major + " 方向。", contextCue), true, 1),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.MEDIUM, appendContext("结合你的本科背景，谈一谈 " + major + " 中你最熟悉的一个基础概念。", contextCue), true, 2),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.MEDIUM, appendContext("请介绍一个你最有代表性的项目，并说明你在其中的具体贡献。", contextCue), true, 3),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.ADMISSION_MOTIVE, DifficultyLevel.EASY, appendContext("为什么选择报考 " + school + "，以及为什么选择这个专业？", contextCue), true, 4),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.RESEARCH_DIRECTION, DifficultyLevel.MEDIUM, appendContext("请结合 " + direction + " 方向，谈谈你未来可能关注的研究问题。", contextCue), true, 5),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.HARD, appendContext("如果老师追问你一个专业基础细节，你会如何把知识点讲清楚？", contextCue), true, 6),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.HARD, appendContext("如果让你重新做一次这个项目，你最想优化哪一部分？为什么？", contextCue), true, 7),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, appendContext("请用 30 秒总结一下你自己。", contextCue), false, 8)
        );
    }

    private String appendContext(String question, String contextCue) {
        if (contextCue == null || contextCue.isBlank()) {
            return question;
        }
        return question + " 也可以结合检索资料中的 " + contextCue + " 来回答。";
    }

    private String contextSuffix(List<RetrievedContextDto> contexts, int index) {
        String cue = contextCue(contexts);
        if (cue.isBlank()) {
            return index % 2 == 0 ? "（追问版）" : "（补充版）";
        }
        return index % 2 == 0 ? " 结合 " + cue + " 再补充一点。" : " 如果老师继续追问，可围绕 " + cue + " 说明。";
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

    private String renderContextMarkdown(List<RetrievedContextDto> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "- 本次未检索到额外资料，当前总结基于答题表现生成。\n";
        }
        StringBuilder builder = new StringBuilder();
        for (RetrievedContextDto context : contexts) {
            builder.append("- ")
                    .append(safe(context.getDocumentTitle()).isBlank() ? context.getDocumentId() : context.getDocumentTitle())
                    .append("（")
                    .append(safe(context.getSourceType()))
                    .append("）")
                    .append("\n");
            String content = safe(context.getContent());
            if (!content.isBlank()) {
                builder.append("  - ").append(content.length() > 120 ? content.substring(0, 120) + "..." : content).append("\n");
            }
        }
        return builder.toString();
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
