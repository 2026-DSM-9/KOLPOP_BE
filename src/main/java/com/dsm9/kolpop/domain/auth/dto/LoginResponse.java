package com.dsm9.kolpop.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        long expiresIn,
        LoginUserResponse user
) {
}
