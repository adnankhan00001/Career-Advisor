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
public class PlannerAdminStatsDto {
    private long activePlanners;
    private long totalPlanners;
    private double avgCompletionPercentage;
    private double avgPlannedMinutes;
    private double avgCompletedMinutes;
    private Map<String, Long> tasksByCategory;
    private double overallAdherencePercentage;
}
