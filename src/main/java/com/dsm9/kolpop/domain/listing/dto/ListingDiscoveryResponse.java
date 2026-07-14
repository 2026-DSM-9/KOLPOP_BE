package com.dsm9.kolpop.domain.listing.dto;

public record ListingDiscoveryResponse(
        ListingMapResponse map,
        ListingListResponse nearbyListings
) {
}
