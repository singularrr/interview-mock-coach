package com.example.interviewmockcoach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Table(name = "answer_evaluations")
public class AnswerEvaluation {

    @Id
    @Column(length = 64)
    private String evaluationId;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 64)
    private String questionId;

    @Lob
    @Column(nullable = false)
    private String answerText;

    @Column(nullable = false)
    private int score;

    @Lob
    @Column(nullable = false)
    private String strengthsJson;

    @Lob
    @Column(nullable = false)
    private String weaknessesJson;

    @Lob
    @Column(nullable = false)
    private String suggestionsJson;

    @Lob
    @Column(nullable = false)
    private String followUpPointsJson;

    @Column(nullable = false)
    private boolean shouldFollowUp;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (evaluationId == null || evaluationId.isBlank()) {
            evaluationId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
