package com.dsm9.kolpop.domain.listing.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.listing.dto.CloseListingResponse;
import com.dsm9.kolpop.domain.listing.dto.CreateListingRequest;
import com.dsm9.kolpop.domain.listing.dto.CreateListingResponse;
import com.dsm9.kolpop.domain.listing.dto.LikeListingResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingAddressSuggestionResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingDiscoveryResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingDetailResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingListResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingMapItemResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingMapResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingStatusResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingSummaryResponse;
import com.dsm9.kolpop.domain.listing.dto.UpdateListingResponse;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingLike;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;
import com.dsm9.kolpop.domain.listing.repository.ListingLikeCount;
import com.dsm9.kolpop.domain.listing.repository.ListingLikeRepository;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

@Service
public class ListingService {

    private static final int DEFAULT_RESERVATION_COUNT = 0;
    private static final int DEFAULT_ADDRESS_SUGGESTION_LIMIT = 5;
    private static final int MAX_ADDRESS_SUGGESTION_LIMIT = 20;
    private static final String POPULAR_SORT = "popular";

    private final ListingRepository listingRepository;
    private final ListingLikeRepository listingLikeRepository;
    private final UserRepository userRepository;

    @Autowired
    public ListingService(
            ListingRepository listingRepository,
            ListingLikeRepository listingLikeRepository,
            UserRepository userRepository
    ) {
        this.listingRepository = listingRepository;
        this.listingLikeRepository = listingLikeRepository;
        this.userRepository = userRepository;
    }

    ListingService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.listingLikeRepository = null;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreateListingResponse createListing(Long userId, CreateListingRequest request) {
        User landlord = getLandlord(userId);
        validatePeriod(request);
        List<String> imageUrls = normalizeImageUrls(request.imageUrls());

        Listing listing = new Listing(
                landlord,
                request.title().trim(),
                imageUrls,
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
        List<String> imageUrls = normalizeImageUrls(request.imageUrls());

        listing.update(
                request.title().trim(),
                imageUrls,
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
        return getListingsForMap(minLatitude, maxLatitude, minLongitude, maxLongitude, null);
    }

    @Transactional(readOnly = true)
    public ListingMapResponse getListingsForMap(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude,
            String keyword
    ) {
        List<Listing> foundListings = findMapListings(minLatitude, maxLatitude, minLongitude, maxLongitude, keyword);
        return toMapResponse(foundListings);
    }

    @Transactional(readOnly = true)
    public List<ListingAddressSuggestionResponse> getAddressSuggestions(String keyword, Integer limit) {
        String normalizedKeyword = normalizeNullable(keyword);
        if (normalizedKeyword == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SEARCH_KEYWORD_REQUIRED", "검색어를 입력해주세요.");
        }

        int normalizedLimit = normalizeAddressSuggestionLimit(limit);
        Map<String, ListingAddressSuggestionResponse> suggestions = new LinkedHashMap<>();

        for (Listing listing : listingRepository.findAllByStatusAndKeywordOrderByCreatedAtDesc(
                ListingStatus.RECRUITING,
                normalizedKeyword
        )) {
            ListingAddressSuggestionResponse suggestion = new ListingAddressSuggestionResponse(
                    listing.getAddress(),
                    listing.getDetailAddress(),
                    buildFullAddress(listing)
            );
            suggestions.putIfAbsent(suggestion.fullAddress(), suggestion);
            if (suggestions.size() >= normalizedLimit) {
                break;
            }
        }

        return List.copyOf(suggestions.values());
    }

    @Transactional(readOnly = true)
    public ListingListResponse getListings(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude,
            String sort
    ) {
        return getListings(minLatitude, maxLatitude, minLongitude, maxLongitude, null, sort);
    }

    @Transactional(readOnly = true)
    public ListingListResponse getListings(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude,
            String keyword,
            String sort
    ) {
        List<Listing> foundListings = findListings(minLatitude, maxLatitude, minLongitude, maxLongitude, keyword, sort);
        Map<Long, Long> likeCounts = getLikeCounts(foundListings);
        return toListResponse(foundListings, likeCounts);
    }

    @Transactional(readOnly = true)
    public ListingListResponse getListings(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude
    ) {
        return getListings(minLatitude, maxLatitude, minLongitude, maxLongitude, null);
    }

    @Transactional(readOnly = true)
    public ListingListResponse getMyListings(Long userId) {
        User landlord = getLandlord(userId);
        List<Listing> ownedListings = listingRepository.findAllByLandlordIdOrderByCreatedAtDesc(landlord.getId());
        Map<Long, Long> likeCounts = getLikeCounts(ownedListings);
        return toListResponse(ownedListings, likeCounts);
    }

    @Transactional(readOnly = true)
    public ListingListResponse getLikedListings(Long userId) {
        User user = getUser(userId);
        List<Listing> likedListings = listingLikeRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ListingLike::getListing)
                .toList();
        Map<Long, Long> likeCounts = getLikeCounts(likedListings);

        return toListResponse(likedListings, likeCounts);
    }

    @Transactional(readOnly = true)
    public ListingDiscoveryResponse getListingsForDiscovery(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude,
            String keyword,
            String sort
    ) {
        List<Listing> foundListings = findListings(minLatitude, maxLatitude, minLongitude, maxLongitude, keyword, sort);
        Map<Long, Long> likeCounts = getLikeCounts(foundListings);

        return new ListingDiscoveryResponse(
                toMapResponse(foundListings),
                toListResponse(foundListings, likeCounts)
        );
    }

    @Transactional
    public ListingDetailResponse getListingDetail(Long listingId) {
        Listing listing = getListing(listingId);
        listing.increaseViewCount();

        return toDetailResponse(listing);
    }

    @Transactional
    public LikeListingResponse likeListing(Long userId, Long listingId) {
        User user = getUser(userId);
        Listing listing = getListing(listingId);

        if (!listingLikeRepository.existsByListingIdAndUserId(listingId, user.getId())) {
            listingLikeRepository.save(new ListingLike(listing, user, LocalDateTime.now()));
        }

        return new LikeListingResponse(listingId, listingLikeRepository.countByListingId(listingId), true);
    }

    @Transactional
    public LikeListingResponse unlikeListing(Long userId, Long listingId) {
        User user = getUser(userId);
        Listing listing = getListing(listingId);

        listingLikeRepository.findByListingIdAndUserId(listing.getId(), user.getId())
                .ifPresent(listingLikeRepository::delete);

        return new LikeListingResponse(listingId, listingLikeRepository.countByListingId(listingId), false);
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

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    private User getLandlord(Long userId) {
        User user = getUser(userId);

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
            BigDecimal maxLongitude,
            String keyword,
            String sort
    ) {
        boolean hasNoBounds = minLatitude == null
                && maxLatitude == null
                && minLongitude == null
                && maxLongitude == null;
        String normalizedKeyword = normalizeNullable(keyword);
        boolean hasKeyword = normalizedKeyword != null;
        boolean popularSort = POPULAR_SORT.equalsIgnoreCase(normalizeNullable(sort));

        if (hasNoBounds) {
            if (hasKeyword) {
                return listingRepository.findAllByStatusAndKeywordOrderByCreatedAtDesc(
                        ListingStatus.RECRUITING,
                        normalizedKeyword
                );
            }
            if (popularSort) {
                return listingRepository.findAllByStatusOrderByLikeCountDesc(ListingStatus.RECRUITING);
            }
            return listingRepository.findAllByStatusOrderByCreatedAtDesc(ListingStatus.RECRUITING);
        }

        if (minLatitude == null || maxLatitude == null || minLongitude == null || maxLongitude == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MAP_BOUNDS_REQUIRED", "지도 범위 조회 시 위도/경도 범위를 모두 전달해야 합니다.");
        }

        validateBounds(minLatitude, maxLatitude, minLongitude, maxLongitude);
        if (hasKeyword) {
            return listingRepository.findAllByStatusAndBoundsAndKeywordOrderByCreatedAtDesc(
                    ListingStatus.RECRUITING,
                    minLatitude,
                    maxLatitude,
                    minLongitude,
                    maxLongitude,
                    normalizedKeyword
            );
        }
        if (popularSort) {
            return listingRepository.findAllByStatusAndBoundsOrderByLikeCountDesc(
                    ListingStatus.RECRUITING,
                    minLatitude,
                    maxLatitude,
                    minLongitude,
                    maxLongitude
            );
        }
        return listingRepository.findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                ListingStatus.RECRUITING,
                minLatitude,
                maxLatitude,
                minLongitude,
                maxLongitude
        );
    }

    private List<Listing> findMapListings(
            BigDecimal minLatitude,
            BigDecimal maxLatitude,
            BigDecimal minLongitude,
            BigDecimal maxLongitude,
            String keyword
    ) {
        String normalizedKeyword = normalizeNullable(keyword);
        boolean hasKeyword = normalizedKeyword != null;
        boolean hasNoBounds = minLatitude == null
                && maxLatitude == null
                && minLongitude == null
                && maxLongitude == null;

        if (hasNoBounds) {
            if (!hasKeyword) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "MAP_BOUNDS_OR_KEYWORD_REQUIRED", "지도 조회에는 좌표 범위 또는 검색어가 필요합니다.");
            }
            return listingRepository.findAllByStatusAndKeywordOrderByCreatedAtDesc(
                    ListingStatus.RECRUITING,
                    normalizedKeyword
            );
        }

        if (minLatitude == null || maxLatitude == null || minLongitude == null || maxLongitude == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MAP_BOUNDS_REQUIRED", "지도 범위 조회 시 위도/경도 범위를 모두 전달해야 합니다.");
        }

        validateBounds(minLatitude, maxLatitude, minLongitude, maxLongitude);

        if (hasKeyword) {
            return listingRepository.findAllByStatusAndBoundsAndKeywordOrderByCreatedAtDesc(
                    ListingStatus.RECRUITING,
                    minLatitude,
                    maxLatitude,
                    minLongitude,
                    maxLongitude,
                    normalizedKeyword
            );
        }

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

    private ListingMapResponse toMapResponse(List<Listing> foundListings) {
        List<ListingMapItemResponse> listings = foundListings.stream()
                .map(this::toMapItemResponse)
                .toList();

        return new ListingMapResponse(listings.size(), listings);
    }

    private ListingListResponse toListResponse(List<Listing> foundListings, Map<Long, Long> likeCounts) {
        List<ListingSummaryResponse> listings = foundListings.stream()
                .map(listing -> toSummaryResponse(listing, likeCounts.getOrDefault(listing.getId(), 0L)))
                .toList();

        return new ListingListResponse(listings.size(), listings);
    }

    private ListingSummaryResponse toSummaryResponse(Listing listing, Long likeCount) {
        return new ListingSummaryResponse(
                listing.getId(),
                listing.getTitle(),
                getThumbnailUrl(listing),
                buildFullAddress(listing),
                listing.getDailyFee(),
                listing.getDeposit(),
                listing.getArea(),
                likeCount,
                listing.getViewCount(),
                DEFAULT_RESERVATION_COUNT,
                toStatusResponse(listing)
        );
    }

    private ListingDetailResponse toDetailResponse(Listing listing) {
        Long likeCount = getLikeCount(listing.getId());

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
                likeCount,
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

    private Map<Long, Long> getLikeCounts(List<Listing> listings) {
        if (listingLikeRepository == null || listings.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> listingIds = listings.stream()
                .map(Listing::getId)
                .toList();

        return listingLikeRepository.countByListingIds(listingIds)
                .stream()
                .collect(Collectors.toMap(ListingLikeCount::getListingId, ListingLikeCount::getLikeCount));
    }

    private Long getLikeCount(Long listingId) {
        if (listingLikeRepository == null) {
            return 0L;
        }
        return listingLikeRepository.countByListingId(listingId);
    }

    private ListingStatusResponse toStatusResponse(Listing listing) {
        return new ListingStatusResponse(listing.getStatus().getCode(), listing.getStatus().getLabel());
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        List<String> normalized = normalizeList(imageUrls);
        if (normalized.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "IMAGE_URL_REQUIRED", "이미지는 최소 1장 이상 필요합니다.");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private int normalizeAddressSuggestionLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_ADDRESS_SUGGESTION_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_ADDRESS_SUGGESTION_LIMIT);
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
