package com.example.interviewmockcoach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = false;
    private int topK = 4;
    private double similarityThreshold = 0.75;
    private int chunkSize = 500;
    private int chunkOverlap = 80;
    private final VectorStore vectorStore = new VectorStore();

    @Data
    public static class VectorStore {
        private String collectionName = "interview_knowledge_chunks";
    }
}
