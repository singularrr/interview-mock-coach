package com.example.interviewmockcoach.util;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.CategoryCountDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;
import com.example.interviewmockcoach.dto.response.InterviewSessionResponse;
import com.example.interviewmockcoach.entity.AnswerEvaluation;
import com.example.interviewmockcoach.entity.CandidateProfile;
import com.example.interviewmockcoach.entity.InterviewQuestion;
import com.example.interviewmockcoach.entity.InterviewSession;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collections;
import java.util.List;

public final class InterviewMapper {

    private InterviewMapper() {
    }

    public static CandidateProfile toEntity(CandidateProfileDto dto) {
        if (dto == null) {
            return null;
        }
        return new CandidateProfile(dto.getSchool(), dto.getMajor(), dto.getResearchDirection(), dto.getResumePoints());
    }

    public static CandidateProfileDto toDto(CandidateProfile entity) {
        if (entity == null) {
            return null;
        }
        return new CandidateProfileDto(entity.getSchool(), entity.getMajor(), entity.getResearchDirection(), entity.getResumePoints());
    }

    public static InterviewQuestionDto toDto(InterviewQuestion entity) {
        if (entity == null) {
            return null;
        }
        return new InterviewQuestionDto(entity.getQuestionId(), entity.getCategory(), entity.getDifficulty(), entity.getContent(), entity.isShouldFollowUp(), entity.getOrderIndex());
    }

    public static AnswerEvaluationDto toDto(AnswerEvaluation entity) {
        if (entity == null) {
            return null;
        }
        return new AnswerEvaluationDto(
                entity.getEvaluationId(),
                entity.getQuestionId(),
                entity.getScore(),
                JsonUtils.fromJson(entity.getStrengthsJson(), new TypeReference<List<String>>() {
                }),
                JsonUtils.fromJson(entity.getWeaknessesJson(), new TypeReference<List<String>>() {
                }),
                JsonUtils.fromJson(entity.getSuggestionsJson(), new TypeReference<List<String>>() {
                }),
                JsonUtils.fromJson(entity.getFollowUpPointsJson(), new TypeReference<List<String>>() {
                }),
                entity.isShouldFollowUp(),
                entity.getAnswerText()
        );
    }

    public static InterviewSummaryDto toDto(com.example.interviewmockcoach.entity.InterviewSummary entity) {
        if (entity == null) {
            return null;
        }
        List<CategoryCountDto> weakAreas = JsonUtils.fromJson(entity.getWeakAreasJson(), new TypeReference<List<CategoryCountDto>>() {
        });
        List<CategoryCountDto> frequentCategories = JsonUtils.fromJson(entity.getFrequentQuestionCategoriesJson(), new TypeReference<List<CategoryCountDto>>() {
        });
        return new InterviewSummaryDto(
                entity.getSummaryId(),
                entity.getSessionId(),
                entity.getOverallScore(),
                weakAreas == null ? Collections.emptyList() : weakAreas,
                frequentCategories == null ? Collections.emptyList() : frequentCategories,
                entity.getSummaryAdvice(),
                entity.getMarkdownContent()
        );
    }

    public static InterviewSessionResponse toSessionResponse(InterviewSession session, List<InterviewQuestion> questions) {
        List<InterviewQuestionDto> questionDtos = questions == null ? Collections.emptyList() : questions.stream().map(InterviewMapper::toDto).toList();
        InterviewQuestionDto currentQuestion = null;
        if (session != null && session.getCurrentQuestionIndex() > 0 && session.getCurrentQuestionIndex() <= questionDtos.size()) {
            currentQuestion = questionDtos.get(session.getCurrentQuestionIndex() - 1);
        }
        return new InterviewSessionResponse(
                session == null ? null : session.getSessionId(),
                session == null ? null : session.getStatus(),
                session == null ? 0 : session.getCurrentQuestionIndex(),
                session == null ? 0 : session.getTotalQuestions(),
                toDto(session == null ? null : session.getCandidateProfile()),
                currentQuestion,
                questionDtos
        );
    }
}
