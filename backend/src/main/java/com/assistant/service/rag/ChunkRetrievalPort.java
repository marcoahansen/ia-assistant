package com.assistant.service.rag;

import java.util.List;

public interface ChunkRetrievalPort {

    List<ChunkResult> findSimilar(String embedding, int topK, double minSimilarity);
}
