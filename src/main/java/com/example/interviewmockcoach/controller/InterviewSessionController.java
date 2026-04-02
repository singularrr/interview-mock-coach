package com.example.interviewmockcoach.controller;

import com.example.interviewmockcoach.dto.common.ApiResponse;
import com.example.interviewmockcoach.dto.response.InterviewSessionResponse;
import com.example.interviewmockcoach.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview/sessions")
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;

    @GetMapping("/{sessionId}")
    public ApiResponse<InterviewSessionResponse> getSession(@PathVariable String sessionId) {
        return ApiResponse.success(interviewSessionService.buildSessionResponse(sessionId));
    }
}
