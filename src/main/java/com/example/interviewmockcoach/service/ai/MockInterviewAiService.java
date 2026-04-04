package com.example.interviewmockcoach.service.ai;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.CategoryCountDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.enums.DifficultyLevel;
import com.example.interviewmockcoach.enums.QuestionCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.mode", havingValue = "mock", matchIfMissing = true)
public class MockInterviewAiService implements InterviewAiService {

    @Override
    public List<InterviewQuestionDto> generateQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
        return InterviewQuestionComposer.composeQuestions(profile, contexts, questionCount);
    }

    @Override
    public AnswerEvaluationDto evaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts) {
        String answer = safe(answerText);
        String lower = answer.toLowerCase(Locale.ROOT);
        int score = 60;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> followUpPoints = new ArrayList<>();

        if (answer.length() >= 40) {
            score += 8;
            strengths.add("回答内容比较完整");
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

        String school = safe(profile == null ? null : profile.getSchool());
        String major = safe(profile == null ? null : profile.getMajor());
        String direction = safe(profile == null ? null : profile.getResearchDirection());
        if (containsAny(lower, school.toLowerCase(Locale.ROOT), major.toLowerCase(Locale.ROOT), direction.toLowerCase(Locale.ROOT))) {
            score += 6;
            strengths.add("和报考方向关联较强");
        } else {
            suggestions.add("可以更明确地结合报考学校、专业和研究方向来回答");
        }

        if (contexts != null && !contexts.isEmpty() && containsAny(lower, "资料", "文档", "培养方案", "导师", "研究方向", "官网")) {
            score += 6;
            strengths.add("结合了检索资料进行回答");
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

        if (contexts != null && !contexts.isEmpty()) {
            String cue = pickContextCue(contexts);
            if (!cue.isBlank()) {
                followUpPoints.add("可以结合检索到的资料中的 " + cue + " 继续展开");
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

    @Override
    public InterviewSummaryDto generateSummary(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations, List<RetrievedContextDto> contexts) {
        int overallScore = (evaluations == null || evaluations.isEmpty())
                ? 0
                : (int) Math.round(evaluations.stream().mapToInt(AnswerEvaluationDto::getScore).average().orElse(0));

        Map<String, Long> weakCounts = new HashMap<>();
        Map<String, Long> categoryCounts = new HashMap<>();
        if (questions != null) {
            categoryCounts = questions.stream().collect(java.util.stream.Collectors.groupingBy(
                    q -> q.getCategory().name(),
                    java.util.LinkedHashMap::new,
                    java.util.stream.Collectors.counting()));
        }
        if (questions != null && evaluations != null) {
            Map<String, QuestionCategory> questionCategoryMap = questions.stream().collect(java.util.stream.Collectors.toMap(
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

    private String contextSuffix(String cue) {
        if (cue.isBlank()) {
            return "";
        }
        return " 你也可以结合资料中的 " + cue + " 来组织回答。";
    }

    private String extraSuffix(String cue, int index) {
        if (cue.isBlank()) {
            return index % 2 == 0 ? "（可追问）" : "（可补充）";
        }
        return index % 2 == 0 ? " 请结合 " + cue + " 进一步展开。" : " 如果老师追问，可围绕 " + cue + " 回答。";
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
}