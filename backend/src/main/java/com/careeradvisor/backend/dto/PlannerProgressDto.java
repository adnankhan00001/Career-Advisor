package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerProgressDto {
    private Long activePlanId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private int totalTasks;
    private int completedTasks;
    private int pendingTasks;
    private int skippedTasks;
    private int overdueTasks;
    private int completionPercentage;
    private int weeklyMinutesPlanned;
    private int weeklyMinutesCompleted;
    private int currentStreak;
}
