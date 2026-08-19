package com.example.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String handleName;
    private String fullName;
    private String phone;
}
