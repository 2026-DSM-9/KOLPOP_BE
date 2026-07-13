package com.dsm9.kolpop.auth.dto;

public record LoginUserResponse(
        String userId,
        String phone,
        String name
) {
}
