package com.example.interviewmockcoach.controller;

import com.example.interviewmockcoach.dto.common.ApiResponse;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.dto.request.IngestKnowledgeDocumentRequest;
import com.example.interviewmockcoach.dto.response.KnowledgeDocumentResponse;
import com.example.interviewmockcoach.service.rag.KnowledgeRetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge-documents")
public class KnowledgeDocumentController {

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @PostMapping("/ingest")
    public ApiResponse<KnowledgeDocumentResponse> ingest(@Valid @RequestBody IngestKnowledgeDocumentRequest request) {
        return ApiResponse.success(knowledgeRetrievalService.ingest(request));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDocumentResponse>> list() {
        return ApiResponse.success(knowledgeRetrievalService.listDocuments());
    }

    @GetMapping("/search")
    public ApiResponse<List<RetrievedContextDto>> search(@RequestParam String query,
                                                         @RequestParam(required = false) String profileContext,
                                                         @RequestParam(defaultValue = "4") int topK) {
        return ApiResponse.success(knowledgeRetrievalService.retrieve(query, profileContext, topK));
    }
}
