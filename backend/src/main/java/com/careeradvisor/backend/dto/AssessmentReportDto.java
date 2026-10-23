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
public class AssessmentReportDto {

    private Long sessionId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime submittedAt;
    private int overallScore; // percentage (0-100)
    private String overallProficiency;

    @Builder.Default
    private List<TechnologyResultDto> technologyResults = new ArrayList<>();

    @Builder.Default
    private List<QuestionReviewDto> questionReviews = new ArrayList<>();
}
