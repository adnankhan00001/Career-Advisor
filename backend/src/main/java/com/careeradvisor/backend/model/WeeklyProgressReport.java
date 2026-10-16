package com.careeradvisor.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "weekly_progress_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyProgressReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "overall_progress_score", nullable = false)
    private Integer overallProgressScore;

    @Column(name = "career_readiness_score", nullable = false)
    private Integer careerReadinessScore;

    @Column(name = "roadmap_progress_percent", nullable = false)
    private Integer roadmapProgressPercent;

    @Column(name = "dsa_solved_count", nullable = false)
    private Integer dsaSolvedCount;

    @Column(name = "dsa_total_count", nullable = false)
    private Integer dsaTotalCount;

    @Column(name = "interview_avg_score", nullable = false)
    private Integer interviewAvgScore;

    @Column(name = "interview_completed_count", nullable = false)
    private Integer interviewCompletedCount;

    @Column(name = "planner_completion_percent", nullable = false)
    private Integer plannerCompletionPercent;

    @Column(name = "planner_completed_tasks", nullable = false)
    private Integer plannerCompletedTasks;

    @Column(name = "planner_skipped_tasks", nullable = false)
    private Integer plannerSkippedTasks;

    @Column(name = "planner_study_minutes", nullable = false)
    private Integer plannerStudyMinutes;

    @Column(name = "streak_days", nullable = false)
    private Integer streakDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "roadmap_trend", nullable = false)
    private TrendDirection roadmapTrend;

    @Enumerated(EnumType.STRING)
    @Column(name = "dsa_trend", nullable = false)
    private TrendDirection dsaTrend;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_trend", nullable = false)
    private TrendDirection interviewTrend;

    @Enumerated(EnumType.STRING)
    @Column(name = "planner_trend", nullable = false)
    private TrendDirection plannerTrend;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_trend", nullable = false)
    private TrendDirection overallTrend;

    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(name = "weaknesses_json", columnDefinition = "TEXT")
    private String weaknessesJson;

    @Column(name = "improvements_json", columnDefinition = "TEXT")
    private String improvementsJson;

    @Column(name = "top_achievements_json", columnDefinition = "TEXT")
    private String topAchievementsJson;

    @Column(name = "priority_areas_json", columnDefinition = "TEXT")
    private String priorityAreasJson;

    @Column(name = "next_week_focus_json", columnDefinition = "TEXT")
    private String nextWeekFocusJson;

    @Column(name = "coach_summary", columnDefinition = "TEXT")
    private String coachSummary;

    @Column(name = "source", length = 32, nullable = false)
    private String source;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
