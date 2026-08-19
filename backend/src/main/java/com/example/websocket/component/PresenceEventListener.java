package com.example.websocket.component;

import com.example.user.service.UserService;
import com.example.websocket.dto.PresenceDTO;
import com.example.websocket.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceEventListener {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() == null) return;

        Long userId =
                Long.parseLong(accessor.getUser().getName());

        presenceService.markOnline(userId);



        messagingTemplate.convertAndSend(
                "/topic/presence",
                new PresenceDTO(
                        userId,
                        true,
                        null
                )
        );
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() == null) return;

        Long userId =
                Long.parseLong(accessor.getUser().getName());

        LocalDateTime lastSeen =
                LocalDateTime.now();

        presenceService.markOffline(userId);

        userService.updateLastSeen(
                userId,
                lastSeen
        );

        messagingTemplate.convertAndSend(
                "/topic/presence",
                new PresenceDTO(
                        userId,
                        false,
                        lastSeen
                )
        );
    }
}