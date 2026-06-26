package com.assistant.mapper;

import com.assistant.domain.model.Document;
import com.assistant.dto.response.DocumentResponse;
import com.assistant.dto.response.DocumentStatusResponse;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .status(document.getStatus())
                .chunksCount(document.getChunksCount())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    public DocumentStatusResponse toStatusResponse(Document document) {
        return DocumentStatusResponse.builder()
                .id(document.getId())
                .originalFilename(document.getOriginalFilename())
                .status(document.getStatus())
                .chunksCount(document.getChunksCount())
                .uploadedAt(document.getUploadedAt())
                .completedAt(document.getStatus() == com.assistant.domain.model.DocumentStatus.COMPLETED
                        ? document.getUploadedAt() : null)
                .build();
    }
}
