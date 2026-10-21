package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.AdminStatsOverviewDto;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private com.careeradvisor.backend.service.AdminMentorshipService adminMentorshipService;

    @MockBean
    private com.careeradvisor.backend.service.AdminAnalyticsService adminAnalyticsService;

    @MockBean
    private com.careeradvisor.backend.service.AdminAuditLogService adminAuditLogService;

    @MockBean
    private com.careeradvisor.backend.service.PlatformSettingsService platformSettingsService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Unauthenticated request to /api/admin/health returns 401 Unauthorized")
    void testUnauthenticatedAccessBlocked() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_USER request to /api/admin/health returns 403 Forbidden")
    @WithMockUser(username = "student@careeradvisor.com", roles = {"USER"})
    void testUserRoleForbiddenOnAdminHealth() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER request to /api/admin/stats/overview returns 403 Forbidden")
    @WithMockUser(username = "student@careeradvisor.com", roles = {"USER"})
    void testUserRoleForbiddenOnAdminStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER request to /api/admin/users returns 403 Forbidden")
    @WithMockUser(username = "student@careeradvisor.com", roles = {"USER"})
    void testUserRoleForbiddenOnAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN request to /api/admin/health succeeds with 200 OK")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testAdminRoleCanAccessAdminHealth() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("ROLE_ADMIN request to /api/admin/stats/overview succeeds with 200 OK")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testAdminRoleCanAccessAdminStats() throws Exception {
        AdminStatsOverviewDto stats = AdminStatsOverviewDto.builder()
                .totalUsers(10L)
                .totalResumes(5L)
                .careerGoalsDistribution(Map.of("Java Backend Developer", 8L))
                .userLevelDistribution(Map.of("Intermediate", 10L))
                .build();
        when(adminService.getStatsOverview()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10L))
                .andExpect(jsonPath("$.careerGoalsDistribution['Java Backend Developer']").value(8L));
    }

    @Test
    @DisplayName("ROLE_ADMIN request to /api/admin/users succeeds with 200 OK")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testAdminRoleCanAccessAdminUsers() throws Exception {
        com.careeradvisor.backend.dto.PageResponse<com.careeradvisor.backend.dto.AdminUserDto> emptyPage =
                com.careeradvisor.backend.dto.PageResponse.<com.careeradvisor.backend.dto.AdminUserDto>builder()
                        .content(Collections.emptyList())
                        .page(0)
                        .size(20)
                        .totalElements(0L)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .build();
        when(adminService.getUsers(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_USER request to /api/admin/mentorships/summary returns 403 Forbidden")
    @WithMockUser(username = "student@careeradvisor.com", roles = {"USER"})
    void testUserRoleForbiddenOnAdminMentorships() throws Exception {
        mockMvc.perform(get("/api/admin/mentorships/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER request to /api/admin/analytics/users returns 403 Forbidden")
    @WithMockUser(username = "student@careeradvisor.com", roles = {"USER"})
    void testUserRoleForbiddenOnAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN request to /api/admin/mentorships/summary succeeds with 200 OK")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testAdminRoleCanAccessAdminMentorships() throws Exception {
        when(adminMentorshipService.getMentorshipSummary()).thenReturn(
                com.careeradvisor.backend.dto.MentorshipOversightSummaryDto.builder()
                        .totalRequests(10L)
                        .build()
        );
        mockMvc.perform(get("/api/admin/mentorships/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_ADMIN request to /api/admin/analytics/users succeeds with 200 OK")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testAdminRoleCanAccessAdminAnalytics() throws Exception {
        when(adminAnalyticsService.getUserAnalytics(30)).thenReturn(
                com.careeradvisor.backend.dto.UserAnalyticsDto.builder()
                        .totalUsers(10L)
                        .build()
        );
        mockMvc.perform(get("/api/admin/analytics/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/admin/audit-logs returns 401 Unauthorized")
    void testUnauthenticatedAccessBlockedOnAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_USER request to /api/admin/audit-logs returns 403 Forbidden")
    @WithMockUser(username = "student@careeradvisor.com", roles = {"USER"})
    void testUserRoleForbiddenOnAdminAuditLogs() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER request to /api/admin/audit-logs/summary returns 403 Forbidden")
    @WithMockUser(username = "student@careeradvisor.com", roles = {"USER"})
    void testUserRoleForbiddenOnAdminAuditSummary() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN request to /api/admin/audit-logs succeeds with 200 OK")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testAdminRoleCanAccessAdminAuditLogs() throws Exception {
        when(adminAuditLogService.getAuditLogs(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(
                com.careeradvisor.backend.dto.PageResponse.<com.careeradvisor.backend.dto.AdminAuditLogDto>builder()
                        .content(Collections.emptyList())
                        .page(0)
                        .size(20)
                        .totalElements(0L)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .build()
        );
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_ADMIN request to /api/admin/audit-logs/summary succeeds with 200 OK")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testAdminRoleCanAccessAdminAuditSummary() throws Exception {
        when(adminAuditLogService.getAuditSummary()).thenReturn(
                com.careeradvisor.backend.dto.AdminAuditSummaryDto.builder()
                        .totalEvents(10L)
                        .build()
        );
        mockMvc.perform(get("/api/admin/audit-logs/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/admin/settings returns 401 Unauthorized")
    void testUnauthenticatedAccessBlockedOnSettings() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_USER request to /api/admin/settings returns 403 Forbidden")
    @WithMockUser(username = "student@careeradvisor.com", roles = {"USER"})
    void testUserRoleForbiddenOnAdminSettings() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN request to /api/admin/settings succeeds with 200 OK")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testAdminRoleCanAccessAdminSettings() throws Exception {
        when(platformSettingsService.getPlatformSettings()).thenReturn(
                com.careeradvisor.backend.dto.PlatformSettingsDto.builder()
                        .mentorshipCapacity(new com.careeradvisor.backend.dto.MentorshipCapacityConfigDto(1, 10, 3))
                        .build()
        );
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk());
    }
}
