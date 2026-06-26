package com.assistant.domain.exception;

public class RagServiceException extends RuntimeException {

    public RagServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RagServiceException(String message) {
        super(message);
    }
}
