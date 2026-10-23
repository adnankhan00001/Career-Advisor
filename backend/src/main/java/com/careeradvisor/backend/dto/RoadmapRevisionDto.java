package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapRevisionDto {
    private Long id;
    private int revisionNumber;
    private String changeType;
    private Long affectedMilestoneId;
    private String affectedMilestoneTitle;
    private Long affectedStepId;
    private String affectedStepTitle;
    private String summary;
    private String previousValue;
    private String newValue;
    private String actorEmail;
    private LocalDateTime createdAt;
}
