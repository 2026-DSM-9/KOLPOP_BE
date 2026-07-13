package com.dsm9.kolpop.auth.dto;

public record SignupResponse(
        String accessToken,
        long expiresIn,
        SignupUserResponse user
) {
}
