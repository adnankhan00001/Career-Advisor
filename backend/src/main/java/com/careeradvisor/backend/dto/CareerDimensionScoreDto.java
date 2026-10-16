package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerDimensionScoreDto {
    private DimensionType dimension;
    private String displayName;
    private int score;
    private int weightPercentage;
    private double weightedContribution;
    private boolean attempted;
    private String statusDescription;
    private String detailMetric;
}
