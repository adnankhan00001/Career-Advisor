package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnologyResultDto {

    private String technologySlug;
    private String technologyName;
    private int rawScore; // percentage (0-100)
    private int marksEarned;
    private int marksAvailable;
    private int evaluatedQuestionsCount;
    private int correctCount;
    private int incorrectCount;
    private int skippedCount;
    private int unansweredCount;

    @Builder.Default
    private List<String> topicCoverage = new ArrayList<>();

    private String estimatedProficiency; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, INSUFFICIENT_EVIDENCE
    private String confidenceLevel;     // LOW, MEDIUM, HIGH

    @Builder.Default
    private List<String> strengths = new ArrayList<>();

    @Builder.Default
    private List<String> weaknesses = new ArrayList<>();
}
