package com.dsm9.kolpop.domain.festival.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dsm9.kolpop.domain.festival.dto.FestivalDetailResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalListResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalSummaryResponse;
import com.dsm9.kolpop.domain.festival.service.FestivalService;
import com.dsm9.kolpop.global.exception.BusinessException;
import com.dsm9.kolpop.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/festivals")
@Tag(name = "Festivals", description = "지역 축제 조회 API")
public class FestivalController {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<DateTimeFormatter> LOCAL_DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );
    private static final List<DateTimeFormatter> OFFSET_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")
    );
    private static final List<DateTimeFormatter> LOCAL_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    );

    private final FestivalService festivalService;

    public FestivalController(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping
    @Operation(summary = "지역 축제 목록 조회")
    public ApiResponse<FestivalListResponse> getFestivals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(festivalService.getFestivals(
                keyword,
                region,
                parseOptionalDate(from, "from"),
                parseOptionalDate(to, "to"),
                page,
                size
        ));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "다가오는 지역 축제 조회")
    public ApiResponse<List<FestivalSummaryResponse>> getUpcomingFestivals(
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(festivalService.getUpcomingFestivals(limit));
    }

    @GetMapping("/date")
    @Operation(summary = "날짜별 지역 축제 조회")
    public ApiResponse<List<FestivalSummaryResponse>> getFestivalsByDate(
            @RequestParam String date
    ) {
        return ApiResponse.success(festivalService.getFestivalsByDate(parseRequiredDate(date, "date")));
    }

    @GetMapping("/{festivalId}")
    @Operation(summary = "지역 축제 상세 조회")
    public ApiResponse<FestivalDetailResponse> getFestivalDetail(@PathVariable String festivalId) {
        return ApiResponse.success(festivalService.getFestivalDetail(festivalId));
    }

    private LocalDate parseOptionalDate(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDate(value, parameterName);
    }

    private LocalDate parseRequiredDate(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw invalidDate(parameterName);
        }
        return parseDate(value, parameterName);
    }

    private LocalDate parseDate(String value, String parameterName) {
        String trimmed = value.trim();

        return Stream.<DateParser>of(
                        this::parseLocalDate,
                        this::parseOffsetDateTime,
                        this::parseLocalDateTime
                )
                .map(parser -> parser.parse(trimmed))
                .filter(date -> date != null)
                .findFirst()
                .orElseThrow(() -> invalidDate(parameterName));
    }

    private LocalDate parseLocalDate(String value) {
        for (DateTimeFormatter formatter : LOCAL_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalDate parseOffsetDateTime(String value) {
        for (DateTimeFormatter formatter : OFFSET_DATE_TIME_FORMATTERS) {
            try {
                return OffsetDateTime.parse(value, formatter)
                        .atZoneSameInstant(APP_ZONE)
                        .toLocalDate();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalDate parseLocalDateTime(String value) {
        for (DateTimeFormatter formatter : LOCAL_DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private BusinessException invalidDate(String parameterName) {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_FESTIVAL_DATE",
                String.format(Locale.ROOT, "%s must be a date like 2026-07-25.", parameterName)
        );
    }

    @FunctionalInterface
    private interface DateParser {
        LocalDate parse(String value);
    }
}
