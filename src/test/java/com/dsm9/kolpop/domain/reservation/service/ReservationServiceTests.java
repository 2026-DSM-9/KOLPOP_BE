package com.dsm9.kolpop.domain.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.chat.entity.ChatRoom;
import com.dsm9.kolpop.domain.chat.repository.ChatRoomRepository;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.reservation.dto.CreateReservationRequest;
import com.dsm9.kolpop.domain.reservation.dto.CreateReservationResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationDecisionResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationManagementResponse;
import com.dsm9.kolpop.domain.reservation.entity.Reservation;
import com.dsm9.kolpop.domain.reservation.entity.ReservationStatus;
import com.dsm9.kolpop.domain.reservation.repository.ReservationRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

class ReservationServiceTests {

    @Test
    void founderCanCreateReservation() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                listingRepository,
                userRepository,
                chatRoomRepository
        );

        User founder = createUser(2L, UserRole.FOUNDER, "박창업");
        Listing listing = createListing(10L, createUser(1L, UserRole.LANDLORD, "김임대"), "홍대입구역 1번 출구 앞 1층 점포");

        when(userRepository.findById(2L)).thenReturn(Optional.of(founder));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            ReflectionTestUtils.setField(reservation, "id", 100L);
            return reservation;
        });

        CreateReservationResponse response = reservationService.createReservation(2L, createReservationRequest());

        assertEquals(100L, response.reservationId());
        assertEquals("PENDING", response.status().code());
        assertEquals("승인 대기", response.status().label());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void landlordCanGetReservationManagementSummaryAndList() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                listingRepository,
                userRepository,
                chatRoomRepository
        );

        User landlord = createUser(1L, UserRole.LANDLORD, "김임대");
        User founder1 = createUser(2L, UserRole.FOUNDER, "박창업");
        User founder2 = createUser(3L, UserRole.FOUNDER, "이스타트");
        Listing listing = createListing(10L, landlord, "홍대입구역 1번 출구 앞 1층 점포");
        Reservation pendingReservation = createReservation(
                200L,
                listing,
                founder1,
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12),
                "패션 브랜드 팝업스토어를 운영하고 싶습니다.",
                LocalDateTime.of(2026, 7, 10, 10, 0),
                ReservationStatus.PENDING
        );
        Reservation approvedReservation = createReservation(
                201L,
                listing,
                founder2,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                "뷰티 브랜드 런칭 팝업을 기획 중입니다.",
                LocalDateTime.of(2026, 7, 8, 9, 30),
                ReservationStatus.APPROVED
        );
        ChatRoom chatRoom = new ChatRoom(founder2, landlord, listing);
        ReflectionTestUtils.setField(chatRoom, "id", 999L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(reservationRepository.findAllByListingLandlordIdOrderByAppliedAtDesc(1L))
                .thenReturn(List.of(pendingReservation, approvedReservation));
        when(chatRoomRepository.findAllByLandlordIdAndListingIdIn(1L, List.of(10L)))
                .thenReturn(List.of(chatRoom));

        ReservationManagementResponse response = reservationService.getManagementReservations(1L);

        assertEquals(1L, response.summary().pendingCount());
        assertEquals(1L, response.summary().approvedCount());
        assertEquals(2L, response.summary().totalCount());
        assertEquals("박창업", response.reservations().getFirst().founderName());
        assertEquals("승인 대기", response.reservations().getFirst().status().label());
        assertEquals(999L, response.reservations().get(1).chatRoomId());
        assertEquals(true, response.reservations().get(1).canChat());
    }

    @Test
    void landlordCanApproveReservationAndCreateChatRoom() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                listingRepository,
                userRepository,
                chatRoomRepository
        );

        User landlord = createUser(1L, UserRole.LANDLORD, "김임대");
        User founder = createUser(2L, UserRole.FOUNDER, "박창업");
        Listing listing = createListing(10L, landlord, "홍대입구역 1번 출구 앞 1층 점포");
        Reservation reservation = createReservation(
                300L,
                listing,
                founder,
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12),
                "패션 브랜드 팝업스토어를 운영하고 싶습니다.",
                LocalDateTime.of(2026, 7, 10, 10, 0),
                ReservationStatus.PENDING
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(reservationRepository.findById(300L)).thenReturn(Optional.of(reservation));
        when(chatRoomRepository.findByFounderIdAndLandlordIdAndListingId(2L, 1L, 10L)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            ReflectionTestUtils.setField(room, "id", 500L);
            return room;
        });

        ReservationDecisionResponse response = reservationService.approveReservation(1L, 300L);

        assertEquals("APPROVED", response.status().code());
        assertEquals("승인 완료", response.status().label());
        assertEquals(500L, response.chatRoomId());
        assertEquals(ReservationStatus.APPROVED, reservation.getStatus());
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    void landlordCanRejectPendingReservation() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                listingRepository,
                userRepository,
                chatRoomRepository
        );

        User landlord = createUser(1L, UserRole.LANDLORD, "김임대");
        User founder = createUser(2L, UserRole.FOUNDER, "박창업");
        Listing listing = createListing(10L, landlord, "홍대입구역 1번 출구 앞 1층 점포");
        Reservation reservation = createReservation(
                400L,
                listing,
                founder,
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12),
                "패션 브랜드 팝업스토어를 운영하고 싶습니다.",
                LocalDateTime.of(2026, 7, 10, 10, 0),
                ReservationStatus.PENDING
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(reservationRepository.findById(400L)).thenReturn(Optional.of(reservation));

        ReservationDecisionResponse response = reservationService.rejectReservation(1L, 400L);

        assertEquals("REJECTED", response.status().code());
        assertEquals("거절", response.status().label());
        assertNull(response.chatRoomId());
        assertEquals(ReservationStatus.REJECTED, reservation.getStatus());
    }

    @Test
    void approveFailsWhenApprovedReservationOverlaps() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ListingRepository listingRepository = mock(ListingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                listingRepository,
                userRepository,
                chatRoomRepository
        );

        User landlord = createUser(1L, UserRole.LANDLORD, "김임대");
        User founder = createUser(2L, UserRole.FOUNDER, "박창업");
        Listing listing = createListing(10L, landlord, "홍대입구역 1번 출구 앞 1층 점포");
        Reservation reservation = createReservation(
                500L,
                listing,
                founder,
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12),
                "패션 브랜드 팝업스토어를 운영하고 싶습니다.",
                LocalDateTime.of(2026, 7, 10, 10, 0),
                ReservationStatus.PENDING
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(reservationRepository.findById(500L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.countOverlappingReservations(
                10L,
                ReservationStatus.APPROVED,
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12),
                500L
        )).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reservationService.approveReservation(1L, 500L)
        );

        assertEquals("RESERVATION_SCHEDULE_CONFLICT", exception.getCode());
    }

    private CreateReservationRequest createReservationRequest() {
        return new CreateReservationRequest(
                10L,
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12),
                "패션 브랜드 팝업스토어를 운영하고 싶습니다."
        );
    }

    private Reservation createReservation(
            Long reservationId,
            Listing listing,
            User founder,
            LocalDate startDate,
            LocalDate endDate,
            String message,
            LocalDateTime appliedAt,
            ReservationStatus status
    ) {
        int usageDays = (int) (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
        Reservation reservation = new Reservation(
                listing,
                founder,
                startDate,
                endDate,
                usageDays,
                message,
                appliedAt
        );
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        if (status == ReservationStatus.APPROVED) {
            reservation.approve(appliedAt.plusDays(1));
        } else if (status == ReservationStatus.REJECTED) {
            reservation.reject(appliedAt.plusDays(1));
        }

        return reservation;
    }

    private Listing createListing(Long listingId, User landlord, String title) {
        Listing listing = new Listing(
                landlord,
                title,
                List.of("https://cdn.example.com/listings/1.jpg"),
                "서울 마포구 양화로 153",
                "1층",
                new BigDecimal("37.5551000"),
                new BigDecimal("126.9235000"),
                150000L,
                1000000L,
                new BigDecimal("66.1"),
                List.of("와이파이"),
                List.of(),
                List.of(),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31),
                1,
                14,
                "홍대 메인 거리 인근에 위치한 팝업 공간입니다.",
                List.of("#홍대", "#1층"),
                LocalDateTime.of(2026, 7, 14, 9, 0)
        );
        ReflectionTestUtils.setField(listing, "id", listingId);
        return listing;
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
