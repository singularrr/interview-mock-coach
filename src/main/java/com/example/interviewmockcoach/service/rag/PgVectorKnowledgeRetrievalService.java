package com.example.interviewmockcoach.service.rag;

import com.example.interviewmockcoach.config.RagProperties;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.dto.request.IngestKnowledgeDocumentRequest;
import com.example.interviewmockcoach.dto.response.KnowledgeDocumentResponse;
import com.example.interviewmockcoach.entity.KnowledgeChunk;
import com.example.interviewmockcoach.entity.KnowledgeDocument;
import com.example.interviewmockcoach.repository.KnowledgeChunkRepository;
import com.example.interviewmockcoach.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class PgVectorKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private final RagProperties ragProperties;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    @Override
    @Transactional
    public KnowledgeDocumentResponse ingest(IngestKnowledgeDocumentRequest request) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(request.getTitle());
        document.setSourceType(normalizeSourceType(request.getSourceType()));
        document.setSourceUrl(request.getSourceUrl());
        document.setContent(request.getContent());
        List<String> chunks = splitContent(request.getContent(), ragProperties.getChunkSize(), ragProperties.getChunkOverlap());
        document.setChunkCount(chunks.size());
        document.setCreatedAt(LocalDateTime.now());
        KnowledgeDocument saved = documentRepository.save(document);

        List<KnowledgeChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(saved.getDocumentId());
            chunk.setDocumentTitle(saved.getTitle());
            chunk.setSourceType(saved.getSourceType());
            chunk.setChunkIndex(i + 1);
            chunk.setContent(chunkText);
            chunk.setCreatedAt(LocalDateTime.now());
            chunkEntities.add(chunk);
        }
        chunkRepository.saveAll(chunkEntities);

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore != null) {
            List<Document> vectorDocuments = chunkEntities.stream()
                    .map(chunk -> new Document(chunk.getContent(), Map.of(
                            "chunkId", chunk.getChunkId(),
                            "documentId", chunk.getDocumentId(),
                            "documentTitle", chunk.getDocumentTitle(),
                            "sourceType", chunk.getSourceType(),
                            "chunkIndex", chunk.getChunkIndex()
                    )))
                    .toList();
            vectorStore.add(vectorDocuments);
        }

        return new KnowledgeDocumentResponse(saved.getDocumentId(), saved.getTitle(), saved.getSourceType(), saved.getSourceUrl(), saved.getChunkCount(), saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listDocuments() {
        return documentRepository.findAll().stream()
                .sorted(Comparator.comparing(KnowledgeDocument::getCreatedAt).reversed())
                .map(doc -> new KnowledgeDocumentResponse(doc.getDocumentId(), doc.getTitle(), doc.getSourceType(), doc.getSourceUrl(), doc.getChunkCount(), doc.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetrievedContextDto> retrieve(String query, String profileContext, int topK) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore != null) {
            String fullQuery = (profileContext == null || profileContext.isBlank()) ? query : query + "\n" + profileContext;
            SearchRequest request = SearchRequest.builder()
                    .query(fullQuery)
                    .topK(topK > 0 ? topK : ragProperties.getTopK())
                    .similarityThreshold(ragProperties.getSimilarityThreshold())
                    .build();
            List<Document> documents = vectorStore.similaritySearch(request);
            return documents.stream()
                    .map(document -> new RetrievedContextDto(
                            stringMeta(document, "chunkId"),
                            stringMeta(document, "documentId"),
                            stringMeta(document, "documentTitle"),
                            stringMeta(document, "sourceType"),
                            document.getText(),
                            document.getScore() == null ? 0.0 : document.getScore()
                    ))
                    .toList();
        }

        return keywordRetrieve(query, topK);
    }

    private List<RetrievedContextDto> keywordRetrieve(String query, int topK) {
        String[] tokens = normalizeTokens(query);
        return chunkRepository.findAll().stream()
                .map(chunk -> new RetrievedContextDto(
                        chunk.getChunkId(),
                        chunk.getDocumentId(),
                        chunk.getDocumentTitle(),
                        chunk.getSourceType(),
                        chunk.getContent(),
                        keywordScore(chunk.getContent(), tokens)
                ))
                .filter(item -> item.getScore() > 0)
                .sorted(Comparator.comparingDouble(RetrievedContextDto::getScore).reversed())
                .limit(topK > 0 ? topK : ragProperties.getTopK())
                .toList();
    }

    private double keywordScore(String content, String[] tokens) {
        if (content == null || tokens.length == 0) {
            return 0;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String token : tokens) {
            if (!token.isBlank() && lower.contains(token)) {
                matched++;
            }
        }
        return (double) matched / tokens.length;
    }

    private String[] normalizeTokens(String query) {
        if (query == null || query.isBlank()) {
            return new String[0];
        }
        return query.toLowerCase(Locale.ROOT)
                .replaceAll("[^\u4e00-\u9fa5a-zA-Z0-9]+", " ")
                .trim()
                .split("\\s+");
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return "GENERAL";
        }
        return sourceType.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> splitContent(String content, int chunkSize, int overlap) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        int safeChunkSize = Math.max(200, chunkSize);
        int safeOverlap = Math.max(0, Math.min(overlap, safeChunkSize - 1));
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + safeChunkSize);
            chunks.add(content.substring(start, end).trim());
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - safeOverlap, start + 1);
        }
        return chunks.stream().filter(chunk -> !chunk.isBlank()).toList();
    }

    private String stringMeta(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
