package com.dsm9.kolpop.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyPageRequest(
        @NotBlank
        @Size(max = 30)
        @Schema(example = "홍길동")
        String name,

        @Size(max = 100)
        @Schema(example = "user@example.com")
        String email,

        @NotBlank
        @Size(max = 20)
        @Schema(example = "010-1234-5678")
        String phone,

        @Size(max = 500)
        @Schema(example = "팝업 공간 운영 경험이 있습니다.")
        String introduction
) {
}
