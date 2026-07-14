package com.dsm9.kolpop.domain.festival.dto;

import java.util.List;

public record FestivalCalendarResponse(
        int year,
        int month,
        List<FestivalCalendarDayResponse> days
) {
}
