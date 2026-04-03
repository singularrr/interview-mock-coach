package com.example.interviewmockcoach.controller;

import com.example.interviewmockcoach.dto.common.ApiResponse;
import com.example.interviewmockcoach.dto.common.RetrievedContextDto;
import com.example.interviewmockcoach.dto.request.IngestKnowledgeDocumentRequest;
import com.example.interviewmockcoach.dto.response.KnowledgeDocumentResponse;
import com.example.interviewmockcoach.exception.BusinessException;
import com.example.interviewmockcoach.service.rag.KnowledgeRetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge-documents")
public class KnowledgeDocumentController {

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @PostMapping("/ingest")
    public ApiResponse<KnowledgeDocumentResponse> ingest(@Valid @RequestBody IngestKnowledgeDocumentRequest request) {
        return ApiResponse.success(knowledgeRetrievalService.ingest(request));
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeDocumentResponse> ingestFile(@RequestParam("title") String title,
                                                             @RequestParam(value = "sourceType", required = false) String sourceType,
                                                             @RequestParam(value = "sourceUrl", required = false) String sourceUrl,
                                                             @RequestParam("file") MultipartFile file) throws IOException {
        String content = extractContent(file);
        IngestKnowledgeDocumentRequest request = new IngestKnowledgeDocumentRequest(title, content, sourceType, sourceUrl);
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

    private String extractContent(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40001, "上传文件不能为空");
        }

        if (isPdf(file)) {
            try (var document = Loader.loadPDF(file.getBytes())) {
                String text = new PDFTextStripper().getText(document);
                if (text == null || text.isBlank()) {
                    throw new BusinessException(40004, "PDF未提取到可用文本。如果这是扫描版PDF，请先做OCR再上传。");
                }
                return text;
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BusinessException(40005, "PDF解析失败：" + safeMessage(ex.getMessage()));
            }
        }

        if (isDocx(file)) {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getBytes()));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                String text = extractor.getText();
                if (text == null || text.isBlank()) {
                    throw new BusinessException(40007, "Word文档未提取到可用文本，请确认文件不是空文档。");
                }
                return text;
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BusinessException(40008, "Word文档解析失败：" + safeMessage(ex.getMessage()));
            }
        }

        if (isDoc(file)) {
            try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(file.getBytes()));
                 WordExtractor extractor = new WordExtractor(document)) {
                String text = extractor.getText();
                if (text == null || text.isBlank()) {
                    throw new BusinessException(40009, "Word文档未提取到可用文本，请确认文件不是空文档。");
                }
                return text;
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BusinessException(40010, "Word文档解析失败：" + safeMessage(ex.getMessage()));
            }
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.isBlank()) {
            throw new BusinessException(40006, "上传文件内容为空");
        }
        return content;
    }

    private boolean isPdf(MultipartFile file) {
        String filename = normalizedFilename(file);
        String contentType = normalizedContentType(file);
        return filename.endsWith(".pdf") || contentType.contains("pdf");
    }

    private boolean isDocx(MultipartFile file) {
        String filename = normalizedFilename(file);
        String contentType = normalizedContentType(file);
        return filename.endsWith(".docx") || contentType.contains("officedocument.wordprocessingml.document");
    }

    private boolean isDoc(MultipartFile file) {
        String filename = normalizedFilename(file);
        String contentType = normalizedContentType(file);
        return filename.endsWith(".doc") || contentType.contains("msword");
    }

    private String normalizedFilename(MultipartFile file) {
        return file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    }

    private String normalizedContentType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank() ? "未知错误" : message;
    }
}