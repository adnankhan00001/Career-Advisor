package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSelectCareerRequest {

    @NotBlank(message = "Career slug cannot be blank")
    private String careerSlug;
}
