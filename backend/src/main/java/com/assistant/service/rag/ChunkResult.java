package com.assistant.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class ChunkResult {

    private UUID chunkId;
    private String chunkContent;
    private UUID documentId;
    private String documentName;
    private Double similarityScore;
}
