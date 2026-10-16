package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.LearningPlanSource;
import com.careeradvisor.backend.model.LearningPlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPlanDto {
    private Long id;
    private Long userId;
    private String targetCareer;
    private Integer readinessScoreAtGeneration;
    private LearningPlanStatus status;
    private LearningPlanSource source;
    private String weeklyGoal;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private int totalTasks;
    private int completedTasks;
    private int completionPercentage;
    private int totalMinutesPlanned;
    private int totalMinutesCompleted;
    private int currentStreak;

    @Builder.Default
    private List<DailyScheduleDto> days = new ArrayList<>();

    private LocalDateTime generatedAt;
}
