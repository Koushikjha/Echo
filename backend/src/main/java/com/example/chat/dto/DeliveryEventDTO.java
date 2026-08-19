package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class DeliveryEventDTO {
    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private boolean isLastMessage;
    public DeliveryEventDTO(Long messageId,Long conversationId,Long senderId){
        this.messageId=messageId;
        this.conversationId=conversationId;
        this.senderId=senderId;
    }
}
