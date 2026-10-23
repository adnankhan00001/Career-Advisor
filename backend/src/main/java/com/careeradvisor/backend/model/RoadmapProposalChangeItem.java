package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "roadmap_proposal_change_items", indexes = {
    @Index(name = "idx_rpci_proposal", columnList = "proposal_id"),
    @Index(name = "idx_rpci_step", columnList = "step_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapProposalChangeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    @JsonIgnore
    private RoadmapUpdateProposal proposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 64)
    private RoadmapChangeAction actionType;

    @Column(name = "milestone_id")
    private Long milestoneId;

    @Column(name = "milestone_title")
    private String milestoneTitle;

    @Column(name = "technology_slug")
    private String technologySlug;

    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "step_title", nullable = false)
    private String stepTitle;

    @Column(name = "competency_key")
    private String competencyKey;

    @Column(name = "current_priority")
    private String currentPriority;

    @Column(name = "proposed_priority")
    private String proposedPriority;

    @Column(name = "current_estimated_minutes")
    private Integer currentEstimatedMinutes;

    @Column(name = "proposed_estimated_minutes")
    private Integer proposedEstimatedMinutes;

    @Column(name = "current_tested_out")
    private Boolean currentTestedOut;

    @Column(name = "proposed_tested_out")
    private Boolean proposedTestedOut;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "state", length = 32)
    private String state;

    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private int sequenceOrder = 1;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
