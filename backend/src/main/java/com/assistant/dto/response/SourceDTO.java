package com.assistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceDTO {

    private UUID chunkId;
    private String chunkContent;
    private UUID documentId;
    private String documentName;
    private Double similarityScore;
}
