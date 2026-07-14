package com.dsm9.kolpop.domain.festival.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FestivalSummaryResponse(
        String id,
        String name,
        String place,
        String address,
        String region,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        long dDay,
        int nearbyListingCount,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
