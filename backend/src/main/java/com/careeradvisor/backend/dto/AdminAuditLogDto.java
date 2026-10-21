package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogDto {
    private Long id;
    private Long adminUserId;
    private String adminName;
    private String adminEmail;
    private AdminAuditAction action;
    private AdminAuditTargetType targetType;
    private String targetId;
    private String summary;
    private String detailsJson;
    private String ipAddress;
    private String correlationId;
    private LocalDateTime createdAt;
}
