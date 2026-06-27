package com.assistant.service.ingestion;

import com.assistant.domain.exception.DocumentNotFoundException;
import com.assistant.domain.exception.IngestionFailedException;
import com.assistant.domain.exception.UnsupportedFileTypeException;
import com.assistant.domain.model.Document;
import com.assistant.domain.model.DocumentChunk;
import com.assistant.domain.model.DocumentStatus;
import com.assistant.repository.DocumentChunkRepository;
import com.assistant.repository.DocumentRepository;
import com.assistant.service.embedding.EmbeddingService;
import com.assistant.service.notification.N8nWebhookNotifier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final N8nWebhookNotifier n8nWebhookNotifier;

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Value("${app.rag.chunk.max-size:512}")
    private int chunkMaxSize;

    @Value("${app.rag.chunk.overlap:64}")
    private int chunkOverlap;

    @Async
    @Transactional
    public void ingestDocument(UUID documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));

            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            String rawText = parseFile(
                    Path.of(uploadDir, document.getStoredFilename()),
                    document.getContentType()
            );

            List<String> chunks = chunkText(rawText);

            List<DocumentChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                List<Float> embedding = embeddingService.embed(chunkText);

                DocumentChunk chunk = DocumentChunk.builder()
                        .document(document)
                        .content(chunkText)
                        .chunkIndex(i)
                        .embedding(formatVector(embedding))
                        .createdAt(Instant.now())
                        .build();
                chunkEntities.add(chunk);
            }

            documentChunkRepository.saveAll(chunkEntities);

            document.setStatus(DocumentStatus.COMPLETED);
            document.setChunksCount(chunkEntities.size());
            documentRepository.save(document);

            n8nWebhookNotifier.notifyIngestionComplete(documentId, DocumentStatus.COMPLETED, chunkEntities.size());

        } catch (Exception e) {
            log.error("Ingestion failed for document {}: {}", documentId, e.getMessage());
            handleIngestionError(documentId, e);
        }
    }

    public String parseFile(Path storedFilePath, String contentType) {
        if (contentType == null) {
            throw new UnsupportedFileTypeException("null");
        }

        try {
            return switch (contentType) {
                case "text/plain" -> Files.readString(storedFilePath);
                case "application/pdf" -> parsePdf(storedFilePath);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                        parseDocx(storedFilePath);
                default -> throw new UnsupportedFileTypeException(contentType);
            };
        } catch (IOException e) {
            throw new IngestionFailedException(null, "Failed to parse file: " + e.getMessage());
        }
    }

    private String parsePdf(Path path) throws IOException {
        try (var document = org.apache.pdfbox.Loader.loadPDF(path.toFile())) {
            var stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(Path path) throws IOException {
        try (var fis = Files.newInputStream(path);
             var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(fis)) {
            var text = new StringBuilder();
            for (var paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() > chunkMaxSize && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().strip());
                String overlap = extractOverlap(currentChunk.toString());
                currentChunk = new StringBuilder(overlap);
            }

            if (!currentChunk.isEmpty()) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed);
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().strip());
        }

        return chunks;
    }

    private String extractOverlap(String chunk) {
        if (chunk.length() <= chunkOverlap) {
            return chunk;
        }
        int lastNewline = chunk.lastIndexOf("\n", chunk.length() - chunkOverlap);
        if (lastNewline > 0) {
            return chunk.substring(lastNewline + 1);
        }
        return chunk.substring(chunk.length() - chunkOverlap);
    }

    private void handleIngestionError(UUID documentId, Exception e) {
        try {
            documentRepository.findById(documentId).ifPresentOrElse(
                    document -> {
                        document.setStatus(DocumentStatus.FAILED);
                        documentRepository.save(document);
                    },
                    () -> log.warn("Document {} not found in DB to mark as FAILED", documentId)
            );
        } catch (Exception dbEx) {
            log.error("Failed to update document {} status to FAILED: {}", documentId, dbEx.getMessage());
        }

        try {
            n8nWebhookNotifier.notifyIngestionComplete(documentId, DocumentStatus.FAILED, 0);
        } catch (Exception notifEx) {
            log.error("Failed to notify n8n for document {}: {}", documentId, notifEx.getMessage());
        }
    }

    private String formatVector(List<Float> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
