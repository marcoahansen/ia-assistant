package com.assistant.domain.exception;

public class EmbeddingServiceException extends RuntimeException {

    public EmbeddingServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingServiceException(String message) {
        super(message);
    }
}
