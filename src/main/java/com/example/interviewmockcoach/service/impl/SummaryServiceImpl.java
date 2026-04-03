package com.example.interviewmockcoach.service.impl;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.dto.request.GenerateSummaryRequest;
import com.example.interviewmockcoach.dto.response.InterviewSummaryResponse;
import com.example.interviewmockcoach.entity.AnswerEvaluation;
import com.example.interviewmockcoach.entity.InterviewQuestion;
import com.example.interviewmockcoach.entity.InterviewSession;
import com.example.interviewmockcoach.entity.InterviewSummary;
import com.example.interviewmockcoach.repository.AnswerEvaluationRepository;
import com.example.interviewmockcoach.repository.InterviewQuestionRepository;
import com.example.interviewmockcoach.repository.InterviewSummaryRepository;
import com.example.interviewmockcoach.service.InterviewSessionService;
import com.example.interviewmockcoach.service.SummaryService;
import com.example.interviewmockcoach.service.ai.InterviewAiService;
import com.example.interviewmockcoach.service.rag.KnowledgeRetrievalService;
import com.example.interviewmockcoach.util.InterviewMapper;
import com.example.interviewmockcoach.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final InterviewSessionService sessionService;
    private final InterviewQuestionRepository questionRepository;
    private final AnswerEvaluationRepository evaluationRepository;
    private final InterviewSummaryRepository summaryRepository;
    private final InterviewAiService interviewAiService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @Override
    @Transactional
    public InterviewSummaryResponse generateSummary(GenerateSummaryRequest request) {
        InterviewSession session = sessionService.getSessionOrThrow(request.getSessionId());
        List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByOrderIndexAsc(request.getSessionId());
        List<AnswerEvaluation> evaluations = evaluationRepository.findBySessionIdOrderByCreatedAtAsc(request.getSessionId());

        List<InterviewQuestionDto> questionDtos = questions.stream().map(InterviewMapper::toDto).toList();
        List<AnswerEvaluationDto> evaluationDtos = evaluations.stream().map(InterviewMapper::toDto).toList();
        List<RetrievedContextDto> contexts = knowledgeRetrievalService.retrieve(
                buildSummaryQuery(session, questions),
                buildProfileContext(session),
                6
        );

        InterviewSummaryDto summaryDto = interviewAiService.generateSummary(
                InterviewMapper.toDto(session.getCandidateProfile()),
                questionDtos,
                evaluationDtos,
                contexts
        );

        InterviewSummary summary = new InterviewSummary();
        summary.setSummaryId(summaryDto.getSummaryId());
        summary.setSessionId(request.getSessionId());
        summary.setOverallScore(summaryDto.getOverallScore());
        summary.setWeakAreasJson(JsonUtils.toJson(summaryDto.getWeakAreas()));
        summary.setFrequentQuestionCategoriesJson(JsonUtils.toJson(summaryDto.getFrequentQuestionCategories()));
        summary.setSummaryAdvice(summaryDto.getSummaryAdvice());
        summary.setMarkdownContent(summaryDto.getMarkdownContent());
        summary.setCreatedAt(LocalDateTime.now());

        Optional<InterviewSummary> existing = summaryRepository.findBySessionId(request.getSessionId());
        existing.ifPresent(old -> summary.setSummaryId(old.getSummaryId()));
        summaryRepository.save(summary);

        sessionService.markCompleted(request.getSessionId());

        return new InterviewSummaryResponse(request.getSessionId(), InterviewMapper.toDto(summary));
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

    private String buildSummaryQuery(InterviewSession session, List<InterviewQuestion> questions) {
        StringBuilder builder = new StringBuilder("复试总结 改进建议 ");
        if (session != null && session.getCandidateProfile() != null) {
            builder.append(safe(session.getCandidateProfile().getSchool())).append(' ')
                    .append(safe(session.getCandidateProfile().getMajor())).append(' ')
                    .append(safe(session.getCandidateProfile().getResearchDirection())).append(' ');
        }
        if (questions != null) {
            for (InterviewQuestion question : questions) {
                if (question != null) {
                    builder.append(safe(question.getContent())).append(' ');
                }
            }
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
