package com.dsm9.kolpop.domain.chat.controller;

import com.dsm9.kolpop.domain.chat.dto.ChatMessageResponse;
import com.dsm9.kolpop.domain.chat.dto.ChatRoomRequestResponse;
import com.dsm9.kolpop.domain.chat.dto.ChatRoomResponse;
import com.dsm9.kolpop.domain.chat.dto.CreateChatRoomRequest;
import com.dsm9.kolpop.domain.chat.service.ChatService;
import com.dsm9.kolpop.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/rooms")
    public ApiResponse<ChatRoomResponse> createRoom(
            @Valid @RequestBody CreateChatRoomRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(chatService.createRoom(request, authentication));
    }

    @GetMapping("/rooms")
    public ApiResponse<List<ChatRoomResponse>> getRooms(Authentication authentication) {
        return ApiResponse.success(chatService.getRooms(authentication));
    }

    @GetMapping("/room-requests")
    public ApiResponse<List<ChatRoomRequestResponse>> getRoomRequests(Authentication authentication) {
        return ApiResponse.success(chatService.getRoomRequests(authentication));
    }

    @PatchMapping("/rooms/{roomId}/accept")
    public ApiResponse<ChatRoomResponse> acceptRoom(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        return ApiResponse.success(chatService.acceptRoom(roomId, authentication));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        return ApiResponse.success(chatService.getMessages(roomId, authentication));
    }
}
