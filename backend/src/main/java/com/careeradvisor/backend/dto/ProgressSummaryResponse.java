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
public class ProgressSummaryResponse {
    private String careerGoal;
    private int roadmapPercent;
    private int completedStepsCount;
    private int totalStepsCount;
    private int skillsCount;
    private List<String> skills;
    private List<String> completedSteps;
    private String userLevel;
    private String latestQuizScore;
    private String nextTopicToLearn;
    private int skillMatchPercentage;
}
