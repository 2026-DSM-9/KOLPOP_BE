package com.dsm9.kolpop.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "사용자 ID는 필수입니다.")
        @Size(max = 30)
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
