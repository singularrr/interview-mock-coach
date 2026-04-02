package com.example.interviewmockcoach.repository;

import com.example.interviewmockcoach.entity.AnswerEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluation, String> {

    List<AnswerEvaluation> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
