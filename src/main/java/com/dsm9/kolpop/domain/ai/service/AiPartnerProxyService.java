package com.dsm9.kolpop.domain.ai.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.dsm9.kolpop.domain.ai.config.AiServerProperties;
import com.dsm9.kolpop.global.exception.BusinessException;

import tools.jackson.databind.JsonNode;

@Service
public class AiPartnerProxyService {

    private static final String HEALTH_PATH = "/health";
    private static final String CHAT_LISTINGS_PATH = "/api/v1/chat/listings";
    private static final String RECOMMEND_LISTINGS_PATH = "/api/v1/recommend/listings";
    private static final String RECOMMEND_REGIONS_PATH = "/api/v1/recommend/regions";
    private static final String RECOMMEND_BUSINESS_ITEMS_PATH = "/api/v1/recommend/business-items";
    private static final String MARKETING_AUTOMATION_PATH = "/api/v1/marketing/automation";

    private final RestClient restClient;
    private final AiServerProperties aiServerProperties;

    public AiPartnerProxyService(RestClient.Builder restClientBuilder, AiServerProperties aiServerProperties) {
        this.restClient = restClientBuilder.build();
        this.aiServerProperties = aiServerProperties;
    }

    public JsonNode health() {
        return get(HEALTH_PATH);
    }

    public JsonNode chatListings(JsonNode request) {
        return post(CHAT_LISTINGS_PATH, request);
    }

    public JsonNode recommendListings(JsonNode request) {
        return post(RECOMMEND_LISTINGS_PATH, request);
    }

    public JsonNode recommendRegions(JsonNode request) {
        return post(RECOMMEND_REGIONS_PATH, request);
    }

    public JsonNode recommendBusinessItems(JsonNode request) {
        return post(RECOMMEND_BUSINESS_ITEMS_PATH, request);
    }

    public JsonNode createMarketingAutomation(JsonNode request) {
        return post(MARKETING_AUTOMATION_PATH, request);
    }

    private JsonNode get(String path) {
        validateConfiguration();

        try {
            return restClient.get()
                    .uri(buildUri(path))
                    .headers(this::addAuthorization)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private JsonNode post(String path, JsonNode request) {
        validateConfiguration();

        try {
            return restClient.post()
                    .uri(buildUri(path))
                    .headers(this::addAuthorization)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private String buildUri(String path) {
        return aiServerProperties.getBaseUrl().replaceAll("/+$", "") + path;
    }

    private void addAuthorization(HttpHeaders headers) {
        if (!isBlank(aiServerProperties.getApiKey())) {
            headers.setBearerAuth(aiServerProperties.getApiKey());
        }
    }

    private void validateConfiguration() {
        if (isBlank(aiServerProperties.getBaseUrl())) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI_SERVER_BASE_URL_REQUIRED",
                    "AI server URL configuration is required."
            );
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI_SERVER_UNAVAILABLE",
                "AI server response could not be received."
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
