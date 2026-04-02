package com.example.interviewmockcoach.service;

import com.example.interviewmockcoach.dto.request.EvaluateAnswerRequest;
import com.example.interviewmockcoach.dto.response.AnswerEvaluationResponse;

public interface AnswerService {

    AnswerEvaluationResponse evaluateAnswer(EvaluateAnswerRequest request);
}
