package com.dsm9.kolpop.domain.chat.dto;

import com.dsm9.kolpop.domain.listing.entity.Listing;

public record ChatListingResponse(
        Long listingId,
        String title,
        String thumbnailUrl
) {

    public static ChatListingResponse from(Listing listing) {
        if (listing == null) {
            return null;
        }
        String thumbnailUrl = listing.getImageUrls().isEmpty() ? null : listing.getImageUrls().getFirst();
        return new ChatListingResponse(listing.getId(), listing.getTitle(), thumbnailUrl);
    }
}
