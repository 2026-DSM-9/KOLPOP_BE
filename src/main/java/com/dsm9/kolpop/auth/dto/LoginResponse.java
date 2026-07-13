package com.dsm9.kolpop.auth.dto;

public record LoginResponse(
        String accessToken,
        long expiresIn,
        LoginUserResponse user
) {
}
