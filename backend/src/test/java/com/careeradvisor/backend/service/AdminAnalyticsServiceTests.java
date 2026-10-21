package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.model.MentorVerificationStatus;
import com.careeradvisor.backend.model.MentorshipRequestStatus;
import com.careeradvisor.backend.model.UserStatus;
import com.careeradvisor.backend.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private MockInterviewRepository mockInterviewRepository;

    @Mock
    private UserProblemProgressRepository problemProgressRepository;

    @Mock
    private MentorshipRequestRepository mentorshipRequestRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @InjectMocks
    private AdminAnalyticsService adminAnalyticsService;

    private CategoryCountProjection createProjection(String category, Long count) {
        return new CategoryCountProjection() {
            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    @Test
    @DisplayName("getUserAnalytics returns continuous timeline and correct distributions")
    void testGetUserAnalytics() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(15L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(48L);
        when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(2L);
        when(userRepository.countByStatus(UserStatus.DEACTIVATED)).thenReturn(0L);

        List<Object[]> roleGrouped = List.of(
                new Object[]{"USER", 45L},
                new Object[]{"ADMIN", 5L}
        );
        when(userRepository.countGroupedByRole()).thenReturn(roleGrouped);

        List<Object[]> statusGrouped = List.of(
                new Object[]{"ACTIVE", 48L},
                new Object[]{"SUSPENDED", 2L}
        );
        when(userRepository.countGroupedByStatus()).thenReturn(statusGrouped);

        LocalDate today = LocalDate.now();
        List<Object[]> dateCounts = List.of(
                new Object[]{today.minusDays(2).toString(), 3L},
                new Object[]{today.toString(), 5L}
        );
        when(userRepository.countRegistrationsByDateSince(any(LocalDateTime.class))).thenReturn(dateCounts);

        UserAnalyticsDto result = adminAnalyticsService.getUserAnalytics(7);

        assertNotNull(result);
        assertEquals(50L, result.getTotalUsers());
        assertEquals(15L, result.getNewUsersInPeriod());
        assertEquals(48L, result.getActiveUsers());
        assertEquals(2L, result.getSuspendedUsers());
        assertEquals(0L, result.getDeactivatedUsers());

        // 7 days window: today - 6 days through today = 7 points
        assertNotNull(result.getRegistrationTrend());
        assertEquals(7, result.getRegistrationTrend().size());

        // Check that continuous timeline filled missing days with 0L
        TimeSeriesPointDto todayPoint = result.getRegistrationTrend().get(6);
        assertEquals(today.toString(), todayPoint.getDate());
        assertEquals(5L, todayPoint.getCount());
    }

    @Test
    @DisplayName("getCareerPrepAnalytics returns metrics, distributions, and trends")
    void testGetCareerPrepAnalytics() {
        when(resumeRepository.count()).thenReturn(20L);
        when(resumeRepository.countByUploadTimestampAfter(any(LocalDateTime.class))).thenReturn(6L);
        when(userRepository.countByLatestQuizScoreIsNotNull()).thenReturn(14L);
        when(problemProgressRepository.countBySolvedTrue()).thenReturn(85L);
        when(mockInterviewRepository.count()).thenReturn(12L);
        when(mockInterviewRepository.countByStartedAtAfter(any(LocalDateTime.class))).thenReturn(4L);
        when(mockInterviewRepository.countByStatus(InterviewStatus.COMPLETED)).thenReturn(9L);

        when(userRepository.countGroupedByCareerGoal()).thenReturn(List.of(
                createProjection("Backend Developer", 10L)
        ));
        when(userRepository.countGroupedByUserLevel()).thenReturn(List.of(
                createProjection("Beginner", 8L)
        ));

        List<Object[]> resumeCounts = Collections.singletonList(
                new Object[]{LocalDate.now().toString(), 2L}
        );
        when(resumeRepository.countUploadsByDateSince(any(LocalDateTime.class))).thenReturn(resumeCounts);

        List<Object[]> interviewCounts = Collections.singletonList(
                new Object[]{LocalDate.now().toString(), 1L}
        );
        when(mockInterviewRepository.countInterviewsByDateSince(any(LocalDateTime.class))).thenReturn(interviewCounts);

        CareerPrepAnalyticsDto result = adminAnalyticsService.getCareerPrepAnalytics(14);

        assertNotNull(result);
        assertEquals(20L, result.getTotalResumes());
        assertEquals(6L, result.getResumesUploadedInPeriod());
        assertEquals(14L, result.getTotalQuizAttempts());
        assertEquals(85L, result.getTotalSolvedProblems());
        assertEquals(12L, result.getTotalMockInterviews());
        assertEquals(4L, result.getMockInterviewsInPeriod());
        assertEquals(9L, result.getCompletedInterviewsCount());

        assertEquals(14, result.getResumeUploadTrend().size());
        assertEquals(14, result.getMockInterviewTrend().size());
        assertEquals(10L, result.getCareerGoalDistribution().get("Backend Developer"));
        assertEquals(8L, result.getUserLevelDistribution().get("Beginner"));
    }

    @Test
    @DisplayName("getMentorshipAnalytics calculates rates accurately and avoids division by zero")
    void testGetMentorshipAnalyticsZeroRequestsSafe() {
        when(mentorshipRequestRepository.count()).thenReturn(0L);
        when(mentorshipRequestRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(mentorshipRequestRepository.countGroupedByStatus()).thenReturn(List.of());
        when(mentorProfileRepository.countByVerificationStatus(MentorVerificationStatus.APPROVED)).thenReturn(0L);
        when(mentorshipRequestRepository.countDistinctMentorsByStatus(MentorshipRequestStatus.ACCEPTED)).thenReturn(0L);
        when(mentorProfileRepository.sumMaxMenteesByVerificationStatusAndIsActiveTrue(MentorVerificationStatus.APPROVED)).thenReturn(0L);
        when(mentorshipRequestRepository.countByStatus(MentorshipRequestStatus.ACCEPTED)).thenReturn(0L);
        when(mentorshipRequestRepository.countRequestsByDateSince(any(LocalDateTime.class))).thenReturn(List.of());
        when(mentorshipRequestRepository.findResponseTimestamps(anyList())).thenReturn(List.of());

        MentorshipAnalyticsDto result = adminAnalyticsService.getMentorshipAnalytics(30);

        assertNotNull(result);
        assertEquals(0L, result.getTotalRequests());
        assertEquals(0.0, result.getAcceptanceRate());
        assertEquals(0.0, result.getRejectionRate());
        assertEquals(0.0, result.getCompletionRate());
        assertNull(result.getAverageResponseTimeHours());
        assertEquals(30, result.getRequestCreationTrend().size());
    }

    @Test
    @DisplayName("getMentorshipAnalytics computes realistic conversion rates and response time average")
    void testGetMentorshipAnalyticsWithData() {
        when(mentorshipRequestRepository.count()).thenReturn(20L);
        when(mentorshipRequestRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(8L);

        List<Object[]> statusCounts = List.of(
                new Object[]{MentorshipRequestStatus.PENDING, 4L},
                new Object[]{MentorshipRequestStatus.ACCEPTED, 10L},
                new Object[]{MentorshipRequestStatus.DECLINED, 4L},
                new Object[]{MentorshipRequestStatus.COMPLETED, 2L}
        );
        when(mentorshipRequestRepository.countGroupedByStatus()).thenReturn(statusCounts);

        when(mentorProfileRepository.countByVerificationStatus(MentorVerificationStatus.APPROVED)).thenReturn(5L);
        when(mentorshipRequestRepository.countDistinctMentorsByStatus(MentorshipRequestStatus.ACCEPTED)).thenReturn(3L);
        when(mentorProfileRepository.sumMaxMenteesByVerificationStatusAndIsActiveTrue(MentorVerificationStatus.APPROVED)).thenReturn(25L);
        when(mentorshipRequestRepository.countByStatus(MentorshipRequestStatus.ACCEPTED)).thenReturn(10L);

        LocalDateTime now = LocalDateTime.now();
        // 2 response events: 1st took 4 hours, 2nd took 6 hours -> average = 5.0 hours
        List<Object[]> timestamps = List.of(
                new Object[]{now.minusHours(10), now.minusHours(6)},
                new Object[]{now.minusHours(12), now.minusHours(6)}
        );
        when(mentorshipRequestRepository.findResponseTimestamps(anyList())).thenReturn(timestamps);

        MentorshipAnalyticsDto result = adminAnalyticsService.getMentorshipAnalytics(30);

        assertNotNull(result);
        assertEquals(20L, result.getTotalRequests());
        assertEquals(8L, result.getRequestsInPeriod());
        // Acceptance rate: (10 + 2) / 16 = 75.0%
        assertEquals(75.0, result.getAcceptanceRate());
        // Rejection rate: 4 / 16 = 25.0%
        assertEquals(25.0, result.getRejectionRate());
        // Completion rate: 2 / 12 = 16.7%
        assertEquals(16.7, result.getCompletionRate());
        // Average response latency: (4 + 6) / 2 = 5.0 hours
        assertEquals(5.0, result.getAverageResponseTimeHours());
        assertEquals(25L, result.getTotalCapacity());
        assertEquals(10L, result.getUtilizedCapacity());
    }
}
