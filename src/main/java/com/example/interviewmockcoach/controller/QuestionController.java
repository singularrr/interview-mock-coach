package com.example.interviewmockcoach.controller;

import com.example.interviewmockcoach.dto.common.ApiResponse;
import com.example.interviewmockcoach.dto.request.GenerateQuestionsRequest;
import com.example.interviewmockcoach.dto.response.InterviewSessionResponse;
import com.example.interviewmockcoach.service.InterviewQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview/questions")
public class QuestionController {

    private final InterviewQuestionService interviewQuestionService;

    @PostMapping("/generate")
    public ApiResponse<InterviewSessionResponse> generateQuestions(@Valid @RequestBody GenerateQuestionsRequest request) {
        return ApiResponse.success(interviewQuestionService.generateQuestions(request));
    }
}
