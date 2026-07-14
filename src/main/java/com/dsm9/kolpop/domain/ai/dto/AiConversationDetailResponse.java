package com.dsm9.kolpop.domain.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.dsm9.kolpop.domain.ai.entity.AiConversation;

public record AiConversationDetailResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        List<AiConversationMessageResponse> messages
) {

    public static AiConversationDetailResponse from(AiConversation conversation) {
        LocalDateTime createdAt = conversation.getCreatedAt();
        return new AiConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                createdAt,
                List.of(
                        new AiConversationMessageResponse("USER", conversation.getUserMessage(), createdAt),
                        new AiConversationMessageResponse("AI", conversation.getAiMessage(), createdAt)
                )
        );
    }
}
