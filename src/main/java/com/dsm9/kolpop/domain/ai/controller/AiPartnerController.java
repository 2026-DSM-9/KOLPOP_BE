package com.dsm9.kolpop.domain.ai.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dsm9.kolpop.domain.ai.dto.AiConversationDetailResponse;
import com.dsm9.kolpop.domain.ai.dto.AiConversationSummaryResponse;
import com.dsm9.kolpop.domain.ai.service.AiPartnerProxyService;
import com.dsm9.kolpop.global.exception.BusinessException;
import com.dsm9.kolpop.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI Partner", description = "AI server proxy API")
@SecurityRequirement(name = "bearerAuth")
public class AiPartnerController {

    private final AiPartnerProxyService aiPartnerProxyService;

    public AiPartnerController(AiPartnerProxyService aiPartnerProxyService) {
        this.aiPartnerProxyService = aiPartnerProxyService;
    }

    @GetMapping("/health")
    @Operation(summary = "AI server health check")
    public ApiResponse<JsonNode> health() {
        return ApiResponse.success(aiPartnerProxyService.health());
    }

    @PostMapping("/chat/listings")
    @Operation(summary = "Conversational listing recommendation")
    public ApiResponse<JsonNode> chatListings(@RequestBody JsonNode request, Authentication authentication) {
        return ApiResponse.success(aiPartnerProxyService.chatListings(extractUserId(authentication), request));
    }

    @PostMapping("/chat")
    @Operation(summary = "AI chat")
    public ApiResponse<JsonNode> chat(@RequestBody JsonNode request, Authentication authentication) {
        return ApiResponse.success(aiPartnerProxyService.chat(extractUserId(authentication), request));
    }

    @GetMapping("/conversations")
    @Operation(summary = "AI conversation history list")
    public ApiResponse<List<AiConversationSummaryResponse>> getConversations(Authentication authentication) {
        return ApiResponse.success(aiPartnerProxyService.getConversations(extractUserId(authentication)));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "AI conversation history detail")
    public ApiResponse<AiConversationDetailResponse> getConversation(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                aiPartnerProxyService.getConversation(extractUserId(authentication), conversationId)
        );
    }

    @PostMapping("/recommend/listings")
    @Operation(summary = "Listing recommendation")
    public ApiResponse<JsonNode> recommendListings(@RequestBody JsonNode request) {
        return ApiResponse.success(aiPartnerProxyService.recommendListings(request));
    }

    @PostMapping("/recommend/regions")
    @Operation(summary = "Region recommendation")
    public ApiResponse<JsonNode> recommendRegions(@RequestBody JsonNode request) {
        return ApiResponse.success(aiPartnerProxyService.recommendRegions(request));
    }

    @PostMapping("/recommend/business-items")
    @Operation(summary = "Business item recommendation")
    public ApiResponse<JsonNode> recommendBusinessItems(@RequestBody JsonNode request) {
        return ApiResponse.success(aiPartnerProxyService.recommendBusinessItems(request));
    }

    @PostMapping("/marketing/automation")
    @Operation(summary = "Marketing copy and operation schedule generation")
    public ApiResponse<JsonNode> createMarketingAutomation(@RequestBody JsonNode request) {
        return ApiResponse.success(aiPartnerProxyService.createMarketingAutomation(request));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required.");
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION", "Authentication is invalid.");
        }
    }
}
