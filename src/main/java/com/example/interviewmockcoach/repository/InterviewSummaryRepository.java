package com.example.interviewmockcoach.repository;

import com.example.interviewmockcoach.entity.InterviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewSummaryRepository extends JpaRepository<InterviewSummary, String> {

    Optional<InterviewSummary> findBySessionId(String sessionId);
}
