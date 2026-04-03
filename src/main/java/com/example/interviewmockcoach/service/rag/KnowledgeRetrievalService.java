package com.example.interviewmockcoach.service.rag;

import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.dto.request.IngestKnowledgeDocumentRequest;
import com.example.interviewmockcoach.dto.response.KnowledgeDocumentResponse;

import java.util.List;

public interface KnowledgeRetrievalService {

    KnowledgeDocumentResponse ingest(IngestKnowledgeDocumentRequest request);

    List<KnowledgeDocumentResponse> listDocuments();

    List<RetrievedContextDto> retrieve(String query, String profileContext, int topK);
}
