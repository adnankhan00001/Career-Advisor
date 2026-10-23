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
public class QuestionReviewDto {

    private int presentationOrder;
    private String technologySlug;
    private String technologyName;
    private String topic;
    private String difficulty;
    private String questionText;

    @Builder.Default
    private List<String> options = new ArrayList<>();

    private Integer selectedOptionIndex;
    private int correctOptionIndex; // Revealed upon completed assessment report!
    private boolean isCorrect;
    private String explanation;     // Revealed upon completed assessment report!
    private int awardedMarks;
    private int scoringWeight;
    private String answerStatus;
}
