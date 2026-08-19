package com.example.user.dto;

import com.example.user.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long             id;
    private String           phone;
    private String           handleName;
    private String           fullName;
    private UserStatus status;
    private LocalDateTime createdAt;
}
