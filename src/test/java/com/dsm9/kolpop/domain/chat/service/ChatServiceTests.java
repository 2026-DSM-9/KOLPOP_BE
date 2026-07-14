package com.dsm9.kolpop.domain.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.chat.dto.ChatRoomRequestResponse;
import com.dsm9.kolpop.domain.chat.dto.ChatRoomResponse;
import com.dsm9.kolpop.domain.chat.dto.CreateChatRoomRequest;
import com.dsm9.kolpop.domain.chat.entity.ChatMessage;
import com.dsm9.kolpop.domain.chat.entity.ChatRoom;
import com.dsm9.kolpop.domain.chat.entity.ChatRoomStatus;
import com.dsm9.kolpop.domain.chat.repository.ChatMessageRepository;
import com.dsm9.kolpop.domain.chat.repository.ChatRoomRepository;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.reservation.repository.ReservationRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

class ChatServiceTests {

    @Test
    void founderCreatesPendingChatRequestWithFirstMessage() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                userRepository,
                listingRepository,
                reservationRepository
        );
        User founder = createUser(1L, UserRole.FOUNDER, "박창업");
        User landlord = createUser(2L, UserRole.LANDLORD, "김임대");
        Listing listing = createListing(20L, landlord);

        when(userRepository.findById(1L)).thenReturn(Optional.of(founder));
        when(listingRepository.findById(20L)).thenReturn(Optional.of(listing));
        when(chatRoomRepository.findByFounderIdAndListingId(1L, 20L)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            ReflectionTestUtils.setField(room, "id", 10L);
            return room;
        });

        ChatRoomResponse response = chatService.createRoom(
                new CreateChatRoomRequest(20L, "안녕하세요, 문의드립니다."),
                authentication("1")
        );

        assertEquals(10L, response.roomId());
        assertEquals(20L, response.listing().listingId());
        assertEquals(ChatRoomStatus.PENDING.name(), response.status());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void createRoomRejectsBlankFirstMessage() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                userRepository,
                listingRepository,
                reservationRepository
        );
        User founder = createUser(2L, UserRole.FOUNDER, "박창업");

        when(userRepository.findById(2L)).thenReturn(Optional.of(founder));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.createRoom(new CreateChatRoomRequest(20L, "   "), authentication("2"))
        );

        assertEquals("INVALID_CHAT_MESSAGE_CONTENT", exception.getCode());
    }

    @Test
    void landlordCanGetPendingChatRequests() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                userRepository,
                listingRepository,
                reservationRepository
        );
        User founder = createUser(2L, UserRole.FOUNDER, "박창업");
        User landlord = createUser(1L, UserRole.LANDLORD, "김임대");
        Listing listing = createListing(20L, landlord);
        ChatRoom room = createRoom(10L, founder, landlord, listing);
        ChatMessage message = createMessage(100L, room, founder, "안녕하세요, 문의드립니다.");

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(chatRoomRepository.findAllByFounderIdOrLandlordIdOrderByCreatedAtDesc(1L, 1L)).thenReturn(List.of(room));
        when(chatMessageRepository.findFirstByRoomIdOrderByCreatedAtAsc(10L)).thenReturn(Optional.of(message));

        List<ChatRoomRequestResponse> response = chatService.getRoomRequests(authentication("1"));

        assertEquals(1, response.size());
        assertEquals(10L, response.getFirst().roomId());
        assertEquals(20L, response.getFirst().listing().listingId());
        assertEquals("안녕하세요, 문의드립니다.", response.getFirst().message().content());
    }

    @Test
    void pendingChatRequestBlocksSocketMessagesUntilAccepted() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                userRepository,
                listingRepository,
                reservationRepository
        );
        User founder = createUser(2L, UserRole.FOUNDER, "박창업");
        User landlord = createUser(1L, UserRole.LANDLORD, "김임대");
        ChatRoom room = createRoom(10L, founder, landlord, createListing(20L, landlord));

        when(userRepository.findById(2L)).thenReturn(Optional.of(founder));
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.sendMessage(
                        10L,
                        new com.dsm9.kolpop.domain.chat.dto.ChatMessageRequest("수락 전 메시지"),
                        principal("2")
                )
        );

        assertEquals("CHAT_REQUEST_NOT_ACCEPTED", exception.getCode());
    }

    @Test
    void landlordAcceptsPendingChatRequest() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                userRepository,
                listingRepository,
                reservationRepository
        );
        User founder = createUser(2L, UserRole.FOUNDER, "박창업");
        User landlord = createUser(1L, UserRole.LANDLORD, "김임대");
        ChatRoom room = createRoom(10L, founder, landlord, createListing(20L, landlord));

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room));

        ChatRoomResponse response = chatService.acceptRoom(10L, authentication("1"));

        assertEquals(ChatRoomStatus.ACCEPTED.name(), response.status());
        assertEquals(ChatRoomStatus.ACCEPTED, room.getStatus());
    }

    private Authentication authentication(String userId) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userId);
        when(authentication.isAuthenticated()).thenReturn(true);
        return authentication;
    }

    private Principal principal(String userId) {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(userId);
        return principal;
    }

    @Test
    void sameFounderCreatesSeparateRequestsForDifferentListings() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ChatService chatService = new ChatService(
                chatRoomRepository,
                chatMessageRepository,
                userRepository,
                listingRepository,
                reservationRepository
        );
        User founder = createUser(2L, UserRole.FOUNDER, "박창업");
        User landlord = createUser(1L, UserRole.LANDLORD, "김임대");
        Listing firstListing = createListing(20L, landlord);
        Listing secondListing = createListing(21L, landlord);

        when(userRepository.findById(2L)).thenReturn(Optional.of(founder));
        when(listingRepository.findById(20L)).thenReturn(Optional.of(firstListing));
        when(listingRepository.findById(21L)).thenReturn(Optional.of(secondListing));
        when(chatRoomRepository.findByFounderIdAndListingId(2L, 20L)).thenReturn(Optional.empty());
        when(chatRoomRepository.findByFounderIdAndListingId(2L, 21L)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            ReflectionTestUtils.setField(room, "id", room.getListing().getId() + 100L);
            return room;
        });
        when(chatMessageRepository.findFirstByRoomIdOrderByCreatedAtAsc(any(Long.class))).thenReturn(Optional.empty());

        ChatRoomResponse first = chatService.createRoom(
                new CreateChatRoomRequest(20L, "첫 번째 매물 문의"), authentication("2")
        );
        ChatRoomResponse second = chatService.createRoom(
                new CreateChatRoomRequest(21L, "두 번째 매물 문의"), authentication("2")
        );

        assertEquals(120L, first.roomId());
        assertEquals(121L, second.roomId());
        assertEquals(20L, first.listing().listingId());
        assertEquals(21L, second.listing().listingId());
    }

    private ChatRoom createRoom(Long id, User founder, User landlord, Listing listing) {
        ChatRoom room = new ChatRoom(founder, landlord, listing);
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }

    private Listing createListing(Long id, User landlord) {
        Listing listing = new Listing(
                landlord,
                "테스트 매물 " + id,
                List.of("https://cdn.example.com/listings/" + id + ".jpg"),
                "서울시 테스트로 1",
                "1층",
                new BigDecimal("37.5551000"),
                new BigDecimal("126.9235000"),
                150000L,
                1000000L,
                new BigDecimal("66.1"),
                List.of(),
                List.of(),
                List.of(),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                1,
                14,
                "테스트 매물입니다.",
                List.of(),
                LocalDateTime.of(2026, 7, 14, 9, 0)
        );
        ReflectionTestUtils.setField(listing, "id", id);
        return listing;
    }

    private ChatMessage createMessage(Long id, ChatRoom room, User sender, String content) {
        ChatMessage message = new ChatMessage(room, sender, content);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private User createUser(Long id, UserRole role, String name) {
        User user = new User(
                "login_" + id,
                name,
                "user" + id + "@kolpop.test",
                "encodedPassword",
                "0101234" + String.format("%04d", id),
                role
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
