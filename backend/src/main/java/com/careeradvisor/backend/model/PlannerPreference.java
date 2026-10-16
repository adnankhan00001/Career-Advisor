package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "planner_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "daily_study_minutes", nullable = false)
    @Builder.Default
    private Integer dailyStudyMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_study_window", nullable = false)
    @Builder.Default
    private StudyWindow preferredStudyWindow = StudyWindow.FLEXIBLE;

    @Column(name = "preferred_days", nullable = false)
    @Builder.Default
    private String preferredDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";

    @Column(name = "weekly_goal", nullable = false)
    @Builder.Default
    private String weeklyGoal = "Improve target career readiness";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
