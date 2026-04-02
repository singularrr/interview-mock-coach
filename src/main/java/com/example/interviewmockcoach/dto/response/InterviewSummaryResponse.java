package com.example.interviewmockcoach.dto.response;

import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSummaryResponse {

    private String sessionId;
    private InterviewSummaryDto summary;
}
