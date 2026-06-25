package com.assistant.controller;

import com.assistant.domain.exception.ConversationNotFoundException;
import com.assistant.dto.response.ConversationResponse;
import com.assistant.dto.response.MessageDetail;
import com.assistant.service.ConversationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @Test
    void shouldReturn200WhenConversationExists() throws Exception {
        UUID id = UUID.randomUUID();
        ConversationResponse response = ConversationResponse.builder()
                .id(id)
                .title("Olá")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .messages(List.of())
                .build();

        when(conversationService.getConversation(id)).thenReturn(response);

        mockMvc.perform(get("/api/conversations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void shouldReturn404WhenConversationDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(conversationService.getConversation(any())).thenThrow(new ConversationNotFoundException(id));

        mockMvc.perform(get("/api/conversations/{id}", id))
                .andExpect(status().isNotFound());
    }
}
