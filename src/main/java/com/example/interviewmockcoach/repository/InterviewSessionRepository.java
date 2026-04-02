package com.example.interviewmockcoach.repository;

import com.example.interviewmockcoach.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, String> {
}
