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
public class OnboardingSelectTechnologiesRequest {

    @NotEmpty(message = "At least one technology slug must be selected")
    private List<String> technologySlugs;
}
