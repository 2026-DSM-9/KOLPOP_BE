package com.dsm9.kolpop.domain.festival.dto;

import java.util.List;

public record PublicFestivalApiResponse(
        int currentCount,
        List<PublicFestivalItem> data,
        int matchCount,
        int page,
        int perPage,
        int totalCount
) {
}
