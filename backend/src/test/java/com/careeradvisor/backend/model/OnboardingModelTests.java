package com.careeradvisor.backend.model;

import com.careeradvisor.backend.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingModelTests {

    @Test
    @DisplayName("StudentOnboarding builder sets defaults correctly")
    void testStudentOnboardingDefaults() {
        User user = User.builder().id(100L).email("student@test.com").name("Student").build();

        StudentOnboarding onboarding = StudentOnboarding.builder()
                .user(user)
                .build();

        assertEquals(OnboardingState.NOT_STARTED, onboarding.getState());
        assertEquals(60, onboarding.getDailyStudyMinutes());
        assertEquals(StudyWindow.FLEXIBLE, onboarding.getPreferredStudyWindow());
        assertFalse(onboarding.isHasPersonalizedRoadmap());
        assertNotNull(onboarding.getCreatedAt());
        assertNotNull(onboarding.getUpdatedAt());
        assertNotNull(onboarding.getSelectedTechnologies());
        assertTrue(onboarding.getSelectedTechnologies().isEmpty());
    }

    @Test
    @DisplayName("StudentOnboarding lifecycle state progression")
    void testStateProgression() {
        User user = User.builder().id(101L).build();
        StudentOnboarding onboarding = StudentOnboarding.builder().user(user).build();

        assertEquals(OnboardingState.NOT_STARTED, onboarding.getState());

        onboarding.setState(OnboardingState.CAREER_SELECTED);
        assertEquals(OnboardingState.CAREER_SELECTED, onboarding.getState());

        onboarding.setState(OnboardingState.TECHNOLOGIES_SELECTED);
        assertEquals(OnboardingState.TECHNOLOGIES_SELECTED, onboarding.getState());

        onboarding.setState(OnboardingState.STUDY_PREFERENCES_CONFIGURED);
        assertEquals(OnboardingState.STUDY_PREFERENCES_CONFIGURED, onboarding.getState());

        onboarding.setState(OnboardingState.ASSESSMENT_IN_PROGRESS);
        assertEquals(OnboardingState.ASSESSMENT_IN_PROGRESS, onboarding.getState());

        onboarding.setState(OnboardingState.ASSESSMENT_COMPLETED);
        assertEquals(OnboardingState.ASSESSMENT_COMPLETED, onboarding.getState());

        onboarding.setState(OnboardingState.ASSESSMENT_REVIEWED);
        assertEquals(OnboardingState.ASSESSMENT_REVIEWED, onboarding.getState());

        onboarding.setState(OnboardingState.ROADMAP_GENERATED);
        onboarding.setHasPersonalizedRoadmap(true);
        assertEquals(OnboardingState.ROADMAP_GENERATED, onboarding.getState());
        assertTrue(onboarding.isHasPersonalizedRoadmap());
    }

    @Test
    @DisplayName("StudentOnboarding associations with career and technologies")
    void testAssociations() {
        User user = User.builder().id(102L).build();
        Technology java = Technology.builder().id(1L).name("Java").slug("java").build();
        Set<Technology> techs = new HashSet<>();
        techs.add(java);

        CareerTrack track = CareerTrack.builder().id(5L).title("Java Backend Developer").slug("java-backend-developer").build();

        StudentOnboarding onboarding = StudentOnboarding.builder()
                .user(user)
                .selectedCareer(track)
                .selectedTechnologies(techs)
                .dailyStudyMinutes(90)
                .preferredStudyWindow(StudyWindow.MORNING)
                .targetCompletionDate(LocalDate.of(2027, 1, 1))
                .build();

        assertEquals("Java Backend Developer", onboarding.getSelectedCareer().getTitle());
        assertEquals(1, onboarding.getSelectedTechnologies().size());
        assertEquals(90, onboarding.getDailyStudyMinutes());
        assertEquals(StudyWindow.MORNING, onboarding.getPreferredStudyWindow());
        assertEquals(LocalDate.of(2027, 1, 1), onboarding.getTargetCompletionDate());
    }

    @Test
    @DisplayName("Onboarding request and response DTO validation")
    void testDtoMapping() {
        OnboardingSelectCareerRequest careerReq = OnboardingSelectCareerRequest.builder()
                .careerSlug("frontend-developer")
                .build();
        assertEquals("frontend-developer", careerReq.getCareerSlug());

        OnboardingSelectTechnologiesRequest techReq = OnboardingSelectTechnologiesRequest.builder()
                .technologySlugs(List.of("react", "typescript"))
                .build();
        assertEquals(2, techReq.getTechnologySlugs().size());

        OnboardingStudyPreferencesRequest prefReq = OnboardingStudyPreferencesRequest.builder()
                .dailyStudyMinutes(45)
                .preferredStudyWindow(StudyWindow.EVENING)
                .preferredDays(List.of("MONDAY", "WEDNESDAY", "FRIDAY"))
                .targetCompletionDate(LocalDate.now().plusMonths(3))
                .build();
        assertEquals(45, prefReq.getDailyStudyMinutes());
        assertEquals(StudyWindow.EVENING, prefReq.getPreferredStudyWindow());
        assertEquals(3, prefReq.getPreferredDays().size());

        OnboardingStateDto stateDto = OnboardingStateDto.builder()
                .state(OnboardingState.ASSESSMENT_REVIEWED)
                .hasPersonalizedRoadmap(false)
                .canGenerateRoadmap(true)
                .canResumeAssessment(false)
                .build();
        assertEquals(OnboardingState.ASSESSMENT_REVIEWED, stateDto.getState());
        assertTrue(stateDto.isCanGenerateRoadmap());
        assertFalse(stateDto.isHasPersonalizedRoadmap());
    }
}
