package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.AdminAuditLogDto;
import com.careeradvisor.backend.dto.AdminAuditSummaryDto;
import com.careeradvisor.backend.dto.PageResponse;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import com.careeradvisor.backend.service.AdminAuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuditLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAuditLogService auditLogService;

    @Test
    @DisplayName("GET /api/admin/audit-logs returns 200 and paginated audit logs for admin")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testGetAuditLogsSuccess() throws Exception {
        AdminAuditLogDto logDto = AdminAuditLogDto.builder()
                .id(1L)
                .action(AdminAuditAction.USER_SUSPENDED)
                .adminUserId(99L)
                .adminEmail("admin@careeradvisor.com")
                .adminName("Admin User")
                .targetType(AdminAuditTargetType.USER)
                .targetId("42")
                .summary("Suspended user: Bad behavior")
                .ipAddress("127.0.0.1")
                .correlationId("corr-123")
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<AdminAuditLogDto> pageResponse = PageResponse.<AdminAuditLogDto>builder()
                .content(List.of(logDto))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(auditLogService.getAuditLogs(eq(0), eq(20), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].action").value("USER_SUSPENDED"))
                .andExpect(jsonPath("$.content[0].targetType").value("USER"))
                .andExpect(jsonPath("$.content[0].targetId").value("42"))
                .andExpect(jsonPath("$.content[0].adminEmail").value("admin@careeradvisor.com"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(auditLogService).getAuditLogs(eq(0), eq(20), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("GET /api/admin/audit-logs parses query parameters correctly")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testGetAuditLogsWithParameters() throws Exception {
        PageResponse<AdminAuditLogDto> emptyPage = PageResponse.<AdminAuditLogDto>builder()
                .content(Collections.emptyList())
                .page(1)
                .size(10)
                .totalElements(0L)
                .totalPages(0)
                .first(false)
                .last(true)
                .build();

        when(auditLogService.getAuditLogs(eq(1), eq(10), eq("createdAt,asc"), eq(AdminAuditAction.MENTOR_APPROVED),
                eq(AdminAuditTargetType.MENTOR), eq(5L), eq("100"), eq("approved"), any(), any()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "createdAt,asc")
                        .param("action", "MENTOR_APPROVED")
                        .param("targetType", "MENTOR")
                        .param("adminUserId", "5")
                        .param("targetId", "100")
                        .param("search", "approved")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("GET /api/admin/audit-logs/summary returns 200 and summary counts")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testGetAuditSummary() throws Exception {
        AdminAuditSummaryDto summary = AdminAuditSummaryDto.builder()
                .totalEvents(150L)
                .todayEvents(12L)
                .mentorActionsCount(50L)
                .userActionsCount(80L)
                .mentorshipActionsCount(15L)
                .supportActionsCount(10L)
                .settingsActionsCount(5L)
                .build();

        when(auditLogService.getAuditSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/admin/audit-logs/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(150))
                .andExpect(jsonPath("$.todayEvents").value(12))
                .andExpect(jsonPath("$.mentorActionsCount").value(50))
                .andExpect(jsonPath("$.supportActionsCount").value(10))
                .andExpect(jsonPath("$.settingsActionsCount").value(5));
    }

    @Test
    @DisplayName("GET /api/admin/audit-logs/{id} returns 200 when record exists")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testGetAuditLogByIdSuccess() throws Exception {
        AdminAuditLogDto logDto = AdminAuditLogDto.builder()
                .id(42L)
                .action(AdminAuditAction.MENTORSHIP_TERMINATED)
                .adminUserId(1L)
                .adminEmail("superadmin@careeradvisor.com")
                .targetType(AdminAuditTargetType.MENTORSHIP_REQUEST)
                .targetId("99")
                .summary("Terminated mentorship")
                .detailsJson("{\"reason\":\"Inactivity\"}")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogService.getAuditLogById(42L)).thenReturn(logDto);

        mockMvc.perform(get("/api/admin/audit-logs/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.action").value("MENTORSHIP_TERMINATED"))
                .andExpect(jsonPath("$.detailsJson").value("{\"reason\":\"Inactivity\"}"));
    }

    @Test
    @DisplayName("GET /api/admin/audit-logs/{id} returns 404 when record does not exist")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testGetAuditLogByIdNotFound() throws Exception {
        when(auditLogService.getAuditLogById(999L))
                .thenThrow(new ResourceNotFoundException("Audit log not found with id: 999"));

        mockMvc.perform(get("/api/admin/audit-logs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
