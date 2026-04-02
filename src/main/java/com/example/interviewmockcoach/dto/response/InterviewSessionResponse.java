package com.example.interviewmockcoach.dto.response;

import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionResponse {

    private String sessionId;
    private SessionStatus status;
    private int currentQuestionIndex;
    private int totalQuestions;
    private CandidateProfileDto candidateProfile;
    private InterviewQuestionDto currentQuestion;
    private List<InterviewQuestionDto> questions;
}
