// com/gigshield/user/controller/UserController.java
package com.example.user.controller;


import com.example.user.dto.UpdateProfileRequest;
import com.example.user.dto.UserDTO;
import com.example.user.dto.UserResponse;
import com.example.user.entity.User;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), request));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = userService.resolveUserId(userDetails);

        return ResponseEntity.ok(
                userService.getAllUsersExcept(currentUserId)
        );
    }


}