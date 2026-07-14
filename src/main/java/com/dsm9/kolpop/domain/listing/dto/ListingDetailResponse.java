package com.dsm9.kolpop.domain.listing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ListingDetailResponse(
        Long listingId,
        String title,
        String address,
        String detailAddress,
        List<String> imageUrls,
        Long dailyFee,
        Long deposit,
        BigDecimal area,
        Long sevenDayTotalFee,
        String landlordName,
        Long likeCount,
        Long viewCount,
        Integer reservationCount,
        LocalDate operatingStartDate,
        LocalDate operatingEndDate,
        Integer minOperatingDays,
        Integer maxOperatingDays,
        List<String> facilities,
        List<String> industryRestrictions,
        List<String> additionalRestrictions,
        String description,
        List<String> hashtags,
        BigDecimal latitude,
        BigDecimal longitude,
        ListingStatusResponse status
) {
}
