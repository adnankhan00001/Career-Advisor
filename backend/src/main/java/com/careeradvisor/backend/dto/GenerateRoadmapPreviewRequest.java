package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRoadmapPreviewRequest {

    @NotNull(message = "Reassessment session ID is required")
    private Long reassessmentSessionId;

    private Long baselineSessionId;
}
