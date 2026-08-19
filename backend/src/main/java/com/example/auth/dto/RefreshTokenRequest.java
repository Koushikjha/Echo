// com/gigshield/auth/dto/RefreshTokenRequest.java
package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}