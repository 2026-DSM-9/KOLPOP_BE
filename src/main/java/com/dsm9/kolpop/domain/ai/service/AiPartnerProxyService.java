package com.dsm9.kolpop.domain.ai.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.dsm9.kolpop.domain.ai.config.AiServerProperties;
import com.dsm9.kolpop.domain.ai.dto.AiConversationDetailResponse;
import com.dsm9.kolpop.domain.ai.dto.AiConversationSummaryResponse;
import com.dsm9.kolpop.domain.ai.entity.AiConversation;
import com.dsm9.kolpop.domain.ai.repository.AiConversationRepository;
import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.global.exception.BusinessException;

import tools.jackson.databind.JsonNode;

@Service
public class AiPartnerProxyService {

    private static final Logger log = LoggerFactory.getLogger(AiPartnerProxyService.class);

    private static final String HEALTH_PATH = "/health";
    private static final String CHAT_LISTINGS_PATH = "/api/v1/chat/listings";
    private static final String RECOMMEND_LISTINGS_PATH = "/api/v1/recommend/listings";
    private static final String RECOMMEND_REGIONS_PATH = "/api/v1/recommend/regions";
    private static final String RECOMMEND_BUSINESS_ITEMS_PATH = "/api/v1/recommend/business-items";
    private static final String MARKETING_AUTOMATION_PATH = "/api/v1/marketing/automation";

    private final RestClient restClient;
    private final AiServerProperties aiServerProperties;
    private final AiConversationRepository aiConversationRepository;
    private final UserRepository userRepository;

    @Autowired
    public AiPartnerProxyService(
            RestClient.Builder restClientBuilder,
            AiServerProperties aiServerProperties,
            AiConversationRepository aiConversationRepository,
            UserRepository userRepository
    ) {
        this.restClient = restClientBuilder.build();
        this.aiServerProperties = aiServerProperties;
        this.aiConversationRepository = aiConversationRepository;
        this.userRepository = userRepository;
    }

    public AiPartnerProxyService(RestClient.Builder restClientBuilder, AiServerProperties aiServerProperties) {
        this(restClientBuilder, aiServerProperties, null, null);
    }

    public JsonNode health() {
        return get(HEALTH_PATH);
    }

    public JsonNode chatListings(JsonNode request) {
        return post(CHAT_LISTINGS_PATH, request);
    }

    public JsonNode chatListings(Long userId, JsonNode request) {
        JsonNode response = post(CHAT_LISTINGS_PATH, request);
        saveConversation(userId, request, response);
        return response;
    }

    public List<AiConversationSummaryResponse> getConversations(Long userId) {
        return aiConversationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AiConversationSummaryResponse::from)
                .toList();
    }

    public AiConversationDetailResponse getConversation(Long userId, Long conversationId) {
        AiConversation conversation = aiConversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "AI_CONVERSATION_NOT_FOUND",
                        "AI conversation history could not be found."
                ));

        return AiConversationDetailResponse.from(conversation);
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
            log.error("AI server GET request failed: path={}", path, exception);
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
            log.error("AI server POST request failed: path={}", path, exception);
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

    private void saveConversation(Long userId, JsonNode request, JsonNode response) {
        if (aiConversationRepository == null || userRepository == null) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_NOT_FOUND",
                        "User could not be found."
                ));
        String userMessage = extractText(request, "message", "input", "query", "content");
        String aiMessage = extractText(response, "answer", "message", "content", "response");
        String title = createTitle(userMessage);

        aiConversationRepository.save(new AiConversation(
                user,
                title,
                truncate(userMessage, 1000),
                truncate(aiMessage, 4000),
                request.toString(),
                response.toString(),
                LocalDateTime.now()
        ));
    }

    private String extractText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return "";
        }

        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (!isBlank(text)) {
                    return text.trim();
                }
            }
        }
        return node.toString();
    }

    private String createTitle(String userMessage) {
        if (isBlank(userMessage)) {
            return "AI 대화";
        }
        return truncate(userMessage.trim(), 40);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
