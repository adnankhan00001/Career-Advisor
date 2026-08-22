package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsOverviewDto {
    private long totalUsers;
    private long totalResumes;
    private long totalQuizAttempts;
    private long totalSolvedProblems;
    private long totalMockInterviews;
    private long totalCompletedInterviews;
    private long totalCareerGoals;
    private Map<String, Long> careerGoalsDistribution;
    private Map<String, Long> userLevelDistribution;
}
