package com.example.auth.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserTracker {
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public void userLoggedIn(String username) {
        onlineUsers.add(username);
    }

    public void userLoggedOut(String username) {
        onlineUsers.remove(username);
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }

    public Set<String> getOnlineUsers() {
        return Set.copyOf(onlineUsers); // immutable snapshot
    }
}

