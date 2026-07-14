package com.dsm9.kolpop.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChatRoomRequest(
        @NotNull Long listingId,
        @NotBlank
        @Size(max = 1000)
        String content
) {
}
