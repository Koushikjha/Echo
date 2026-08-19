package com.example.user.entity;

import com.example.user.enums.UserRole;
import com.example.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_user_phone", columnList = "phone")
        })
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = true)
    private String handleName;

    @Column(nullable = true)
    private String fullName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false, unique = true, length = 10)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private boolean profileComplete = false;

    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastSeen;

    @PrePersist
    void prePersist() {
        lastSeen         =LocalDateTime.now();
        createdAt        = LocalDateTime.now();
        updatedAt        = LocalDateTime.now();
        status           = UserStatus.ONLINE;
        role             = UserRole.ROLE_ADMIN;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override public String  getPassword()             { return null; }
    @Override public String  getUsername()             { return phone; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
