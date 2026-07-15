package com.dsm9.kolpop.domain.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

import tools.jackson.databind.JsonNode;

class AiPartnerProxyServiceTests {

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
    void chatListingsBuildsMessageAndRecruitingListingsPayload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        AiPartnerProxyService service = new AiPartnerProxyService(
                builder,
                createProperties(),
                conversationRepository,
                userRepository,
                listingRepository
        );
        User user = createUser(7L);
        AiChatMessageRequest request = new AiChatMessageRequest("비건 디저트 팝업에 맞는 매물 추천해줘");

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(listingRepository.findAllByStatusOrderByCreatedAtDesc(ListingStatus.RECRUITING))
                .thenReturn(List.of(createListing(101L)));

        server.expect(once(), requestTo("https://ai.example.test/api/v1/chat/listings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-ai-key"))
                .andExpect(content().json("""
                        {
                          "message": "비건 디저트 팝업에 맞는 매물 추천해줘",
                          "listings": [
                            {
                              "listing_id": "101",
                              "title": "성수 1층 쇼룸",
                              "address": "서울 성동구 성수이로 00",
                              "detail_address": "1층",
                              "price_per_day": 200000,
                              "deposit": 1000000,
                              "area_sqm": 42.5,
                              "summary": "브랜딩형 디저트 팝업에 어울리는 전면 노출형 공간",
                              "facilities": ["조명", "와이파이"],
                              "restrictions": ["취사 불가", "늦은 밤 운영 제한"],
                              "hashtags": ["#성수", "#쇼룸"],
                              "available_from": "2026-07-20",
                              "available_to": "2026-08-31"
                            }
                          ]
                        }
                        """, true))
                .andRespond(withSuccess("""
                        {"assistant_message":"성수 쇼룸을 추천할게요."}
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = service.chatListings(7L, request);

        assertEquals("성수 쇼룸을 추천할게요.", response.get("assistant_message").asText());
        verify(conversationRepository).save(any(AiConversation.class));
        server.verify();
    }

    @Test
    void chatBusinessItemsProxiesMessageOnlyPayload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        AiPartnerProxyService service = new AiPartnerProxyService(
                builder,
                createProperties(),
                conversationRepository,
                userRepository,
                listingRepository
        );
        User user = createUser(7L);
        AiChatMessageRequest request = new AiChatMessageRequest("연남에서 하면 잘 될 팝업 아이템 추천해줘");

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        server.expect(once(), requestTo("https://ai.example.test/api/v1/chat/business-items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-ai-key"))
                .andExpect(content().json("""
                        {"message":"연남에서 하면 잘 될 팝업 아이템 추천해줘"}
                        """, true))
                .andRespond(withSuccess("""
                        {"assistant_message":"연남에서는 체험형 디저트 팝업이 잘 맞아요."}
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = service.chatBusinessItems(7L, request);

        assertEquals("연남에서는 체험형 디저트 팝업이 잘 맞아요.", response.get("assistant_message").asText());
        verify(conversationRepository).save(any(AiConversation.class));
        server.verify();
    }

    @Test
    void chatMarketingProxiesMessageOnlyPayload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        AiPartnerProxyService service = new AiPartnerProxyService(
                builder,
                createProperties(),
                conversationRepository,
                userRepository,
                listingRepository
        );
        User user = createUser(7L);
        AiChatMessageRequest request = new AiChatMessageRequest("성수에서 여는 미니 선풍기 팝업 홍보 문구 만들어줘");

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        server.expect(once(), requestTo("https://ai.example.test/api/v1/chat/marketing"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-ai-key"))
                .andExpect(content().json("""
                        {"message":"성수에서 여는 미니 선풍기 팝업 홍보 문구 만들어줘"}
                        """, true))
                .andRespond(withSuccess("""
                        {"assistant_message":"성수 감성에 맞는 홍보 문구를 준비했어요."}
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = service.chatMarketing(7L, request);

        assertEquals("성수 감성에 맞는 홍보 문구를 준비했어요.", response.get("assistant_message").asText());
        verify(conversationRepository).save(any(AiConversation.class));
        server.verify();
    }

    @Test
    void marketingAutomationProxiesMarketingEndpoint() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiPartnerProxyService service = new AiPartnerProxyService(builder, createProperties());
        tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();
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
                userRepository,
                mock(ListingRepository.class)
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
                userRepository,
                mock(ListingRepository.class)
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
                userRepository,
                mock(ListingRepository.class)
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

    private Listing createListing(Long id) {
        Listing listing = new Listing(
                createLandlord(30L),
                "성수 1층 쇼룸",
                List.of("https://example.com/listing-101.jpg"),
                "서울 성동구 성수이로 00",
                "1층",
                new BigDecimal("37.5441234"),
                new BigDecimal("127.0551234"),
                200000L,
                1000000L,
                new BigDecimal("42.5"),
                List.of("조명", "와이파이"),
                List.of("취사 불가"),
                List.of("늦은 밤 운영 제한"),
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 8, 31),
                1,
                7,
                "브랜딩형 디저트 팝업에 어울리는 전면 노출형 공간",
                List.of("#성수", "#쇼룸"),
                LocalDateTime.of(2026, 7, 10, 9, 0)
        );
        ReflectionTestUtils.setField(listing, "id", id);
        return listing;
    }

    private User createLandlord(Long id) {
        User user = new User(
                "landlord_" + id,
                "landlord" + id,
                "landlord" + id + "@kolpop.test",
                "encodedPassword",
                "0109876" + String.format("%04d", id),
                UserRole.LANDLORD
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
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
