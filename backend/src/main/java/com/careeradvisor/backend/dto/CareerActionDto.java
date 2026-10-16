package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerActionDto {
    private String id;
    private String title;
    private String description;
    private ActionCategory category;
    private GapPriority priority;
    private String reasoning;
    private int estimatedImpactPoints;
    private String actionUrl;
    private boolean completed;
}
