package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotBlank;
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
public class AddCustomMilestoneRequest {
    @NotBlank(message = "Milestone title is required")
    private String title;

    private String technologyName;

    private Double estimatedHours;

    private String customNotes;

    @Builder.Default
    private List<String> stepTitles = new ArrayList<>();
}
