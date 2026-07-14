package com.dsm9.kolpop.domain.auth.dto;

public record SignupUserResponse(
        Long userId,
        String phone,
        String name,
        String role
) {
}
