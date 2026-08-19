// com/gigshield/auth/dto/VerifyOtpRequest.java
package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$",
            message = "Invalid Indian mobile number")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{4}$", message = "OTP must be 4 digits")
    private String otp;

    // ── Registration fields — only required for new users ────────────────────
    // All optional — if user already exists these are ignored

    private String fullName;

    private String handleName;
}