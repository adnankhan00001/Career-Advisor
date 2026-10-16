package com.careeradvisor.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerAiSummaryDto {
    private String weeklyGoal;
    private int completionPercentage;
    private int totalTasks;
    private int completedTasks;
    private int dailyStudyMinutes;
    private int currentStreak;

    @Builder.Default
    private List<String> todayScheduledTasks = new ArrayList<>();
}
