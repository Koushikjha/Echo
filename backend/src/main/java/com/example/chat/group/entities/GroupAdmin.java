package com.example.chat.group.entities;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "group_admin",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_group_admin",
                        columnNames = {"group_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_ga_group", columnList = "group_id")
        }
)
@Getter
@Setter
@Builder
public class GroupAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;              // conversationId

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private Long assignedBy;           // null if original creator

    @PrePersist
    public void prePersist() {
        this.assignedAt = LocalDateTime.now();
    }
}
