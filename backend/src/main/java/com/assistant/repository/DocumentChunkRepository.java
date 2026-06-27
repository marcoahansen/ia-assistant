package com.assistant.repository;

import com.assistant.domain.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentId(UUID documentId);

    @Query(value = """
            SELECT dc.id as chunkId, dc.content as chunkContent,
                   d.id as documentId, d.original_filename as documentName,
                   (1 - (CAST(dc.embedding AS vector) <=> CAST(:embedding AS vector))) as similarityScore
            FROM document_chunks dc
            JOIN documents d ON d.id = dc.document_id
            WHERE (1 - (CAST(dc.embedding AS vector) <=> CAST(:embedding AS vector))) >= :minSimilarity
            ORDER BY similarityScore DESC
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkResultProjection> findSimilar(@Param("embedding") String embeddingStr,
                                            @Param("topK") int topK,
                                            @Param("minSimilarity") double minSimilarity);

    interface ChunkResultProjection {
        UUID getChunkId();
        String getChunkContent();
        UUID getDocumentId();
        String getDocumentName();
        Double getSimilarityScore();
    }
}
