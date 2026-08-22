package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mock_interviews", indexes = {
    @Index(name = "idx_mock_interview_user_id", columnList = "user_id"),
    @Index(name = "idx_mock_interview_started_at", columnList = "started_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProblemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Builder.Default
    @Column(nullable = false)
    private int score = 0;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Builder.Default
    @Column(name = "correct_count", nullable = false)
    private int correctCount = 0;

    @Column(name = "strong_areas", columnDefinition = "TEXT")
    private String strongAreas;

    @Column(name = "weak_areas", columnDefinition = "TEXT")
    private String weakAreas;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
