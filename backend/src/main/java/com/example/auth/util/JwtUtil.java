package com.example.auth.util;

import com.example.config.AppConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private final Key signingKey;

    public JwtUtil(@Value("${Echo.jwt.secret}") String secret) {
        this.signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes());
    }

    // 🔥 Generate token using PHONE as subject
    public String generateToken(String phone) {
        return Jwts.builder()
                .setSubject(phone) // 👈 phone stored in subject
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + AppConstants.JWT_EXPIRY_MS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // optional: if you still want claims version
    public String generateAccessToken(String phone, Map<String, Object> claims) {
        return Jwts.builder()
                .setSubject(phone)
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + AppConstants.JWT_EXPIRY_MS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 🔥 Extract PHONE (not username anymore)
    public String extractPhone(String token) {
        return parseClaims(token).getSubject();
    }



    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}