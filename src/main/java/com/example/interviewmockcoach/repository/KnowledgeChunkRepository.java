package com.example.interviewmockcoach.repository;

import com.example.interviewmockcoach.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, String> {

    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId);
}