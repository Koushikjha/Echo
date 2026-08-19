package com.example.websocket.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDeletedEvent {

    private String type;

    private Long conversationId;

    private Long messageId;

    private boolean lastMessageDeleted;
}
