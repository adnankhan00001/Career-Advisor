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
public class RoadmapAiProgressDto {
    private String careerGoal;
    private int completedStepsCount;
    private int totalStepsCount;
    private int completionPercentage;

    @Builder.Default
    private List<String> completedStepTitles = new ArrayList<>();

    private String nextRecommendedStep;
}
