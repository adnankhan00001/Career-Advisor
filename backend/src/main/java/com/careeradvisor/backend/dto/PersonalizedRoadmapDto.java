package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedRoadmapDto {
    private Long id;
    private String careerTitle;
    private String careerSlug;
    private Double totalEstimatedHours;
    private int completedStepsCount;
    private int totalStepsCount;
    private int progressPercentage;
    private Integer dailyStudyMinutes;
    private LocalDate targetCompletionDate;
    private Boolean isTargetDateRealistic;
    private LocalDate projectedCompletionDate;
    private int testedOutCount;
    private int focusAreasCount;
    private String status;
    @Builder.Default
    private List<PersonalizedMilestoneDto> milestones = new ArrayList<>();
}
