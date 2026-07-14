package com.dsm9.kolpop.domain.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.chat.entity.ChatRoom;
import com.dsm9.kolpop.domain.chat.repository.ChatRoomRepository;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.reservation.dto.CreateReservationRequest;
import com.dsm9.kolpop.domain.reservation.dto.CreateReservationResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationDecisionResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationManagementItemResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationManagementResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationManagementSummaryResponse;
import com.dsm9.kolpop.domain.reservation.dto.ReservationStatusResponse;
import com.dsm9.kolpop.domain.reservation.entity.Reservation;
import com.dsm9.kolpop.domain.reservation.entity.ReservationStatus;
import com.dsm9.kolpop.domain.reservation.repository.ReservationRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            ListingRepository listingRepository,
            UserRepository userRepository,
            ChatRoomRepository chatRoomRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
    }

    @Transactional
    public CreateReservationResponse createReservation(Long userId, CreateReservationRequest request) {
        User founder = getFounder(userId);
        Listing listing = getReservableListing(request.listingId());
        validateReservationPeriod(listing, request.startDate(), request.endDate(), null);

        int usageDays = calculateUsageDays(request.startDate(), request.endDate());
        Reservation reservation = new Reservation(
                listing,
                founder,
                request.startDate(),
                request.endDate(),
                usageDays,
                request.message().trim(),
                LocalDateTime.now()
        );

        Reservation savedReservation = reservationRepository.save(reservation);
        return new CreateReservationResponse(
                savedReservation.getId(),
                toStatusResponse(savedReservation.getStatus()),
                savedReservation.getAppliedAt()
        );
    }

    @Transactional(readOnly = true)
    public ReservationManagementResponse getManagementReservations(Long userId) {
        User landlord = getLandlord(userId);
        List<Reservation> reservations = reservationRepository.findAllByListingLandlordIdOrderByAppliedAtDesc(landlord.getId());
        Map<ChatRoomKey, Long> chatRoomIds = getChatRoomIds(landlord.getId(), reservations);

        long pendingCount = reservations.stream().filter(Reservation::isPending).count();
        long approvedCount = reservations.stream().filter(Reservation::isApproved).count();

        List<ReservationManagementItemResponse> items = reservations.stream()
                .map(reservation -> toManagementItemResponse(
                        reservation,
                        chatRoomIds.get(ChatRoomKey.from(reservation))
                ))
                .toList();

        return new ReservationManagementResponse(
                new ReservationManagementSummaryResponse(
                        pendingCount,
                        approvedCount,
                        reservations.size()
                ),
                items
        );
    }

    @Transactional
    public ReservationDecisionResponse approveReservation(Long userId, Long reservationId) {
        User landlord = getLandlord(userId);
        Reservation reservation = getOwnedReservation(landlord, reservationId);
        validatePendingReservation(reservation);
        validateReservationPeriod(
                reservation.getListing(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getId()
        );

        reservation.approve(LocalDateTime.now());
        Long chatRoomId = chatRoomRepository.findByFounderIdAndListingId(
                        reservation.getFounder().getId(),
                        reservation.getListing().getId()
                )
                .filter(ChatRoom::isAccepted)
                .map(ChatRoom::getId)
                .orElse(null);

        return new ReservationDecisionResponse(
                reservation.getId(),
                toStatusResponse(reservation.getStatus()),
                chatRoomId
        );
    }

    @Transactional
    public ReservationDecisionResponse rejectReservation(Long userId, Long reservationId) {
        User landlord = getLandlord(userId);
        Reservation reservation = getOwnedReservation(landlord, reservationId);
        validatePendingReservation(reservation);

        reservation.reject(LocalDateTime.now());

        return new ReservationDecisionResponse(
                reservation.getId(),
                toStatusResponse(reservation.getStatus()),
                null
        );
    }

    private ReservationManagementItemResponse toManagementItemResponse(Reservation reservation, Long chatRoomId) {
        boolean canApprove = reservation.isPending();
        boolean canReject = reservation.isPending();
        boolean canChat = reservation.isApproved() && chatRoomId != null;

        return new ReservationManagementItemResponse(
                reservation.getId(),
                reservation.getListing().getId(),
                reservation.getListing().getTitle(),
                reservation.getFounder().getId(),
                reservation.getFounder().getName(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getUsageDays(),
                reservation.getAppliedAt(),
                reservation.getMessage(),
                toStatusResponse(reservation.getStatus()),
                chatRoomId,
                canApprove,
                canReject,
                canChat
        );
    }

    private Map<ChatRoomKey, Long> getChatRoomIds(Long landlordId, List<Reservation> reservations) {
        List<Long> listingIds = reservations.stream()
                .filter(Reservation::isApproved)
                .map(reservation -> reservation.getListing().getId())
                .distinct()
                .toList();

        if (listingIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return chatRoomRepository.findAllByLandlordIdAndListingIdIn(landlordId, listingIds)
                .stream()
                .filter(ChatRoom::isAccepted)
                .collect(Collectors.toMap(ChatRoomKey::from, ChatRoom::getId));
    }

    private record ChatRoomKey(Long founderId, Long listingId) {

        private static ChatRoomKey from(Reservation reservation) {
            return new ChatRoomKey(reservation.getFounder().getId(), reservation.getListing().getId());
        }

        private static ChatRoomKey from(ChatRoom room) {
            return new ChatRoomKey(room.getFounder().getId(), room.getListing().getId());
        }
    }

    private void validatePendingReservation(Reservation reservation) {
        if (!reservation.isPending()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "RESERVATION_ALREADY_PROCESSED",
                    "이미 처리된 예약입니다."
            );
        }
    }

    private void validateReservationPeriod(
            Listing listing,
            LocalDate startDate,
            LocalDate endDate,
            Long excludeReservationId
    ) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_PERIOD",
                    "예약 시작일은 종료일보다 늦을 수 없습니다."
            );
        }

        if (startDate.isBefore(listing.getOperatingStartDate()) || endDate.isAfter(listing.getOperatingEndDate())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "RESERVATION_OUT_OF_RANGE",
                    "예약 가능 기간을 벗어난 요청입니다."
            );
        }

        int usageDays = calculateUsageDays(startDate, endDate);
        if (usageDays < listing.getMinOperatingDays() || usageDays > listing.getMaxOperatingDays()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RESERVATION_DAYS",
                    "매물의 최소/최대 운영 일수 조건을 만족하지 않습니다."
            );
        }

        long overlappingApprovedReservations = reservationRepository.countOverlappingReservations(
                listing.getId(),
                ReservationStatus.APPROVED,
                startDate,
                endDate,
                excludeReservationId
        );
        if (overlappingApprovedReservations > 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESERVATION_SCHEDULE_CONFLICT",
                    "이미 승인된 예약과 기간이 겹칩니다."
            );
        }
    }

    private int calculateUsageDays(LocalDate startDate, LocalDate endDate) {
        return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }

    private ReservationStatusResponse toStatusResponse(ReservationStatus status) {
        return new ReservationStatusResponse(status.getCode(), status.getLabel());
    }

    private Listing getReservableListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", "매물을 찾을 수 없습니다."));

        if (!listing.isRecruiting()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "LISTING_NOT_RECRUITING", "모집중인 매물만 예약할 수 있습니다.");
        }

        return listing;
    }

    private Reservation getOwnedReservation(User landlord, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예약을 찾을 수 없습니다."));

        if (!reservation.getListing().getLandlord().getId().equals(landlord.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "RESERVATION_ACCESS_DENIED", "본인 매물의 예약만 처리할 수 있습니다.");
        }

        return reservation;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    private User getFounder(Long userId) {
        User user = getUser(userId);
        if (user.getRole() != UserRole.FOUNDER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FOUNDER_ONLY", "창업자 회원만 예약 신청할 수 있습니다.");
        }
        return user;
    }

    private User getLandlord(Long userId) {
        User user = getUser(userId);
        if (user.getRole() != UserRole.LANDLORD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "LANDLORD_ONLY", "임대인 회원만 예약을 관리할 수 있습니다.");
        }
        return user;
    }
}
