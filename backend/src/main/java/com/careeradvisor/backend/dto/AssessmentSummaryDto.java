package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSummaryDto {

    private Long sessionId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime submittedAt;
    private Integer overallScore;
    private String overallProficiency;

    @Builder.Default
    private List<String> assessedTechnologies = new ArrayList<>();
}
