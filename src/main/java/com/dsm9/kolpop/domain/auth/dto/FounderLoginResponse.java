package com.dsm9.kolpop.domain.auth.dto;

public record FounderLoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        LoginUserResponse user
) {
}
