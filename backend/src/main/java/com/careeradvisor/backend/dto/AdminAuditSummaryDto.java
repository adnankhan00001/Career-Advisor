package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditSummaryDto {
    private long totalEvents;
    private long todayEvents;
    private long mentorActionsCount;
    private long userActionsCount;
    private long mentorshipActionsCount;
    private long supportActionsCount;
    private long settingsActionsCount;
}
