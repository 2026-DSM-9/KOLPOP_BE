package com.dsm9.kolpop.domain.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import com.dsm9.kolpop.domain.ai.config.AiServerProperties;
import com.dsm9.kolpop.domain.ai.dto.AiConversationDetailResponse;
import com.dsm9.kolpop.domain.ai.dto.AiConversationSummaryResponse;
import com.dsm9.kolpop.domain.ai.entity.AiConversation;
import com.dsm9.kolpop.domain.ai.repository.AiConversationRepository;
import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

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
    void chatProxiesGeneralAiConversation() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AiPartnerProxyService service = new AiPartnerProxyService(
                builder,
                createProperties(),
                conversationRepository,
                userRepository
        );
        User user = createUser(7L);
        JsonNode request = objectMapper.readTree("""
                {"message":"Which festival should I target?","history":[]}
                """);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        server.expect(once(), requestTo("https://ai.example.test/api/v1/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-ai-key"))
                .andExpect(content().json("""
                        {"message":"Which festival should I target?","history":[]}
                        """))
                .andRespond(withSuccess("""
                        {"answer":"Target a high-footfall weekend festival."}
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = service.chat(7L, request);

        assertEquals("Target a high-footfall weekend festival.", response.get("answer").asText());
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

    @Test
    void getConversationsReturnsCurrentUsersConversationSummaries() {
        AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AiPartnerProxyService service = new AiPartnerProxyService(
                RestClient.builder(),
                createProperties(),
                conversationRepository,
                userRepository
        );
        User user = createUser(7L);
        AiConversation first = createConversation(
                11L,
                user,
                "lipstick business location",
                "Which location is good?",
                "Thanks, AI partner!",
                LocalDateTime.of(2026, 7, 13, 14, 0)
        );
        AiConversation second = createConversation(
                10L,
                user,
                "festival business idea",
                "Recommend a festival business.",
                "A food booth would work well.",
                LocalDateTime.of(2026, 7, 12, 9, 30)
        );

        when(conversationRepository.findAllByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(first, second));

        List<AiConversationSummaryResponse> response = service.getConversations(7L);

        assertEquals(2, response.size());
        assertEquals(11L, response.get(0).id());
        assertEquals("lipstick business location", response.get(0).title());
        assertEquals("Thanks, AI partner!", response.get(0).preview());
        assertEquals(LocalDateTime.of(2026, 7, 13, 14, 0), response.get(0).createdAt());
        assertEquals(10L, response.get(1).id());
    }

    @Test
    void getConversationReturnsSelectedConversationDetail() {
        AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AiPartnerProxyService service = new AiPartnerProxyService(
                RestClient.builder(),
                createProperties(),
                conversationRepository,
                userRepository
        );
        AiConversation conversation = createConversation(
                11L,
                createUser(7L),
                "lipstick business location",
                "Which location is good?",
                "Thanks, AI partner!",
                LocalDateTime.of(2026, 7, 13, 14, 0)
        );

        when(conversationRepository.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(conversation));

        AiConversationDetailResponse response = service.getConversation(7L, 11L);

        assertEquals(11L, response.id());
        assertEquals("lipstick business location", response.title());
        assertEquals(2, response.messages().size());
        assertEquals("USER", response.messages().get(0).role());
        assertEquals("Which location is good?", response.messages().get(0).content());
        assertEquals("AI", response.messages().get(1).role());
        assertEquals("Thanks, AI partner!", response.messages().get(1).content());
    }

    @Test
    void getConversationRejectsMissingOrOtherUsersConversation() {
        AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AiPartnerProxyService service = new AiPartnerProxyService(
                RestClient.builder(),
                createProperties(),
                conversationRepository,
                userRepository
        );

        when(conversationRepository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getConversation(7L, 99L)
        );

        assertEquals("AI_CONVERSATION_NOT_FOUND", exception.getCode());
    }

    private AiServerProperties createProperties() {
        AiServerProperties properties = new AiServerProperties();
        properties.setBaseUrl("https://ai.example.test/");
        properties.setApiKey("test-ai-key");
        return properties;
    }

    private AiConversation createConversation(
            Long id,
            User user,
            String title,
            String userMessage,
            String aiMessage,
            LocalDateTime createdAt
    ) {
        AiConversation conversation = new AiConversation(
                user,
                title,
                userMessage,
                aiMessage,
                "{}",
                "{}",
                createdAt
        );
        ReflectionTestUtils.setField(conversation, "id", id);
        return conversation;
    }

    private User createUser(Long id) {
        User user = new User(
                "login_" + id,
                "user" + id,
                "user" + id + "@kolpop.test",
                "encodedPassword",
                "0101234" + String.format("%04d", id),
                UserRole.FOUNDER
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
