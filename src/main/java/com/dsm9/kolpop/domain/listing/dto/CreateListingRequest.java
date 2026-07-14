package com.dsm9.kolpop.domain.listing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateListingRequest(
        @NotBlank
        @Size(max = 120)
        String title,

        @NotEmpty
        @Size(max = 10)
        List<@Size(max = 2048) String> imageUrls,

        @NotBlank
        @Size(max = 255)
        String address,

        @Size(max = 255)
        String detailAddress,

        @NotNull
        BigDecimal latitude,

        @NotNull
        BigDecimal longitude,

        @NotNull
        @Positive
        Long dailyFee,

        @NotNull
        @PositiveOrZero
        Long deposit,

        @NotNull
        @DecimalMin(value = "0.1")
        BigDecimal area,

        List<@NotBlank @Size(max = 50) String> facilities,

        List<@NotBlank @Size(max = 80) String> industryRestrictions,

        List<@NotBlank @Size(max = 80) String> additionalRestrictions,

        @NotNull
        LocalDate operatingStartDate,

        @NotNull
        LocalDate operatingEndDate,

        @NotNull
        @Positive
        Integer minOperatingDays,

        @NotNull
        @Positive
        Integer maxOperatingDays,

        @NotBlank
        @Size(max = 500)
        String description,

        @Size(max = 8)
        List<@NotBlank @Size(max = 50) String> hashtags
) {
}
