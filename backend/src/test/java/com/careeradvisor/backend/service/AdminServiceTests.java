package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AdminStatsOverviewDto;
import com.careeradvisor.backend.dto.CategoryCountProjection;
import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private UserProblemProgressRepository problemProgressRepository;

    @Mock
    private MockInterviewRepository mockInterviewRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private UserRoadmapProgressRepository roadmapProgressRepository;

    @Mock
    private AdminAuditLogService auditLogService;

    @InjectMocks
    private AdminService adminService;

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
    @DisplayName("getStatsOverview returns correct counts and database-aggregated distributions")
    void testGetStatsOverviewWithValidData() {
        when(userRepository.count()).thenReturn(25L);
        when(resumeRepository.count()).thenReturn(14L);
        when(userRepository.countByLatestQuizScoreIsNotNull()).thenReturn(18L);
        when(problemProgressRepository.countBySolvedTrue()).thenReturn(42L);
        when(mockInterviewRepository.count()).thenReturn(10L);
        when(mockInterviewRepository.countByStatus(InterviewStatus.COMPLETED)).thenReturn(7L);

        List<CategoryCountProjection> careerGoals = List.of(
                createProjection("Java Backend Developer", 15L),
                createProjection("Frontend Engineer", 10L)
        );
        when(userRepository.countGroupedByCareerGoal()).thenReturn(careerGoals);

        List<CategoryCountProjection> userLevels = List.of(
                createProjection("Beginner", 5L),
                createProjection("Intermediate", 15L),
                createProjection("Advanced", 5L)
        );
        when(userRepository.countGroupedByUserLevel()).thenReturn(userLevels);

        AdminStatsOverviewDto stats = adminService.getStatsOverview();

        assertNotNull(stats);
        assertEquals(25L, stats.getTotalUsers());
        assertEquals(14L, stats.getTotalResumes());
        assertEquals(18L, stats.getTotalQuizAttempts());
        assertEquals(42L, stats.getTotalSolvedProblems());
        assertEquals(10L, stats.getTotalMockInterviews());
        assertEquals(7L, stats.getTotalCompletedInterviews());
        assertEquals(25L, stats.getTotalCareerGoals());

        assertEquals(2, stats.getCareerGoalsDistribution().size());
        assertEquals(15L, stats.getCareerGoalsDistribution().get("Java Backend Developer"));
        assertEquals(10L, stats.getCareerGoalsDistribution().get("Frontend Engineer"));

        assertEquals(3, stats.getUserLevelDistribution().size());
        assertEquals(5L, stats.getUserLevelDistribution().get("Beginner"));
        assertEquals(15L, stats.getUserLevelDistribution().get("Intermediate"));
        assertEquals(5L, stats.getUserLevelDistribution().get("Advanced"));

        // Ensure userRepository.findAll() was NEVER called
        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("getStatsOverview safely handles null, empty string, and whitespace categories")
    void testGetStatsOverviewHandlesNullAndWhitespaceCategories() {
        when(userRepository.count()).thenReturn(10L);
        when(resumeRepository.count()).thenReturn(5L);
        when(userRepository.countByLatestQuizScoreIsNotNull()).thenReturn(4L);
        when(problemProgressRepository.countBySolvedTrue()).thenReturn(10L);
        when(mockInterviewRepository.count()).thenReturn(2L);
        when(mockInterviewRepository.countByStatus(InterviewStatus.COMPLETED)).thenReturn(1L);

        List<CategoryCountProjection> careerGoals = new ArrayList<>();
        careerGoals.add(createProjection("Full Stack Developer", 6L));
        careerGoals.add(createProjection(null, 2L));
        careerGoals.add(createProjection("   ", 1L));
        careerGoals.add(createProjection("", 1L));
        when(userRepository.countGroupedByCareerGoal()).thenReturn(careerGoals);

        List<CategoryCountProjection> userLevels = new ArrayList<>();
        userLevels.add(createProjection("Intermediate", 6L));
        userLevels.add(createProjection(null, 4L));
        when(userRepository.countGroupedByUserLevel()).thenReturn(userLevels);

        AdminStatsOverviewDto stats = adminService.getStatsOverview();

        assertNotNull(stats);
        assertEquals(1, stats.getCareerGoalsDistribution().size());
        assertEquals(6L, stats.getCareerGoalsDistribution().get("Full Stack Developer"));
        assertFalse(stats.getCareerGoalsDistribution().containsKey(null));
        assertFalse(stats.getCareerGoalsDistribution().containsKey(""));
        assertEquals(6L, stats.getTotalCareerGoals());

        assertEquals(1, stats.getUserLevelDistribution().size());
        assertEquals(6L, stats.getUserLevelDistribution().get("Intermediate"));
        assertFalse(stats.getUserLevelDistribution().containsKey(null));

        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("getStatsOverview handles empty database gracefully")
    void testGetStatsOverviewEmpty() {
        when(userRepository.count()).thenReturn(0L);
        when(resumeRepository.count()).thenReturn(0L);
        when(userRepository.countByLatestQuizScoreIsNotNull()).thenReturn(0L);
        when(problemProgressRepository.countBySolvedTrue()).thenReturn(0L);
        when(mockInterviewRepository.count()).thenReturn(0L);
        when(mockInterviewRepository.countByStatus(InterviewStatus.COMPLETED)).thenReturn(0L);
        when(userRepository.countGroupedByCareerGoal()).thenReturn(List.of());
        when(userRepository.countGroupedByUserLevel()).thenReturn(List.of());

        AdminStatsOverviewDto stats = adminService.getStatsOverview();

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalUsers());
        assertEquals(0L, stats.getTotalCareerGoals());
        assertTrue(stats.getCareerGoalsDistribution().isEmpty());
        assertTrue(stats.getUserLevelDistribution().isEmpty());

        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("getUsers fetches paginated users with bulk batch metric queries (no N+1)")
    void testGetUsersWithBulkBatchMetrics() {
        com.careeradvisor.backend.model.User u1 = com.careeradvisor.backend.model.User.builder()
                .id(101L)
                .name("Alice Smith")
                .email("alice@test.com")
                .role(com.careeradvisor.backend.model.Role.USER)
                .status(com.careeradvisor.backend.model.UserStatus.ACTIVE)
                .careerGoal("Java Developer")
                .userLevel("Intermediate")
                .build();

        com.careeradvisor.backend.model.User u2 = com.careeradvisor.backend.model.User.builder()
                .id(102L)
                .name("Bob Admin")
                .email("bob@test.com")
                .role(com.careeradvisor.backend.model.Role.ADMIN)
                .status(com.careeradvisor.backend.model.UserStatus.ACTIVE)
                .careerGoal("DevOps Engineer")
                .userLevel("Advanced")
                .build();

        org.springframework.data.domain.Page<com.careeradvisor.backend.model.User> page =
                new org.springframework.data.domain.PageImpl<>(List.of(u1, u2));

        when(userRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<com.careeradvisor.backend.model.User>>any(),
                org.mockito.ArgumentMatchers.<org.springframework.data.domain.Pageable>any()))
                .thenReturn(page);

        // Bulk batch mocks:
        when(userSkillRepository.countSkillsByUserIds(List.of(101L, 102L)))
                .thenReturn(List.<Object[]>of(new Object[]{101L, 5L}, new Object[]{102L, 8L}));
        when(resumeRepository.findUserIdsWithResumes(List.of(101L, 102L)))
                .thenReturn(List.of(101L));
        when(mockInterviewRepository.countInterviewsByUserIds(List.of(101L, 102L)))
                .thenReturn(List.<Object[]>of(new Object[]{101L, 3L}));
        when(problemProgressRepository.countSolvedProblemsByUserIds(List.of(101L, 102L)))
                .thenReturn(List.<Object[]>of(new Object[]{101L, 12L}, new Object[]{102L, 25L}));

        com.careeradvisor.backend.dto.PageResponse<com.careeradvisor.backend.dto.AdminUserDto> response =
                adminService.getUsers(0, 20, "createdAt,desc", "test", null, null);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());

        com.careeradvisor.backend.dto.AdminUserDto dto1 = response.getContent().get(0);
        assertEquals(101L, dto1.getId());
        assertEquals("Alice Smith", dto1.getName());
        assertEquals(5L, dto1.getSkillCount());
        assertTrue(dto1.isResumePresent());
        assertEquals(3L, dto1.getMockInterviewCount());
        assertEquals(12L, dto1.getSolvedProblemsCount());

        com.careeradvisor.backend.dto.AdminUserDto dto2 = response.getContent().get(1);
        assertEquals(102L, dto2.getId());
        assertEquals(8L, dto2.getSkillCount());
        assertFalse(dto2.isResumePresent());
        assertEquals(0L, dto2.getMockInterviewCount());
        assertEquals(25L, dto2.getSolvedProblemsCount());

        // Verify individual count queries were NEVER called in a loop
        verify(userSkillRepository, never()).countByUser(any());
        verify(mockInterviewRepository, never()).countByUser(any());
    }

    @Test
    @DisplayName("updateUserStatus successfully suspends an account with mandatory reason")
    void testUpdateUserStatusSuspendSuccess() {
        com.careeradvisor.backend.model.User targetUser = com.careeradvisor.backend.model.User.builder()
                .id(201L)
                .name("Student Target")
                .email("student@target.com")
                .role(com.careeradvisor.backend.model.Role.USER)
                .status(com.careeradvisor.backend.model.UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(201L)).thenReturn(java.util.Optional.of(targetUser));
        when(userSkillRepository.findByUserOrderByAddedAtDesc(targetUser)).thenReturn(List.of());
        when(resumeRepository.findByUserOrderByUploadTimestampDesc(targetUser)).thenReturn(List.of());
        when(mockInterviewRepository.findByUserOrderByStartedAtDesc(targetUser)).thenReturn(List.of());
        when(problemProgressRepository.countByUserAndSolvedTrue(targetUser)).thenReturn(0);
        when(roadmapProgressRepository.countByUser(targetUser)).thenReturn(0L);

        com.careeradvisor.backend.dto.UpdateUserStatusRequest request =
                new com.careeradvisor.backend.dto.UpdateUserStatusRequest(
                        com.careeradvisor.backend.model.UserStatus.SUSPENDED,
                        "Violated community terms"
                );

        com.careeradvisor.backend.dto.AdminUserDetailDto result =
                adminService.updateUserStatus(100L, 201L, request);

        assertNotNull(result);
        assertEquals(com.careeradvisor.backend.model.UserStatus.SUSPENDED, targetUser.getStatus());
        assertEquals("Violated community terms", targetUser.getSuspensionReason());
        assertNotNull(targetUser.getSuspendedAt());
        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("updateUserStatus throws exception when suspending without reason")
    void testUpdateUserStatusSuspendWithoutReasonThrows() {
        com.careeradvisor.backend.model.User targetUser = com.careeradvisor.backend.model.User.builder()
                .id(201L)
                .name("Student Target")
                .role(com.careeradvisor.backend.model.Role.USER)
                .status(com.careeradvisor.backend.model.UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(201L)).thenReturn(java.util.Optional.of(targetUser));

        com.careeradvisor.backend.dto.UpdateUserStatusRequest request =
                new com.careeradvisor.backend.dto.UpdateUserStatusRequest(
                        com.careeradvisor.backend.model.UserStatus.SUSPENDED,
                        "   " // Blank reason
                );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminService.updateUserStatus(100L, 201L, request)
        );

        assertTrue(ex.getMessage().contains("reason is mandatory"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserStatus prevents admin from self-suspending")
    void testUpdateUserStatusSelfSuspensionThrows() {
        com.careeradvisor.backend.model.User admin = com.careeradvisor.backend.model.User.builder()
                .id(100L)
                .role(com.careeradvisor.backend.model.Role.ADMIN)
                .status(com.careeradvisor.backend.model.UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(100L)).thenReturn(java.util.Optional.of(admin));

        com.careeradvisor.backend.dto.UpdateUserStatusRequest request =
                new com.careeradvisor.backend.dto.UpdateUserStatusRequest(
                        com.careeradvisor.backend.model.UserStatus.SUSPENDED,
                        "Self suspension test"
                );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminService.updateUserStatus(100L, 100L, request)
        );

        assertTrue(ex.getMessage().contains("Administrators cannot modify their own account status"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserStatus prevents suspending the last remaining active admin")
    void testUpdateUserStatusLastAdminProtectionThrows() {
        com.careeradvisor.backend.model.User otherAdmin = com.careeradvisor.backend.model.User.builder()
                .id(102L)
                .role(com.careeradvisor.backend.model.Role.ADMIN)
                .status(com.careeradvisor.backend.model.UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(102L)).thenReturn(java.util.Optional.of(otherAdmin));
        when(userRepository.countByRoleAndStatus(com.careeradvisor.backend.model.Role.ADMIN, com.careeradvisor.backend.model.UserStatus.ACTIVE))
                .thenReturn(1L); // Only 1 active admin exists!

        com.careeradvisor.backend.dto.UpdateUserStatusRequest request =
                new com.careeradvisor.backend.dto.UpdateUserStatusRequest(
                        com.careeradvisor.backend.model.UserStatus.SUSPENDED,
                        "Suspending admin"
                );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                adminService.updateUserStatus(100L, 102L, request)
        );

        assertTrue(ex.getMessage().contains("Cannot suspend or deactivate the last remaining active administrator"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserStatus successfully reactivates a suspended user and clears reason")
    void testUpdateUserStatusReactivateSuccess() {
        com.careeradvisor.backend.model.User suspendedUser = com.careeradvisor.backend.model.User.builder()
                .id(202L)
                .name("Suspended Candidate")
                .role(com.careeradvisor.backend.model.Role.USER)
                .status(com.careeradvisor.backend.model.UserStatus.SUSPENDED)
                .suspensionReason("Previous infraction")
                .suspendedAt(java.time.LocalDateTime.now())
                .build();

        when(userRepository.findById(202L)).thenReturn(java.util.Optional.of(suspendedUser));
        when(userSkillRepository.findByUserOrderByAddedAtDesc(suspendedUser)).thenReturn(List.of());
        when(resumeRepository.findByUserOrderByUploadTimestampDesc(suspendedUser)).thenReturn(List.of());
        when(mockInterviewRepository.findByUserOrderByStartedAtDesc(suspendedUser)).thenReturn(List.of());
        when(problemProgressRepository.countByUserAndSolvedTrue(suspendedUser)).thenReturn(0);
        when(roadmapProgressRepository.countByUser(suspendedUser)).thenReturn(0L);

        com.careeradvisor.backend.dto.UpdateUserStatusRequest request =
                new com.careeradvisor.backend.dto.UpdateUserStatusRequest(
                        com.careeradvisor.backend.model.UserStatus.ACTIVE,
                        null
                );

        com.careeradvisor.backend.dto.AdminUserDetailDto result =
                adminService.updateUserStatus(100L, 202L, request);

        assertNotNull(result);
        assertEquals(com.careeradvisor.backend.model.UserStatus.ACTIVE, suspendedUser.getStatus());
        assertNull(suspendedUser.getSuspensionReason());
        assertNull(suspendedUser.getSuspendedAt());
        verify(userRepository).save(suspendedUser);
    }
}
