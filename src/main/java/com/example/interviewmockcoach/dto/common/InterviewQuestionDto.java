package com.example.interviewmockcoach.dto.common;

import com.example.interviewmockcoach.enums.DifficultyLevel;
import com.example.interviewmockcoach.enums.QuestionCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionDto {

    private String questionId;
    private QuestionCategory category;
    private DifficultyLevel difficulty;
    private String content;
    private boolean shouldFollowUp;
    private int orderIndex;
}
