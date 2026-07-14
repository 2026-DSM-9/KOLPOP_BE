package com.dsm9.kolpop.domain.listing.dto;

public record ListingAddressSuggestionResponse(
        String address,
        String detailAddress,
        String fullAddress
) {
}
