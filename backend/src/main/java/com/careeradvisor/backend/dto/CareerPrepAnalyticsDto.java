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
public class CareerPrepAnalyticsDto {
    private long totalResumes;
    private long resumesUploadedInPeriod;
    private List<TimeSeriesPointDto> resumeUploadTrend;
    private long totalQuizAttempts;
    private long totalSolvedProblems;
    private long totalMockInterviews;
    private long mockInterviewsInPeriod;
    private long completedInterviewsCount;
    private List<TimeSeriesPointDto> mockInterviewTrend;
    private Map<String, Long> careerGoalDistribution;
    private Map<String, Long> userLevelDistribution;
}
