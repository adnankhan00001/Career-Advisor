package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.OnboardingState;
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
public class OnboardingStateDto {
    private OnboardingState state;
    private CatalogCareerDto selectedCareer;
    @Builder.Default
    private List<CatalogTechnologyDto> selectedTechnologies = new ArrayList<>();
    private OnboardingStudyPreferencesDto studyPreferences;
    private Long currentAssessmentSessionId;
    private String assessmentStatus;
    private boolean hasPersonalizedRoadmap;
    private boolean canResumeAssessment;
    private boolean canGenerateRoadmap;
}
