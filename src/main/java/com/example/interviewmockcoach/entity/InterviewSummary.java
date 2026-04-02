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
@Table(name = "interview_summaries")
public class InterviewSummary {

    @Id
    @Column(length = 64)
    private String summaryId;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false)
    private int overallScore;

    @Lob
    @Column(nullable = false)
    private String weakAreasJson;

    @Lob
    @Column(nullable = false)
    private String frequentQuestionCategoriesJson;

    @Lob
    @Column(nullable = false)
    private String summaryAdvice;

    @Lob
    @Column(nullable = false)
    private String markdownContent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (summaryId == null || summaryId.isBlank()) {
            summaryId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
