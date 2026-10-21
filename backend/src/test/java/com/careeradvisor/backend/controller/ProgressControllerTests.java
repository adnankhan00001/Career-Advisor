package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.ProgressSummaryResponse;
import com.careeradvisor.backend.dto.RecentActivityDto;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.UserProgressService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTests {

    private MockMvc mockMvc;

    @Mock
    private UserProgressService progressService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProgressController progressController;

    private User testUser;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(101L)
                .name("Alex Learner")
                .email("alex@careeradvisor.dev")
                .role(Role.USER)
                .careerGoal("Java Backend Developer")
                .userLevel("Intermediate")
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

        mockMvc = MockMvcBuilders.standaloneSetup(progressController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/progress/summary returns authenticated student progress summary")
    void testGetProgressSummary() throws Exception {
        when(userRepository.findById(101L)).thenReturn(Optional.of(testUser));

        ProgressSummaryResponse mockSummary = ProgressSummaryResponse.builder()
                .careerGoal("Java Backend Developer")
                .userLevel("Intermediate")
                .roadmapPercent(45)
                .completedStepsCount(9)
                .totalStepsCount(20)
                .skillsCount(5)
                .skills(List.of("Java", "Spring Boot", "MySQL", "Docker", "REST APIs"))
                .nextTopicToLearn("Spring Security & JWT")
                .skillMatchPercentage(75)
                .build();

        when(progressService.getProgressSummary(any(User.class))).thenReturn(mockSummary);

        mockMvc.perform(get("/api/progress/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careerGoal").value("Java Backend Developer"))
                .andExpect(jsonPath("$.roadmapPercent").value(45))
                .andExpect(jsonPath("$.completedStepsCount").value(9))
                .andExpect(jsonPath("$.totalStepsCount").value(20))
                .andExpect(jsonPath("$.skillsCount").value(5))
                .andExpect(jsonPath("$.nextTopicToLearn").value("Spring Security & JWT"))
                .andExpect(jsonPath("$.skillMatchPercentage").value(75));
    }

    @Test
    @DisplayName("GET /api/progress/recent-activity returns chronological activity feed")
    void testGetRecentActivity() throws Exception {
        when(userRepository.findById(101L)).thenReturn(Optional.of(testUser));

        RecentActivityDto act1 = RecentActivityDto.builder()
                .id("quiz-1")
                .activityType("ASSESSMENT")
                .title("Completed Diagnostic Assessment")
                .subtitle("Score: 85% • Intermediate Tier")
                .timestamp(LocalDateTime.now())
                .actionUrl("/quiz")
                .status("COMPLETED")
                .build();

        RecentActivityDto act2 = RecentActivityDto.builder()
                .id("roadmap-2")
                .activityType("ROADMAP")
                .title("Mastered Milestone: Spring Security & JWT")
                .subtitle("Curriculum Track: Java Backend Developer")
                .timestamp(LocalDateTime.now().minusHours(2))
                .actionUrl("/roadmap")
                .status("COMPLETED")
                .build();

        when(progressService.getRecentActivities(any(User.class))).thenReturn(List.of(act1, act2));

        mockMvc.perform(get("/api/progress/recent-activity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].activityType").value("ASSESSMENT"))
                .andExpect(jsonPath("$[0].title").value("Completed Diagnostic Assessment"))
                .andExpect(jsonPath("$[1].activityType").value("ROADMAP"))
                .andExpect(jsonPath("$[1].actionUrl").value("/roadmap"));
    }
}
