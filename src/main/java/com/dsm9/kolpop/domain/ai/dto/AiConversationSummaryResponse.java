package com.dsm9.kolpop.domain.ai.dto;

import java.time.LocalDateTime;

import com.dsm9.kolpop.domain.ai.entity.AiConversation;

public record AiConversationSummaryResponse(
        Long id,
        String title,
        String preview,
        LocalDateTime createdAt
) {

    public static AiConversationSummaryResponse from(AiConversation conversation) {
        return new AiConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getAiMessage(),
                conversation.getCreatedAt()
        );
    }
}
