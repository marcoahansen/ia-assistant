package com.assistant.service.embedding;

import com.assistant.domain.exception.EmbeddingServiceException;

import java.util.List;

public interface EmbeddingService {

    List<Float> embed(String text) throws EmbeddingServiceException;
}
