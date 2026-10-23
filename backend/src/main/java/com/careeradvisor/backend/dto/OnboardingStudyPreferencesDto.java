package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.StudyWindow;
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
public class OnboardingStudyPreferencesDto {
    private Integer dailyStudyMinutes;
    private StudyWindow preferredStudyWindow;
    private List<String> preferredDays;
    private LocalDate targetCompletionDate;
}
