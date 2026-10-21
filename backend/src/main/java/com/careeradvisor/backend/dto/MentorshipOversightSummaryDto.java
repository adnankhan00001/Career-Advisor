package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorshipOversightSummaryDto {
    private long totalRequests;
    private long pendingRequests;
    private long acceptedRequests;
    private long completedRequests;
    private long declinedRequests;
    private long cancelledRequests;
    private long terminatedRequests;
    private long activeMentorships;
}
