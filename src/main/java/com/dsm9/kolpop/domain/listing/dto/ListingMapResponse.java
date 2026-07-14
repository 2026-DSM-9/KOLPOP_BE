package com.dsm9.kolpop.domain.listing.dto;

import java.util.List;

public record ListingMapResponse(
        int count,
        List<ListingMapItemResponse> listings
) {
}
