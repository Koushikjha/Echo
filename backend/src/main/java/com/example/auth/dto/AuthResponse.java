// com/gigshield/auth/dto/AuthResponse.java
package com.example.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;    // "Bearer"
    private long   expiresIn;
    private boolean newUser;    // seconds
}