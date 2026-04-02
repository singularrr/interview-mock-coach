package com.example.interviewmockcoach.service.impl;

import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.request.GenerateQuestionsRequest;
import com.example.interviewmockcoach.dto.response.InterviewSessionResponse;
import com.example.interviewmockcoach.entity.InterviewQuestion;
import com.example.interviewmockcoach.entity.InterviewSession;
import com.example.interviewmockcoach.enums.AiMode;
import com.example.interviewmockcoach.enums.SessionStatus;
import com.example.interviewmockcoach.exception.ResourceNotFoundException;
import com.example.interviewmockcoach.repository.InterviewQuestionRepository;
import com.example.interviewmockcoach.repository.InterviewSessionRepository;
import com.example.interviewmockcoach.service.InterviewQuestionService;
import com.example.interviewmockcoach.service.InterviewSessionService;
import com.example.interviewmockcoach.service.ai.InterviewAiService;
import com.example.interviewmockcoach.util.InterviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewSessionService sessionService;
    private final InterviewAiService interviewAiService;

    @Value("${ai.mode:mock}")
    private String aiMode;

    @Override
    @Transactional
    public InterviewSessionResponse generateQuestions(GenerateQuestionsRequest request) {
        CandidateProfileDto profile = request.getCandidateProfile();
        int questionCount = request.getQuestionCount() == null ? 8 : request.getQuestionCount();
        AiMode mode = parseMode(aiMode);

        InterviewSession session = sessionService.createSession(profile, questionCount, mode);
        List<InterviewQuestionDto> generatedQuestions = interviewAiService.generateQuestions(profile, questionCount);

        for (InterviewQuestionDto questionDto : generatedQuestions) {
            InterviewQuestion question = new InterviewQuestion();
            question.setQuestionId(questionDto.getQuestionId());
            question.setSessionId(session.getSessionId());
            question.setCategory(questionDto.getCategory());
            question.setDifficulty(questionDto.getDifficulty());
            question.setContent(questionDto.getContent());
            question.setShouldFollowUp(questionDto.isShouldFollowUp());
            question.setOrderIndex(questionDto.getOrderIndex());
            question.setCreatedAt(LocalDateTime.now());
            questionRepository.save(question);
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCurrentQuestionIndex(generatedQuestions.isEmpty() ? 0 : 1);
        session.setTotalQuestions(generatedQuestions.size());
        sessionRepository.save(session);

        return sessionService.buildSessionResponse(session.getSessionId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewQuestion> listQuestions(String sessionId) {
        return questionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewQuestion getQuestionOrThrow(String sessionId, String questionId) {
        return questionRepository.findBySessionIdAndQuestionId(sessionId, questionId).orElseThrow(() -> new ResourceNotFoundException("面试题不存在: " + questionId));
    }

    private AiMode parseMode(String mode) {
        if (mode == null) {
            return AiMode.MOCK;
        }
        return switch (mode.trim().toLowerCase(Locale.ROOT)) {
            case "openai" -> AiMode.OPENAI;
            default -> AiMode.MOCK;
        };
    }
}
