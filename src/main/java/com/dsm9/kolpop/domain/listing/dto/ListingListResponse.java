package com.dsm9.kolpop.domain.listing.dto;

import java.util.List;

public record ListingListResponse(
        int count,
        List<ListingSummaryResponse> listings
) {
}
