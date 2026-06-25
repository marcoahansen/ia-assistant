package com.assistant.controller;

import com.assistant.dto.response.HealthResponse;
import com.assistant.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    void shouldReturn200WhenAllUp() throws Exception {
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .timestamp(Instant.now())
                .components(Map.of("database", "UP", "storage", "UP"))
                .build();

        when(healthService.checkHealth()).thenReturn(response);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldReturn503WhenDatabaseIsDown() throws Exception {
        HealthResponse response = HealthResponse.builder()
                .status("DOWN")
                .timestamp(Instant.now())
                .components(Map.of("database", "DOWN", "storage", "UP"))
                .build();

        when(healthService.checkHealth()).thenReturn(response);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
