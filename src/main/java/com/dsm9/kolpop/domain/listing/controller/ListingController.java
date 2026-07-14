package com.dsm9.kolpop.domain.listing.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dsm9.kolpop.domain.listing.dto.CreateListingRequest;
import com.dsm9.kolpop.domain.listing.dto.CreateListingResponse;
import com.dsm9.kolpop.domain.listing.dto.CloseListingResponse;
import com.dsm9.kolpop.domain.listing.dto.LikeListingResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingAddressSuggestionResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingDiscoveryResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingDetailResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingListResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingMapResponse;
import com.dsm9.kolpop.domain.listing.dto.UpdateListingResponse;
import com.dsm9.kolpop.domain.listing.service.ListingService;
import com.dsm9.kolpop.global.exception.BusinessException;
import com.dsm9.kolpop.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/listings")
@Tag(name = "매물", description = "매물 등록, 조회, 수정, 삭제 API")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/map")
    @Operation(summary = "지도용 매물 조회")
    public ApiResponse<ListingMapResponse> getListingsForMap(
            @RequestParam(required = false) BigDecimal minLatitude,
            @RequestParam(required = false) BigDecimal maxLatitude,
            @RequestParam(required = false) BigDecimal minLongitude,
            @RequestParam(required = false) BigDecimal maxLongitude,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
                listingService.getListingsForMap(minLatitude, maxLatitude, minLongitude, maxLongitude, keyword)
        );
    }

    @GetMapping("/address-suggestions")
    @Operation(summary = "매물 주소 검색어 추천")
    public ApiResponse<List<ListingAddressSuggestionResponse>> getAddressSuggestions(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(listingService.getAddressSuggestions(keyword, limit));
    }

    @GetMapping
    @Operation(summary = "매물 목록 조회")
    public ApiResponse<ListingListResponse> getListings(
            @RequestParam(required = false) BigDecimal minLatitude,
            @RequestParam(required = false) BigDecimal maxLatitude,
            @RequestParam(required = false) BigDecimal minLongitude,
            @RequestParam(required = false) BigDecimal maxLongitude,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.success(
                listingService.getListings(minLatitude, maxLatitude, minLongitude, maxLongitude, keyword, sort)
        );
    }

    @GetMapping("/discovery")
    @Operation(summary = "지도 + 주변 매물 통합 조회")
    public ApiResponse<ListingDiscoveryResponse> getListingsForDiscovery(
            @RequestParam(required = false) BigDecimal minLatitude,
            @RequestParam(required = false) BigDecimal maxLatitude,
            @RequestParam(required = false) BigDecimal minLongitude,
            @RequestParam(required = false) BigDecimal maxLongitude,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.success(
                listingService.getListingsForDiscovery(minLatitude, maxLatitude, minLongitude, maxLongitude, keyword, sort)
        );
    }

    @GetMapping("/my")
    @Operation(summary = "내 매물 조회")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ListingListResponse> getMyListings(Authentication authentication) {
        return ApiResponse.success(listingService.getMyListings(extractUserId(authentication)));
    }

    @GetMapping("/liked")
    @Operation(summary = "찜한 매물 조회")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ListingListResponse> getLikedListings(Authentication authentication) {
        return ApiResponse.success(listingService.getLikedListings(extractUserId(authentication)));
    }

    @GetMapping("/{listingId}")
    @Operation(summary = "매물 상세 조회")
    public ApiResponse<ListingDetailResponse> getListingDetail(@PathVariable Long listingId) {
        return ApiResponse.success(listingService.getListingDetail(listingId));
    }

    @PostMapping("/{listingId}/likes")
    @Operation(summary = "매물 좋아요")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<LikeListingResponse> likeListing(
            @PathVariable Long listingId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                listingService.likeListing(extractUserId(authentication), listingId)
        );
    }

    @DeleteMapping("/{listingId}/likes")
    @Operation(summary = "매물 좋아요 취소")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<LikeListingResponse> unlikeListing(
            @PathVariable Long listingId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                listingService.unlikeListing(extractUserId(authentication), listingId)
        );
    }

    @PostMapping
    @Operation(summary = "매물 등록")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<CreateListingResponse>> createListing(
            @Valid @RequestBody CreateListingRequest request,
            Authentication authentication
    ) {
        CreateListingResponse response = listingService.createListing(extractUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{listingId}")
    @Operation(summary = "매물 수정")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UpdateListingResponse> updateListing(
            @PathVariable Long listingId,
            @Valid @RequestBody CreateListingRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                listingService.updateListing(extractUserId(authentication), listingId, request)
        );
    }

    @PatchMapping("/{listingId}/close")
    @Operation(summary = "매물 모집 종료")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CloseListingResponse> closeListing(
            @PathVariable Long listingId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                listingService.closeListing(extractUserId(authentication), listingId)
        );
    }

    @DeleteMapping("/{listingId}")
    @Operation(summary = "매물 삭제")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> deleteListing(
            @PathVariable Long listingId,
            Authentication authentication
    ) {
        listingService.deleteListing(extractUserId(authentication), listingId);
        return ApiResponse.success(null);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "인증이 필요합니다.");
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION", "인증 정보가 올바르지 않습니다.");
        }
    }
}
