package com.careeradvisor.backend.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnologyComparisonDto {

    private String technologySlug;
    private String technologyName;
    private Integer previousScore;
    private Integer newScore;

    /**
     * Difference in score expressed as percentage points (newScore - previousScore), e.g., +15 percentage points.
     * Distinct from relative percentage change.
     */
    private Integer scoreDelta;

    private String previousProficiency;
    private String newProficiency;
    private String previousConfidence;
    private String newConfidence;
    private boolean isComparable;
    private String incomparableReason;

    @Builder.Default
    private List<CompetencyChangeDto> competencies = new ArrayList<>();
}
