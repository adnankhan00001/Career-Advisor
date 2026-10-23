package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.ProposalStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapUpdateProposalDto {

    private Long id;
    private Long roadmapId;
    private Long roadmapVersion;
    private Long sourceSessionId;
    private Long baselineSessionId;
    private ProposalStatus status;

    private String currentRoadmapTitle;
    private int currentCompletionPercentage;
    private double currentRemainingHours;
    private double proposedRemainingHours;
    private int currentEstimatedWeeks;
    private int proposedEstimatedWeeks;
    private int currentDailyStudyMinutes;
    private LocalDate proposedTargetCompletionDate;

    private int testedOutCandidatesCount;
    private int priorityUpdatesCount;
    private int preservedCompletedCount;

    private AssessmentComparisonDto comparison;

    @Builder.Default
    private List<RoadmapProposalChangeItemDto> changeItems = new ArrayList<>();

    @Builder.Default
    private String preservationNotice = "Your completed work and existing project evidence will be preserved.";

    private LocalDateTime createdAt;
    private LocalDateTime appliedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;

    private boolean isStale;
}
