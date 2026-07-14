package com.dsm9.kolpop.domain.auth.dto;

public record LoginUserResponse(
        String userId,
        String phone,
        String name
) {
}
