package com.dsm9.kolpop.domain.chat.dto;

import com.dsm9.kolpop.domain.chat.entity.ChatMessage;
import com.dsm9.kolpop.domain.chat.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomRequestResponse(
        Long roomId,
        ChatListingResponse listing,
        ChatUserResponse founder,
        ChatUserResponse landlord,
        ChatMessageResponse message,
        String status,
        LocalDateTime createdAt
) {

    public static ChatRoomRequestResponse from(ChatRoom room, ChatMessage message) {
        return new ChatRoomRequestResponse(
                room.getId(),
                ChatListingResponse.from(room.getListing()),
                ChatUserResponse.from(room.getFounder()),
                ChatUserResponse.from(room.getLandlord()),
                ChatMessageResponse.from(message),
                room.getStatus().name(),
                room.getCreatedAt()
        );
    }
}
