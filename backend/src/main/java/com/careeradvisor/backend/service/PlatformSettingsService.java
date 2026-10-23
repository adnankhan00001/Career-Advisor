package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.MentorshipCapacityConfigDto;
import com.careeradvisor.backend.dto.PlatformSettingsDto;
import com.careeradvisor.backend.dto.UpdatePlatformSettingsRequest;
import com.careeradvisor.backend.model.PlatformSetting;
import com.careeradvisor.backend.repository.PlatformSettingRepository;
import com.careeradvisor.backend.ai.config.AiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformSettingsService {

    private final MentorshipCapacityConfigService capacityConfigService;
    private final PlatformSettingRepository platformSettingRepository;

    @Autowired(required = false)
    private AiConfigProperties aiConfigProperties;

    public PlatformSettingsService(
            MentorshipCapacityConfigService capacityConfigService,
            PlatformSettingRepository platformSettingRepository) {
        this.capacityConfigService = capacityConfigService;
        this.platformSettingRepository = platformSettingRepository;
    }

    public void setAiConfigProperties(AiConfigProperties aiConfigProperties) {
        this.aiConfigProperties = aiConfigProperties;
    }

    @Transactional(readOnly = true)
    public PlatformSettingsDto getPlatformSettings() {
        MentorshipCapacityConfigDto capacity = capacityConfigService.getConfig();

        List<PlatformSetting> recentSettings = platformSettingRepository.findAllByOrderByUpdatedAtDesc();
        LocalDateTime lastUpdatedAt = null;
        String lastUpdatedBy = null;

        if (!recentSettings.isEmpty()) {
            PlatformSetting latest = recentSettings.get(0);
            lastUpdatedAt = latest.getUpdatedAt();
            lastUpdatedBy = latest.getUpdatedBy();
        }

        return PlatformSettingsDto.builder()
                .mentorshipCapacity(capacity)
                .platformConfig(buildPlatformConfig())
                .securityPolicy(buildSecurityPolicy())
                .systemInfo(buildSystemInfo())
                .lastUpdatedAt(lastUpdatedAt)
                .lastUpdatedBy(lastUpdatedBy)
                .build();
    }

    private Map<String, Object> buildPlatformConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        if (aiConfigProperties != null) {
            config.put("aiEnabled", aiConfigProperties.isEnabled());
            config.put("aiProvider", aiConfigProperties.getProvider());
            config.put("aiModel", aiConfigProperties.getModel());
            config.put("maxTokens", aiConfigProperties.getMaxTokens());
            config.put("temperature", aiConfigProperties.getTemperature());
            config.put("chatMaxMessageLength", aiConfigProperties.getChatMaxMessageLength());
            config.put("chatHistoryLimit", aiConfigProperties.getChatHistoryLimit());
            config.put("chatTitleMaxLength", aiConfigProperties.getChatTitleMaxLength());
            config.put("chatEnabled", aiConfigProperties.isChatEnabled());
        } else {
            config.put("aiEnabled", true);
            config.put("aiProvider", "mock");
            config.put("aiModel", "gemini-1.5-flash");
            config.put("maxTokens", 1000);
            config.put("temperature", 0.7);
            config.put("chatMaxMessageLength", 4000);
            config.put("chatHistoryLimit", 20);
            config.put("chatTitleMaxLength", 80);
            config.put("chatEnabled", true);
        }
        return config;
    }

    private Map<String, Object> buildSecurityPolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("authenticationMethod", "Stateless JWT (HMAC-SHA256)");
        policy.put("tokenExpiration", "24 Hours (86,400 seconds)");
        policy.put("rbacHierarchy", "ADMIN > MENTOR > STUDENT");
        policy.put("adminSafeguard", "Last remaining active administrator cannot be suspended or deactivated");
        policy.put("selfStatusModification", "Restricted (Admins cannot modify own account status)");
        policy.put("auditTrailImmutability", "Strictly Immutable (Append-Only in MySQL)");
        policy.put("credentialSanitization", "Active (Sensitive keys stripped prior to persistence)");
        return policy;
    }

    private Map<String, Object> buildSystemInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("applicationName", "Career Advisor");
        info.put("serviceName", "Backend API Engine");
        info.put("databaseEngine", "MySQL 8.0 (InnoDB)");
        info.put("databaseDialect", "org.hibernate.dialect.MySQLDialect");
        info.put("javaVersion", System.getProperty("java.version", "21"));
        info.put("operatingSystem", System.getProperty("os.name", "Windows"));
        info.put("serverStatus", "OPERATIONAL");
        info.put("mentorshipConstraintInvariant", "1 <= minMentees <= defaultMentees <= maxMentees <= 100");
        return info;
    }

    @Transactional
    public PlatformSettingsDto updatePlatformSettings(
            Long adminUserId,
            String adminEmail,
            UpdatePlatformSettingsRequest request) {
        if (request == null || request.getMentorshipCapacity() == null) {
            throw new IllegalArgumentException("Platform settings update payload must contain mentorshipCapacity");
        }

        capacityConfigService.updateConfig(adminUserId, adminEmail, request.getMentorshipCapacity());

        return getPlatformSettings();
    }
}
