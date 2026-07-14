package com.dsm9.kolpop.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

public record CreateChatRoomRequest(
        @NotNull Long landlordId
) {
}
