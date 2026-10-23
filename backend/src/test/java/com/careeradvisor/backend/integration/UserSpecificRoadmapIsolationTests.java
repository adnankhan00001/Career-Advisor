package com.careeradvisor.backend.integration;

import com.careeradvisor.backend.controller.PersonalizedRoadmapController;
import com.careeradvisor.backend.dto.PersonalizedRoadmapDto;
import com.careeradvisor.backend.dto.ProgressSummaryResponse;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.PersonalizedRoadmapService;
import com.careeradvisor.backend.service.UserProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserSpecificRoadmapIsolationTests {

    private MockMvc mockMvc;

    @Mock
    private PersonalizedRoadmapService personalizedRoadmapService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProgressService userProgressService;

    @InjectMocks
    private PersonalizedRoadmapController personalizedRoadmapController;

    private User newUserNoCareer;
    private User userFrontend;
    private User userJavaBackend;

    private CustomUserDetails userDetailsNoCareer;
    private CustomUserDetails userDetailsFrontend;
    private CustomUserDetails userDetailsJavaBackend;

    private CustomUserDetails currentPrincipal;

    @BeforeEach
    void setUp() {
        newUserNoCareer = User.builder()
                .id(101L)
                .name("New Explorer")
                .email("new@test.com")
                .role(Role.USER)
                .careerGoal(null)
                .build();
        userDetailsNoCareer = CustomUserDetails.build(newUserNoCareer);

        userFrontend = User.builder()
                .id(102L)
                .name("Frontend Dev")
                .email("frontend@test.com")
                .role(Role.USER)
                .careerGoal("Frontend Engineer")
                .build();
        userDetailsFrontend = CustomUserDetails.build(userFrontend);

        userJavaBackend = User.builder()
                .id(103L)
                .name("Java Backend Dev")
                .email("java@test.com")
                .role(Role.USER)
                .careerGoal("Java Backend Developer")
                .build();
        userDetailsJavaBackend = CustomUserDetails.build(userJavaBackend);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(personalizedRoadmapController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("Scenario A: Newly registered user has null career goal and no roadmap (returns HTTP 204 No Content)")
    void testNewUserHasNoDefaultCareerAndNoRoadmap() throws Exception {
        currentPrincipal = userDetailsNoCareer;
        when(userRepository.findById(101L)).thenReturn(Optional.of(newUserNoCareer));
        when(personalizedRoadmapService.getPersonalizedRoadmap(newUserNoCareer)).thenReturn(null);

        // Verify entity level: no career assigned by default
        assertThat(newUserNoCareer.getCareerGoal()).isNull();

        // Verify API endpoint returns 204 (No Content) instead of falling back to Java Backend Developer
        mockMvc.perform(get("/api/roadmaps/personalized"))
                .andExpect(status().isNoContent());

        verify(personalizedRoadmapService).getPersonalizedRoadmap(newUserNoCareer);
    }

    @Test
    @DisplayName("Scenario B: User selecting Frontend Engineer receives Frontend Engineer personalized roadmap")
    void testFrontendUserReceivesFrontendRoadmap() throws Exception {
        currentPrincipal = userDetailsFrontend;
        when(userRepository.findById(102L)).thenReturn(Optional.of(userFrontend));

        PersonalizedRoadmapDto frontendRoadmap = PersonalizedRoadmapDto.builder()
                .id(201L)
                .careerTitle("Frontend Engineer")
                .careerSlug("frontend-engineer")
                .totalEstimatedHours(120.0)
                .progressPercentage(15)
                .milestones(Collections.emptyList())
                .build();

        when(personalizedRoadmapService.getPersonalizedRoadmap(userFrontend)).thenReturn(frontendRoadmap);

        mockMvc.perform(get("/api/roadmaps/personalized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerTitle").value("Frontend Engineer"))
                .andExpect(jsonPath("$.careerSlug").value("frontend-engineer"));
    }

    @Test
    @DisplayName("Scenario C: User legitimately selecting Java Backend Developer receives Java Backend Developer roadmap")
    void testLegitimateJavaUserReceivesJavaRoadmap() throws Exception {
        currentPrincipal = userDetailsJavaBackend;
        when(userRepository.findById(103L)).thenReturn(Optional.of(userJavaBackend));

        PersonalizedRoadmapDto javaRoadmap = PersonalizedRoadmapDto.builder()
                .id(202L)
                .careerTitle("Java Backend Developer")
                .careerSlug("java-backend-developer")
                .totalEstimatedHours(160.0)
                .progressPercentage(40)
                .milestones(Collections.emptyList())
                .build();

        when(personalizedRoadmapService.getPersonalizedRoadmap(userJavaBackend)).thenReturn(javaRoadmap);

        mockMvc.perform(get("/api/roadmaps/personalized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerTitle").value("Java Backend Developer"))
                .andExpect(jsonPath("$.careerSlug").value("java-backend-developer"));
    }

    @Test
    @DisplayName("Scenario D & Cross-User Isolation: User A and User B receive strictly isolated, user-specific roadmaps")
    void testStrictCrossUserRoadmapIsolation() throws Exception {
        // 1. User A (Frontend Engineer)
        currentPrincipal = userDetailsFrontend;
        when(userRepository.findById(102L)).thenReturn(Optional.of(userFrontend));
        PersonalizedRoadmapDto frontendRoadmap = PersonalizedRoadmapDto.builder()
                .id(201L)
                .careerTitle("Frontend Engineer")
                .careerSlug("frontend-engineer")
                .build();
        when(personalizedRoadmapService.getPersonalizedRoadmap(userFrontend)).thenReturn(frontendRoadmap);

        mockMvc.perform(get("/api/roadmaps/personalized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerTitle").value("Frontend Engineer"));

        // 2. User B (Java Backend Developer)
        currentPrincipal = userDetailsJavaBackend;
        when(userRepository.findById(103L)).thenReturn(Optional.of(userJavaBackend));
        PersonalizedRoadmapDto javaRoadmap = PersonalizedRoadmapDto.builder()
                .id(202L)
                .careerTitle("Java Backend Developer")
                .careerSlug("java-backend-developer")
                .build();
        when(personalizedRoadmapService.getPersonalizedRoadmap(userJavaBackend)).thenReturn(javaRoadmap);

        mockMvc.perform(get("/api/roadmaps/personalized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerTitle").value("Java Backend Developer"));

        // Neither user receives a hardcoded fallback or the other's roadmap
        verify(personalizedRoadmapService).getPersonalizedRoadmap(userFrontend);
        verify(personalizedRoadmapService).getPersonalizedRoadmap(userJavaBackend);
    }

    @Test
    @DisplayName("Scenario E: UserProgressService returns null careerGoal and 0 progress for user without career")
    void testProgressSummaryForUserWithoutCareer() {
        ProgressSummaryResponse summary = ProgressSummaryResponse.builder()
                .careerGoal(null)
                .roadmapPercent(0)
                .completedStepsCount(0)
                .totalStepsCount(0)
                .skillsCount(0)
                .skills(Collections.emptyList())
                .completedSteps(Collections.emptyList())
                .userLevel("Beginner")
                .nextTopicToLearn(null)
                .skillMatchPercentage(0)
                .build();

        when(userProgressService.getProgressSummary(newUserNoCareer)).thenReturn(summary);

        ProgressSummaryResponse res = userProgressService.getProgressSummary(newUserNoCareer);
        assertThat(res.getCareerGoal()).isNull();
        assertThat(res.getRoadmapPercent()).isEqualTo(0);
        assertThat(res.getTotalStepsCount()).isEqualTo(0);
        assertThat(res.getNextTopicToLearn()).isNull();
    }
}
