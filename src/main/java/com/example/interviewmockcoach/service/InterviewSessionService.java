package com.example.interviewmockcoach.service;

import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.response.InterviewSessionResponse;
import com.example.interviewmockcoach.entity.InterviewSession;
import com.example.interviewmockcoach.enums.AiMode;

public interface InterviewSessionService {

    InterviewSession createSession(CandidateProfileDto profile, int totalQuestions, AiMode aiMode);

    InterviewSession getSessionOrThrow(String sessionId);

    InterviewSession save(InterviewSession session);

    InterviewSessionResponse buildSessionResponse(String sessionId);

    void markProgress(String sessionId, int currentQuestionIndex);

    void markCompleted(String sessionId);
}
