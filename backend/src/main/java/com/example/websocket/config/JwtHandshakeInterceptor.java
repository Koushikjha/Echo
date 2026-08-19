package com.example.websocket.config;

import com.example.auth.util.JwtUtil;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        try {

            if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                return false;
            }

            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            Cookie[] cookies = httpRequest.getCookies();

            if (cookies == null) {
                log.warn("[WS_AUTH] No cookies found");
                return false;
            }

            for (Cookie cookie : cookies) {

                if ("JWT_TOKEN".equals(cookie.getName())) {

                    String token = cookie.getValue();

                    if (!jwtUtil.isTokenValid(token)) {
                        log.warn("[WS_AUTH] Invalid JWT");
                        return false;
                    }

                    String phone = jwtUtil.extractPhone(token);
                    User user = userRepository.findByPhone(phone)
                            .orElseThrow();

                    attributes.put("userId", user.getId());
                    attributes.put("phone", user.getPhone());
                    attributes.put("handleName", user.getHandleName());

                    log.info("[WS_AUTH_SUCCESS] phone={}", phone);

                    return true;
                }
            }

            log.warn("[WS_AUTH] JWT cookie missing");
            return false;

        } catch (Exception ex) {

            log.error("[WS_AUTH_ERROR]", ex);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {

    }
}
