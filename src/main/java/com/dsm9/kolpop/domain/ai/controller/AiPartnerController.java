package com.dsm9.kolpop.domain.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dsm9.kolpop.domain.ai.service.AiPartnerProxyService;
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
    public ApiResponse<JsonNode> chatListings(@RequestBody JsonNode request) {
        return ApiResponse.success(aiPartnerProxyService.chatListings(request));
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
}
