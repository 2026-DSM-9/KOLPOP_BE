package com.dsm9.kolpop.domain.listing.dto;

public record LikeListingResponse(
        Long listingId,
        Long likeCount,
        boolean liked
) {
}
