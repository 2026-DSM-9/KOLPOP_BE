package com.dsm9.kolpop.domain.chat.dto;

import com.dsm9.kolpop.domain.chat.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long roomId,
        ChatUserResponse founder,
        ChatUserResponse landlord,
        LocalDateTime createdAt
) {

    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                ChatUserResponse.from(room.getFounder()),
                ChatUserResponse.from(room.getLandlord()),
                room.getCreatedAt()
        );
    }
}
