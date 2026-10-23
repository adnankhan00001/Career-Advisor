package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roadmap_update_proposals", indexes = {
    @Index(name = "idx_rup_user_status", columnList = "user_id, status"),
    @Index(name = "idx_rup_roadmap", columnList = "roadmap_id"),
    @Index(name = "idx_rup_source_session", columnList = "source_session_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapUpdateProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    @JsonIgnore
    private PersonalizedRoadmap roadmap;

    @Column(name = "roadmap_version", nullable = false)
    @Builder.Default
    private Long roadmapVersion = 0L;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_session_id", nullable = false)
    @JsonIgnore
    private AssessmentSession sourceSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baseline_session_id")
    @JsonIgnore
    private AssessmentSession baselineSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(name = "comparison_summary_json", columnDefinition = "TEXT")
    private String comparisonSummaryJson;

    @Column(name = "proposed_estimated_weeks")
    private Integer proposedEstimatedWeeks;

    @Column(name = "proposed_target_completion_date")
    private LocalDate proposedTargetCompletionDate;

    @Column(name = "current_remaining_hours")
    @Builder.Default
    private Double currentRemainingHours = 0.0;

    @Column(name = "proposed_remaining_hours")
    @Builder.Default
    private Double proposedRemainingHours = 0.0;

    @Column(name = "tested_out_candidates_count")
    @Builder.Default
    private int testedOutCandidatesCount = 0;

    @Column(name = "priority_updates_count")
    @Builder.Default
    private int priorityUpdatesCount = 0;

    @Column(name = "preserved_completed_count")
    @Builder.Default
    private int preservedCompletedCount = 0;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<RoadmapProposalChangeItem> changeItems = new ArrayList<>();

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addChangeItem(RoadmapProposalChangeItem item) {
        changeItems.add(item);
        item.setProposal(this);
    }
}
