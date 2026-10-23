package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTests {

    @Mock
    private StudentOnboardingRepository studentOnboardingRepository;

    @Mock
    private CareerTrackRepository careerTrackRepository;

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private AssessmentSessionRepository assessmentSessionRepository;

    @Mock
    private UserRoadmapProgressRepository userRoadmapProgressRepository;

    @Mock
    private PersonalizedRoadmapRepository personalizedRoadmapRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private PlannerService plannerService;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private OnboardingService onboardingService;

    private User student;
    private CareerTrack trackJava;
    private Technology techJava;
    private Technology techSpring;
    private Technology techReact;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L)
                .name("Alex Student")
                .email("alex@test.com")
                .careerGoal("Java Backend Developer")
                .build();

        techJava = Technology.builder()
                .id(10L)
                .name("Java")
                .slug("java")
                .category("Backend")
                .active(true)
                .build();

        techSpring = Technology.builder()
                .id(11L)
                .name("Spring Boot")
                .slug("spring-boot")
                .category("Framework")
                .active(true)
                .build();

        techReact = Technology.builder()
                .id(12L)
                .name("React")
                .slug("react")
                .category("Frontend")
                .active(true)
                .build();

        Set<Technology> trackTechs = new HashSet<>();
        trackTechs.add(techJava);
        trackTechs.add(techSpring);

        trackJava = CareerTrack.builder()
                .id(100L)
                .title("Java Backend Developer")
                .slug("java-backend-developer")
                .active(true)
                .technologies(trackTechs)
                .build();
    }

    @Test
    @DisplayName("First time user gets initialized onboarding state")
    void testGetOnboardingStateFirstTimeUser() {
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.empty());
        when(userRoadmapProgressRepository.countByUser(student)).thenReturn(0L);
        when(careerTrackRepository.findByTitleIgnoreCase("Java Backend Developer")).thenReturn(Optional.of(trackJava));

        StudentOnboarding saved = StudentOnboarding.builder()
                .id(50L)
                .user(student)
                .selectedCareer(trackJava)
                .state(OnboardingState.CAREER_SELECTED)
                .dailyStudyMinutes(60)
                .preferredStudyWindow(StudyWindow.FLEXIBLE)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .build();

        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenReturn(saved);

        CatalogCareerDto careerDto = CatalogCareerDto.builder().id(100L).slug("java-backend-developer").title("Java Backend Developer").build();
        when(catalogService.mapToCareerDto(trackJava)).thenReturn(careerDto);

        OnboardingStateDto result = onboardingService.getOnboardingState(student);

        assertNotNull(result);
        assertEquals(OnboardingState.CAREER_SELECTED, result.getState());
        assertEquals("java-backend-developer", result.getSelectedCareer().getSlug());
        assertFalse(result.isHasPersonalizedRoadmap());
        assertEquals(60, result.getStudyPreferences().getDailyStudyMinutes());
    }

    @Test
    @DisplayName("Returning user with active personalized roadmap marks roadmap generated")
    void testReturningUserWithPersonalizedRoadmapMarksRoadmapGenerated() {
        StudentOnboarding existing = StudentOnboarding.builder()
                .id(51L)
                .user(student)
                .state(OnboardingState.NOT_STARTED)
                .hasPersonalizedRoadmap(false)
                .build();

        PersonalizedRoadmap activeRoadmap = PersonalizedRoadmap.builder()
                .id(101L)
                .user(student)
                .status("ACTIVE")
                .build();

        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));
        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(activeRoadmap));
        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenAnswer(i -> i.getArgument(0));

        OnboardingStateDto result = onboardingService.getOnboardingState(student);

        assertTrue(result.isHasPersonalizedRoadmap());
        assertEquals(OnboardingState.ROADMAP_GENERATED, result.getState());
        assertTrue(result.isCanGenerateRoadmap());
    }

    @Test
    @DisplayName("Select career successfully updates onboarding and user goal")
    void testSelectCareerSuccess() {
        StudentOnboarding existing = StudentOnboarding.builder()
                .id(52L)
                .user(student)
                .state(OnboardingState.NOT_STARTED)
                .selectedTechnologies(new HashSet<>())
                .build();

        when(careerTrackRepository.findBySlugWithTechnologies("java-backend-developer")).thenReturn(Optional.of(trackJava));
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));
        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenAnswer(i -> i.getArgument(0));

        CatalogCareerDto careerDto = CatalogCareerDto.builder().id(100L).slug("java-backend-developer").title("Java Backend Developer").build();
        when(catalogService.mapToCareerDto(trackJava)).thenReturn(careerDto);

        OnboardingStateDto result = onboardingService.selectCareer(student, "java-backend-developer");

        assertNotNull(result);
        assertEquals(OnboardingState.CAREER_SELECTED, result.getState());
        verify(userRepository).save(student);
        assertEquals("Java Backend Developer", student.getCareerGoal());
    }

    @Test
    @DisplayName("Select career with non-existent slug throws IllegalArgumentException")
    void testSelectCareerNotFoundThrowsException() {
        when(careerTrackRepository.findBySlugWithTechnologies("invalid-slug")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> onboardingService.selectCareer(student, "invalid-slug"));
    }

    @Test
    @DisplayName("Select technologies successfully validates and persists technologies")
    void testSelectTechnologiesSuccess() {
        StudentOnboarding existing = StudentOnboarding.builder()
                .id(53L)
                .user(student)
                .selectedCareer(trackJava)
                .state(OnboardingState.CAREER_SELECTED)
                .selectedTechnologies(new HashSet<>())
                .build();

        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));
        when(technologyRepository.findBySlug("java")).thenReturn(Optional.of(techJava));
        when(technologyRepository.findBySlug("spring-boot")).thenReturn(Optional.of(techSpring));
        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenAnswer(i -> i.getArgument(0));

        OnboardingStateDto result = onboardingService.selectTechnologies(student, List.of("java", "spring-boot"));

        assertNotNull(result);
        assertEquals(OnboardingState.TECHNOLOGIES_SELECTED, result.getState());
        assertEquals(2, existing.getSelectedTechnologies().size());
    }

    @Test
    @DisplayName("Select technologies not belonging to selected career throws IllegalArgumentException")
    void testSelectTechnologiesNotBelongingToCareerThrowsException() {
        StudentOnboarding existing = StudentOnboarding.builder()
                .id(54L)
                .user(student)
                .selectedCareer(trackJava) // Track Java has java and spring-boot, NOT react
                .state(OnboardingState.CAREER_SELECTED)
                .selectedTechnologies(new HashSet<>())
                .build();

        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
                onboardingService.selectTechnologies(student, List.of("react")));
    }

    @Test
    @DisplayName("Save study preferences updates onboarding and synchronizes PlannerService")
    void testSaveStudyPreferencesSuccess() {
        StudentOnboarding existing = StudentOnboarding.builder()
                .id(55L)
                .user(student)
                .selectedCareer(trackJava)
                .state(OnboardingState.TECHNOLOGIES_SELECTED)
                .selectedTechnologies(new HashSet<>())
                .build();

        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));
        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenAnswer(i -> i.getArgument(0));

        OnboardingStudyPreferencesRequest request = OnboardingStudyPreferencesRequest.builder()
                .dailyStudyMinutes(90)
                .preferredStudyWindow(StudyWindow.MORNING)
                .preferredDays(List.of("MONDAY", "WEDNESDAY", "FRIDAY"))
                .targetCompletionDate(LocalDate.now().plusMonths(6))
                .build();

        OnboardingStateDto result = onboardingService.saveStudyPreferences(student, request);

        assertNotNull(result);
        assertEquals(OnboardingState.STUDY_PREFERENCES_CONFIGURED, result.getState());
        assertEquals(90, result.getStudyPreferences().getDailyStudyMinutes());
        assertEquals(StudyWindow.MORNING, result.getStudyPreferences().getPreferredStudyWindow());
        verify(plannerService).updatePreferences(eq(student), any(PlannerPreferenceDto.class));
    }

    @Test
    @DisplayName("Save study preferences with invalid daily study time throws IllegalArgumentException")
    void testSaveStudyPreferencesInvalidMinutesThrowsException() {
        OnboardingStudyPreferencesRequest request = OnboardingStudyPreferencesRequest.builder()
                .dailyStudyMinutes(10) // Below minimum 15 mins
                .preferredDays(List.of("MONDAY"))
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                onboardingService.saveStudyPreferences(student, request));
    }

    @Test
    @DisplayName("Start assessment successfully invokes AssessmentService and links session")
    void testStartOnboardingAssessmentSuccess() {
        Set<Technology> techs = new HashSet<>();
        techs.add(techJava);

        StudentOnboarding existing = StudentOnboarding.builder()
                .id(56L)
                .user(student)
                .selectedCareer(trackJava)
                .selectedTechnologies(techs)
                .state(OnboardingState.STUDY_PREFERENCES_CONFIGURED)
                .build();

        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));

        AssessmentSessionDto sessionDto = AssessmentSessionDto.builder()
                .sessionId(200L)
                .status("IN_PROGRESS")
                .timeLimitMinutes(20)
                .build();

        when(assessmentService.startAssessment(eq(student), any(StartAssessmentRequest.class))).thenReturn(sessionDto);

        AssessmentSession sessionEntity = AssessmentSession.builder()
                .id(200L)
                .user(student)
                .status(AssessmentStatus.IN_PROGRESS)
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();

        when(assessmentSessionRepository.findById(200L)).thenReturn(Optional.of(sessionEntity));
        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenAnswer(i -> i.getArgument(0));

        AssessmentSessionDto result = onboardingService.startOnboardingAssessment(student);

        assertNotNull(result);
        assertEquals(200L, result.getSessionId());
        assertEquals(OnboardingState.ASSESSMENT_IN_PROGRESS, existing.getState());
        assertEquals(sessionEntity, existing.getAssessmentSession());
    }

    @Test
    @DisplayName("Review assessment report marks state as ASSESSMENT_REVIEWED")
    void testReviewAssessmentReportSuccess() {
        AssessmentSession session = AssessmentSession.builder()
                .id(201L)
                .status(AssessmentStatus.SUBMITTED)
                .build();

        StudentOnboarding existing = StudentOnboarding.builder()
                .id(57L)
                .user(student)
                .assessmentSession(session)
                .state(OnboardingState.ASSESSMENT_COMPLETED)
                .selectedTechnologies(new HashSet<>())
                .build();

        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));
        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenAnswer(i -> i.getArgument(0));

        OnboardingStateDto result = onboardingService.reviewAssessmentReport(student);

        assertEquals(OnboardingState.ASSESSMENT_REVIEWED, result.getState());
        assertTrue(result.isCanGenerateRoadmap());
    }

    @Test
    @DisplayName("Complete onboarding marks state as ROADMAP_GENERATED and hasPersonalizedRoadmap true")
    void testCompleteOnboardingSuccess() {
        StudentOnboarding existing = StudentOnboarding.builder()
                .id(58L)
                .user(student)
                .state(OnboardingState.ASSESSMENT_REVIEWED)
                .selectedTechnologies(new HashSet<>())
                .build();

        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));
        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenAnswer(i -> i.getArgument(0));

        OnboardingStateDto result = onboardingService.completeOnboarding(student);

        assertEquals(OnboardingState.ROADMAP_GENERATED, result.getState());
        assertTrue(result.isHasPersonalizedRoadmap());
    }

    @Test
    @DisplayName("Reset onboarding resets state and clears selected technologies")
    void testResetOnboardingSuccess() {
        Set<Technology> techs = new HashSet<>();
        techs.add(techJava);

        StudentOnboarding existing = StudentOnboarding.builder()
                .id(59L)
                .user(student)
                .selectedCareer(trackJava)
                .selectedTechnologies(techs)
                .state(OnboardingState.ROADMAP_GENERATED)
                .hasPersonalizedRoadmap(true)
                .build();

        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.of(existing));
        when(studentOnboardingRepository.save(any(StudentOnboarding.class))).thenAnswer(i -> i.getArgument(0));

        OnboardingStateDto result = onboardingService.resetOnboarding(student);

        assertEquals(OnboardingState.NOT_STARTED, result.getState());
        assertFalse(result.isHasPersonalizedRoadmap());
        assertTrue(existing.getSelectedTechnologies().isEmpty());
    }
}
