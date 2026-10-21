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
public class UserAnalyticsDto {
    private long totalUsers;
    private long newUsersInPeriod;
    private long activeUsers;
    private long suspendedUsers;
    private long deactivatedUsers;
    private Map<String, Long> roleDistribution;
    private Map<String, Long> statusDistribution;
    private List<TimeSeriesPointDto> registrationTrend;
}
