package com.example.interviewmockcoach.service.impl;

import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.response.InterviewSessionResponse;
import com.example.interviewmockcoach.entity.InterviewQuestion;
import com.example.interviewmockcoach.entity.InterviewSession;
import com.example.interviewmockcoach.enums.AiMode;
import com.example.interviewmockcoach.enums.SessionStatus;
import com.example.interviewmockcoach.exception.ResourceNotFoundException;
import com.example.interviewmockcoach.repository.InterviewQuestionRepository;
import com.example.interviewmockcoach.repository.InterviewSessionRepository;
import com.example.interviewmockcoach.service.InterviewSessionService;
import com.example.interviewmockcoach.util.InterviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;

    @Override
    @Transactional
    public InterviewSession createSession(CandidateProfileDto profile, int totalQuestions, AiMode aiMode) {
        InterviewSession session = new InterviewSession();
        session.setCandidateProfile(InterviewMapper.toEntity(profile));
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCurrentQuestionIndex(totalQuestions > 0 ? 1 : 0);
        session.setTotalQuestions(totalQuestions);
        session.setAiMode(aiMode);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Override
    public InterviewSession getSessionOrThrow(String sessionId) {
        return sessionRepository.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("面试会话不存在: " + sessionId));
    }

    @Override
    public InterviewSession save(InterviewSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSessionResponse buildSessionResponse(String sessionId) {
        InterviewSession session = getSessionOrThrow(sessionId);
        List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
        return InterviewMapper.toSessionResponse(session, questions);
    }

    @Override
    @Transactional
    public void markProgress(String sessionId, int currentQuestionIndex) {
        InterviewSession session = getSessionOrThrow(sessionId);
        session.setCurrentQuestionIndex(Math.max(session.getCurrentQuestionIndex(), currentQuestionIndex));
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void markCompleted(String sessionId) {
        InterviewSession session = getSessionOrThrow(sessionId);
        session.setStatus(SessionStatus.COMPLETED);
        session.setCurrentQuestionIndex(session.getTotalQuestions());
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }
}
