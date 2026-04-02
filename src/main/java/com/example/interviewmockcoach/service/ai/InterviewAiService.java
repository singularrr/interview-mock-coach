package com.example.interviewmockcoach.service.ai;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;

import java.util.List;

public interface InterviewAiService {

    List<InterviewQuestionDto> generateQuestions(CandidateProfileDto profile, int questionCount);

    AnswerEvaluationDto evaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText);

    InterviewSummaryDto generateSummary(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations);
}
