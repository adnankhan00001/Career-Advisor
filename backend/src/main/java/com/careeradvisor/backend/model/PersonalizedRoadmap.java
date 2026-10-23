package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "personalized_roadmaps", indexes = {
    @Index(name = "idx_pr_user", columnList = "user_id"),
    @Index(name = "idx_pr_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_track_id")
    @JsonIgnore
    private CareerTrack careerTrack;

    @Column(name = "career_title", nullable = false)
    private String careerTitle;

    @Column(name = "career_slug")
    private String careerSlug;

    @Column(name = "total_estimated_hours")
    @Builder.Default
    private Double totalEstimatedHours = 0.0;

    @Column(name = "completed_steps_count")
    @Builder.Default
    private int completedStepsCount = 0;

    @Column(name = "total_steps_count")
    @Builder.Default
    private int totalStepsCount = 0;

    @Column(name = "progress_percentage")
    @Builder.Default
    private int progressPercentage = 0;

    @Column(name = "daily_study_minutes")
    @Builder.Default
    private Integer dailyStudyMinutes = 60;

    @Column(name = "target_completion_date")
    private LocalDate targetCompletionDate;

    @Column(name = "is_target_date_realistic")
    @Builder.Default
    private Boolean isTargetDateRealistic = true;

    @Column(name = "projected_completion_date")
    private LocalDate projectedCompletionDate;

    @Column(name = "status", length = 32)
    @Builder.Default
    private String status = "ACTIVE";

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<PersonalizedRoadmapMilestone> milestones = new ArrayList<>();

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addMilestone(PersonalizedRoadmapMilestone milestone) {
        milestones.add(milestone);
        milestone.setRoadmap(this);
    }
}
