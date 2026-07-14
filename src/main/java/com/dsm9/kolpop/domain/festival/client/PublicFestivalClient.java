package com.dsm9.kolpop.domain.festival.client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.dsm9.kolpop.domain.festival.config.PublicFestivalProperties;
import com.dsm9.kolpop.domain.festival.dto.PublicFestivalApiResponse;
import com.dsm9.kolpop.domain.festival.dto.PublicFestivalItem;
import com.dsm9.kolpop.global.exception.BusinessException;

@Component
public class PublicFestivalClient {

    private static final int MIN_PAGE = 1;
    private static final String NOT_CONFIGURED_ENDPOINT_SUFFIX = "configure-me";
    private static final Pattern ENCODED_OCTET = Pattern.compile("%[0-9a-fA-F]{2}");

    private final RestClient restClient;
    private final PublicFestivalProperties properties;
    private List<PublicFestivalItem> cachedItems = Collections.emptyList();
    private Instant cachedAt = Instant.EPOCH;

    public PublicFestivalClient(RestClient.Builder restClientBuilder, PublicFestivalProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public synchronized List<PublicFestivalItem> fetchAll() {
        if (isCacheFresh()) {
            return cachedItems;
        }

        validateConfiguration();

        List<PublicFestivalItem> items = new ArrayList<>();
        int page = MIN_PAGE;
        int totalCount = Integer.MAX_VALUE;

        while (items.size() < totalCount && page <= properties.getMaxPages()) {
            PublicFestivalApiResponse response = fetchPage(page);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }

            items.addAll(response.data());
            totalCount = response.totalCount() > 0 ? response.totalCount() : items.size();
            page++;
        }

        cachedItems = List.copyOf(items);
        cachedAt = Instant.now();
        return cachedItems;
    }

    private PublicFestivalApiResponse fetchPage(int page) {
        URI uri = UriComponentsBuilder
                .fromUriString(properties.getEndpoint())
                .queryParam("pageNo", page)
                .queryParam("numOfRows", properties.getPerPage())
                .queryParam("type", "json")
                .queryParam("serviceKey", encodeServiceKey(properties.getServiceKey()))
                .build(true)
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(PublicFestivalApiResponse.class);
        } catch (Exception exception) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PUBLIC_FESTIVAL_API_UNAVAILABLE",
                    "공공데이터 축제 API를 호출할 수 없습니다."
            );
        }
    }

    private boolean isCacheFresh() {
        return !cachedItems.isEmpty()
                && Duration.between(cachedAt, Instant.now()).getSeconds() < properties.getCacheTtlSeconds();
    }

    private String encodeServiceKey(String serviceKey) {
        String trimmed = serviceKey.trim();
        if (ENCODED_OCTET.matcher(trimmed).find()) {
            return trimmed;
        }
        return URLEncoder.encode(trimmed, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void validateConfiguration() {
        if (isBlank(properties.getEndpoint()) || properties.getEndpoint().endsWith(NOT_CONFIGURED_ENDPOINT_SUFFIX)) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PUBLIC_FESTIVAL_ENDPOINT_NOT_CONFIGURED",
                    "공공데이터 축제 API endpoint 설정이 필요합니다."
            );
        }

        if (isBlank(properties.getServiceKey())) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PUBLIC_DATA_SERVICE_KEY_NOT_CONFIGURED",
                    "공공데이터 서비스키 설정이 필요합니다."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
