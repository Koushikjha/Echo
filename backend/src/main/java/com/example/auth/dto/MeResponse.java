package com.example.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeResponse {
    private String phone;
    private boolean profileComplete;
}
