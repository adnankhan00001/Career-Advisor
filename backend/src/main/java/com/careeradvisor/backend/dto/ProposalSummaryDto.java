package com.careeradvisor.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalSummaryDto {

    private Long id;
    private Long sourceSessionId;
    private String status;
    private int testedOutCandidatesCount;
    private int priorityUpdatesCount;
    private int preservedCompletedCount;
    private Double proposedRemainingHours;
    private LocalDateTime createdAt;
    private LocalDateTime appliedAt;
}
