package com.dsm9.kolpop.domain.listing.dto;

public record CloseListingResponse(
        Long listingId,
        ListingStatusResponse status
) {
}
