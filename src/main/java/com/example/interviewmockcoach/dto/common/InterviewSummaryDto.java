package com.example.interviewmockcoach.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSummaryDto {

    private String summaryId;
    private String sessionId;
    private int overallScore;
    private List<CategoryCountDto> weakAreas;
    private List<CategoryCountDto> frequentQuestionCategories;
    private String summaryAdvice;
    private String markdownContent;
}
