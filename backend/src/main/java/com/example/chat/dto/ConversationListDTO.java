package com.example.chat.dto;

import com.example.chat.conversation.enums.ConversationType;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class ConversationListDTO {
    private Long conversationId;
    private Long otherUserId;
    private String otherHandleName;
    private LocalDateTime lastMessageAt;
    private ConversationType type;
    private Long unreadCount;
    private Long lastMessageId;
    private String lastMessage;
    private boolean online;
    private LocalDateTime lastSeen;
    private boolean lastMessageDeleted;
    ConversationListDTO(Long conversationId,Long otherUserId,String otherHandleName,LocalDateTime lastMessageAt,
                        ConversationType conversationType){
        this.conversationId=conversationId;
        this.otherUserId=otherUserId;
        this.otherHandleName=otherHandleName;
        this.lastMessageAt=lastMessageAt;
        this.type=conversationType;
    }
}
