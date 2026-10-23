package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "personalized_roadmap_steps", indexes = {
    @Index(name = "idx_prs_milestone", columnList = "milestone_id"),
    @Index(name = "idx_prs_order", columnList = "milestone_id, sequence_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedRoadmapStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    @JsonIgnore
    private PersonalizedRoadmapMilestone milestone;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "competency_key", length = 120)
    private String competencyKey;

    @Column(length = 32)
    @Builder.Default
    private String level = "Intermediate";

    @Column(name = "estimated_minutes", nullable = false)
    @Builder.Default
    private int estimatedMinutes = 60;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private int sequenceOrder = 1;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private boolean isCompleted = false;

    @Column(name = "state", length = 32)
    @Builder.Default
    private String state = "NOT_STARTED";

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_tested_out", nullable = false)
    @Builder.Default
    private boolean isTestedOut = false;

    @Column(name = "priority", length = 32)
    @Builder.Default
    private String priority = "CORE";

    @Column(name = "evidence_status", length = 32)
    @Builder.Default
    private String evidenceStatus = "NOT_ASSESSED";

    @Column(name = "assessment_score")
    private Integer assessmentScore;
}
