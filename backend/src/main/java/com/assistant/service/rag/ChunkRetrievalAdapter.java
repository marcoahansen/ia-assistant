package com.assistant.service.rag;

import com.assistant.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChunkRetrievalAdapter implements ChunkRetrievalPort {

    private final DocumentChunkRepository documentChunkRepository;

    @Override
    public List<ChunkResult> findSimilar(String embedding, int topK, double minSimilarity) {
        List<DocumentChunkRepository.ChunkResultProjection> projections =
                documentChunkRepository.findSimilar(embedding, topK, minSimilarity);

        return projections.stream()
                .map(p -> ChunkResult.builder()
                        .chunkId(p.getChunkId())
                        .chunkContent(p.getChunkContent())
                        .documentId(p.getDocumentId())
                        .documentName(p.getDocumentName())
                        .similarityScore(p.getSimilarityScore())
                        .build())
                .toList();
    }
}
