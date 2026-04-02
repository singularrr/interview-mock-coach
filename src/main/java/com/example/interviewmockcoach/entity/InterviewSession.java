package com.example.interviewmockcoach.entity;

import com.example.interviewmockcoach.enums.AiMode;
import com.example.interviewmockcoach.enums.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    @Column(length = 64)
    private String sessionId;

    @Embedded
    private CandidateProfile candidateProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SessionStatus status;

    @Column(nullable = false)
    private int currentQuestionIndex;

    @Column(nullable = false)
    private int totalQuestions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiMode aiMode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
}
