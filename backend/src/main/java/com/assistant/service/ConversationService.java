package com.assistant.service;

import com.assistant.domain.exception.ConversationNotFoundException;
import com.assistant.domain.model.Conversation;
import com.assistant.domain.model.Message;
import com.assistant.dto.response.ConversationListResponse;
import com.assistant.dto.response.ConversationResponse;
import com.assistant.dto.response.ConversationSummaryResponse;
import com.assistant.mapper.ConversationMapper;
import com.assistant.repository.ConversationRepository;
import com.assistant.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;

    public ConversationResponse getConversation(UUID id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException(id));
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        conversation.getMessages().clear();
        conversation.getMessages().addAll(messages);
        return conversationMapper.toResponse(conversation);
    }

    public ConversationListResponse listConversations() {
        List<Conversation> conversations = conversationRepository.findAllByOrderByUpdatedAtDesc();
        List<ConversationSummaryResponse> summaries = conversations.stream()
                .map(conversationMapper::toSummary)
                .collect(Collectors.toList());
        return ConversationListResponse.builder()
                .conversations(summaries)
                .total(summaries.size())
                .build();
    }
}
