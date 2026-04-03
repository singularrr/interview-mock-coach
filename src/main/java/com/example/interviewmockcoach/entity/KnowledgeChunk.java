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
@Table(name = "knowledge_chunks")
public class KnowledgeChunk {

    @Id
    @Column(length = 64)
    private String chunkId;

    @Column(nullable = false, length = 64)
    private String documentId;

    @Column(nullable = false, length = 256)
    private String documentTitle;

    @Column(nullable = false, length = 64)
    private String sourceType;

    @Column(nullable = false)
    private int chunkIndex;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (chunkId == null || chunkId.isBlank()) {
            chunkId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}