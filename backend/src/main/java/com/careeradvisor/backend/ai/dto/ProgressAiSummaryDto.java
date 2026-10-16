package com.careeradvisor.backend.ai.dto;

import com.careeradvisor.backend.model.TrendDirection;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressAiSummaryDto {
    private Integer overallProgressScore;
    private Integer careerReadinessScore;
    private Integer roadmapProgress;
    private Integer dsaSolvedCount;
    private Integer interviewAvgScore;
    private Integer plannerConsistency;
    private Integer currentStreak;
    private TrendDirection overallTrend;
    private List<String> topAchievements;
    private List<String> priorityAreas;
    private String coachSummary;
}
