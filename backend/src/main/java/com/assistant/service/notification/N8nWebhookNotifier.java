package com.assistant.service.notification;

import com.assistant.domain.model.DocumentStatus;

import java.util.UUID;

public interface N8nWebhookNotifier {

    void notifyIngestionComplete(UUID documentId, DocumentStatus status, int chunksCount);
}
