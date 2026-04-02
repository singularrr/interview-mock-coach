package com.example.interviewmockcoach.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerEvaluationDto {

    private String evaluationId;
    private String questionId;
    private int score;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
    private List<String> followUpPoints;
    private boolean shouldFollowUp;
    private String answerText;
}
