package com.dsm9.kolpop.domain.user.dto;

public record MyPageResponse(
        String name,
        String email,
        String phone,
        String address,
        String detailAddress,
        String introduction,
        String role
) {
}
