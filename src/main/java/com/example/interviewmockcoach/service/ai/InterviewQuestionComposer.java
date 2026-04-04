package com.example.interviewmockcoach.service.ai;

import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.enums.DifficultyLevel;
import com.example.interviewmockcoach.enums.QuestionCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class InterviewQuestionComposer {

    private InterviewQuestionComposer() {
    }

    static List<InterviewQuestionDto> composeQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount) {
        String school = safe(profile == null ? null : profile.getSchool(), "目标院校");
        String major = safe(profile == null ? null : profile.getMajor(), "目标专业");
        String direction = safe(profile == null ? null : profile.getResearchDirection(), "研究方向");
        String resume = safe(profile == null ? null : profile.getResumePoints(), "你的经历");
        String cue = pickContextCue(contexts);

        List<String> selfIntroPool = new ArrayList<>(List.of(
                "先做一个简单的自我介绍吧，重点讲讲你为什么想报考" + school + "的" + major + "。",
                "如果现在正式开始面试，你会怎么用一分钟介绍自己，并让老师记住你？",
                "请你围绕本科经历、能力特点和报考原因，做一个自然一点的自我介绍。"
        ));
        List<String> professionalPool = new ArrayList<>(List.of(
                "你本科阶段学过哪些和" + major + "最相关的核心课程？挑一个你最有把握的知识点展开讲讲。",
                "如果老师让你讲一个" + major + "中的基础概念，你最想讲哪一个？为什么？",
                "结合你的学习经历，说一个你理解比较深入的专业基础知识，并说明它的应用场景。"
        ));
        List<String> projectPool = new ArrayList<>(List.of(
                "聊一个你最能体现自己能力的项目吧，你在里面具体负责什么？",
                "如果老师对你的项目经历最感兴趣，你会先介绍哪一个项目？为什么？",
                "请挑一个项目，讲讲背景、你的职责，以及最后做出了什么结果。"
        ));
        List<String> motivePool = new ArrayList<>(List.of(
                "为什么最后把" + school + "的" + major + "作为重点报考目标？",
                "你在选择学校和专业时最看重什么？为什么会落到" + school + "和" + major + "上？",
                "如果老师问你报考动机，你会怎么说明自己和这个专业方向是匹配的？"
        ));
        List<String> researchPool = new ArrayList<>(List.of(
                "你现在对" + direction + "这个方向的理解到了什么程度？",
                "如果后续读研真的做" + direction + "，你最想先解决什么问题？",
                "请结合你的经历谈谈，你为什么会对" + direction + "这个方向感兴趣。"
        ));
        List<String> deeperPool = new ArrayList<>(List.of(
                "刚才如果老师继续深挖你的专业基础，你准备怎么把原理讲得更清楚？",
                "如果老师追问你提到的那个知识点，你会从哪些层次继续展开？",
                "碰到专业追问时，你通常会怎么把概念、原理和应用串起来回答？"
        ));
        List<String> optimizationPool = new ArrayList<>(List.of(
                "如果把你刚才讲的项目再重做一遍，你最想优化哪一部分？",
                "回头看你的项目经历，你觉得最值得复盘和改进的点是什么？",
                "如果老师问你项目里有没有做得不够好的地方，你会怎么回答？"
        ));
        List<String> closingPool = new ArrayList<>(List.of(
                "最后请你用三十秒做个收束，概括一下你的优势和读研目标。",
                "如果这是最后一个问题，你会怎么简短总结自己，让老师留下印象？",
                "请用一句主线清晰的话，总结你为什么适合继续深造。"
        ));

        maybeBlendContext(selfIntroPool, cue);
        maybeBlendContext(professionalPool, cue);
        maybeBlendContext(projectPool, cue);
        maybeBlendContext(researchPool, cue);

        shuffle(selfIntroPool, professionalPool, projectPool, motivePool, researchPool, deeperPool, optimizationPool, closingPool);

        List<QuestionSeed> seeds = List.of(
                new QuestionSeed(QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, selfIntroPool.get(0), true),
                new QuestionSeed(QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.MEDIUM, professionalPool.get(0), true),
                new QuestionSeed(QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.MEDIUM, projectPool.get(0), true),
                new QuestionSeed(QuestionCategory.ADMISSION_MOTIVE, DifficultyLevel.EASY, motivePool.get(0), true),
                new QuestionSeed(QuestionCategory.RESEARCH_DIRECTION, DifficultyLevel.MEDIUM, researchPool.get(0), true),
                new QuestionSeed(QuestionCategory.PROFESSIONAL_BASE, DifficultyLevel.HARD, deeperPool.get(0), true),
                new QuestionSeed(QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.HARD, optimizationPool.get(0), true),
                new QuestionSeed(QuestionCategory.SELF_INTRO, DifficultyLevel.EASY, closingPool.get(0), false)
        );

        List<QuestionSeed> orderedSeeds = new ArrayList<>(seeds);
        if (questionCount > seeds.size()) {
            List<QuestionSeed> expansion = List.of(
                    new QuestionSeed(QuestionCategory.PROJECT_EXPERIENCE, DifficultyLevel.MEDIUM, "你在" + resume + "里最希望老师注意到的是哪段经历？", true),
                    new QuestionSeed(QuestionCategory.RESEARCH_DIRECTION, DifficultyLevel.MEDIUM, "如果现在就让你开始准备研究计划，你会先补哪方面能力？", true),
                    new QuestionSeed(QuestionCategory.ADMISSION_MOTIVE, DifficultyLevel.EASY, "除了学校和专业本身，还有什么因素坚定了你继续深造的决定？", false)
            );
            while (orderedSeeds.size() < questionCount) {
                orderedSeeds.add(expansion.get(orderedSeeds.size() % expansion.size()));
            }
        }

        int limit = Math.min(questionCount, orderedSeeds.size());
        List<InterviewQuestionDto> questions = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            QuestionSeed seed = orderedSeeds.get(i);
            questions.add(new InterviewQuestionDto(
                    UUID.randomUUID().toString(),
                    seed.category(),
                    seed.difficulty(),
                    seed.content(),
                    seed.shouldFollowUp(),
                    i + 1
            ));
        }
        return questions;
    }

    private static void maybeBlendContext(List<String> pool, String cue) {
        if (cue.isBlank()) {
            return;
        }
        int index = ThreadLocalRandom.current().nextInt(pool.size());
        pool.set(index, pool.get(index) + " 你也可以顺带结合资料里的“" + cue + "”来回答。");
    }

    @SafeVarargs
    private static void shuffle(List<String>... pools) {
        for (List<String> pool : pools) {
            Collections.shuffle(pool);
        }
    }

    private static String pickContextCue(List<RetrievedContextDto> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "";
        }
        for (RetrievedContextDto context : contexts) {
            if (context == null) {
                continue;
            }
            String title = safe(context.getDocumentTitle(), "");
            if (!title.isBlank()) {
                return title;
            }
            String content = safe(context.getContent(), "");
            if (!content.isBlank()) {
                return content.length() <= 18 ? content : content.substring(0, 18);
            }
        }
        return "";
    }

    private static String safe(String value, String defaultValue) {
        if (value == null || value.trim().isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private record QuestionSeed(QuestionCategory category, DifficultyLevel difficulty, String content, boolean shouldFollowUp) {
    }
}