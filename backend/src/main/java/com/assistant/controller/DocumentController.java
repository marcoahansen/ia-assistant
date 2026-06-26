package com.assistant.controller;

import com.assistant.dto.response.DocumentListResponse;
import com.assistant.dto.response.DocumentResponse;
import com.assistant.dto.response.DocumentStatusResponse;
import com.assistant.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(file));
    }

    @GetMapping
    public ResponseEntity<DocumentListResponse> listDocuments() {
        return ResponseEntity.ok(documentService.listDocuments());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getDocumentStatus(id));
    }
}
