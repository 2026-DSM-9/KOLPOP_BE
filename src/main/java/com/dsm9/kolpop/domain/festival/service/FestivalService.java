package com.dsm9.kolpop.domain.festival.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.dsm9.kolpop.domain.festival.client.PublicFestivalClient;
import com.dsm9.kolpop.domain.festival.dto.FestivalDetailResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalListResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalSummaryResponse;
import com.dsm9.kolpop.domain.festival.dto.PublicFestivalItem;
import com.dsm9.kolpop.domain.listing.dto.ListingStatusResponse;
import com.dsm9.kolpop.domain.listing.dto.ListingSummaryResponse;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;
import com.dsm9.kolpop.domain.listing.repository.ListingLikeCount;
import com.dsm9.kolpop.domain.listing.repository.ListingLikeRepository;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.global.exception.BusinessException;

@Service
public class FestivalService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
    private static final double DEFAULT_NEARBY_RADIUS_KM = 3.0;
    private static final int DEFAULT_RESERVATION_COUNT = 0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final PublicFestivalClient publicFestivalClient;
    private final ListingRepository listingRepository;
    private final ListingLikeRepository listingLikeRepository;

    public FestivalService(
            PublicFestivalClient publicFestivalClient,
            ListingRepository listingRepository,
            ListingLikeRepository listingLikeRepository
    ) {
        this.publicFestivalClient = publicFestivalClient;
        this.listingRepository = listingRepository;
        this.listingLikeRepository = listingLikeRepository;
    }

    public FestivalListResponse getFestivals(
            String keyword,
            String region,
            LocalDate from,
            LocalDate to,
            Integer page,
            Integer size
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        List<FestivalSummaryResponse> filtered = publicFestivalClient.fetchAll()
                .stream()
                .filter(this::hasRequiredFields)
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> matchesRegion(item, region))
                .filter(item -> matchesPeriod(item, from, to))
                .sorted(Comparator.comparing(this::parseStartDate).thenComparing(this::normalizeName))
                .map(this::toSummaryResponse)
                .toList();

        int fromIndex = Math.min(normalizedPage * normalizedSize, filtered.size());
        int toIndex = Math.min(fromIndex + normalizedSize, filtered.size());

        return new FestivalListResponse(
                filtered.size(),
                normalizedPage,
                normalizedSize,
                filtered.subList(fromIndex, toIndex)
        );
    }

    public List<FestivalSummaryResponse> getUpcomingFestivals(Integer limit) {
        int normalizedLimit = normalizeSize(limit == null ? 5 : limit);
        LocalDate today = today();

        return publicFestivalClient.fetchAll()
                .stream()
                .filter(this::hasRequiredFields)
                .filter(item -> !parseEndDate(item).isBefore(today))
                .sorted(Comparator.comparing(this::parseStartDate).thenComparing(this::normalizeName))
                .limit(normalizedLimit)
                .map(this::toSummaryResponse)
                .toList();
    }

    public List<FestivalSummaryResponse> getFestivalsByDate(LocalDate date) {
        if (date == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FESTIVAL_DATE_REQUIRED", "조회할 날짜를 입력해주세요.");
        }

        return publicFestivalClient.fetchAll()
                .stream()
                .filter(this::hasRequiredFields)
                .filter(item -> containsDate(item, date))
                .sorted(Comparator.comparing(this::parseStartDate).thenComparing(this::normalizeName))
                .map(this::toSummaryResponse)
                .toList();
    }

    public FestivalDetailResponse getFestivalDetail(String festivalId) {
        return publicFestivalClient.fetchAll()
                .stream()
                .filter(this::hasRequiredFields)
                .filter(item -> buildId(item).equals(festivalId))
                .findFirst()
                .map(this::toDetailResponse)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "FESTIVAL_NOT_FOUND",
                        "축제 정보를 찾을 수 없습니다."
                ));
    }

    private FestivalSummaryResponse toSummaryResponse(PublicFestivalItem item) {
        LocalDate startDate = parseStartDate(item);
        LocalDate endDate = parseEndDate(item);

        return new FestivalSummaryResponse(
                buildId(item),
                normalize(item.name()),
                normalize(item.place()),
                buildAddress(item),
                extractRegion(item),
                startDate,
                endDate,
                resolveStatus(startDate, endDate),
                resolveDDay(startDate, endDate),
                getNearbyListings(item).size(),
                parseDecimal(item.latitude()),
                parseDecimal(item.longitude())
        );
    }

    private FestivalDetailResponse toDetailResponse(PublicFestivalItem item) {
        LocalDate startDate = parseStartDate(item);
        LocalDate endDate = parseEndDate(item);

        return new FestivalDetailResponse(
                buildId(item),
                normalize(item.name()),
                normalize(item.place()),
                normalize(item.content()),
                buildAddress(item),
                normalize(item.roadAddress()),
                normalize(item.lotAddress()),
                extractRegion(item),
                startDate,
                endDate,
                resolveStatus(startDate, endDate),
                resolveDDay(startDate, endDate),
                normalize(item.managingAgency()),
                normalize(item.hostAgency()),
                normalize(item.sponsorAgency()),
                normalize(item.phoneNumber()),
                normalizeHomepage(item.homepageUrl()),
                normalize(item.relatedInfo()),
                parseDecimal(item.latitude()),
                parseDecimal(item.longitude()),
                parseDateOrNull(item.referenceDate()),
                toListingSummaryResponses(getNearbyListings(item))
        );
    }

    private boolean hasRequiredFields(PublicFestivalItem item) {
        return item != null
                && !isBlank(item.name())
                && parseDateOrNull(item.startDate()) != null
                && parseDateOrNull(item.endDate()) != null;
    }

    private boolean matchesKeyword(PublicFestivalItem item, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return Stream.of(item.name(), item.place(), item.content(), item.roadAddress(), item.lotAddress())
                .filter(value -> !isBlank(value))
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(normalizedKeyword));
    }

    private boolean matchesRegion(PublicFestivalItem item, String region) {
        if (isBlank(region)) {
            return true;
        }

        String normalizedRegion = region.trim().toLowerCase(Locale.ROOT);
        return Stream.of(extractRegion(item), item.roadAddress(), item.lotAddress(), item.place())
                .filter(value -> !isBlank(value))
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(normalizedRegion));
    }

    private boolean matchesPeriod(PublicFestivalItem item, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }

        LocalDate startDate = parseStartDate(item);
        LocalDate endDate = parseEndDate(item);
        LocalDate queryStart = from == null ? LocalDate.MIN : from;
        LocalDate queryEnd = to == null ? LocalDate.MAX : to;

        if (queryStart.isAfter(queryEnd)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FESTIVAL_PERIOD", "조회 시작일은 종료일보다 늦을 수 없습니다.");
        }

        return !endDate.isBefore(queryStart) && !startDate.isAfter(queryEnd);
    }

    private boolean containsDate(PublicFestivalItem item, LocalDate date) {
        return !date.isBefore(parseStartDate(item)) && !date.isAfter(parseEndDate(item));
    }

    private List<Listing> getNearbyListings(PublicFestivalItem item) {
        BigDecimal latitude = parseDecimal(item.latitude());
        BigDecimal longitude = parseDecimal(item.longitude());
        if (latitude == null || longitude == null) {
            return Collections.emptyList();
        }

        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();
        double latDelta = DEFAULT_NEARBY_RADIUS_KM / 111.32;
        double cosLat = Math.cos(Math.toRadians(lat));
        double lonDelta = Math.abs(cosLat) < 0.000001
                ? 180.0
                : DEFAULT_NEARBY_RADIUS_KM / (111.32 * Math.abs(cosLat));

        return listingRepository.findAllByStatusAndLatitudeBetweenAndLongitudeBetweenOrderByCreatedAtDesc(
                        ListingStatus.RECRUITING,
                        BigDecimal.valueOf(lat - latDelta),
                        BigDecimal.valueOf(lat + latDelta),
                        BigDecimal.valueOf(lon - lonDelta),
                        BigDecimal.valueOf(lon + lonDelta)
                )
                .stream()
                .filter(listing -> distanceKm(lat, lon, listing.getLatitude().doubleValue(), listing.getLongitude().doubleValue())
                        <= DEFAULT_NEARBY_RADIUS_KM)
                .toList();
    }

    private List<ListingSummaryResponse> toListingSummaryResponses(List<Listing> listings) {
        Map<Long, Long> likeCounts = getLikeCounts(listings);
        return listings.stream()
                .map(listing -> toListingSummaryResponse(listing, likeCounts.getOrDefault(listing.getId(), 0L)))
                .toList();
    }

    private ListingSummaryResponse toListingSummaryResponse(Listing listing, Long likeCount) {
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
                new ListingStatusResponse(listing.getStatus().getCode(), listing.getStatus().getLabel())
        );
    }

    private Map<Long, Long> getLikeCounts(List<Listing> listings) {
        if (listings.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> listingIds = listings.stream()
                .map(Listing::getId)
                .toList();

        return listingLikeRepository.countByListingIds(listingIds)
                .stream()
                .collect(Collectors.toMap(ListingLikeCount::getListingId, ListingLikeCount::getLikeCount));
    }

    private String getThumbnailUrl(Listing listing) {
        if (listing.getImageUrls().isEmpty()) {
            return null;
        }
        return listing.getImageUrls().getFirst();
    }

    private String buildFullAddress(Listing listing) {
        if (listing.getDetailAddress() == null || listing.getDetailAddress().isBlank()) {
            return listing.getAddress();
        }
        return listing.getAddress() + " " + listing.getDetailAddress();
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private LocalDate parseStartDate(PublicFestivalItem item) {
        return parseDateOrNull(item.startDate());
    }

    private LocalDate parseEndDate(PublicFestivalItem item) {
        return parseDateOrNull(item.endDate());
    }

    private LocalDate parseDateOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (Exception exception) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildId(PublicFestivalItem item) {
        String rawId = String.join("|",
                normalize(item.name()),
                normalize(item.startDate()),
                normalize(item.endDate()),
                normalize(item.latitude()),
                normalize(item.longitude())
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 22);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private String resolveStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = today();
        if (today.isBefore(startDate)) {
            return "UPCOMING";
        }
        if (today.isAfter(endDate)) {
            return "ENDED";
        }
        return "ONGOING";
    }

    private long resolveDDay(LocalDate startDate, LocalDate endDate) {
        LocalDate today = today();
        if (today.isAfter(endDate)) {
            return 0;
        }
        return Math.max(ChronoUnit.DAYS.between(today, startDate), 0);
    }

    private String buildAddress(PublicFestivalItem item) {
        if (!isBlank(item.roadAddress())) {
            return normalize(item.roadAddress());
        }
        return normalize(item.lotAddress());
    }

    private String extractRegion(PublicFestivalItem item) {
        String address = buildAddress(item);
        if (isBlank(address)) {
            return null;
        }

        String[] parts = address.split("\\s+");
        return parts.length == 0 ? null : parts[0];
    }

    private String normalizeHomepage(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        return "https://" + normalized;
    }

    private String normalizeName(PublicFestivalItem item) {
        return normalize(item.name());
    }

    private String normalize(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        return Math.max(page, DEFAULT_PAGE);
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    private LocalDate today() {
        return LocalDate.now(APP_ZONE);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
