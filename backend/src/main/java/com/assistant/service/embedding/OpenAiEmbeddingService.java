package com.assistant.service.embedding;

import com.assistant.domain.exception.EmbeddingServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingService(
            RestTemplate restTemplate,
            @Value("${app.embedding.url}") String apiUrl,
            @Value("${app.embedding.api-key}") String apiKey,
            @Value("${app.embedding.model}") String model) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Float> embed(String text) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "input", text,
                    "model", model
            );

            Map<String, Object> response = restTemplate.postForObject(
                    apiUrl,
                    createRequestEntity(requestBody),
                    Map.class);

            if (response == null || !response.containsKey("data")) {
                throw new EmbeddingServiceException("Invalid embedding response: missing 'data' field");
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data.isEmpty()) {
                throw new EmbeddingServiceException("Invalid embedding response: empty 'data' array");
            }

            List<Double> doubleEmbedding = (List<Double>) data.get(0).get("embedding");
            return doubleEmbedding.stream()
                    .map(Double::floatValue)
                    .toList();

        } catch (Exception e) {
            throw new EmbeddingServiceException("Embedding API call failed: " + e.getMessage(), e);
        }
    }

    private org.springframework.http.HttpEntity<Map<String, Object>> createRequestEntity(Map<String, Object> body) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");
        return new org.springframework.http.HttpEntity<>(body, headers);
    }
}
