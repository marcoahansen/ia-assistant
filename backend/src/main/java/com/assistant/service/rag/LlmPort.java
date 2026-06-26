package com.assistant.service.rag;

import com.assistant.domain.exception.LlmServiceException;

public interface LlmPort {

    String call(String prompt) throws LlmServiceException;
}
