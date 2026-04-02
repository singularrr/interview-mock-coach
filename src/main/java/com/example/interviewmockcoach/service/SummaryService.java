package com.example.interviewmockcoach.service;

import com.example.interviewmockcoach.dto.request.GenerateSummaryRequest;
import com.example.interviewmockcoach.dto.response.InterviewSummaryResponse;

public interface SummaryService {

    InterviewSummaryResponse generateSummary(GenerateSummaryRequest request);
}
