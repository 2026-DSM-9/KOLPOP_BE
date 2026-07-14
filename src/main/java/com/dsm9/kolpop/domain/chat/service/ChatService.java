package com.dsm9.kolpop.domain.chat.service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.chat.dto.ChatMessageRequest;
import com.dsm9.kolpop.domain.chat.dto.ChatMessageResponse;
import com.dsm9.kolpop.domain.chat.dto.ChatRoomRequestResponse;
import com.dsm9.kolpop.domain.chat.dto.ChatRoomResponse;
import com.dsm9.kolpop.domain.chat.dto.CreateChatRoomRequest;
import com.dsm9.kolpop.domain.chat.entity.ChatMessage;
import com.dsm9.kolpop.domain.chat.entity.ChatRoom;
import com.dsm9.kolpop.domain.chat.repository.ChatMessageRepository;
import com.dsm9.kolpop.domain.chat.repository.ChatRoomRepository;
import com.dsm9.kolpop.domain.listing.entity.Listing;
import com.dsm9.kolpop.domain.listing.repository.ListingRepository;
import com.dsm9.kolpop.domain.reservation.entity.ReservationStatus;
import com.dsm9.kolpop.domain.reservation.repository.ReservationRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ChatRoomResponse createRoom(CreateChatRoomRequest request, Authentication authentication) {
        User founder = getAuthenticatedUser(authentication);
        if (founder.getRole() != UserRole.FOUNDER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ONLY_FOUNDER_CAN_CREATE_CHAT", "창업자만 채팅방을 만들 수 있습니다.");
        }

        Listing listing = findListing(request.listingId());
        User landlord = listing.getLandlord();
        if (landlord.getRole() != UserRole.LANDLORD) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TARGET_MUST_BE_LANDLORD", "임대인과만 채팅방을 만들 수 있습니다.");
        }

        ChatRoom room = chatRoomRepository.findByFounderIdAndLandlordIdAndListingId(
                        founder.getId(),
                        landlord.getId(),
                        listing.getId()
                )
                .orElseGet(() -> createAcceptedRoomAfterReservationApproval(founder, landlord, listing, request.content()));

        return ChatRoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return chatRoomRepository.findAllByFounderIdOrLandlordIdOrderByCreatedAtDesc(user.getId(), user.getId())
                .stream()
                .filter(ChatRoom::isAccepted)
                .map(ChatRoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomRequestResponse> getRoomRequests(Authentication authentication) {
        User landlord = getAuthenticatedUser(authentication);
        if (landlord.getRole() != UserRole.LANDLORD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "LANDLORD_ONLY", "임대인만 채팅 요청을 조회할 수 있습니다.");
        }

        return chatRoomRepository.findAllByFounderIdOrLandlordIdOrderByCreatedAtDesc(landlord.getId(), landlord.getId())
                .stream()
                .filter(room -> room.getLandlord().getId().equals(landlord.getId()))
                .filter(ChatRoom::isPending)
                .map(this::toRoomRequestResponse)
                .toList();
    }

    @Transactional
    public ChatRoomResponse acceptRoom(Long roomId, Authentication authentication) {
        User landlord = getAuthenticatedUser(authentication);
        if (landlord.getRole() != UserRole.LANDLORD) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "LANDLORD_ONLY", "임대인만 채팅 요청을 수락할 수 있습니다.");
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."));
        if (!room.getLandlord().getId().equals(landlord.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "CHAT_ROOM_ACCESS_DENIED", "채팅방 임대인만 요청을 수락할 수 있습니다.");
        }

        room.accept(LocalDateTime.now());
        return ChatRoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        ChatRoom room = getRoomForParticipant(roomId, user.getId());
        validateAcceptedRoom(room);
        return chatMessageRepository.findAllByRoomIdOrderByCreatedAtAsc(room.getId())
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, ChatMessageRequest request, Principal principal) {
        if (principal == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
        }

        Long userId = parseUserId(principal.getName());
        User sender = findUser(userId);
        ChatRoom room = getRoomForParticipant(roomId, sender.getId());
        validateAcceptedRoom(room);
        ChatMessage message = chatMessageRepository.save(new ChatMessage(room, sender, request.content()));
        return ChatMessageResponse.from(message);
    }

    private ChatRoom createAcceptedRoomAfterReservationApproval(
            User founder,
            User landlord,
            Listing listing,
            String content
    ) {
        boolean approvedReservationExists = reservationRepository.existsByFounderIdAndListingIdAndStatus(
                founder.getId(),
                listing.getId(),
                ReservationStatus.APPROVED
        );

        if (!approvedReservationExists) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "RESERVATION_APPROVAL_REQUIRED",
                    "임대인 승인 후 채팅을 시작할 수 있습니다."
            );
        }

        ChatRoom room = new ChatRoom(founder, landlord, listing);
        room.accept(LocalDateTime.now());
        ChatRoom savedRoom = chatRoomRepository.save(room);
        chatMessageRepository.save(new ChatMessage(savedRoom, founder, content.trim()));
        return savedRoom;
    }

    private ChatRoomRequestResponse toRoomRequestResponse(ChatRoom room) {
        ChatMessage message = chatMessageRepository.findFirstByRoomIdOrderByCreatedAtAsc(room.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CHAT_REQUEST_MESSAGE_NOT_FOUND", "채팅 요청 메시지를 찾을 수 없습니다."));
        return ChatRoomRequestResponse.from(room, message);
    }

    private ChatRoom getRoomForParticipant(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."));
        if (!room.hasParticipant(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "CHAT_ROOM_ACCESS_DENIED", "채팅방 참여자만 접근할 수 있습니다.");
        }
        return room;
    }

    private void validateAcceptedRoom(ChatRoom room) {
        if (!room.isAccepted()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "CHAT_REQUEST_NOT_ACCEPTED", "채팅 요청이 수락된 후 메시지를 주고받을 수 있습니다.");
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
        }
        return findUser(parseUserId(authentication.getName()));
    }

    private Listing findListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", "매물을 찾을 수 없습니다."));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION", "인증 정보가 올바르지 않습니다.");
        }
    }
}
