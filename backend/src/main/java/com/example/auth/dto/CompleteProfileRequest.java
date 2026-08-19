package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteProfileRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String handleName;
}