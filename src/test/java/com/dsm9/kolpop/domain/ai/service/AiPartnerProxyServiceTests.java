package com.dsm9.kolpop.domain.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dsm9.kolpop.domain.ai.config.AiServerProperties;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AiPartnerProxyServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void healthProxiesAiServerHealthEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiPartnerProxyService service = new AiPartnerProxyService(builder, createProperties());

        server.expect(once(), requestTo("https://ai.example.test/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"status":"ok","model":"claude-test"}
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = service.health();

        assertEquals("ok", response.get("status").asText());
        assertEquals("claude-test", response.get("model").asText());
        server.verify();
    }

    @Test
    void chatListingsProxiesConversationalListingRecommendation() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiPartnerProxyService service = new AiPartnerProxyService(builder, createProperties());
        JsonNode request = objectMapper.readTree("""
                {"message":"팝업 매물 추천해줘","history":[]}
                """);

        server.expect(once(), requestTo("https://ai.example.test/api/v1/chat/listings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-ai-key"))
                .andExpect(content().json("""
                        {"message":"팝업 매물 추천해줘","history":[]}
                        """))
                .andRespond(withSuccess("""
                        {"answer":"성수동 단기 매물을 추천합니다."}
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = service.chatListings(request);

        assertEquals("성수동 단기 매물을 추천합니다.", response.get("answer").asText());
        server.verify();
    }

    @Test
    void marketingAutomationProxiesMarketingEndpoint() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiPartnerProxyService service = new AiPartnerProxyService(builder, createProperties());
        JsonNode request = objectMapper.readTree("""
                {"brand":"미니 선풍기","region":"대전"}
                """);

        server.expect(once(), requestTo("https://ai.example.test/api/v1/marketing/automation"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"copy":"시원한 대전 팝업을 시작하세요.","schedule":[]}
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = service.createMarketingAutomation(request);

        assertEquals("시원한 대전 팝업을 시작하세요.", response.get("copy").asText());
        server.verify();
    }

    private AiServerProperties createProperties() {
        AiServerProperties properties = new AiServerProperties();
        properties.setBaseUrl("https://ai.example.test/");
        properties.setApiKey("test-ai-key");
        return properties;
    }
}
