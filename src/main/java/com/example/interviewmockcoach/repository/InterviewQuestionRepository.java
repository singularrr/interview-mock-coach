package com.example.interviewmockcoach.repository;

import com.example.interviewmockcoach.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, String> {

    List<InterviewQuestion> findBySessionIdOrderByOrderIndexAsc(String sessionId);

    Optional<InterviewQuestion> findBySessionIdAndQuestionId(String sessionId, String questionId);
}
