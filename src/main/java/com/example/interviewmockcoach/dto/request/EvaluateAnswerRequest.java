package com.example.interviewmockcoach.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateAnswerRequest {

    @NotBlank(message = "sessionId不能为空")
    private String sessionId;

    @NotBlank(message = "questionId不能为空")
    private String questionId;

    @NotBlank(message = "回答内容不能为空")
    private String answerText;
}
