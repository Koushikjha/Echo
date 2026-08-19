package com.example.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            Map<String, Object> attributes =
                    accessor.getSessionAttributes();

            if (attributes == null) {
                throw new IllegalStateException("No websocket session attributes");
            }

            Long userId =
                    (Long) attributes.get("userId");

            if (userId == null) {
                throw new IllegalStateException("Unauthenticated websocket user");
            }

            accessor.setUser(
                    new StompPrincipal(userId.toString())
            );

            log.info(
                    "[WS_CONNECT_AUTHENTICATED] userId={}",
                    userId
            );
        }

        return message;
    }
}