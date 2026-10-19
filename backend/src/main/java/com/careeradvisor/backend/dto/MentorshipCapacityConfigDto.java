package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorshipCapacityConfigDto {

    @NotNull(message = "minMentees is required")
    @Min(value = 1, message = "minMentees must be at least 1")
    private Integer minMentees;

    @NotNull(message = "maxMentees is required")
    @Min(value = 1, message = "maxMentees must be at least 1")
    private Integer maxMentees;

    @NotNull(message = "defaultMentees is required")
    @Min(value = 1, message = "defaultMentees must be at least 1")
    private Integer defaultMentees;
}
