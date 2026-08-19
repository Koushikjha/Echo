package com.example.eventLog.entity;

import com.example.eventLog.enums.AggregateType;
import com.example.eventLog.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_log",
        indexes = {
                @Index(
                        name = "idx_event_aggregate",
                        columnList = "aggregate_type, aggregate_id, version"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_idempotency",
                        columnNames = "idempotency_key"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AggregateType aggregateType;

    @Column(nullable = false, updatable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EventType eventType;

    @Column(nullable = false, updatable = false)
    private Long actorId;

    @Column(nullable = false, updatable = false)
    private Long version;

    @Column(nullable = false, updatable = false)
    private String idempotencyKey;

    @Lob
    @Column(nullable = false, updatable = false)
    private String payload;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}