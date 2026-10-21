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
public class MentorshipAnalyticsDto {
    private long totalRequests;
    private long requestsInPeriod;
    private List<TimeSeriesPointDto> requestCreationTrend;
    private Map<String, Long> statusDistribution;
    private double acceptanceRate;
    private double rejectionRate;
    private double completionRate;
    private long totalApprovedMentors;
    private long activeMentorsCount;
    private long totalCapacity;
    private long utilizedCapacity;
    private Double averageResponseTimeHours;
}
