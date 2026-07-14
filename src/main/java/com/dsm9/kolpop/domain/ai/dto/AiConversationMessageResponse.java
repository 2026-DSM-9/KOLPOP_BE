package com.dsm9.kolpop.domain.ai.dto;

import java.time.LocalDateTime;

public record AiConversationMessageResponse(
        String role,
        String content,
        LocalDateTime createdAt
) {
}
