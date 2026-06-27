package com.assistant.service.rag;

import com.assistant.service.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingService embeddingService;
    private final ChunkRetrievalPort chunkRetrievalPort;
    private final LlmPort llmPort;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.min-similarity:0.7}")
    private double minSimilarity;

    public RagContext enrichPrompt(String question, String conversationHistory) {
        List<ChunkResult> relevantChunks = retrieveRelevantChunks(question);

        String prompt = buildPrompt(question, conversationHistory, relevantChunks);

        return RagContext.builder()
                .enrichedPrompt(prompt)
                .sources(relevantChunks)
                .build();
    }

    private List<ChunkResult> retrieveRelevantChunks(String question) {
        if (topK <= 0) {
            return List.of();
        }

        try {
            List<Float> questionEmbedding = embeddingService.embed(question);
            String embeddingStr = formatVector(questionEmbedding);
            return chunkRetrievalPort.findSimilar(embeddingStr, topK, minSimilarity);
        } catch (Exception e) {
            log.warn("Failed to retrieve relevant chunks (RAG skipped): {}", e.getMessage());
            return List.of();
        }
    }

    public String buildPrompt(String question, String conversationHistory, List<ChunkResult> relevantChunks) {
        StringBuilder prompt = new StringBuilder();

        if (relevantChunks != null && !relevantChunks.isEmpty()) {
            prompt.append("Contexto dos documentos:\n\n");
            for (int i = 0; i < relevantChunks.size(); i++) {
                ChunkResult chunk = relevantChunks.get(i);
                prompt.append("[").append(i + 1).append("] (documento: ")
                        .append(chunk.getDocumentName())
                        .append(", relevância: ")
                        .append(String.format("%.2f", chunk.getSimilarityScore()))
                        .append(")\n");
                prompt.append(chunk.getChunkContent()).append("\n\n");
            }
        }

        if (conversationHistory != null && !conversationHistory.isBlank()) {
            prompt.append("Histórico da conversa:\n").append(conversationHistory).append("\n\n");
        }

        prompt.append("Pergunta do usuário:\n").append(question);

        return prompt.toString();
    }

    public String callLlm(String prompt) {
        return llmPort.call(prompt);
    }

    private String formatVector(List<Float> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
