package com.dsm9.kolpop.domain.chat.service;

import com.dsm9.kolpop.domain.auth.repository.UserRepository;
import com.dsm9.kolpop.domain.chat.dto.ChatMessageRequest;
import com.dsm9.kolpop.domain.chat.dto.ChatMessageResponse;
import com.dsm9.kolpop.domain.chat.dto.ChatRoomResponse;
import com.dsm9.kolpop.domain.chat.dto.CreateChatRoomRequest;
import com.dsm9.kolpop.domain.chat.entity.ChatMessage;
import com.dsm9.kolpop.domain.chat.entity.ChatRoom;
import com.dsm9.kolpop.domain.chat.repository.ChatMessageRepository;
import com.dsm9.kolpop.domain.chat.repository.ChatRoomRepository;
import com.dsm9.kolpop.domain.user.entity.User;
import com.dsm9.kolpop.domain.user.entity.UserRole;
import com.dsm9.kolpop.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoomResponse createRoom(CreateChatRoomRequest request, Authentication authentication) {
        User founder = getAuthenticatedUser(authentication);
        if (founder.getRole() != UserRole.FOUNDER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ONLY_FOUNDER_CAN_CREATE_CHAT", "창업자만 채팅방을 만들 수 있습니다.");
        }

        User landlord = findUser(request.landlordId());
        if (landlord.getRole() != UserRole.LANDLORD) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TARGET_MUST_BE_LANDLORD", "임대인과만 채팅방을 만들 수 있습니다.");
        }

        ChatRoom room = chatRoomRepository.findByFounderIdAndLandlordId(founder.getId(), landlord.getId())
                .orElseGet(() -> chatRoomRepository.save(new ChatRoom(founder, landlord)));
        return ChatRoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getRooms(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return chatRoomRepository.findAllByFounderIdOrLandlordIdOrderByCreatedAtDesc(user.getId(), user.getId())
                .stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        ChatRoom room = getRoomForParticipant(roomId, user.getId());
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
        ChatMessage message = chatMessageRepository.save(new ChatMessage(room, sender, request.content()));
        return ChatMessageResponse.from(message);
    }

    private ChatRoom getRoomForParticipant(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."));
        if (!room.hasParticipant(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "CHAT_ROOM_ACCESS_DENIED", "채팅방 참여자만 접근할 수 있습니다.");
        }
        return room;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
        }
        return findUser(parseUserId(authentication.getName()));
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
