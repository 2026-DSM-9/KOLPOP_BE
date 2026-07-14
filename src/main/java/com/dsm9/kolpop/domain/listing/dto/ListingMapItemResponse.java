package com.dsm9.kolpop.domain.listing.dto;

import java.math.BigDecimal;

public record ListingMapItemResponse(
        Long listingId,
        Long landlordId,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Long deposit,
        Long dailyFee,
        ListingStatusResponse status
) {
}
