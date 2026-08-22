package com.careeradvisor.backend.ai.model;

import com.careeradvisor.backend.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_logs", indexes = {
    @Index(name = "idx_ai_usage_user_id", columnList = "user_id"),
    @Index(name = "idx_ai_usage_created_at", columnList = "created_at"),
    @Index(name = "idx_ai_usage_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "request_type", nullable = false)
    private String requestType; // "CONTEXT_EVALUATION", "CHAT_COMPLETION", "ASSESSMENT_ASSIST"

    @Builder.Default
    @Column(name = "prompt_tokens")
    private int promptTokens = 0;

    @Builder.Default
    @Column(name = "completion_tokens")
    private int completionTokens = 0;

    @Builder.Default
    @Column(name = "total_tokens")
    private int totalTokens = 0;

    @Column(name = "status", nullable = false)
    private String status; // "SUCCESS", "FAILED", "DISABLED", "TIMEOUT", "RATE_LIMITED"

    @Builder.Default
    @Column(name = "latency_ms")
    private long latencyMs = 0;

    @Column(name = "failure_category")
    private String failureCategory;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
