package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$",
            message = "Invalid Indian mobile number")
    private String phone;

    @NotBlank
    private String fullName;

    @NotBlank
    private String handleName;
}
