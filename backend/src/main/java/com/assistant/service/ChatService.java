package com.assistant.service;

import com.assistant.domain.exception.ConversationNotFoundException;
import com.assistant.domain.model.Conversation;
import com.assistant.domain.model.Message;
import com.assistant.domain.model.MessageRole;
import com.assistant.dto.request.ChatMessageRequest;
import com.assistant.dto.response.ChatMessageResponse;
import com.assistant.mapper.MessageMapper;
import com.assistant.repository.ConversationRepository;
import com.assistant.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        Conversation conversation;
        if (request.getConversationId() == null) {
            conversation = Conversation.builder()
                    .title(deriveTitle(request.getContent()))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            conversation = conversationRepository.save(conversation);
        } else {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ConversationNotFoundException(request.getConversationId()));
        }

        Message userMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.getContent())
                .createdAt(Instant.now())
                .build();
        userMessage = messageRepository.save(userMessage);

        String stubResponse = "Recebi sua mensagem: " + request.getContent();
        Message assistantMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(stubResponse)
                .createdAt(Instant.now())
                .build();
        assistantMessage = messageRepository.save(assistantMessage);

        conversation.setUpdatedAt(Instant.now());
        if (conversation.getTitle() == null) {
            conversation.setTitle(deriveTitle(request.getContent()));
        }
        conversationRepository.save(conversation);

        return ChatMessageResponse.builder()
                .conversationId(conversation.getId())
                .userMessage(messageMapper.toDetail(userMessage))
                .assistantMessage(messageMapper.toDetail(assistantMessage))
                .build();
    }

    private String deriveTitle(String content) {
        String title = content.length() > 100 ? content.substring(0, 100) : content;
        return title.strip();
    }
}
