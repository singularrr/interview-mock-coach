package com.example.interviewmockcoach.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSummaryRequest {

    @NotBlank(message = "sessionId不能为空")
    private String sessionId;
}
