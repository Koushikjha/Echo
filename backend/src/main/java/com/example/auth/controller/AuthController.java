package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AuthService;
import com.example.auth.service.OnlineUserTracker;
import com.example.user.entity.User;
import com.example.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final UserService userService;

    private final OnlineUserTracker onlineUserTracker;

    /**
     * Step 1 — request OTP.
     * Accepts both JSON (API) and form POST (browser login page).
     */
    @PostMapping(value = "/send-otp",
            consumes = {"application/json", "application/x-www-form-urlencoded"})
    public ResponseEntity<Void> sendOtp(
            @Valid @RequestBody(required = false) SendOtpRequest jsonRequest,
            @RequestParam(required = false) String phone) {

        SendOtpRequest req = jsonRequest;
        if (req == null) {
            req = new SendOtpRequest();
            req.setPhone(phone);
        }
        authService.sendOtp(req);
        return ResponseEntity.ok().build();
    }

    /**
     * Step 2 — verify OTP and receive JWT.
     * Sets JWT as HttpOnly cookie so the browser can use it,
     * and also returns JSON for API clients.
     */
    @PostMapping(value = "/verify-otp",
            consumes = {"application/json", "application/x-www-form-urlencoded"})
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody(required = false) VerifyOtpRequest jsonRequest,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String otp,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String handleName,
            HttpServletRequest request,
            HttpServletResponse response) {

        VerifyOtpRequest req = jsonRequest;
        if (req == null) {
            req = new VerifyOtpRequest();
            req.setPhone(phone);
            req.setOtp(otp);
            req.setFullName(fullName);
            req.setHandleName(handleName);
        }

        AuthResponse auth = authService.verifyOtp(req);

        // Set JWT cookie so browser pages are authenticated
        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", auth.getAccessToken())
                .httpOnly(true)
                .secure(false)        // true if HTTPS
                .path("/")
                .sameSite("Lax")
                .maxAge(auth.getExpiresIn())
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        // If this was a browser form POST, redirect to home
        String accept = request.getHeader("Accept");
        if (accept == null || !accept.contains("application/json")) {
            try {
                response.sendRedirect("/home");
                return null;
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(auth);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) {

        if (userDetails != null) {
            authService.logout(userDetails.getUsername());
            onlineUserTracker.userLoggedOut(userDetails.getUsername());
        }

        // 🔥 Bulletproof delete — EXACT match of how browser stored it
        response.setHeader("Set-Cookie",
                "JWT_TOKEN=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/auth/me")
    public ResponseEntity<MeResponse> me(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByPhone(userDetails.getUsername());

        return ResponseEntity.ok(MeResponse.builder()
                .phone(user.getPhone())
                .profileComplete(user.isProfileComplete())
                .build());
    }
}