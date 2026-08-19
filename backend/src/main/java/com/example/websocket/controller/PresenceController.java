package com.example.websocket.controller;

import com.example.websocket.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/presence")
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/online-users")
    public Set<Long> onlineUsers() {
        return presenceService.getOnlineUsers();
    }
}