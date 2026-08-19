package com.example.websocket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WebSocketMessageRequest {
    private Long receiverId;
    private String content;
    private Long conversationId;  // null for first message
}