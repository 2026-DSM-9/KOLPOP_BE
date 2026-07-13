package com.dsm9.kolpop.auth.dto;

public record SignupUserResponse(
        Long userId,
        String phone,
        String name,
        String role
) {
}
