package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerIntelligenceDto {
    private Long userId;
    private String userName;
    private String targetCareer;
    private String userLevel;
    private int overallReadinessScore;
    private ReadinessState readinessState;
    private String readinessStateDisplayName;
    private String summary;
    private Map<DimensionType, CareerDimensionScoreDto> dimensionScores;
    private List<String> strongestDimensions;
    private List<String> weakestDimensions;
    private List<CareerGapDto> skillGaps;
    private List<CareerActionDto> prioritizedActions;
    private Instant calculatedAt;
}
