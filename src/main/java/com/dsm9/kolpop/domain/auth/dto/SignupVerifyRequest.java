package com.dsm9.kolpop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupVerifyRequest(
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "전화번호는 01012345678 또는 010-0000-0000 형식으로 입력해주세요.")
        @Schema(example = "010-1234-5678")
        String phone,

        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자로 입력해주세요.")
        @Schema(example = "123456")
        String code
) {
}
