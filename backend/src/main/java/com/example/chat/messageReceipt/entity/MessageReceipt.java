package com.example.chat.messageReceipt.entity;

import com.example.chat.message.entity.ChatMessage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_receipt",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_user",
                        columnNames = {"message_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_receipt_message", columnList = "message_id"),
                @Index(name = "idx_receipt_user", columnList = "user_id"),
                @Index(name = "idx_receipt_user_delivered", columnList = "user_id, delivered"),
                @Index(name = "idx_receipt_user_seen", columnList = "user_id, seen")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String handleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id",nullable = false)
    private ChatMessage message;

    @Column(nullable = false)
    private boolean delivered = false;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(nullable = false)
    private boolean seen = false;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

    @Column(
            name = "deleted_for_me",
            nullable = false
    )
    private boolean deletedForMe ;

    @PrePersist
    public void prePersist(){
        this.deletedForMe=false;
    }

}