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
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    @Column(length = 64)
    private String documentId;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(nullable = false, length = 64)
    private String sourceType;

    @Column(length = 1024)
    private String sourceUrl;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int chunkCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (documentId == null || documentId.isBlank()) {
            documentId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}