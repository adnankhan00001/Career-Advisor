package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderMilestonesRequest {
    @NotEmpty(message = "Milestone IDs list cannot be empty")
    private List<Long> milestoneIds;
}
