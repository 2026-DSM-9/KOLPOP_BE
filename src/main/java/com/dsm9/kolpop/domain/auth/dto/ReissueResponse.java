package com.dsm9.kolpop.domain.auth.dto;

public record ReissueResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}
