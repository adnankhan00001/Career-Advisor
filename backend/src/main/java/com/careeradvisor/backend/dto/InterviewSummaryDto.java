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
public class InterviewSummaryDto {
    private int totalInterviews;
    private int completedInterviews;
    private int averageScore;
    private int bestScore;
    private int latestScore;
    private String strongestCategory;
    private String weakestCategory;
    private List<MockInterviewHistoryItemDto> recentInterviews;
}
