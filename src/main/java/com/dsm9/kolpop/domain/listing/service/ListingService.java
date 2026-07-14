package com.dsm9.kolpop.domain.listing.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.listing.dto.CloseListingResponse;
import com.dsm9.kolpop.domain.listing.dto.CreateListingRequest;
import com.dsm9.kolpop.domain.listing.dto.CreateListingResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingDetailResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingListResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingMapItemResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingMapResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingStatusResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingSummaryResponse;
import com.dsm9.kolpop.domain.listing.dto.UpdateListingResponse;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

@Service
public class ListingService {

    private static final int DEFAULT_RESERVATION_COUNT = 0;

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateListingResponse createListing(Long userId, CreateListingRequest request) {
        User landlord = getLandlord(userId);
        validatePeriod(request);

        Listing listing = new Listing(
                landlord,
                request.title().trim(),
                normalizeList(request.imageUrls()),
                request.address().trim(),
                normalizeNullable(request.detailAddress()),
                request.latitude(),
                request.longitude(),
                request.dailyFee(),
                request.deposit(),
                request.area(),
                normalizeList(request.facilities()),
                normalizeList(request.industryRestrictions()),
                normalizeList(request.additionalRestrictions()),
                request.operatingStartDate(),
                request.operatingEndDate(),
                request.minOperatingDays(),
                request.maxOperatingDays(),
                request.description().trim(),
                normalizeList(request.hashtags()),
                LocalDateTime.now()
        );

        Listing savedListing = listingRepository.save(listing);
        return new CreateListingResponse(savedListing.getId());
    }

    @Transactional
    public UpdateListingResponse updateListing(Long userId, Long listingId, CreateListingRequest request) {
        User landlord = getLandlord(userId);
        Listing listing = getOwnedListing(landlord, listingId, "LISTING_UPDATE_FORBIDDEN", "본인이 등록한 매물만 수정할 수 있습니다.");
        validatePeriod(request);

        listing.update(
                request.title().trim(),
                normalizeList(request.imageUrls()),
                request.address().trim(),
                normalizeNullable(request.detailAddress()),
                request.latitude(),
                request.longitude(),
                request.dailyFee(),
                request.deposit(),
                request.area(),
                normalizeList(request.facilities()),
                normalizeList(request.industryRestrictions()),
                normalizeList(request.additionalRestrictions()),
                request.operatingStartDate(),
                request.operatingEndDate(),
                request.minOperatingDays(),
                request.maxOperatingDays(),
                request.description().trim(),
                normalizeList(request.hashtags())
        );

        return new UpdateListingResponse(listing.getId());
    }

    @Transactional(readOnly = true)
    public ListingMapResponse getListingsForMap(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude
    ) {
        validateBounds(minLatitude, maxLatitude, minLongitude, maxLongitude);

        List<ListingMapItemResponse> listings = listingRepository
                .findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                        ListingStatus.RECRUITING,
                        minLatitude,
                        maxLatitude,
                        minLongitude,
                        maxLongitude
                )
                .stream()
                .map(this::toMapItemResponse)
                .toList();

        return new ListingMapResponse(listings.size(), listings);
    }

    @Transactional(readOnly = true)
    public ListingListResponse getListings(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude
    ) {
        List<ListingSummaryResponse> listings = findListings(minLatitude, maxLatitude, minLongitude, maxLongitude)
                .stream()
                .map(this::toSummaryResponse)
                .toList();

        return new ListingListResponse(listings.size(), listings);
    }

    @Transactional(readOnly = true)
    public ListingListResponse getMyListings(Long userId) {
        User landlord = getLandlord(userId);
        List<ListingSummaryResponse> listings = listingRepository.findAllByLandlordIdOrderByCreatedAtDesc(landlord.getId())
                .stream()
                .map(this::toSummaryResponse)
                .toList();

        return new ListingListResponse(listings.size(), listings);
    }

    @Transactional
    public ListingDetailResponse getListingDetail(Long listingId) {
        Listing listing = getListing(listingId);
        listing.increaseViewCount();

        return toDetailResponse(listing);
    }

    @Transactional
    public CloseListingResponse closeListing(Long userId, Long listingId) {
        User landlord = getLandlord(userId);
        Listing listing = getOwnedListing(landlord, listingId, "LISTING_CLOSE_FORBIDDEN", "본인이 등록한 매물만 모집 종료할 수 있습니다.");

        listing.closeRecruitment();

        return new CloseListingResponse(listing.getId(), toStatusResponse(listing));
    }

    @Transactional
    public void deleteListing(Long userId, Long listingId) {
        User landlord = getLandlord(userId);
        Listing listing = getOwnedListing(landlord, listingId, "LISTING_DELETE_FORBIDDEN", "본인이 등록한 매물만 삭제할 수 있습니다.");

        listingRepository.delete(listing);
    }

    private Listing getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", "매물을 찾을 수 없습니다."));
    }

    private User getLandlord(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));

        if (user.getRole() != UserRole.LANDLORD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "LANDLORD_ONLY", "임대인 회원만 매물을 등록하거나 삭제할 수 있습니다.");
        }

        return user;
    }

    private Listing getOwnedListing(User landlord, Long listingId, String errorCode, String errorMessage) {
        Listing listing = getListing(listingId);

        if (!listing.getLandlord().getId().equals(landlord.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, errorCode, errorMessage);
        }

        return listing;
    }

    private void validatePeriod(CreateListingRequest request) {
        if (request.operatingStartDate().isAfter(request.operatingEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_OPERATING_PERIOD", "운영 가능 시작일은 종료일보다 늦을 수 없습니다.");
        }

        if (request.minOperatingDays() > request.maxOperatingDays()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_OPERATING_DAYS", "최소 운영 일수는 최대 운영 일수보다 클 수 없습니다.");
        }
    }

    private void validateBounds(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude
    ) {
        if (minLatitude.compareTo(maxLatitude) > 0 || minLongitude.compareTo(maxLongitude) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_MAP_BOUNDS", "지도 조회 범위가 올바르지 않습니다.");
        }
    }

    private List<Listing> findListings(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude
    ) {
        boolean hasNoBounds = minLatitude == null
                && maxLatitude == null
                && minLongitude == null
                && maxLongitude == null;

        if (hasNoBounds) {
            return listingRepository.findAllByStatusOrderByCreatedAtDesc(ListingStatus.RECRUITING);
        }

        if (minLatitude == null || maxLatitude == null || minLongitude == null || maxLongitude == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MAP_BOUNDS_REQUIRED", "지도 범위 조회 시 위도/경도 범위를 모두 전달해야 합니다.");
        }

        validateBounds(minLatitude, maxLatitude, minLongitude, maxLongitude);
        return listingRepository.findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                ListingStatus.RECRUITING,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
        );
    }

    private ListingMapItemResponse toMapItemResponse(Listing listing) {
        return new ListingMapItemResponse(
                listing.getId(),
                listing.getTitle(),
                buildFullAddress(listing),
                listing.getLatitude(),
                listing.getLongitude(),
                listing.getDeposit(),
                listing.getDailyFee(),
                toStatusResponse(listing)
        );
    }

    private ListingSummaryResponse toSummaryResponse(Listing listing) {
        return new ListingSummaryResponse(
                listing.getId(),
                listing.getTitle(),
                getThumbnailUrl(listing),
                buildFullAddress(listing),
                listing.getDailyFee(),
                listing.getDeposit(),
                listing.getArea(),
                listing.getViewCount(),
                DEFAULT_RESERVATION_COUNT,
                toStatusResponse(listing)
        );
    }

    private ListingDetailResponse toDetailResponse(Listing listing) {
        return new ListingDetailResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getAddress(),
                listing.getDetailAddress(),
                listing.getImageUrls(),
                listing.getDailyFee(),
                listing.getDeposit(),
                listing.getArea(),
                listing.getDailyFee() * 7,
                listing.getLandlord().getName(),
                listing.getViewCount(),
                DEFAULT_RESERVATION_COUNT,
                listing.getOperatingStartDate(),
                listing.getOperatingEndDate(),
                listing.getMinOperatingDays(),
                listing.getMaxOperatingDays(),
                listing.getFacilities(),
                listing.getIndustryRestrictions(),
                listing.getAdditionalRestrictions(),
                listing.getDescription(),
                listing.getHashtags(),
                listing.getLatitude(),
                listing.getLongitude(),
                toStatusResponse(listing)
        );
    }

    private ListingStatusResponse toStatusResponse(Listing listing) {
        return new ListingStatusResponse(listing.getStatus().getCode(), listing.getStatus().getLabel());
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String buildFullAddress(Listing listing) {
        if (listing.getDetailAddress() == null || listing.getDetailAddress().isBlank()) {
            return listing.getAddress();
        }
        return listing.getAddress() + " " + listing.getDetailAddress();
    }

    private String getThumbnailUrl(Listing listing) {
        if (listing.getImageUrls().isEmpty()) {
            return null;
        }
        return listing.getImageUrls().getFirst();
    }
}
