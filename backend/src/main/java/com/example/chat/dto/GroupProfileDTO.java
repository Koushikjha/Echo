package com.example.chat.dto;

import com.example.chat.group.enums.GroupType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class GroupProfileDTO {
    private Long conversationId;
    private String name;
    private String description;
    private String avatar;
    private GroupType groupType;
    private Long createdBy;
    private LocalDateTime createdAt;

}
