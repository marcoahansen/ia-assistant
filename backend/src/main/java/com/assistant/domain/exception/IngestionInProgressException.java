package com.assistant.domain.exception;

import java.util.UUID;

public class IngestionInProgressException extends RuntimeException {

    public IngestionInProgressException(UUID documentId) {
        super("Ingestion already in progress for document: " + documentId);
    }
}
