package com.dsm9.kolpop.domain.festival.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.dsm9.kolpop.domain.listing.dto.ListingSummaryResponse;

public record FestivalDetailResponse(
        String id,
        String name,
        String place,
        String content,
        String address,
        String roadAddress,
        String lotAddress,
        String region,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        long dDay,
        String managingAgency,
        String hostAgency,
        String sponsorAgency,
        String phoneNumber,
        String homepageUrl,
        String relatedInfo,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate referenceDate,
        List<ListingSummaryResponse> nearbyListings
) {
}
