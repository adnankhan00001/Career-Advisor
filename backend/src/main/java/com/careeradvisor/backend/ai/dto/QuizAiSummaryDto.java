package com.careeradvisor.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAiSummaryDto {
    private Integer score;
    private Integer totalQuestions;
    private Integer percentage;
    private String evaluatedLevel;
    private String recommendedCareer;
    private LocalDateTime attemptDate;
}
