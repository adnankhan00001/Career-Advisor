package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemProgressSummaryDto {
    private int totalProblems;
    private int solvedProblems;
    private int unsolvedProblems;
    private int completionPercentage;

    private int easyTotal;
    private int easySolved;

    private int mediumTotal;
    private int mediumSolved;

    private int hardTotal;
    private int hardSolved;

    private List<TopicProgressStat> topicStats;
    private List<CategoryProgressStat> categoryStats;
    private String nextRecommendedProblem;
}
