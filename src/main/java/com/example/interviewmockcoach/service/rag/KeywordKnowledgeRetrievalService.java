package com.example.interviewmockcoach.service.rag;

import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.dto.request.IngestKnowledgeDocumentRequest;
import com.example.interviewmockcoach.dto.response.KnowledgeDocumentResponse;
import com.example.interviewmockcoach.entity.KnowledgeChunk;
import com.example.interviewmockcoach.entity.KnowledgeDocument;
import com.example.interviewmockcoach.repository.KnowledgeChunkRepository;
import com.example.interviewmockcoach.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "false", matchIfMissing = true)
public class KeywordKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;

    @Override
    @Transactional
    public KnowledgeDocumentResponse ingest(IngestKnowledgeDocumentRequest request) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(request.getTitle());
        document.setSourceType(request.getSourceType() == null || request.getSourceType().isBlank() ? "GENERAL" : request.getSourceType().trim().toUpperCase());
        document.setSourceUrl(request.getSourceUrl());
        document.setContent(request.getContent());
        document.setChunkCount(1);
        document.setCreatedAt(LocalDateTime.now());
        KnowledgeDocument saved = documentRepository.save(document);

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setDocumentId(saved.getDocumentId());
        chunk.setDocumentTitle(saved.getTitle());
        chunk.setSourceType(saved.getSourceType());
        chunk.setChunkIndex(1);
        chunk.setContent(saved.getContent());
        chunk.setCreatedAt(LocalDateTime.now());
        chunkRepository.save(chunk);

        return new KnowledgeDocumentResponse(saved.getDocumentId(), saved.getTitle(), saved.getSourceType(), saved.getSourceUrl(), saved.getChunkCount(), saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listDocuments() {
        return documentRepository.findAll().stream()
                .map(doc -> new KnowledgeDocumentResponse(doc.getDocumentId(), doc.getTitle(), doc.getSourceType(), doc.getSourceUrl(), doc.getChunkCount(), doc.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetrievedContextDto> retrieve(String query, String profileContext, int topK) {
        String search = (profileContext == null || profileContext.isBlank()) ? query : query + " " + profileContext;
        String[] tokens = search == null ? new String[0] : search.toLowerCase().split("\\s+");
        return chunkRepository.findAll().stream()
                .map(chunk -> new RetrievedContextDto(chunk.getChunkId(), chunk.getDocumentId(), chunk.getDocumentTitle(), chunk.getSourceType(), chunk.getContent(), score(chunk.getContent(), tokens)))
                .filter(item -> item.getScore() > 0)
                .limit(topK <= 0 ? 4 : topK)
                .toList();
    }

    private double score(String content, String[] tokens) {
        if (content == null || tokens.length == 0) {
            return 0;
        }
        String lower = content.toLowerCase();
        long hit = 0;
        for (String token : tokens) {
            if (!token.isBlank() && lower.contains(token)) {
                hit++;
            }
        }
        return (double) hit / tokens.length;
    }
}