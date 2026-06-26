package com.assistant.service;

import com.assistant.dto.response.HealthResponse;
import com.assistant.service.embedding.EmbeddingService;
import com.assistant.storage.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final DataSource dataSource;
    private final FileStoragePort fileStoragePort;
    private final EmbeddingService embeddingService;

    public HealthResponse checkHealth() {
        String dbStatus = checkDatabase() ? "UP" : "DOWN";
        String storageStatus = fileStoragePort.isAccessible() ? "UP" : "DOWN";
        String embeddingStatus = checkEmbedding() ? "UP" : "DOWN";

        String overallStatus = dbStatus.equals("UP") && storageStatus.equals("UP") && embeddingStatus.equals("UP")
                ? "UP" : "DOWN";

        Map<String, String> components = new LinkedHashMap<>();
        components.put("database", dbStatus);
        components.put("storage", storageStatus);
        components.put("embedding", embeddingStatus);

        return HealthResponse.builder()
                .status(overallStatus)
                .timestamp(Instant.now())
                .components(components)
                .build();
    }

    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkEmbedding() {
        try {
            embeddingService.embed("health");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
