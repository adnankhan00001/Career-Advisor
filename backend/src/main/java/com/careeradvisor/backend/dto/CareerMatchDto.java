package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerMatchDto {
    private String title;
    private String category;
    private int matchScore; // 0 - 100%
    private int skillScore;
    private int goalScore;
    private int quizScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private boolean targetGoal;
}
