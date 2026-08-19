package com.example.auth.controller;

import com.example.auth.dto.CompleteProfileRequest;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CompleteProfileController {

    private final UserService userService;

    @PostMapping("/complete-profile")
    public ResponseEntity<Void> completeProfile(
            @Valid @RequestBody CompleteProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        userService.completeProfile(
                userDetails.getUsername(),   // phone (your UserDetails.getUsername() returns phone)
                request.getFullName(),
                request.getHandleName()
        );
        return ResponseEntity.ok().build();
    }
}