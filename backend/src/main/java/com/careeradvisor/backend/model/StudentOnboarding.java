package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_onboardings", indexes = {
    @Index(name = "idx_so_user", columnList = "user_id"),
    @Index(name = "idx_so_state", columnList = "state")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private OnboardingState state = OnboardingState.NOT_STARTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_track_id")
    @JsonIgnore
    private CareerTrack selectedCareer;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "onboarding_technologies",
        joinColumns = @JoinColumn(name = "onboarding_id"),
        inverseJoinColumns = @JoinColumn(name = "technology_id")
    )
    @Builder.Default
    private Set<Technology> selectedTechnologies = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_session_id")
    @JsonIgnore
    private AssessmentSession assessmentSession;

    @Column(name = "daily_study_minutes")
    @Builder.Default
    private Integer dailyStudyMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_study_window", length = 32)
    @Builder.Default
    private StudyWindow preferredStudyWindow = StudyWindow.FLEXIBLE;

    @Column(name = "preferred_days", length = 200)
    @Builder.Default
    private String preferredDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";

    @Column(name = "target_completion_date")
    private LocalDate targetCompletionDate;

    @Column(name = "has_personalized_roadmap", nullable = false)
    @Builder.Default
    private boolean hasPersonalizedRoadmap = false;

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
}
