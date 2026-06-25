package com.assistant.mapper;

import com.assistant.domain.model.Message;
import com.assistant.dto.response.MessageDetail;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageDetail toDetail(Message message) {
        return MessageDetail.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
