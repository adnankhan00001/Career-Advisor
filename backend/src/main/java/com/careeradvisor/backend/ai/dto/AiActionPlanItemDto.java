package com.careeradvisor.backend.ai.dto;

import com.careeradvisor.backend.dto.ActionCategory;
import com.careeradvisor.backend.dto.GapPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiActionPlanItemDto {
    private String id;
    private String title;
    private String description;
    private ActionCategory category;
    private GapPriority priority;
    private String whyItMatters;
    private String expectedOutcome;
    private String estimatedEffort;
    private String actionUrl;
    private int estimatedImpactPoints;
}
