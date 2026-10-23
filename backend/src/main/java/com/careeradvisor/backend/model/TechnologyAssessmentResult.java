package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "technology_assessment_results", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tech_result", columnNames = {"session_id", "technology_id"})
}, indexes = {
    @Index(name = "idx_tar_session", columnList = "session_id"),
    @Index(name = "idx_tar_tech", columnList = "technology_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnologyAssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private AssessmentSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technology_id", nullable = false)
    @JsonIgnore
    private Technology technology;

    @Column(name = "raw_score", nullable = false)
    private int rawScore;

    @Column(name = "marks_earned", nullable = false)
    private int marksEarned;

    @Column(name = "marks_available", nullable = false)
    private int marksAvailable;

    @Column(name = "evaluated_questions_count", nullable = false)
    private int evaluatedQuestionsCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "incorrect_count", nullable = false)
    private int incorrectCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "unanswered_count", nullable = false)
    private int unansweredCount;

    @Column(name = "topic_coverage_json", columnDefinition = "TEXT")
    private String topicCoverageJson;

    @Column(name = "estimated_proficiency", nullable = false, length = 50)
    private String estimatedProficiency;

    @Column(name = "confidence_level", nullable = false, length = 32)
    private String confidenceLevel;

    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(name = "weaknesses_json", columnDefinition = "TEXT")
    private String weaknessesJson;

    @Column(name = "scoring_version", nullable = false, length = 32)
    @Builder.Default
    private String scoringVersion = "v2.0-heuristic";

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
