package com.dsm9.kolpop.domain.ai.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dsm9.kolpop.domain.ai.config.AiServerProperties;
import com.dsm9.kolpop.domain.ai.dto.AiChatMessageRequest;
import com.dsm9.kolpop.domain.ai.dto.AiConversationDetailResponse;
import com.dsm9.kolpop.domain.ai.dto.AiConversationSummaryResponse;
import com.dsm9.kolpop.domain.ai.entity.AiConversation;
import com.dsm9.kolpop.domain.ai.repository.AiConversationRepository;
import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.entity.ListingStatus;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.global.exception.BusinessException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
public class AiPartnerProxyService {

    private static final Logger log = LoggerFactory.getLogger(AiPartnerProxyService.class);

    private static final String HEALTH_PATH = "/health";
    private static final String CHAT_LISTINGS_PATH = "/api/v1/chat/listings";
    private static final String CHAT_BUSINESS_ITEMS_PATH = "/api/v1/chat/business-items";
    private static final String CHAT_MARKETING_PATH = "/api/v1/chat/marketing";
    private static final String RECOMMEND_LISTINGS_PATH = "/api/v1/recommend/listings";
    private static final String RECOMMEND_REGIONS_PATH = "/api/v1/recommend/regions";
    private static final String RECOMMEND_BUSINESS_ITEMS_PATH = "/api/v1/recommend/business-items";
    private static final String MARKETING_AUTOMATION_PATH = "/api/v1/marketing/automation";
    private static final int MAX_LISTING_SUMMARY_LENGTH = 300;

    private final RestClient restClient;
    private final AiServerProperties aiServerProperties;
    private final AiConversationRepository aiConversationRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AiPartnerProxyService(
            RestClient.Builder restClientBuilder,
            AiServerProperties aiServerProperties,
            AiConversationRepository aiConversationRepository,
            UserRepository userRepository,
            ListingRepository listingRepository
    ) {
        this.restClient = restClientBuilder.build();
        this.aiServerProperties = aiServerProperties;
        this.aiConversationRepository = aiConversationRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    public AiPartnerProxyService(RestClient.Builder restClientBuilder, AiServerProperties aiServerProperties) {
        this(restClientBuilder, aiServerProperties, null, null, null);
    }

    public JsonNode health() {
        return get(HEALTH_PATH);
    }

    public JsonNode chatListings(Long userId, AiChatMessageRequest request) {
        JsonNode payload = buildListingsChatPayload(request);
        return postAndSave(userId, CHAT_LISTINGS_PATH, payload);
    }

    public JsonNode chatBusinessItems(Long userId, AiChatMessageRequest request) {
        JsonNode payload = buildMessageOnlyPayload(request);
        return postAndSave(userId, CHAT_BUSINESS_ITEMS_PATH, payload);
    }

    public JsonNode chatMarketing(Long userId, AiChatMessageRequest request) {
        JsonNode payload = buildMessageOnlyPayload(request);
        return postAndSave(userId, CHAT_MARKETING_PATH, payload);
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

    private JsonNode postAndSave(Long userId, String path, JsonNode request) {
        JsonNode response = post(path, request);
        saveConversation(userId, request, response);
        return response;
    }

    private JsonNode get(String path) {
        validateConfiguration();

        try {
            return restClient.get()
                    .uri(buildUri(path))
                    .headers(this::addAuthorization)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw upstreamError(exception);
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
        } catch (RestClientResponseException exception) {
            throw upstreamError(exception);
        } catch (RestClientException exception) {
            log.error("AI server POST request failed: path={}", path, exception);
            throw unavailable();
        }
    }

    private JsonNode buildMessageOnlyPayload(AiChatMessageRequest request) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("message", request.message().trim());
        return payload;
    }

    private JsonNode buildListingsChatPayload(AiChatMessageRequest request) {
        if (listingRepository == null) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "LISTING_REPOSITORY_REQUIRED",
                    "Listing repository is required for listing recommendation chat."
            );
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("message", request.message().trim());

        ArrayNode listingsNode = payload.putArray("listings");
        for (Listing listing : listingRepository.findAllByStatusOrderByCreatedAtDesc(ListingStatus.RECRUITING)) {
            ObjectNode listingNode = listingsNode.addObject();
            listingNode.put("listing_id", String.valueOf(listing.getId()));
            listingNode.put("title", normalizeText(listing.getTitle()));
            listingNode.put("address", normalizeText(listing.getAddress()));
            listingNode.put("detail_address", normalizeText(listing.getDetailAddress()));
            listingNode.put("price_per_day", listing.getDailyFee());
            listingNode.put("deposit", listing.getDeposit());
            if (listing.getArea() != null) {
                listingNode.put("area_sqm", listing.getArea());
            }
            listingNode.put("summary", truncate(normalizeText(listing.getDescription()), MAX_LISTING_SUMMARY_LENGTH));
            addTextArray(listingNode.putArray("facilities"), listing.getFacilities());
            addTextArray(listingNode.putArray("restrictions"), mergeRestrictions(listing));
            addTextArray(listingNode.putArray("hashtags"), listing.getHashtags());
            listingNode.put("available_from", listing.getOperatingStartDate() == null ? "" : listing.getOperatingStartDate().toString());
            listingNode.put("available_to", listing.getOperatingEndDate() == null ? "" : listing.getOperatingEndDate().toString());
        }

        return payload;
    }

    private List<String> mergeRestrictions(Listing listing) {
        List<String> restrictions = new ArrayList<>();
        addNormalizedValues(restrictions, listing.getIndustryRestrictions());
        addNormalizedValues(restrictions, listing.getAdditionalRestrictions());
        return restrictions;
    }

    private void addNormalizedValues(List<String> target, List<String> values) {
        if (values == null) {
            return;
        }

        for (String value : values) {
            String normalizedValue = normalizeText(value);
            if (!normalizedValue.isEmpty()) {
                target.add(normalizedValue);
            }
        }
    }

    private void addTextArray(ArrayNode target, List<String> values) {
        if (values == null) {
            return;
        }

        for (String value : values) {
            String normalizedValue = normalizeText(value);
            if (!normalizedValue.isEmpty()) {
                target.add(normalizedValue);
            }
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

    private BusinessException upstreamError(RestClientResponseException exception) {
        HttpStatus status = resolveStatus(exception);
        String code = status.is4xxClientError() ? "AI_SERVER_BAD_REQUEST" : "AI_SERVER_ERROR";
        String responseBody = truncate(exception.getResponseBodyAsString(), 1000);
        String message = "AI server returned " + exception.getStatusCode().value();

        if (!isBlank(responseBody)) {
            message += ": " + responseBody;
        }

        return new BusinessException(status, code, message);
    }

    private HttpStatus resolveStatus(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            return HttpStatus.BAD_GATEWAY;
        }
        return status;
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
        String aiMessage = extractText(response, "assistant_message", "assistantMessage", "answer", "message", "content", "response");
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

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
