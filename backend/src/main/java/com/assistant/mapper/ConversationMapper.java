package com.assistant.mapper;

import com.assistant.domain.model.Conversation;
import com.assistant.dto.response.ConversationResponse;
import com.assistant.dto.response.ConversationSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConversationMapper {

    private final MessageMapper messageMapper;

    public ConversationResponse toResponse(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messages(conversation.getMessages().stream()
                        .map(messageMapper::toDetail)
                        .collect(Collectors.toList()))
                .build();
    }

    public ConversationSummaryResponse toSummary(Conversation conversation) {
        return ConversationSummaryResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
