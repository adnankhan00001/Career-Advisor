package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.StudyWindow;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerPreferenceDto {

    @NotNull
    @Min(value = 15, message = "Daily study time must be at least 15 minutes")
    @Max(value = 480, message = "Daily study time cannot exceed 480 minutes (8 hours)")
    private Integer dailyStudyMinutes;

    @NotNull
    private StudyWindow preferredStudyWindow;

    @NotNull
    private List<String> preferredDays;

    private String weeklyGoal;
}
