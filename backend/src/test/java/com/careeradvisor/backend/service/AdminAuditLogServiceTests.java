package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AdminAuditLogDto;
import com.careeradvisor.backend.dto.AdminAuditSummaryDto;
import com.careeradvisor.backend.dto.PageResponse;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditLog;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import com.careeradvisor.backend.repository.AdminAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTests {

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    private ObjectMapper objectMapper;
    private AdminAuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditLogService = new AdminAuditLogService(auditLogRepository, objectMapper);
    }

    @Test
    @DisplayName("recordAction persists audit log and strips sensitive secrets from details")
    void testRecordActionWithSanitization() {
        when(auditLogRepository.save(any(AdminAuditLog.class))).thenAnswer(invocation -> {
            AdminAuditLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", "Violation of code of conduct");
        details.put("password", "Secret123!");
        details.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        details.put("jwt", "sensitive_jwt_token");
        details.put("targetEmail", "user@example.com");

        AdminAuditLog saved = auditLogService.recordAction(
                10L,
                "Admin User",
                "admin@careeradvisor.dev",
                AdminAuditAction.USER_SUSPENDED,
                AdminAuditTargetType.USER,
                "42",
                "Suspended user account: user@example.com",
                details
        );

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        assertEquals(10L, saved.getAdminUserId());
        assertEquals("Admin User", saved.getAdminName());
        assertEquals("admin@careeradvisor.dev", saved.getAdminEmail());
        assertEquals(AdminAuditAction.USER_SUSPENDED, saved.getAction());
        assertEquals(AdminAuditTargetType.USER, saved.getTargetType());
        assertEquals("42", saved.getTargetId());
        assertEquals("Suspended user account: user@example.com", saved.getSummary());

        // Verify sensitive keys stripped
        assertNotNull(saved.getDetailsJson());
        assertFalse(saved.getDetailsJson().contains("Secret123!"));
        assertFalse(saved.getDetailsJson().contains("sensitive_jwt_token"));
        assertTrue(saved.getDetailsJson().contains("Violation of code of conduct"));
        assertTrue(saved.getDetailsJson().contains("user@example.com"));

        verify(auditLogRepository, times(1)).save(any(AdminAuditLog.class));
    }

    @Test
    @DisplayName("recordAction throws IllegalArgumentException when mandatory fields are missing")
    void testRecordActionValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                auditLogService.recordAction(null, null, null, null, AdminAuditTargetType.USER, "1", "summary", null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                auditLogService.recordAction(null, null, null, AdminAuditAction.USER_SUSPENDED, null, "1", "summary", null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                auditLogService.recordAction(null, null, null, AdminAuditAction.USER_SUSPENDED, AdminAuditTargetType.USER, "  ", "summary", null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                auditLogService.recordAction(null, null, null, AdminAuditAction.USER_SUSPENDED, AdminAuditTargetType.USER, "1", "", null)
        );
    }

    @Test
    @DisplayName("getAuditLogs applies specifications and returns paginated response")
    void testGetAuditLogs() {
        AdminAuditLog logEntry = AdminAuditLog.builder()
                .id(100L)
                .adminUserId(5L)
                .adminName("Super Admin")
                .adminEmail("admin@careeradvisor.dev")
                .action(AdminAuditAction.MENTOR_APPROVED)
                .targetType(AdminAuditTargetType.MENTOR)
                .targetId("15")
                .summary("Approved mentor application")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AdminAuditLog> page = new PageImpl<>(List.of(logEntry), PageRequest.of(0, 20), 1);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AdminAuditLogDto> result = auditLogService.getAuditLogs(
                0, 20, "createdAt,desc", AdminAuditAction.MENTOR_APPROVED, AdminAuditTargetType.MENTOR,
                5L, "15", "Approved", LocalDate.now().minusDays(7), LocalDate.now()
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        AdminAuditLogDto dto = result.getContent().get(0);
        assertEquals(100L, dto.getId());
        assertEquals("Super Admin", dto.getAdminName());
        assertEquals(AdminAuditAction.MENTOR_APPROVED, dto.getAction());
        assertEquals(AdminAuditTargetType.MENTOR, dto.getTargetType());
        assertEquals("15", dto.getTargetId());
    }

    @Test
    @DisplayName("getAuditLogById returns entry or throws ResourceNotFoundException")
    void testGetAuditLogById() {
        AdminAuditLog logEntry = AdminAuditLog.builder()
                .id(100L)
                .adminUserId(5L)
                .adminName("Super Admin")
                .adminEmail("admin@careeradvisor.dev")
                .action(AdminAuditAction.MENTORSHIP_TERMINATED)
                .targetType(AdminAuditTargetType.MENTORSHIP_REQUEST)
                .targetId("55")
                .summary("Terminated mentorship")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogRepository.findById(100L)).thenReturn(Optional.of(logEntry));
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        AdminAuditLogDto found = auditLogService.getAuditLogById(100L);
        assertNotNull(found);
        assertEquals(100L, found.getId());
        assertEquals(AdminAuditAction.MENTORSHIP_TERMINATED, found.getAction());

        assertThrows(ResourceNotFoundException.class, () -> auditLogService.getAuditLogById(999L));
    }

    @Test
    @DisplayName("getAuditSummary aggregates accurate metric counts")
    void testGetAuditSummary() {
        when(auditLogRepository.count()).thenReturn(150L);
        when(auditLogRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(12L);
        when(auditLogRepository.countByTargetType(AdminAuditTargetType.MENTOR)).thenReturn(40L);
        when(auditLogRepository.countByTargetType(AdminAuditTargetType.USER)).thenReturn(75L);
        when(auditLogRepository.countByTargetType(AdminAuditTargetType.MENTORSHIP_REQUEST)).thenReturn(35L);
        when(auditLogRepository.countByTargetType(AdminAuditTargetType.SUPPORT_CONVERSATION)).thenReturn(10L);
        when(auditLogRepository.countByTargetType(AdminAuditTargetType.PLATFORM_SETTING)).thenReturn(5L);

        AdminAuditSummaryDto summary = auditLogService.getAuditSummary();

        assertNotNull(summary);
        assertEquals(150L, summary.getTotalEvents());
        assertEquals(12L, summary.getTodayEvents());
        assertEquals(40L, summary.getMentorActionsCount());
        assertEquals(75L, summary.getUserActionsCount());
        assertEquals(35L, summary.getMentorshipActionsCount());
        assertEquals(10L, summary.getSupportActionsCount());
        assertEquals(5L, summary.getSettingsActionsCount());
    }
}
