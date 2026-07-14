package com.dsm9.kolpop.domain.festival.dto;

import java.util.List;

public record FestivalListResponse(
        int totalCount,
        int page,
        int size,
        List<FestivalSummaryResponse> festivals
) {
}
