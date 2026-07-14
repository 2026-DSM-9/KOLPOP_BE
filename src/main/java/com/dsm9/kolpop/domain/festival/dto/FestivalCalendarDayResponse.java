package com.dsm9.kolpop.domain.festival.dto;

import java.time.LocalDate;
import java.util.List;

public record FestivalCalendarDayResponse(
        LocalDate date,
        List<FestivalSummaryResponse> festivals
) {
}
