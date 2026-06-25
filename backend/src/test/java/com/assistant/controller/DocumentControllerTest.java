package com.assistant.controller;

import com.assistant.domain.exception.InvalidFileTypeException;
import com.assistant.dto.response.DocumentResponse;
import com.assistant.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void shouldReturn201WhenFileIsValid() throws Exception {
        DocumentResponse response = DocumentResponse.builder()
                .id(UUID.randomUUID())
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .sizeBytes(1024L)
                .uploadedAt(Instant.now())
                .build();

        when(documentService.uploadDocument(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("test.pdf"));
    }

    @Test
    void shouldReturn400WhenFileTypeIsInvalid() throws Exception {
        when(documentService.uploadDocument(any()))
                .thenThrow(new InvalidFileTypeException("Invalid file type"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "application/x-msdownload", "test".getBytes());

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isBadRequest());
    }
}
