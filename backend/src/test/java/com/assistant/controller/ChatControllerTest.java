package com.assistant.controller;

import com.assistant.dto.request.ChatMessageRequest;
import com.assistant.dto.response.ChatMessageResponse;
import com.assistant.dto.response.MessageDetail;
import com.assistant.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @Test
    void shouldReturn200WhenMessageIsValid() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ChatMessageResponse response = ChatMessageResponse.builder()
                .conversationId(conversationId)
                .userMessage(MessageDetail.builder()
                        .id(UUID.randomUUID())
                        .role("USER")
                        .content("Olá")
                        .createdAt(Instant.now())
                        .build())
                .assistantMessage(MessageDetail.builder()
                        .id(UUID.randomUUID())
                        .role("ASSISTANT")
                        .content("Recebi sua mensagem: Olá")
                        .createdAt(Instant.now())
                        .build())
                .build();

        when(chatService.processMessage(any())).thenReturn(response);

        ChatMessageRequest request = ChatMessageRequest.builder().content("Olá").build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()));
    }

    @Test
    void shouldReturn400WhenContentIsBlank() throws Exception {
        ChatMessageRequest request = ChatMessageRequest.builder().content("").build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
