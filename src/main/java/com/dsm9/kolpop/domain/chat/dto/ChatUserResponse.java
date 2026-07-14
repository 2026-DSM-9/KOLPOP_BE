package com.dsm9.kolpop.domain.chat.dto;

import com.dsm9.kolpop.domain.user.entity.User;

public record ChatUserResponse(
        Long id,
        String name,
        String role
) {

    public static ChatUserResponse from(User user) {
        return new ChatUserResponse(
                user.getId(),
                user.getName(),
                user.getRole().name()
        );
    }
}
