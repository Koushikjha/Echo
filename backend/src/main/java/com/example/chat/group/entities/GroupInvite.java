package com.example.chat.group.entities;

import com.example.chat.group.enums.InviteStatus;
import com.example.chat.group.enums.InviteType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "group_invite",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invite_code",
                        columnNames = {"invite_code"}
                )
        },
        indexes = {
                @Index(name = "idx_gi_group_status", columnList = "group_id, status"),
                @Index(name = "idx_gi_invited_user", columnList = "invited_user_id, status"),
                @Index(name = "idx_gi_invite_code", columnList = "invite_code")
        }
)
@Getter
@Setter
@Builder
public class GroupInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "invited_user_id")
    private Long invitedUserId;        // null for LINK type

    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;            // adminId

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteType type;           // DIRECT, LINK

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status;       // PENDING, ACCEPTED, REJECTED, REVOKED, EXPIRED

    @Column(name = "invite_code", unique = true)
    private String inviteCode;         // null for DIRECT type

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;   // null = never expires

    @Column(name = "max_uses")
    private Integer maxUses;           // null = unlimited

    @Column(name = "use_count", nullable = false)
    private Integer useCount = 0;      // LINK type only

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;  // null until acted upon

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.useCount = 0;
    }
}
