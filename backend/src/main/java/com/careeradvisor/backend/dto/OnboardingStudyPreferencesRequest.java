package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.StudyWindow;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStudyPreferencesRequest {

    @NotNull(message = "Daily study minutes is required")
    @Min(value = 15, message = "Daily study time must be at least 15 minutes")
    @Max(value = 480, message = "Daily study time cannot exceed 480 minutes (8 hours)")
    private Integer dailyStudyMinutes;

    private StudyWindow preferredStudyWindow;

    @NotEmpty(message = "Preferred days list cannot be empty")
    private List<String> preferredDays;

    private LocalDate targetCompletionDate;
}
