package com.dsm9.kolpop.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatMessageRequest(
        @NotBlank
        @Size(max = 2000)
        @Schema(description = "사용자가 입력한 자연어 메시지", example = "연남에서 하면 잘 될 팝업 아이템 추천해줘")
        String message
) {
}
