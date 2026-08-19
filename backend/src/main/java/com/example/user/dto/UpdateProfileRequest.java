// com/gigshield/user/dto/UpdateProfileRequest.java
package com.example.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    private String handleName;


}