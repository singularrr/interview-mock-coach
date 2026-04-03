package com.example.interviewmockcoach.service.impl;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.dto.request.EvaluateAnswerRequest;
import com.example.interviewmockcoach.dto.response.AnswerEvaluationResponse;
import com.example.interviewmockcoach.entity.AnswerEvaluation;
import com.example.interviewmockcoach.entity.InterviewQuestion;
import com.example.interviewmockcoach.entity.InterviewSession;
import com.example.interviewmockcoach.repository.AnswerEvaluationRepository;
import com.example.interviewmockcoach.repository.InterviewQuestionRepository;
import com.example.interviewmockcoach.service.AnswerService;
import com.example.interviewmockcoach.service.InterviewSessionService;
import com.example.interviewmockcoach.service.ai.InterviewAiService;
import com.example.interviewmockcoach.service.rag.KnowledgeRetrievalService;
import com.example.interviewmockcoach.util.InterviewMapper;
import com.example.interviewmockcoach.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final InterviewQuestionRepository questionRepository;
    private final AnswerEvaluationRepository evaluationRepository;
    private final InterviewSessionService sessionService;
    private final InterviewAiService interviewAiService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @Override
    @Transactional
    public AnswerEvaluationResponse evaluateAnswer(EvaluateAnswerRequest request) {
        InterviewSession session = sessionService.getSessionOrThrow(request.getSessionId());
        InterviewQuestion question = questionRepository.findBySessionIdAndQuestionId(request.getSessionId(), request.getQuestionId())
                .orElseThrow(() -> new com.example.interviewmockcoach.exception.ResourceNotFoundException("题目不存在: " + request.getQuestionId()));

        String profileContext = buildProfileContext(session);
        List<RetrievedContextDto> contexts = knowledgeRetrievalService.retrieve(
                buildEvaluationQuery(question, request.getAnswerText()),
                profileContext,
                4
        );

        AnswerEvaluationDto evaluationDto = interviewAiService.evaluateAnswer(
                InterviewMapper.toDto(session.getCandidateProfile()),
                InterviewMapper.toDto(question),
                request.getAnswerText(),
                contexts
        );

        AnswerEvaluation evaluation = new AnswerEvaluation();
        evaluation.setEvaluationId(evaluationDto.getEvaluationId());
        evaluation.setSessionId(request.getSessionId());
        evaluation.setQuestionId(question.getQuestionId());
        evaluation.setAnswerText(request.getAnswerText());
        evaluation.setScore(evaluationDto.getScore());
        evaluation.setStrengthsJson(JsonUtils.toJson(evaluationDto.getStrengths()));
        evaluation.setWeaknessesJson(JsonUtils.toJson(evaluationDto.getWeaknesses()));
        evaluation.setSuggestionsJson(JsonUtils.toJson(evaluationDto.getSuggestions()));
        evaluation.setFollowUpPointsJson(JsonUtils.toJson(evaluationDto.getFollowUpPoints()));
        evaluation.setShouldFollowUp(evaluationDto.isShouldFollowUp());
        evaluation.setCreatedAt(LocalDateTime.now());
        evaluationRepository.save(evaluation);

        sessionService.markProgress(request.getSessionId(), question.getOrderIndex());

        return new AnswerEvaluationResponse(
                request.getSessionId(),
                request.getQuestionId(),
                sessionService.getSessionOrThrow(request.getSessionId()).getCurrentQuestionIndex(),
                session.getTotalQuestions(),
                InterviewMapper.toDto(evaluation)
        );
    }

    private String buildProfileContext(InterviewSession session) {
        if (session == null || session.getCandidateProfile() == null) {
            return "";
        }
        return String.join(" ",
                safe(session.getCandidateProfile().getSchool()),
                safe(session.getCandidateProfile().getMajor()),
                safe(session.getCandidateProfile().getResearchDirection()),
                safe(session.getCandidateProfile().getResumePoints()));
    }

    private String buildEvaluationQuery(InterviewQuestion question, String answerText) {
        return safe(question.getContent()) + " " + safe(answerText);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
