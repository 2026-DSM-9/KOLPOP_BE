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
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.dsm9.kolpop.domain.festival.client.PublicFestivalClient;
import com.dsm9.kolpop.domain.festival.dto.FestivalCalendarDayResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalCalendarResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalDetailResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalListResponse;
import com.dsm9.kolpop.domain.festival.dto.FestivalSummaryResponse;
import com.dsm9.kolpop.domain.festival.dto.PublicFestivalItem;
import com.dsm9.kolpop.global.exception.BusinessException;

@Service
public class FestivalService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

    private final PublicFestivalClient publicFestivalClient;

    public FestivalService(PublicFestivalClient publicFestivalClient) {
        this.publicFestivalClient = publicFestivalClient;
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

    public FestivalCalendarResponse getCalendar(int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        List<PublicFestivalItem> monthFestivals = publicFestivalClient.fetchAll()
                .stream()
                .filter(this::hasRequiredFields)
                .filter(item -> overlaps(item, firstDay, lastDay))
                .toList();

        List<FestivalCalendarDayResponse> days = IntStream.rangeClosed(1, firstDay.lengthOfMonth())
                .mapToObj(day -> {
                    LocalDate date = firstDay.withDayOfMonth(day);
                    List<FestivalSummaryResponse> festivals = monthFestivals.stream()
                            .filter(item -> containsDate(item, date))
                            .sorted(Comparator.comparing(this::parseStartDate).thenComparing(this::normalizeName))
                            .map(this::toSummaryResponse)
                            .toList();
                    return new FestivalCalendarDayResponse(date, festivals);
                })
                .toList();

        return new FestivalCalendarResponse(year, month, days);
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
                parseDateOrNull(item.referenceDate())
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

    private boolean overlaps(PublicFestivalItem item, LocalDate from, LocalDate to) {
        return !parseEndDate(item).isBefore(from) && !parseStartDate(item).isAfter(to);
    }

    private boolean containsDate(PublicFestivalItem item, LocalDate date) {
        return !date.isBefore(parseStartDate(item)) && !date.isAfter(parseEndDate(item));
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
