package com.careeradvisor.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DsaAiProgressDto {
    private int solvedCount;
    private int totalCount;
    private int completionPercentage;

    @Builder.Default
    private Map<String, Integer> categoryDistribution = new HashMap<>();

    private String nextRecommendedProblem;
}
