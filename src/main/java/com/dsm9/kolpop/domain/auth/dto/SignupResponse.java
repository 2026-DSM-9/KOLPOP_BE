package com.dsm9.kolpop.domain.auth.dto;

public record SignupResponse(
        String accessToken,
        long expiresIn,
        SignupUserResponse user
) {
}
