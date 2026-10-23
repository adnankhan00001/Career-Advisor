package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartReassessmentRequest {

    @NotEmpty(message = "At least one technology must be selected for reassessment")
    private List<String> technologySlugs;

    private List<String> competencyKeys;

    private Long baselineSessionId;

    private Integer timeLimitMinutes;

    private Integer minQuestionsPerTechnology;

    private Integer maxQuestionsPerTechnology;
}
