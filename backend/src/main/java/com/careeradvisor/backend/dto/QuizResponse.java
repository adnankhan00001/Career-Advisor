package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private int score;
    private int totalQuestions;
    private int percentage;
    private String level;
    private String recommendedCareer;
    private Map<String, Integer> categoryScores;

    public QuizResponse(String level) {
        this.level = level;
    }
}