package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AdminAuditLogDto;
import com.careeradvisor.backend.dto.AdminAuditSummaryDto;
import com.careeradvisor.backend.dto.PageResponse;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditLog;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import com.careeradvisor.backend.repository.AdminAuditLogRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditLogService.class);

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwordhash", "secret", "token", "jwt",
            "refreshtoken", "accesstoken", "authorization", "creditcard",
            "auth", "credential", "privatekey"
    );

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt", "id", "action", "targetType", "adminUserId"
    );

    private final AdminAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminAuditLogService(AdminAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Atomically records an administrative action in the active database transaction.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public AdminAuditLog recordAction(
            Long adminUserId,
            String adminName,
            String adminEmail,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            String summary,
            Map<String, Object> details
    ) {
        if (action == null) {
            throw new IllegalArgumentException("Audit action must not be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Audit targetType must not be null");
        }
        if (!StringUtils.hasText(targetId)) {
            throw new IllegalArgumentException("Audit targetId must not be empty");
        }
        if (!StringUtils.hasText(summary)) {
            throw new IllegalArgumentException("Audit summary must not be empty");
        }

        // Auto-resolve administrator identity from SecurityContext if missing
        if (adminUserId == null || !StringUtils.hasText(adminEmail)) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
                    CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
                    if (adminUserId == null) adminUserId = principal.getId();
                    if (!StringUtils.hasText(adminName)) {
                        adminName = principal.getName();
                    }
                    if (!StringUtils.hasText(adminEmail)) {
                        adminEmail = principal.getUsername();
                    }
                }
            } catch (Exception e) {
                log.debug("Unable to resolve admin principal from SecurityContext: {}", e.getMessage());
            }
        }

        String ipAddress = resolveClientIp();
        String correlationId = resolveCorrelationId();
        String sanitizedDetailsJson = sanitizeAndSerializeDetails(details);

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .adminName(truncate(adminName, 150))
                .adminEmail(truncate(adminEmail, 150))
                .action(action)
                .targetType(targetType)
                .targetId(truncate(targetId, 100))
                .summary(truncate(summary, 500))
                .detailsJson(sanitizedDetailsJson)
                .ipAddress(truncate(ipAddress, 64))
                .correlationId(truncate(correlationId, 64))
                .createdAt(LocalDateTime.now())
                .build();

        AdminAuditLog saved = auditLogRepository.save(auditLog);
        log.info("ADMIN AUDIT [action={}, targetType={}, targetId={}, admin={}] - {}",
                action, targetType, targetId, adminEmail, summary);
        return saved;
    }

    /**
     * Overload recording action with automatic SecurityContext identity resolution.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public AdminAuditLog recordAction(
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            String summary,
            Map<String, Object> details
    ) {
        return recordAction(null, null, null, action, targetType, targetId, summary, details);
    }

    /**
     * Retrieves paginated, sorted, and filtered audit logs using database-level criteria.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogDto> getAuditLogs(
            int page,
            int size,
            String sort,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long adminUserId,
            String targetId,
            String search,
            LocalDate startDate,
            LocalDate endDate
    ) {
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.max(1, Math.min(size, 100));

        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",");
            String property = parts[0].trim();
            Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            if (ALLOWED_SORT_PROPERTIES.contains(property)) {
                sortObj = Sort.by(direction, property);
            }
        }

        Pageable pageable = PageRequest.of(clampedPage, clampedSize, sortObj);

        Specification<AdminAuditLog> spec = Specification.where(null);

        if (action != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }

        if (targetType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetType"), targetType));
        }

        if (adminUserId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("adminUserId"), adminUserId));
        }

        if (StringUtils.hasText(targetId)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetId"), targetId.trim()));
        }

        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
        }

        if (endDate != null) {
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
        }

        if (StringUtils.hasText(search)) {
            String term = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("summary")), term),
                    cb.like(cb.lower(root.get("adminName")), term),
                    cb.like(cb.lower(root.get("adminEmail")), term),
                    cb.like(cb.lower(root.get("targetId")), term)
            ));
        }

        Page<AdminAuditLog> logPage = auditLogRepository.findAll(spec, pageable);
        List<AdminAuditLogDto> dtos = logPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PageResponse.of(logPage, dtos);
    }

    /**
     * Retrieves a single audit log entry by ID.
     */
    @Transactional(readOnly = true)
    public AdminAuditLogDto getAuditLogById(Long id) {
        AdminAuditLog logEntry = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log entry not found with id: " + id));
        return mapToDto(logEntry);
    }

    /**
     * Calculates summary metrics for administrative dashboard cards.
     */
    @Transactional(readOnly = true)
    public AdminAuditSummaryDto getAuditSummary() {
        long totalEvents = auditLogRepository.count();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayEvents = auditLogRepository.countByCreatedAtAfter(todayStart);

        long mentorActions = auditLogRepository.countByTargetType(AdminAuditTargetType.MENTOR);
        long userActions = auditLogRepository.countByTargetType(AdminAuditTargetType.USER);
        long mentorshipActions = auditLogRepository.countByTargetType(AdminAuditTargetType.MENTORSHIP_REQUEST);
        long supportActions = auditLogRepository.countByTargetType(AdminAuditTargetType.SUPPORT_CONVERSATION);
        long settingsActions = auditLogRepository.countByTargetType(AdminAuditTargetType.PLATFORM_SETTING);

        return AdminAuditSummaryDto.builder()
                .totalEvents(totalEvents)
                .todayEvents(todayEvents)
                .mentorActionsCount(mentorActions)
                .userActionsCount(userActions)
                .mentorshipActionsCount(mentorshipActions)
                .supportActionsCount(supportActions)
                .settingsActionsCount(settingsActions)
                .build();
    }

    private AdminAuditLogDto mapToDto(AdminAuditLog logEntry) {
        return AdminAuditLogDto.builder()
                .id(logEntry.getId())
                .adminUserId(logEntry.getAdminUserId())
                .adminName(logEntry.getAdminName())
                .adminEmail(logEntry.getAdminEmail())
                .action(logEntry.getAction())
                .targetType(logEntry.getTargetType())
                .targetId(logEntry.getTargetId())
                .summary(logEntry.getSummary())
                .detailsJson(logEntry.getDetailsJson())
                .ipAddress(logEntry.getIpAddress())
                .correlationId(logEntry.getCorrelationId())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    private String sanitizeAndSerializeDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : details.entrySet()) {
            if (entry.getKey() != null) {
                String lowerKey = entry.getKey().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                if (SENSITIVE_KEYS.contains(lowerKey)) {
                    continue; // Strip sensitive secrets completely
                }
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }

        try {
            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit log details map to JSON: {}", e.getMessage());
            return "{\"warning\":\"Structured details could not be serialized\"}";
        }
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String header = request.getHeader("X-Forwarded-For");
                if (StringUtils.hasText(header)) {
                    return header.split(",")[0].trim();
                }
                header = request.getHeader("X-Real-IP");
                if (StringUtils.hasText(header)) {
                    return header.trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Unable to resolve client IP: {}", e.getMessage());
        }
        return null;
    }

    private String resolveCorrelationId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String corr = request.getHeader("X-Correlation-ID");
                if (StringUtils.hasText(corr)) {
                    return corr.trim();
                }
                String reqId = request.getHeader("X-Request-ID");
                if (StringUtils.hasText(reqId)) {
                    return reqId.trim();
                }
            }
        } catch (Exception e) {
            log.debug("Unable to resolve correlation ID: {}", e.getMessage());
        }
        return null;
    }

    private String truncate(String val, int maxLength) {
        if (val == null) return null;
        String trimmed = val.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
