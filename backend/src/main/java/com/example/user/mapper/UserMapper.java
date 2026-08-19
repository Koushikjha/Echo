package com.example.user.mapper;

import com.example.auth.dto.RegisterRequest;
import com.example.user.dto.UserResponse;
import com.example.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .handleName(user.getHandleName())   // was missing
                .fullName(user.getFullName())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public User toEntity(RegisterRequest req) {
        return User.builder()
                .phone(req.getPhone())
                .handleName(req.getHandleName())
                .fullName(req.getFullName())
                .build();
    }
}