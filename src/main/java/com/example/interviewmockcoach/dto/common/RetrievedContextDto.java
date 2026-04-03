package com.example.interviewmockcoach.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedContextDto {

    private String chunkId;
    private String documentId;
    private String documentTitle;
    private String sourceType;
    private String content;
    private double score;
}
