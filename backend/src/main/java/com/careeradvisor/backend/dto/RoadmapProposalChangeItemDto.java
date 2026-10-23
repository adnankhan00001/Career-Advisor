package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.RoadmapChangeAction;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapProposalChangeItemDto {

    private Long id;
    private RoadmapChangeAction actionType;
    private Long milestoneId;
    private String milestoneTitle;
    private String technologySlug;
    private Long stepId;
    private String stepTitle;
    private String competencyKey;
    private String currentPriority;
    private String proposedPriority;
    private Integer currentEstimatedMinutes;
    private Integer proposedEstimatedMinutes;
    private Boolean currentTestedOut;
    private Boolean proposedTestedOut;
    private Boolean isCompleted;
    private String state;
    private String reasoning;
    private int sequenceOrder;
}
