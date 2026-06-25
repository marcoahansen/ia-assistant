package com.assistant.mapper;

import com.assistant.domain.model.Document;
import com.assistant.dto.response.DocumentResponse;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
