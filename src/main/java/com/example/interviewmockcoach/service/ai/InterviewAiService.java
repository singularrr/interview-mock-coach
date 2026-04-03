package com.example.interviewmockcoach.service.ai;

import com.example.interviewmockcoach.dto.common.AnswerEvaluationDto;
import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import com.example.interviewmockcoach.dto.common.InterviewQuestionDto;
import com.example.interviewmockcoach.dto.common.InterviewSummaryDto;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;

import java.util.List;

public interface InterviewAiService {

    default List<InterviewQuestionDto> generateQuestions(CandidateProfileDto profile, int questionCount) {
        return generateQuestions(profile, List.of(), questionCount);
    }

    List<InterviewQuestionDto> generateQuestions(CandidateProfileDto profile, List<RetrievedContextDto> contexts, int questionCount);

    default AnswerEvaluationDto evaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText) {
        return evaluateAnswer(profile, question, answerText, List.of());
    }

    AnswerEvaluationDto evaluateAnswer(CandidateProfileDto profile, InterviewQuestionDto question, String answerText, List<RetrievedContextDto> contexts);

    default InterviewSummaryDto generateSummary(CandidateProfileDto profile, List<InterviewQuestionDto> questions, List<AnswerEvaluationDto> evaluations) {
        return generateSummary(profile, questions, evaluations, List.of());
    }

    InterviewSummaryDto generateSummary(CandidateProfileDto profile,
                                        List<InterviewQuestionDto> questions,
                                        List<AnswerEvaluationDto> evaluations,
                                        List<RetrievedContextDto> contexts);
}
