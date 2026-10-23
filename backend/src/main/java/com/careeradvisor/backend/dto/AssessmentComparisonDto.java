package com.careeradvisor.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentComparisonDto {

    private Long reassessmentSessionId;
    private Long baselineSessionId;
    private LocalDateTime reassessmentDate;
    private LocalDateTime baselineDate;

    @Builder.Default
    private List<TechnologyComparisonDto> technologyComparisons = new ArrayList<>();

    private int improvedCompetenciesCount;
    private int declinedCompetenciesCount;
    private int unchangedCompetenciesCount;
    private int newEvidenceCompetenciesCount;
    private int insufficientEvidenceCount;
    private int unassessedCompetenciesCount;
    private String summary;
}
