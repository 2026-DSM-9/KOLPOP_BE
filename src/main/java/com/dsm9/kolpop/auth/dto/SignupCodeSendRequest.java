package com.dsm9.kolpop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupCodeSendRequest(
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "전화번호는 01012345678 또는 010-0000-0000 형식으로 입력해주세요.")
        String phone
) {
}
