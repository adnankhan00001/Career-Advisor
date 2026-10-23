package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.OnboardingState;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.StudyWindow;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.OnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private OnboardingService onboardingService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OnboardingController onboardingController;

    private User testUser;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(301L)
                .name("Sarah Student")
                .email("sarah@student.dev")
                .role(Role.USER)
                .build();

        testUserDetails = CustomUserDetails.build(testUser);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return testUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(onboardingController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();

        lenient().when(userRepository.findById(301L)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("GET /api/onboarding/state returns 200 and state DTO")
    void testGetOnboardingState() throws Exception {
        OnboardingStateDto stateDto = OnboardingStateDto.builder()
                .state(OnboardingState.CAREER_SELECTED)
                .selectedCareer(CatalogCareerDto.builder().id(1L).slug("frontend-developer").title("Frontend Developer").build())
                .hasPersonalizedRoadmap(false)
                .build();

        when(onboardingService.getOnboardingState(any(User.class))).thenReturn(stateDto);

        mockMvc.perform(get("/api/onboarding/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CAREER_SELECTED"))
                .andExpect(jsonPath("$.selectedCareer.slug").value("frontend-developer"))
                .andExpect(jsonPath("$.hasPersonalizedRoadmap").value(false));

        verify(onboardingService).getOnboardingState(any(User.class));
    }

    @Test
    @DisplayName("POST /api/onboarding/select-career returns 200 with updated state")
    void testSelectCareer() throws Exception {
        OnboardingSelectCareerRequest request = new OnboardingSelectCareerRequest("frontend-developer");

        OnboardingStateDto stateDto = OnboardingStateDto.builder()
                .state(OnboardingState.CAREER_SELECTED)
                .selectedCareer(CatalogCareerDto.builder().id(1L).slug("frontend-developer").title("Frontend Developer").build())
                .build();

        when(onboardingService.selectCareer(any(User.class), eq("frontend-developer"))).thenReturn(stateDto);

        mockMvc.perform(post("/api/onboarding/select-career")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CAREER_SELECTED"))
                .andExpect(jsonPath("$.selectedCareer.slug").value("frontend-developer"));

        verify(onboardingService).selectCareer(any(User.class), eq("frontend-developer"));
    }

    @Test
    @DisplayName("POST /api/onboarding/select-technologies returns 200 with updated state")
    void testSelectTechnologies() throws Exception {
        OnboardingSelectTechnologiesRequest request = new OnboardingSelectTechnologiesRequest(List.of("react", "typescript"));

        OnboardingStateDto stateDto = OnboardingStateDto.builder()
                .state(OnboardingState.TECHNOLOGIES_SELECTED)
                .selectedTechnologies(List.of(
                        CatalogTechnologyDto.builder().id(1L).slug("react").name("React").build(),
                        CatalogTechnologyDto.builder().id(2L).slug("typescript").name("TypeScript").build()
                ))
                .build();

        when(onboardingService.selectTechnologies(any(User.class), eq(List.of("react", "typescript")))).thenReturn(stateDto);

        mockMvc.perform(post("/api/onboarding/select-technologies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("TECHNOLOGIES_SELECTED"))
                .andExpect(jsonPath("$.selectedTechnologies.length()").value(2));

        verify(onboardingService).selectTechnologies(any(User.class), eq(List.of("react", "typescript")));
    }

    @Test
    @DisplayName("POST /api/onboarding/study-preferences returns 200 with updated state")
    void testSaveStudyPreferences() throws Exception {
        OnboardingStudyPreferencesRequest request = OnboardingStudyPreferencesRequest.builder()
                .dailyStudyMinutes(60)
                .preferredStudyWindow(StudyWindow.EVENING)
                .preferredDays(List.of("MONDAY", "WEDNESDAY", "FRIDAY"))
                .targetCompletionDate(LocalDate.now().plusMonths(3))
                .build();

        OnboardingStateDto stateDto = OnboardingStateDto.builder()
                .state(OnboardingState.STUDY_PREFERENCES_CONFIGURED)
                .studyPreferences(OnboardingStudyPreferencesDto.builder()
                        .dailyStudyMinutes(60)
                        .preferredStudyWindow(StudyWindow.EVENING)
                        .preferredDays(List.of("MONDAY", "WEDNESDAY", "FRIDAY"))
                        .build())
                .build();

        when(onboardingService.saveStudyPreferences(any(User.class), any(OnboardingStudyPreferencesRequest.class))).thenReturn(stateDto);

        mockMvc.perform(post("/api/onboarding/study-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("STUDY_PREFERENCES_CONFIGURED"))
                .andExpect(jsonPath("$.studyPreferences.dailyStudyMinutes").value(60));

        verify(onboardingService).saveStudyPreferences(any(User.class), any(OnboardingStudyPreferencesRequest.class));
    }

    @Test
    @DisplayName("POST /api/onboarding/start-assessment returns 200 and session DTO")
    void testStartAssessment() throws Exception {
        AssessmentSessionDto sessionDto = AssessmentSessionDto.builder()
                .sessionId(100L)
                .status("IN_PROGRESS")
                .timeLimitMinutes(20)
                .build();

        when(onboardingService.startOnboardingAssessment(any(User.class))).thenReturn(sessionDto);

        mockMvc.perform(post("/api/onboarding/start-assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(100L))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(onboardingService).startOnboardingAssessment(any(User.class));
    }

    @Test
    @DisplayName("POST /api/onboarding/review-report returns 200 with reviewed state")
    void testReviewReport() throws Exception {
        OnboardingStateDto stateDto = OnboardingStateDto.builder()
                .state(OnboardingState.ASSESSMENT_REVIEWED)
                .canGenerateRoadmap(true)
                .build();

        when(onboardingService.reviewAssessmentReport(any(User.class))).thenReturn(stateDto);

        mockMvc.perform(post("/api/onboarding/review-report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ASSESSMENT_REVIEWED"))
                .andExpect(jsonPath("$.canGenerateRoadmap").value(true));

        verify(onboardingService).reviewAssessmentReport(any(User.class));
    }

    @Test
    @DisplayName("POST /api/onboarding/complete returns 200 with completed state")
    void testCompleteOnboarding() throws Exception {
        OnboardingStateDto stateDto = OnboardingStateDto.builder()
                .state(OnboardingState.ROADMAP_GENERATED)
                .hasPersonalizedRoadmap(true)
                .build();

        when(onboardingService.completeOnboarding(any(User.class))).thenReturn(stateDto);

        mockMvc.perform(post("/api/onboarding/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ROADMAP_GENERATED"))
                .andExpect(jsonPath("$.hasPersonalizedRoadmap").value(true));

        verify(onboardingService).completeOnboarding(any(User.class));
    }

    @Test
    @DisplayName("POST /api/onboarding/reset returns 200 with reset state")
    void testResetOnboarding() throws Exception {
        OnboardingStateDto stateDto = OnboardingStateDto.builder()
                .state(OnboardingState.NOT_STARTED)
                .hasPersonalizedRoadmap(false)
                .build();

        when(onboardingService.resetOnboarding(any(User.class))).thenReturn(stateDto);

        mockMvc.perform(post("/api/onboarding/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("NOT_STARTED"))
                .andExpect(jsonPath("$.hasPersonalizedRoadmap").value(false));

        verify(onboardingService).resetOnboarding(any(User.class));
    }
}
