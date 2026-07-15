package com.dsm9.kolpop.domain.festival.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dsm9.kolpop.domain.festival.dto.FestivalListResponse;
import com.dsm9.kolpop.domain.festival.service.FestivalService;

class FestivalControllerTests {

    @Test
    void getFestivalsAcceptsIosIsoDateTimeQueryParameters() {
        FestivalService festivalService = mock(FestivalService.class);
        FestivalController controller = new FestivalController(festivalService);
        FestivalListResponse emptyResponse = new FestivalListResponse(0, 0, 20, List.of());

        when(festivalService.getFestivals(
                null,
                null,
                LocalDate.of(2026, 7, 25),
                LocalDate.of(2026, 8, 1),
                null,
                null
        )).thenReturn(emptyResponse);

        controller.getFestivals(
                null,
                null,
                "2026-07-24T15:00:00.000Z",
                "2026-08-01T00:00:00+09:00",
                null,
                null
        );

        verify(festivalService).getFestivals(
                null,
                null,
                LocalDate.of(2026, 7, 25),
                LocalDate.of(2026, 8, 1),
                null,
                null
        );
    }

    @Test
    void getFestivalsByDateAcceptsIosIsoDateTimeQueryParameter() {
        FestivalService festivalService = mock(FestivalService.class);
        FestivalController controller = new FestivalController(festivalService);

        when(festivalService.getFestivalsByDate(LocalDate.of(2026, 7, 25))).thenReturn(List.of());

        controller.getFestivalsByDate("2026-07-24T15:00:00.000Z");

        verify(festivalService).getFestivalsByDate(LocalDate.of(2026, 7, 25));
    }

    @Test
    void getFestivalsByDateAcceptsSwiftDefaultDateDescription() {
        FestivalService festivalService = mock(FestivalService.class);
        FestivalController controller = new FestivalController(festivalService);

        when(festivalService.getFestivalsByDate(LocalDate.of(2026, 7, 25))).thenReturn(List.of());

        controller.getFestivalsByDate("2026-07-24 15:00:00 +0000");

        verify(festivalService).getFestivalsByDate(LocalDate.of(2026, 7, 25));
    }
}
