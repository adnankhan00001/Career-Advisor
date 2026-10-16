package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.TrendDirection;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyProgressReportDto {
    private Long id;
    private Long userId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private Integer overallProgressScore;
    private Integer careerReadinessScore;
    private Integer roadmapProgress;
    private Integer dsaProgress;
    private Integer dsaSolvedCount;
    private Integer dsaTotalCount;
    private Integer interviewProgress;
    private Integer interviewCompletedCount;
    private Integer plannerConsistency;
    private Integer completedTasks;
    private Integer skippedTasks;
    private Integer studyMinutes;
    private Integer streakDays;

    private TrendDirection roadmapTrend;
    private TrendDirection dsaTrend;
    private TrendDirection interviewTrend;
    private TrendDirection plannerTrend;
    private TrendDirection overallTrend;

    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> improvements;
    private List<String> topAchievements;
    private List<String> priorityAreas;
    private List<ProgressActionDto> nextWeekFocus;

    private String coachSummary;
    private String source;
    private Instant generatedAt;
}
