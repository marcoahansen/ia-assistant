package com.assistant.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class RagContext {

    private String enrichedPrompt;
    private List<ChunkResult> sources;
}
