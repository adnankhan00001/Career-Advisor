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
public class RecommendationAiSummaryDto {
    private int overallReadinessScore;
    private String userLifecycleState;
    private String topCareerMatch;
    private int topCareerMatchPercentage;

    @Builder.Default
    private List<String> missingSkills = new ArrayList<>();

    @Builder.Default
    private List<String> topActionableRecommendations = new ArrayList<>();
}
