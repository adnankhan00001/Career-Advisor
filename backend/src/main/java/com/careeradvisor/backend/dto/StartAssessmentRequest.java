package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartAssessmentRequest {

    @NotEmpty(message = "At least one technology slug must be selected")
    private List<String> technologySlugs;

    private Integer timeLimitMinutes;

    private Integer minQuestionsPerTechnology;

    private Integer maxQuestionsPerTechnology;
}
