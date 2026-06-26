package com.assistant.domain.exception;

import java.util.UUID;

public class IngestionFailedException extends RuntimeException {

    public IngestionFailedException(UUID documentId, String reason) {
        super("Ingestion failed for document " + documentId + ": " + reason);
    }
}
