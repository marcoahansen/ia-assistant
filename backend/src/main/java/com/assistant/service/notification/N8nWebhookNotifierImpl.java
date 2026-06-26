package com.assistant.service.notification;

import com.assistant.domain.model.DocumentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class N8nWebhookNotifierImpl implements N8nWebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(N8nWebhookNotifierImpl.class);

    private final RestTemplate restTemplate;
    private final String webhookUrl;

    public N8nWebhookNotifierImpl(
            RestTemplate restTemplate,
            @Value("${app.n8n.webhook-url}") String webhookUrl) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void notifyIngestionComplete(UUID documentId, DocumentStatus status, int chunksCount) {
        try {
            Map<String, Object> payload = Map.of(
                    "event", "document.ingestion.completed",
                    "documentId", documentId.toString(),
                    "status", status.name(),
                    "chunksCount", chunksCount,
                    "timestamp", Instant.now().toString()
            );

            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("N8n webhook notified successfully for document {}", documentId);

        } catch (Exception e) {
            log.error("Failed to notify n8n for document {}: {}", documentId, e.getMessage());
        }
    }
}
