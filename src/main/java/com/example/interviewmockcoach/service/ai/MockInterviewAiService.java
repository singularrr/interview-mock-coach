package com.example.interviewmockcoach.service.ai;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.CategoryCountDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;
import com.example.interviewmockcoach.enums.DifficultyLevel;
import com.example.interviewmockcoach.enums.QuestionCategory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    public List<InterviewQuestionDto> generateQuestions(CandidateProfileDto profile, int questionCount) {
        List<InterviewQuestionDto> templates = baseQuestions(profile);
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
                    template.getContent() + "（补充追问版）",
                    template.isShouldFollowUp(),
                    questions.size() + 1
            ));
            index++;
        }
        return questions;
    }

    @Override
    public AnswerEvaluationDto evaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText) {
        String normalized = answerText == null ? "" : answerText.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        int score = 60;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> followUpPoints = new ArrayList<>();

        if (normalized.length() >= 40) {
            score += 8;
            strengths.add("回答长度较为充分");
        } else {
            score -= 5;
            weaknesses.add("回答偏短，信息量不足");
            suggestions.add("建议补充背景、过程和结果");
        }

        if (normalized.length() >= 120) {
            score += 6;
            strengths.add("表达内容较完整");
        }

        if (containsAny(lower, "首先", "其次", "最后", "一方面", "另一方面")) {
            score += 8;
            strengths.add("回答结构清晰");
        } else {
            weaknesses.add("结构感不够强");
            suggestions.add("建议用总分总或分点方式组织回答");
        }

        if (containsAny(lower, "项目", "实验", "论文", "实习", "比赛", "成果")) {
            score += 8;
            strengths.add("包含了具体经历或案例");
        } else {
            weaknesses.add("缺少具体案例支撑");
            suggestions.add("尽量用一个项目或经历来说明你的观点");
        }

        if (containsAny(lower, safeLower(profile.getMajor()), safeLower(profile.getResearchDirection()), safeLower(profile.getSchool()))) {
            score += 6;
            strengths.add("回答和个人背景匹配");
        } else {
            weaknesses.add("与报考方向的关联还不够明显");
            suggestions.add("补充与报考专业和研究方向相关的表述");
        }

        switch (question.getCategory()) {
            case SELF_INTRO -> {
                if (containsAny(lower, "我", "本科", "项目", "实习")) {
                    score += 4;
                    strengths.add("自我介绍要素较完整");
                } else {
                    followUpPoints.add("为什么选择这个学校和专业");
                }
            }
            case PROJECT_EXPERIENCE -> {
                if (containsAny(lower, "负责", "设计", "实现", "优化", "结果")) {
                    score += 5;
                    strengths.add("项目描述体现了个人贡献");
                } else {
                    followUpPoints.add("你在这个项目里具体负责了什么");
                }
            }
            case RESEARCH_DIRECTION -> {
                if (containsAny(lower, "方法", "模型", "实验", "数据", "论文")) {
                    score += 6;
                    strengths.add("研究方向理解较到位");
                } else {
                    followUpPoints.add("你对这个方向目前有哪些具体认识");
                }
            }
            case ADMISSION_MOTIVE -> {
                if (containsAny(lower, "老师", "平台", "方向", "资源", "契合")) {
                    score += 5;
                    strengths.add("动机表达较真诚");
                } else {
                    followUpPoints.add("为什么是这个学校而不是其他学校");
                }
            }
            case PROFESSIONAL_BASE -> {
                if (containsAny(lower, "原理", "公式", "概念", "定义", "机制")) {
                    score += 5;
                    strengths.add("基础概念表达比较准确");
                } else {
                    followUpPoints.add("这个基础概念你能再展开解释一下吗");
                }
            }
            case FOLLOW_UP -> score += 4;
        }

        if (containsAny(lower, "不知道", "不太清楚", "记不太清")) {
            score -= 15;
            weaknesses.add("回答存在明显不确定性");
            suggestions.add("对不熟悉的问题也要尽量先给出思路再回答");
            followUpPoints.add("如果现场追问，你会从哪里开始思考");
        }

        score = Math.max(0, Math.min(100, score));
        if (strengths.isEmpty()) {
            strengths.add("态度整体比较完整");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("暂未发现明显短板");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("继续保持当前回答风格，并增加案例细节");
        }
        if (followUpPoints.isEmpty() && (question.isShouldFollowUp() || score < 75)) {
            followUpPoints.add("请再补充一个具体例子来支撑你的回答");
        }

        return new AnswerEvaluationDto(UUID.randomUUID().toString(), question.getQuestionId(), score, strengths, weaknesses, suggestions, followUpPoints, question.isShouldFollowUp() || score < 75, answerText);
    }

    @Override
    public InterviewSummaryDto generateSummary(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations) {
        int overallScore = evaluations == null || evaluations.isEmpty() ? 0 : (int) Math.round(evaluations.stream().mapToInt(AnswerEvaluationDto::getScore).average().orElse(0));

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

        String advice = overallScore >= 85 ? "整体表现较稳定，建议继续强化项目细节和研究规划表达，保持自信和结构化回答。" : "建议继续强化项目经历、专业基础与研究方向表达，提前准备高频追问。";

        StringBuilder markdown = new StringBuilder();
        markdown.append("# 复试模拟总结\n\n");
        markdown.append("## 基本信息\n");
        markdown.append("- 学校：").append(safe(profile.getSchool())).append("\n");
        markdown.append("- 专业：").append(safe(profile.getMajor())).append("\n");
        markdown.append("- 研究方向：").append(safe(profile.getResearchDirection())).append("\n\n");
        markdown.append("## 总体评分\n");
        markdown.append("- 综合得分：").append(overallScore).append("\n\n");

        return new InterviewSummaryDto(UUID.randomUUID().toString(), null, overallScore, weakAreas, frequentCategories, advice, markdown.toString());
    }

    private List<InterviewQuestionDto> baseQuestions(CandidateProfileDto profile) {
        String school = safe(profile.getSchool());
        String major = safe(profile.getMajor());
        String direction = safe(profile.getResearchDirection());

        return List.of(
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, "请你做一个简短的自我介绍，并重点说明你为什么适合报考 " + school + " 的 " + major + " 方向。", true, 1),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.MEDIUM, "请结合你的本科基础，谈谈 " + major + " 中你最熟悉的一个核心概念。", true, 2),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.MEDIUM, "请介绍一个你最有代表性的项目，并说明你在项目中的具体贡献。", true, 3),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.ADMISSION_MOTIVE, DifficultyLevel.EASY, "为什么选择报考 " + school + "，以及为什么选择这个专业？", true, 4),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.RESEARCH_DIRECTION, DifficultyLevel.MEDIUM, "请谈谈你对 " + direction + " 的理解，以及你未来可能关注的研究问题。", true, 5),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.HARD, "如果老师追问你这个专业中的一个关键原理，你会如何用自己的话解释？", true, 6),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.HARD, "如果把你最熟悉的项目重新做一遍，你最想优化哪一部分，为什么？", true, 7),
                new InterviewQuestionDto(UUID.randomUUID().toString(), QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, "如果让你用 30 秒总结自己，你会怎么说？", false, 8)
        );
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
