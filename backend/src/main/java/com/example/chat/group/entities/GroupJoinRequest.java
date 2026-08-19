package com.example.chat.group.entities;

import com.example.chat.group.enums.JoinRequestStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "group_join_request",
        indexes = {
                @Index(name = "idx_gjr_group_status", columnList = "group_id, status"),
                @Index(name = "idx_gjr_user", columnList = "requesting_user_id, status")
        }
)
@Getter
@Setter
@Builder
public class GroupJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "requesting_user_id", nullable = false)
    private Long requestingUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JoinRequestStatus status;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
