package com.assistant.dto.response;

import com.assistant.domain.model.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStatusResponse {

    private UUID id;
    private String originalFilename;
    private DocumentStatus status;
    private Integer chunksCount;
    private Instant uploadedAt;
    private Instant completedAt;
}
