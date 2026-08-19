package com.example.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL =
            Duration.ofSeconds(30);

    private String key(Long userId) {
        return "presence:user:" + userId;
    }

    public void markOnline(Long userId) {

        redisTemplate.opsForValue().set(
                key(userId),
                "ONLINE",
                TTL
        );

        redisTemplate.opsForSet()
                .add("online-users", userId);

        log.info("[USER_ONLINE] userId={}", userId);
    }

    public void refreshPresence(Long userId) {

        redisTemplate.expire(
                key(userId),
                TTL
        );
    }

    public void markOffline(Long userId) {

        redisTemplate.delete(
                key(userId)
        );

        redisTemplate.opsForSet()
                .remove("online-users", userId);

        log.info("[USER_OFFLINE] userId={}", userId);
    }

    public boolean isOnline(Long userId) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(
                        key(userId)
                )
        );
    }

    public Set<Long> getOnlineUsers() {

        Set<Object> members =
                redisTemplate.opsForSet()
                        .members("online-users");

        if (members == null) {
            return Set.of();
        }

        return members.stream()
                .map(member ->
                        Long.parseLong(member.toString())
                )
                .collect(Collectors.toSet());
    }
}
