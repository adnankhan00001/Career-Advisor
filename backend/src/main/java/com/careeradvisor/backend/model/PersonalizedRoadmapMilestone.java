package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "personalized_roadmap_milestones", indexes = {
    @Index(name = "idx_prm_roadmap", columnList = "roadmap_id"),
    @Index(name = "idx_prm_order", columnList = "roadmap_id, sequence_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedRoadmapMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    @JsonIgnore
    private PersonalizedRoadmap roadmap;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "technology_name", length = 120)
    private String technologyName;

    @Column(name = "technology_slug", length = 120)
    private String technologySlug;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private int sequenceOrder = 1;

    @Column(name = "estimated_hours")
    @Builder.Default
    private Double estimatedHours = 0.0;

    @Column(name = "target_week_number", nullable = false)
    @Builder.Default
    private int targetWeekNumber = 1;

    @Column(length = 32)
    @Builder.Default
    private String status = "UPCOMING";

    @Column(name = "is_custom", nullable = false)
    @Builder.Default
    private boolean isCustom = false;

    @Column(name = "is_skipped", nullable = false)
    @Builder.Default
    private boolean isSkipped = false;

    @Column(name = "custom_notes", columnDefinition = "TEXT")
    private String customNotes;

    @OneToMany(mappedBy = "milestone", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<PersonalizedRoadmapStep> steps = new ArrayList<>();

    public void addStep(PersonalizedRoadmapStep step) {
        steps.add(step);
        step.setMilestone(this);
    }
}
