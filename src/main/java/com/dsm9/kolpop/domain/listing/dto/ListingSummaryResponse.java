package com.dsm9.kolpop.domain.listing.dto;

import java.math.BigDecimal;

public record ListingSummaryResponse(
        Long listingId,
        String title,
        String thumbnailUrl,
        String address,
        Long dailyFee,
        Long deposit,
        BigDecimal area,
        Long viewCount,
        Integer reservationCount,
        ListingStatusResponse status
) {
}
