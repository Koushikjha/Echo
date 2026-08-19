package com.example.websocket.controller;

import com.example.chat.dto.MessageDTO;
import com.example.chat.service.ChatServicePrivate;
import com.example.websocket.dto.WebSocketMessageRequest;
import com.example.websocket.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final PresenceService presenceService;


    private final ChatServicePrivate chatServicePrivate;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload WebSocketMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Long senderId = (Long) headerAccessor.getSessionAttributes().get("userId");

        log.info("[WS_SEND] sender={} receiver={} convoId={}",
                senderId, request.getReceiverId(), request.getConversationId());

        MessageDTO saved = chatServicePrivate.sendMessage(
                senderId,
                request.getReceiverId(),
                request.getContent(),
                request.getConversationId()
        );

        // Send to receiver's private queue
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getReceiverId()),
                "/queue/messages",
                saved
        );

        // Send back to sender for confirmation
        messagingTemplate.convertAndSendToUser(
                String.valueOf(senderId),
                "/queue/messages",
                saved
        );

        log.info("[WS_SEND_SUCCESS] messageId={}", saved.getMessageId());
    }

    @MessageMapping("/presence.heartbeat")
    public void heartbeat(Principal principal) {

        if (principal == null) return;

        Long userId =
                Long.parseLong(principal.getName());

        presenceService.refreshPresence(userId);
    }

}
