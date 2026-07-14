package com.dsm9.kolpop.domain.chat.controller;

import com.dsm9.kolpop.domain.chat.dto.ChatMessageRequest;
import com.dsm9.kolpop.domain.chat.dto.ChatMessageResponse;
import com.dsm9.kolpop.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Valid @Payload ChatMessageRequest request,
            Principal principal
    ) {
        ChatMessageResponse response = chatService.sendMessage(roomId, request, principal);
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, response);
    }
}
