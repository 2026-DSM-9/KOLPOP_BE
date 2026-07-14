package com.dsm9.kolpop.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyPageRequest(
        @NotBlank
        @Size(max = 30)
        String name,

        @Size(max = 100)
        String email,

        @NotBlank
        @Size(max = 20)
        String phone,

        @Size(max = 500)
        String introduction
) {
}
