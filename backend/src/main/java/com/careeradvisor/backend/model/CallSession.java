package com.careeradvisor.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "call_sessions",
    indexes = {
        @Index(name = "idx_call_receiver_status", columnList = "receiver_id, status"),
        @Index(name = "idx_call_caller_created", columnList = "caller_id, created_at"),
        @Index(name = "idx_call_conv_created", columnList = "conversation_id, created_at"),
        @Index(name = "idx_call_status_created", columnList = "status, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 20)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CallStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder.Default
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 30)
    private EndReason endReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (startedAt == null) startedAt = now;
        if (durationSeconds == null) durationSeconds = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
