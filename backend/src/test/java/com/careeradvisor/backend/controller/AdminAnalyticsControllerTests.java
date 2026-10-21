package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.GlobalExceptionHandler;
import com.careeradvisor.backend.service.AdminAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsControllerTests {

    private MockMvc mockMvc;

    @Mock
    private AdminAnalyticsService adminAnalyticsService;

    @InjectMocks
    private AdminAnalyticsController adminAnalyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAnalyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/analytics/users returns 200 OK with user analytics")
    void testGetUserAnalytics() throws Exception {
        UserAnalyticsDto dto = UserAnalyticsDto.builder()
                .totalUsers(100L)
                .newUsersInPeriod(25L)
                .activeUsers(95L)
                .roleDistribution(Map.of("USER", 90L, "ADMIN", 10L))
                .statusDistribution(Map.of("ACTIVE", 95L, "SUSPENDED", 5L))
                .registrationTrend(List.of(
                        TimeSeriesPointDto.builder().date("2026-10-19").count(5L).build(),
                        TimeSeriesPointDto.builder().date("2026-10-20").count(8L).build()
                ))
                .build();

        when(adminAnalyticsService.getUserAnalytics(eq(30))).thenReturn(dto);

        mockMvc.perform(get("/api/admin/analytics/users").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100L))
                .andExpect(jsonPath("$.newUsersInPeriod").value(25L))
                .andExpect(jsonPath("$.registrationTrend[1].count").value(8L));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/career-preparation returns 200 OK with career analytics")
    void testGetCareerPrepAnalytics() throws Exception {
        CareerPrepAnalyticsDto dto = CareerPrepAnalyticsDto.builder()
                .totalResumes(40L)
                .resumesUploadedInPeriod(12L)
                .totalQuizAttempts(30L)
                .totalSolvedProblems(120L)
                .totalMockInterviews(15L)
                .completedInterviewsCount(11L)
                .build();

        when(adminAnalyticsService.getCareerPrepAnalytics(eq(14))).thenReturn(dto);

        mockMvc.perform(get("/api/admin/analytics/career-preparation").param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResumes").value(40L))
                .andExpect(jsonPath("$.totalSolvedProblems").value(120L))
                .andExpect(jsonPath("$.completedInterviewsCount").value(11L));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/mentorship returns 200 OK with mentorship analytics")
    void testGetMentorshipAnalytics() throws Exception {
        MentorshipAnalyticsDto dto = MentorshipAnalyticsDto.builder()
                .totalRequests(50L)
                .requestsInPeriod(15L)
                .acceptanceRate(60.0)
                .rejectionRate(20.0)
                .completionRate(20.0)
                .totalApprovedMentors(10L)
                .activeMentorsCount(8L)
                .totalCapacity(40L)
                .utilizedCapacity(18L)
                .averageResponseTimeHours(4.2)
                .build();

        when(adminAnalyticsService.getMentorshipAnalytics(eq(30))).thenReturn(dto);

        mockMvc.perform(get("/api/admin/analytics/mentorship").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(50L))
                .andExpect(jsonPath("$.acceptanceRate").value(60.0))
                .andExpect(jsonPath("$.averageResponseTimeHours").value(4.2));
    }
}
