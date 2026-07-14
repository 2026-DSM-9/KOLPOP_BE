package com.dsm9.kolpop.domain.reservation.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(
        @NotNull
        @Schema(example = "1")
        Long listingId,

        @NotNull
        @Schema(example = "2026-08-05")
        LocalDate startDate,

        @NotNull
        @Schema(example = "2026-08-12")
        LocalDate endDate,

        @NotBlank
        @Size(max = 500)
        @Schema(example = "패션 브랜드 팝업스토어를 운영하고 싶습니다. 의류 행거와 피팅룸이 필요해요.")
        String message
) {
}
