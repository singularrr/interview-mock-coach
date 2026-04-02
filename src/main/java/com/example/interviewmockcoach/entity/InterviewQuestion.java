package com.example.interviewmockcoach.entity;

import com.example.interviewmockcoach.enums.DifficultyLevel;
import com.example.interviewmockcoach.enums.QuestionCategory;
import jakarta.persistence.Column;
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
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @Column(length = 64)
    private String questionId;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private QuestionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DifficultyLevel difficulty;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private boolean shouldFollowUp;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (questionId == null || questionId.isBlank()) {
            questionId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
