package com.example.interviewmockcoach.service;

import com.example.interviewmockcoach.dto.request.GenerateQuestionsRequest;
import com.example.interviewmockcoach.dto.response.InterviewSessionResponse;
import com.example.interviewmockcoach.entity.InterviewQuestion;

import java.util.List;

public interface InterviewQuestionService {

    InterviewSessionResponse generateQuestions(GenerateQuestionsRequest request);

    List<InterviewQuestion> listQuestions(String sessionId);

    InterviewQuestion getQuestionOrThrow(String sessionId, String questionId);
}
