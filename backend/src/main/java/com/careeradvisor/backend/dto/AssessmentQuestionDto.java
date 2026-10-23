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
public class AssessmentQuestionDto {

    private Long sessionQuestionId;
    private int presentationOrder;
    private String technologySlug;
    private String technologyName;
    private String topic;
    private String difficulty;
    private String questionText;

    @Builder.Default
    private List<String> options = new ArrayList<>();

    private int scoringWeight;
    private Integer selectedOptionIndex;
    private String answerStatus; // UNANSWERED, SKIPPED, ANSWERED
}
