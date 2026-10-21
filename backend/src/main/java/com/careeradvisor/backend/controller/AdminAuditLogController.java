package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.AdminAuditLogDto;
import com.careeradvisor.backend.dto.AdminAuditSummaryDto;
import com.careeradvisor.backend.dto.PageResponse;
import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import com.careeradvisor.backend.service.AdminAuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AdminAuditLogService auditLogService;

    public AdminAuditLogController(AdminAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AdminAuditLogDto>> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
            @RequestParam(value = "action", required = false) AdminAuditAction action,
            @RequestParam(value = "targetType", required = false) AdminAuditTargetType targetType,
            @RequestParam(value = "adminUserId", required = false) Long adminUserId,
            @RequestParam(value = "targetId", required = false) String targetId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        PageResponse<AdminAuditLogDto> response = auditLogService.getAuditLogs(
                page, size, sort, action, targetType, adminUserId, targetId, search, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminAuditSummaryDto> getAuditSummary() {
        AdminAuditSummaryDto summary = auditLogService.getAuditSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminAuditLogDto> getAuditLogById(@PathVariable Long id) {
        AdminAuditLogDto logEntry = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(logEntry);
    }
}
