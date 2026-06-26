package com.assistant.domain.exception;

public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String contentType) {
        super("Unsupported file type for parsing: " + contentType);
    }
}
