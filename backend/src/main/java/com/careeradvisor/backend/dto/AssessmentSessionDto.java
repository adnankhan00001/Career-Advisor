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
public class AssessmentSessionDto {

    private Long sessionId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime expiresAt;
    private long remainingSeconds;
    private int timeLimitMinutes;
    private int totalQuestionsCount;
    private int answeredCount;
    private int skippedCount;
    private int currentOrder;
    private Boolean isReassessment;
    private Long baselineSessionId;

    @Builder.Default
    private List<CatalogTechnologyDto> selectedTechnologies = new ArrayList<>();

    @Builder.Default
    private List<AssessmentQuestionDto> questions = new ArrayList<>();
}
