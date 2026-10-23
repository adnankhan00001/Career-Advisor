package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedMilestoneDto {
    private Long id;
    private String title;
    private String technologyName;
    private String technologySlug;
    private int sequenceOrder;
    private int targetWeekNumber;
    private Double estimatedHours;
    private String status;
    private boolean isCustom;
    private boolean isSkipped;
    private String customNotes;
    @Builder.Default
    private List<PersonalizedStepDto> steps = new ArrayList<>();
}
