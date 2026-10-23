package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "assessment_sessions", indexes = {
    @Index(name = "idx_as_user_status", columnList = "user_id, status"),
    @Index(name = "idx_as_expires", columnList = "expires_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private AssessmentStatus status = AssessmentStatus.IN_PROGRESS;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "assessment_session_technologies",
        joinColumns = @JoinColumn(name = "session_id"),
        inverseJoinColumns = @JoinColumn(name = "technology_id")
    )
    @Builder.Default
    private Set<Technology> selectedTechnologies = new HashSet<>();

    @Column(name = "start_time", nullable = false)
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "time_limit_minutes", nullable = false)
    @Builder.Default
    private int timeLimitMinutes = 20;

    @Column(name = "total_questions_count", nullable = false)
    @Builder.Default
    private int totalQuestionsCount = 0;

    @Column(name = "answered_questions_count", nullable = false)
    @Builder.Default
    private int answeredQuestionsCount = 0;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "overall_proficiency", length = 50)
    private String overallProficiency;

    @Column(name = "assessment_version", length = 32)
    @Builder.Default
    private String assessmentVersion = "v2.0-adaptive";

    @Column(name = "is_reassessment")
    @Builder.Default
    private Boolean isReassessment = false;

    @Column(name = "baseline_session_id")
    private Long baselineSessionId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (startTime == null) startTime = LocalDateTime.now();
        if (expiresAt == null) expiresAt = startTime.plusMinutes(timeLimitMinutes > 0 ? timeLimitMinutes : 20);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
