package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.CompetencyChangeClassification;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyChangeDto {

    private String competencyKey;
    private String competencyTitle;
    private String technologySlug;
    private String level;
    private CompetencyChangeClassification classification;
    private String previousStatus;
    private String newStatus;
    private Integer previousScore;
    private Integer newScore;

    /**
     * Difference in competency score expressed as percentage points (newScore - previousScore).
     * Distinct from relative percentage change.
     */
    private Integer scoreDelta;
    private boolean isComparable;
    private String explanation;
}
