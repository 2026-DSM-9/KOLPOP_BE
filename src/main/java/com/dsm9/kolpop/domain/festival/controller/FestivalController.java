package com.dsm9.kolpop.domain.festival.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dsm9.kolpop.domain.festival.dto.FestivalDetailResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalListResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalSummaryResponse;
import com.dsm9.kolpop.domain.festival.service.FestivalService;
import com.dsm9.kolpop.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/festivals")
@Tag(name = "Festivals", description = "지역 축제 조회 API")
public class FestivalController {

    private final FestivalService festivalService;

    public FestivalController(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping
    @Operation(summary = "지역 축제 목록 조회")
    public ApiResponse<FestivalListResponse> getFestivals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(festivalService.getFestivals(keyword, region, from, to, page, size));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "다가오는 지역 축제 조회")
    public ApiResponse<List<FestivalSummaryResponse>> getUpcomingFestivals(
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(festivalService.getUpcomingFestivals(limit));
    }

    @GetMapping("/{festivalId}")
    @Operation(summary = "지역 축제 상세 조회")
    public ApiResponse<FestivalDetailResponse> getFestivalDetail(@PathVariable String festivalId) {
        return ApiResponse.success(festivalService.getFestivalDetail(festivalId));
    }
}
