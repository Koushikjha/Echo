package com.example.websocket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(
            Long userId,
            Object payload
    ) {

        messagingTemplate.convertAndSend(
                "/topic/user/" + userId,
                payload
        );
    }
}
