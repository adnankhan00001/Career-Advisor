package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.CareerDto;
import com.careeradvisor.backend.dto.ProgressSummaryResponse;
import com.careeradvisor.backend.dto.RecentActivityDto;
import com.careeradvisor.backend.dto.RoadmapSectionDto;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserRoadmapProgress;
import com.careeradvisor.backend.repository.MockInterviewRepository;
import com.careeradvisor.backend.repository.QuizAttemptRepository;
import com.careeradvisor.backend.repository.ResumeRepository;
import com.careeradvisor.backend.repository.UserRoadmapProgressRepository;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.repository.MentorshipRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProgressServiceTests {

    @Mock
    private UserRoadmapProgressRepository progressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoadmapService roadmapService;

    @Mock
    private CareerService careerService;

    @Mock
    private UserSkillService skillService;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private MockInterviewRepository interviewRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private MentorshipRequestRepository mentorshipRequestRepository;

    private UserProgressService userProgressService;

    private User testUser;

    @BeforeEach
    void setUp() {
        userProgressService = new UserProgressService(
                progressRepository,
                userRepository,
                roadmapService,
                careerService,
                skillService
        );

        ReflectionTestUtils.setField(userProgressService, "quizAttemptRepository", quizAttemptRepository);
        ReflectionTestUtils.setField(userProgressService, "interviewRepository", interviewRepository);
        ReflectionTestUtils.setField(userProgressService, "resumeRepository", resumeRepository);
        ReflectionTestUtils.setField(userProgressService, "mentorshipRequestRepository", mentorshipRequestRepository);

        testUser = User.builder()
                .id(1L)
                .name("Jane Student")
                .email("jane@example.com")
                .role(Role.USER)
                .careerGoal("Java Backend Developer")
                .userLevel("Beginner")
                .build();
    }

    @Test
    @DisplayName("getProgressSummary handles normal progress calculation correctly")
    void testGetProgressSummaryNormal() {
        RoadmapSectionDto sec1 = new RoadmapSectionDto();
        sec1.setTitle("Fundamentals");
        sec1.setSteps(List.of("Java Basics", "OOP Principles"));

        RoadmapSectionDto sec2 = new RoadmapSectionDto();
        sec2.setTitle("Frameworks");
        sec2.setSteps(List.of("Spring Boot", "Hibernate"));

        when(roadmapService.getRoadmapForCareer("Java Backend Developer")).thenReturn(List.of(sec1, sec2));

        UserRoadmapProgress p1 = new UserRoadmapProgress(testUser, "Java Backend Developer", "Java Basics");
        when(progressRepository.findByUser(testUser)).thenReturn(List.of(p1));

        when(skillService.getUserSkills(testUser)).thenReturn(List.of("Java Basics", "Git"));

        CareerDto careerDto = new CareerDto();
        careerDto.setTitle("Java Backend Developer");
        careerDto.setRequiredSkills(List.of("Java Basics", "Spring Boot", "Git", "SQL"));
        when(careerService.getCareerByIdOrTitle("Java Backend Developer")).thenReturn(Optional.of(careerDto));

        ProgressSummaryResponse summary = userProgressService.getProgressSummary(testUser);

        assertThat(summary).isNotNull();
        assertThat(summary.getCareerGoal()).isEqualTo("Java Backend Developer");
        assertThat(summary.getTotalStepsCount()).isEqualTo(4);
        assertThat(summary.getCompletedStepsCount()).isEqualTo(1);
        assertThat(summary.getRoadmapPercent()).isEqualTo(25);
        assertThat(summary.getNextTopicToLearn()).isEqualTo("OOP Principles");
        assertThat(summary.getSkillsCount()).isEqualTo(2);
        assertThat(summary.getSkillMatchPercentage()).isEqualTo(50); // 2 out of 4
    }

    @Test
    @DisplayName("getProgressSummary handles edge case: null or empty careerGoal (defaults safely)")
    void testGetProgressSummaryNullCareerGoal() {
        testUser.setCareerGoal(null);

        RoadmapSectionDto sec = new RoadmapSectionDto();
        sec.setTitle("Fundamentals");
        sec.setSteps(List.of("Core Java"));

        when(roadmapService.getRoadmapForCareer("Java Backend Developer")).thenReturn(List.of(sec));
        when(progressRepository.findByUser(testUser)).thenReturn(Collections.emptyList());
        when(skillService.getUserSkills(testUser)).thenReturn(Collections.emptyList());
        when(careerService.getCareerByIdOrTitle("Java Backend Developer")).thenReturn(Optional.empty());

        ProgressSummaryResponse summary = userProgressService.getProgressSummary(testUser);

        assertThat(summary).isNotNull();
        assertThat(summary.getCareerGoal()).isEqualTo("Java Backend Developer");
        assertThat(summary.getRoadmapPercent()).isEqualTo(0);
        assertThat(summary.getNextTopicToLearn()).isEqualTo("Core Java");
    }

    @Test
    @DisplayName("getProgressSummary handles edge case: 0 total steps (prevents zero division)")
    void testGetProgressSummaryZeroSteps() {
        when(roadmapService.getRoadmapForCareer("Java Backend Developer")).thenReturn(Collections.emptyList());
        when(progressRepository.findByUser(testUser)).thenReturn(Collections.emptyList());
        when(skillService.getUserSkills(testUser)).thenReturn(Collections.emptyList());
        when(careerService.getCareerByIdOrTitle("Java Backend Developer")).thenReturn(Optional.empty());

        ProgressSummaryResponse summary = userProgressService.getProgressSummary(testUser);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalStepsCount()).isEqualTo(0);
        assertThat(summary.getCompletedStepsCount()).isEqualTo(0);
        assertThat(summary.getRoadmapPercent()).isEqualTo(0);
        assertThat(summary.getNextTopicToLearn()).isEqualTo("Getting Started with Core Concepts");
    }

    @Test
    @DisplayName("getProgressSummary handles edge case: 100% roadmap completed")
    void testGetProgressSummaryAllCompleted() {
        RoadmapSectionDto sec = new RoadmapSectionDto();
        sec.setTitle("Section 1");
        sec.setSteps(List.of("Step A", "Step B"));

        when(roadmapService.getRoadmapForCareer("Java Backend Developer")).thenReturn(List.of(sec));

        UserRoadmapProgress p1 = new UserRoadmapProgress(testUser, "Java Backend Developer", "Step A");
        UserRoadmapProgress p2 = new UserRoadmapProgress(testUser, "Java Backend Developer", "Step B");
        when(progressRepository.findByUser(testUser)).thenReturn(List.of(p1, p2));

        when(skillService.getUserSkills(testUser)).thenReturn(List.of("Step A", "Step B"));
        when(careerService.getCareerByIdOrTitle("Java Backend Developer")).thenReturn(Optional.empty());

        ProgressSummaryResponse summary = userProgressService.getProgressSummary(testUser);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalStepsCount()).isEqualTo(2);
        assertThat(summary.getCompletedStepsCount()).isEqualTo(2);
        assertThat(summary.getRoadmapPercent()).isEqualTo(100);
        assertThat(summary.getNextTopicToLearn()).isEqualTo("All milestones completed!");
    }

    @Test
    @DisplayName("getRecentActivities returns empty list when no activities exist")
    void testGetRecentActivitiesEmpty() {
        when(quizAttemptRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(Collections.emptyList());
        when(interviewRepository.findByUserOrderByStartedAtDesc(testUser)).thenReturn(Collections.emptyList());
        when(resumeRepository.findFirstByUserOrderByUploadTimestampDesc(testUser)).thenReturn(Optional.empty());
        when(progressRepository.findByUser(testUser)).thenReturn(Collections.emptyList());
        when(mentorshipRequestRepository.findByStudentIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(Collections.emptyList());

        List<RecentActivityDto> activities = userProgressService.getRecentActivities(testUser);

        assertThat(activities).isNotNull();
        assertThat(activities).isEmpty();
    }
}
