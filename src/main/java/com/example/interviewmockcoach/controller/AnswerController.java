package com.example.interviewmockcoach.controller;

import com.example.interviewmockcoach.dto.common.ApiResponse;
import com.example.interviewmockcoach.dto.request.EvaluateAnswerRequest;
import com.example.interviewmockcoach.dto.response.AnswerEvaluationResponse;
import com.example.interviewmockcoach.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview/answer")
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/evaluate")
    public ApiResponse<AnswerEvaluationResponse> evaluateAnswer(@Valid @RequestBody EvaluateAnswerRequest request) {
        return ApiResponse.success(answerService.evaluateAnswer(request));
    }
}
