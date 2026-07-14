package com.dsm9.kolpop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]{4,30}$",
                message = "아이디는 영문, 숫자, 밑줄을 사용해 4자 이상 30자 이하로 입력해주세요."
        )
        @Schema(example = "landlord01")
        String loginId,

        @NotBlank(message = "이름은 필수입니다.")
        @Schema(example = "홍길동")
        String name,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해주세요.")
        @Schema(example = "password123!")
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        @Schema(example = "password123!")
        String passwordConfirm,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "전화번호는 01012345678 또는 010-0000-0000 형식으로 입력해주세요.")
        @Schema(example = "010-1234-5678")
        String phone,

        @Nullable
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Schema(example = "landlord@example.com")
        String email
) {
}
