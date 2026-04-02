package com.example.interviewmockcoach.controller;

import com.example.interviewmockcoach.dto.common.ApiResponse;
import com.example.interviewmockcoach.dto.request.GenerateSummaryRequest;
import com.example.interviewmockcoach.dto.response.InterviewSummaryResponse;
import com.example.interviewmockcoach.service.SummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview/summary")
public class SummaryController {

    private final SummaryService summaryService;

    @PostMapping("/generate")
    public ApiResponse<InterviewSummaryResponse> generateSummary(@Valid @RequestBody GenerateSummaryRequest request) {
        return ApiResponse.success(summaryService.generateSummary(request));
    }
}
