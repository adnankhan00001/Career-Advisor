package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "roadmap_revisions", indexes = {
    @Index(name = "idx_rr_roadmap", columnList = "roadmap_id"),
    @Index(name = "idx_rr_rev_num", columnList = "roadmap_id, revision_number")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    @JsonIgnore
    private PersonalizedRoadmap roadmap;

    @Column(name = "revision_number", nullable = false)
    @Builder.Default
    private int revisionNumber = 1;

    @Column(name = "change_type", nullable = false, length = 64)
    private String changeType;

    @Column(name = "affected_milestone_id")
    private Long affectedMilestoneId;

    @Column(name = "affected_step_id")
    private Long affectedStepId;

    @Column(length = 255)
    private String summary;

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "actor_email", length = 120)
    private String actorEmail;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
