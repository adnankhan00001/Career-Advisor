package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final MockInterviewRepository mockInterviewRepository;
    private final UserProblemProgressRepository problemProgressRepository;
    private final MentorshipRequestRepository mentorshipRequestRepository;
    private final MentorProfileRepository mentorProfileRepository;

    public AdminAnalyticsService(
            UserRepository userRepository,
            ResumeRepository resumeRepository,
            MockInterviewRepository mockInterviewRepository,
            UserProblemProgressRepository problemProgressRepository,
            MentorshipRequestRepository mentorshipRequestRepository,
            MentorProfileRepository mentorProfileRepository) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.mockInterviewRepository = mockInterviewRepository;
        this.problemProgressRepository = problemProgressRepository;
        this.mentorshipRequestRepository = mentorshipRequestRepository;
        this.mentorProfileRepository = mentorProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserAnalyticsDto getUserAnalytics(int days) {
        int clampedDays = Math.max(1, Math.min(days, 365));
        LocalDateTime startDateTime = LocalDate.now().minusDays(clampedDays - 1).atStartOfDay();

        long totalUsers = userRepository.count();
        long newUsers = userRepository.countByCreatedAtAfter(startDateTime);
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        long deactivatedUsers = userRepository.countByStatus(UserStatus.DEACTIVATED);

        Map<String, Long> roleDistribution = new LinkedHashMap<>();
        for (Object[] row : userRepository.countGroupedByRole()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                roleDistribution.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        for (Object[] row : userRepository.countGroupedByStatus()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                statusDistribution.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        List<Object[]> rawTrend = userRepository.countRegistrationsByDateSince(startDateTime);
        List<TimeSeriesPointDto> trend = buildContinuousTimeSeries(clampedDays, rawTrend);

        return UserAnalyticsDto.builder()
                .totalUsers(totalUsers)
                .newUsersInPeriod(newUsers)
                .activeUsers(activeUsers)
                .suspendedUsers(suspendedUsers)
                .deactivatedUsers(deactivatedUsers)
                .roleDistribution(roleDistribution)
                .statusDistribution(statusDistribution)
                .registrationTrend(trend)
                .build();
    }

    @Transactional(readOnly = true)
    public CareerPrepAnalyticsDto getCareerPrepAnalytics(int days) {
        int clampedDays = Math.max(1, Math.min(days, 365));
        LocalDateTime startDateTime = LocalDate.now().minusDays(clampedDays - 1).atStartOfDay();

        long totalResumes = resumeRepository.count();
        long resumesInPeriod = resumeRepository.countByUploadTimestampAfter(startDateTime);
        List<TimeSeriesPointDto> resumeTrend = buildContinuousTimeSeries(
                clampedDays, resumeRepository.countUploadsByDateSince(startDateTime));

        long totalQuizAttempts = userRepository.countByLatestQuizScoreIsNotNull();
        long totalSolvedProblems = problemProgressRepository.countBySolvedTrue();

        long totalMockInterviews = mockInterviewRepository.count();
        long mockInterviewsInPeriod = mockInterviewRepository.countByStartedAtAfter(startDateTime);
        long completedInterviews = mockInterviewRepository.countByStatus(InterviewStatus.COMPLETED);
        List<TimeSeriesPointDto> interviewTrend = buildContinuousTimeSeries(
                clampedDays, mockInterviewRepository.countInterviewsByDateSince(startDateTime));

        Map<String, Long> careerGoals = new LinkedHashMap<>();
        List<CategoryCountProjection> goalsList = userRepository.countGroupedByCareerGoal();
        if (goalsList != null) {
            for (CategoryCountProjection p : goalsList) {
                if (p != null && StringUtils.hasText(p.getCategory())) {
                    careerGoals.put(p.getCategory().trim(), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

        Map<String, Long> userLevels = new LinkedHashMap<>();
        List<CategoryCountProjection> levelsList = userRepository.countGroupedByUserLevel();
        if (levelsList != null) {
            for (CategoryCountProjection p : levelsList) {
                if (p != null && StringUtils.hasText(p.getCategory())) {
                    userLevels.put(p.getCategory().trim(), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

        return CareerPrepAnalyticsDto.builder()
                .totalResumes(totalResumes)
                .resumesUploadedInPeriod(resumesInPeriod)
                .resumeUploadTrend(resumeTrend)
                .totalQuizAttempts(totalQuizAttempts)
                .totalSolvedProblems(totalSolvedProblems)
                .totalMockInterviews(totalMockInterviews)
                .mockInterviewsInPeriod(mockInterviewsInPeriod)
                .completedInterviewsCount(completedInterviews)
                .mockInterviewTrend(interviewTrend)
                .careerGoalDistribution(careerGoals)
                .userLevelDistribution(userLevels)
                .build();
    }

    @Transactional(readOnly = true)
    public MentorshipAnalyticsDto getMentorshipAnalytics(int days) {
        int clampedDays = Math.max(1, Math.min(days, 365));
        LocalDateTime startDateTime = LocalDate.now().minusDays(clampedDays - 1).atStartOfDay();

        long totalRequests = mentorshipRequestRepository.count();
        long requestsInPeriod = mentorshipRequestRepository.countByCreatedAtAfter(startDateTime);
        List<TimeSeriesPointDto> creationTrend = buildContinuousTimeSeries(
                clampedDays, mentorshipRequestRepository.countRequestsByDateSince(startDateTime));

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        for (Object[] row : mentorshipRequestRepository.countGroupedByStatus()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                statusDistribution.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        long accepted = statusDistribution.getOrDefault("ACCEPTED", 0L)
                + statusDistribution.getOrDefault("COMPLETED", 0L)
                + statusDistribution.getOrDefault("TERMINATED", 0L);
        long declined = statusDistribution.getOrDefault("DECLINED", 0L);
        long responded = accepted + declined;

        double acceptanceRate = responded > 0 ? Math.round(((double) accepted / responded) * 1000.0) / 10.0 : 0.0;
        double rejectionRate = responded > 0 ? Math.round(((double) declined / responded) * 1000.0) / 10.0 : 0.0;

        long completed = statusDistribution.getOrDefault("COMPLETED", 0L);
        double completionRate = accepted > 0 ? Math.round(((double) completed / accepted) * 1000.0) / 10.0 : 0.0;

        long totalApprovedMentors = mentorProfileRepository.countByVerificationStatus(MentorVerificationStatus.APPROVED);
        long activeMentors = mentorshipRequestRepository.countDistinctMentorsByStatus(MentorshipRequestStatus.ACCEPTED);
        long totalCapacity = mentorProfileRepository.sumMaxMenteesByVerificationStatusAndIsActiveTrue(MentorVerificationStatus.APPROVED);
        long utilizedCapacity = mentorshipRequestRepository.countByStatus(MentorshipRequestStatus.ACCEPTED);

        Double avgResponseTimeHours = calculateAverageResponseTimeHours();

        return MentorshipAnalyticsDto.builder()
                .totalRequests(totalRequests)
                .requestsInPeriod(requestsInPeriod)
                .requestCreationTrend(creationTrend)
                .statusDistribution(statusDistribution)
                .acceptanceRate(acceptanceRate)
                .rejectionRate(rejectionRate)
                .completionRate(completionRate)
                .totalApprovedMentors(totalApprovedMentors)
                .activeMentorsCount(activeMentors)
                .totalCapacity(totalCapacity)
                .utilizedCapacity(utilizedCapacity)
                .averageResponseTimeHours(avgResponseTimeHours)
                .build();
    }

    private Double calculateAverageResponseTimeHours() {
        List<Object[]> timestamps = mentorshipRequestRepository.findResponseTimestamps(
                List.of(MentorshipRequestStatus.ACCEPTED,
                        MentorshipRequestStatus.DECLINED,
                        MentorshipRequestStatus.COMPLETED,
                        MentorshipRequestStatus.TERMINATED));

        if (timestamps == null || timestamps.isEmpty()) {
            return null;
        }

        double totalMinutes = 0;
        long validCount = 0;

        for (Object[] pair : timestamps) {
            if (pair != null && pair.length >= 2 && pair[0] instanceof LocalDateTime && pair[1] instanceof LocalDateTime) {
                LocalDateTime created = (LocalDateTime) pair[0];
                LocalDateTime updated = (LocalDateTime) pair[1];
                long minutes = Duration.between(created, updated).toMinutes();
                if (minutes >= 0) {
                    totalMinutes += minutes;
                    validCount++;
                }
            }
        }

        if (validCount == 0) {
            return null;
        }

        return Math.round((totalMinutes / validCount / 60.0) * 10.0) / 10.0;
    }

    private List<TimeSeriesPointDto> buildContinuousTimeSeries(int days, List<Object[]> rawGroupings) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        Map<String, Long> dateMap = new LinkedHashMap<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            dateMap.put(curr.toString(), 0L);
            curr = curr.plusDays(1);
        }

        if (rawGroupings != null) {
            for (Object[] row : rawGroupings) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    String dtStr = String.valueOf(row[0]).trim();
                    if (dtStr.length() >= 10) {
                        dtStr = dtStr.substring(0, 10);
                    }
                    if (dateMap.containsKey(dtStr)) {
                        dateMap.put(dtStr, ((Number) row[1]).longValue());
                    }
                }
            }
        }

        List<TimeSeriesPointDto> points = new ArrayList<>();
        for (Map.Entry<String, Long> entry : dateMap.entrySet()) {
            points.add(new TimeSeriesPointDto(entry.getKey(), entry.getValue()));
        }

        return points;
    }
}
