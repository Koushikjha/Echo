package com.example.chat.group.entities;

import com.example.chat.conversation.entity.Conversation;
import com.example.chat.group.enums.GroupType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "group_profile",
        indexes = {
                @Index(name = "idx_gp_conversation", columnList = "conversation_id")
        }
)
@Getter
@Setter
@Builder
public class GroupProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private Conversation conversation;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType groupType;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
