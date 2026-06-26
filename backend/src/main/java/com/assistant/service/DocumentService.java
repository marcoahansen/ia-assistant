package com.assistant.service;

import com.assistant.domain.exception.FileSizeExceededException;
import com.assistant.domain.exception.InvalidFileTypeException;
import com.assistant.domain.model.Document;
import com.assistant.domain.model.DocumentStatus;
import com.assistant.dto.response.DocumentListResponse;
import com.assistant.dto.response.DocumentResponse;
import com.assistant.dto.response.DocumentStatusResponse;
import com.assistant.mapper.DocumentMapper;
import com.assistant.repository.DocumentRepository;
import com.assistant.service.ingestion.DocumentIngestionService;
import com.assistant.storage.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocumentRepository documentRepository;
    private final FileStoragePort fileStoragePort;
    private final DocumentMapper documentMapper;
    private final DocumentIngestionService documentIngestionService;
    private final EntityManager entityManager;

    public DocumentResponse uploadDocument(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                    "Invalid file type: " + contentType + ". Allowed: PDF, TXT, DOCX");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeExceededException(
                    "File size exceeds the 10 MB limit: " + file.getSize() + " bytes");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;

        fileStoragePort.store(file, storedFilename);

        Document document = Document.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .status(DocumentStatus.PENDING)
                .uploadedAt(Instant.now())
                .build();

        document = documentRepository.save(document);

        document.setStatus(DocumentStatus.PROCESSING);
        document = documentRepository.save(document);
        entityManager.flush();

        documentIngestionService.ingestDocument(document.getId());

        return documentMapper.toResponse(document);
    }

    @Transactional(readOnly = true)
    public DocumentListResponse listDocuments() {
        List<Document> documents = documentRepository.findAllByOrderByUploadedAtDesc();
        List<DocumentResponse> responses = documents.stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
        return DocumentListResponse.builder()
                .documents(responses)
                .total(responses.size())
                .build();
    }

    @Transactional(readOnly = true)
    public DocumentStatusResponse getDocumentStatus(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new com.assistant.domain.exception.DocumentNotFoundException(id));
        return documentMapper.toStatusResponse(document);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        String sanitized = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        sanitized = sanitized.replaceAll("\\.\\.", "_");
        sanitized = sanitized.replaceAll("[/\\\\]", "_");
        return sanitized.strip();
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase();
    }
}
