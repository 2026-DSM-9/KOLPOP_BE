package com.dsm9.kolpop.domain.chat.dto;

import com.dsm9.kolpop.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        ChatUserResponse sender,
        String content,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                ChatUserResponse.from(message.getSender()),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
