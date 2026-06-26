package com.assistant.service.rag;

import com.assistant.domain.exception.LlmServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class LlmService implements LlmPort {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public LlmService(
            RestTemplate restTemplate,
            @Value("${app.llm.url}") String apiUrl,
            @Value("${app.llm.api-key}") String apiKey,
            @Value("${app.llm.model}") String model) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String call(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            var requestEntity = new org.springframework.http.HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(apiUrl, requestEntity, Map.class);

            if (response == null || !response.containsKey("choices")) {
                throw new LlmServiceException("Invalid LLM response: missing 'choices' field");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices.isEmpty()) {
                throw new LlmServiceException("Invalid LLM response: empty 'choices' array");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");

        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmServiceException("LLM API call failed: " + e.getMessage(), e);
        }
    }
}
