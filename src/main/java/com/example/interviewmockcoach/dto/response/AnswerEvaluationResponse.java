package com.example.interviewmockcoach.dto.response;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerEvaluationResponse {

    private String sessionId;
    private String questionId;
    private int currentQuestionIndex;
    private int totalQuestions;
    private AnswerEvaluationDto evaluation;
}
