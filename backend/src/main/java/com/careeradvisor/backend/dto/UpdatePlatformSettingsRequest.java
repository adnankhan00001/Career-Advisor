package com.careeradvisor.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlatformSettingsRequest {

    @NotNull(message = "mentorshipCapacity is required")
    @Valid
    private MentorshipCapacityConfigDto mentorshipCapacity;
}
