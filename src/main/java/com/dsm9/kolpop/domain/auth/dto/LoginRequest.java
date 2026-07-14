package com.dsm9.kolpop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "사용자 ID는 필수입니다.")
        @Size(max = 30)
        @Schema(example = "landlord01")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Schema(example = "password123!")
        String password
) {
}
