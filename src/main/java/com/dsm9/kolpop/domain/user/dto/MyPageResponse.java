package com.dsm9.kolpop.domain.user.dto;

public record MyPageResponse(
        String name,
        String email,
        String phone,
        String introduction,
        String role
) {
}
