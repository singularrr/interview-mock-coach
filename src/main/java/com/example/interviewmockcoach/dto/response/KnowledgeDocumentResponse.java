package com.example.interviewmockcoach.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentResponse {

    private String documentId;
    private String title;
    private String sourceType;
    private String sourceUrl;
    private int chunkCount;
    private LocalDateTime createdAt;
}
