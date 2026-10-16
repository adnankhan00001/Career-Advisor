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
public class CareerIntelligenceAiSummaryDto {
    private int overallReadinessScore;
    private String readinessState;
    private String targetCareer;

    @Builder.Default
    private List<String> strongestDimensions = new ArrayList<>();

    @Builder.Default
    private List<String> weakestDimensions = new ArrayList<>();

    @Builder.Default
    private List<String> topSkillGaps = new ArrayList<>();

    @Builder.Default
    private List<String> prioritizedActionTitles = new ArrayList<>();
}
