package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.MentorshipCapacityConfigDto;
import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import com.careeradvisor.backend.model.PlatformSetting;
import com.careeradvisor.backend.repository.PlatformSettingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MentorshipCapacityConfigService {

    private static final Logger logger = LoggerFactory.getLogger(MentorshipCapacityConfigService.class);

    public static final String KEY_MIN_MENTEES = "mentorship.capacity.min_mentees";
    public static final String KEY_MAX_MENTEES = "mentorship.capacity.max_mentees";
    public static final String KEY_DEFAULT_MENTEES = "mentorship.capacity.default_mentees";
    public static final int MAX_PRODUCT_LIMIT = 100;

    private final PlatformSettingRepository platformSettingRepository;
    private final AtomicInteger minMentees = new AtomicInteger(1);
    private final AtomicInteger maxMentees = new AtomicInteger(10);
    private final AtomicInteger defaultMentees = new AtomicInteger(3);

    private final int initialMin;
    private final int initialMax;
    private final int initialDef;

    @Autowired(required = false)
    private AdminAuditLogService auditLogService;

    @Autowired
    public MentorshipCapacityConfigService(
            @Autowired(required = false) PlatformSettingRepository platformSettingRepository,
            @Value("${app.mentorship.capacity.min-mentees:1}") int min,
            @Value("${app.mentorship.capacity.max-mentees:10}") int max,
            @Value("${app.mentorship.capacity.default-mentees:3}") int def) {
        this.platformSettingRepository = platformSettingRepository;
        this.initialMin = Math.max(1, min);
        this.initialMax = Math.max(this.initialMin, max);
        this.initialDef = Math.min(this.initialMax, Math.max(this.initialMin, def));

        this.minMentees.set(this.initialMin);
        this.maxMentees.set(this.initialMax);
        this.defaultMentees.set(this.initialDef);
    }

    public MentorshipCapacityConfigService(int min, int max, int def) {
        this(null, min, max, def);
    }

    public void setAuditLogService(AdminAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    @Transactional
    public void initDefaultsIfAbsent() {
        if (platformSettingRepository == null) {
            return;
        }
        try {
            ensureSettingExists(KEY_MIN_MENTEES, String.valueOf(initialMin), "Minimum mentee capacity per mentor");
            ensureSettingExists(KEY_MAX_MENTEES, String.valueOf(initialMax), "Maximum mentee capacity per mentor");
            ensureSettingExists(KEY_DEFAULT_MENTEES, String.valueOf(initialDef), "Default mentee capacity assigned to approved mentors");
            logger.info("Initialized and verified persistent mentorship capacity settings in MySQL");
        } catch (Exception e) {
            logger.error("Failed to initialize persistent mentorship capacity settings: {}", e.getMessage(), e);
        }
    }

    private void ensureSettingExists(String key, String value, String description) {
        if (!platformSettingRepository.existsBySettingKey(key)) {
            platformSettingRepository.save(PlatformSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .description(description)
                    .updatedBy("system")
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public int getMinMentees() {
        if (platformSettingRepository != null) {
            return platformSettingRepository.findBySettingKey(KEY_MIN_MENTEES)
                    .map(s -> parseIntegerSafely(s.getSettingValue(), initialMin))
                    .orElse(initialMin);
        }
        return minMentees.get();
    }

    @Transactional(readOnly = true)
    public int getMaxMentees() {
        if (platformSettingRepository != null) {
            return platformSettingRepository.findBySettingKey(KEY_MAX_MENTEES)
                    .map(s -> parseIntegerSafely(s.getSettingValue(), initialMax))
                    .orElse(initialMax);
        }
        return maxMentees.get();
    }

    @Transactional(readOnly = true)
    public int getDefaultMentees() {
        if (platformSettingRepository != null) {
            return platformSettingRepository.findBySettingKey(KEY_DEFAULT_MENTEES)
                    .map(s -> parseIntegerSafely(s.getSettingValue(), initialDef))
                    .orElse(initialDef);
        }
        return defaultMentees.get();
    }

    @Transactional(readOnly = true)
    public MentorshipCapacityConfigDto getConfig() {
        return MentorshipCapacityConfigDto.builder()
                .minMentees(getMinMentees())
                .maxMentees(getMaxMentees())
                .defaultMentees(getDefaultMentees())
                .build();
    }

    @Transactional
    public synchronized MentorshipCapacityConfigDto updateConfig(Long adminUserId, String adminEmail, MentorshipCapacityConfigDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Capacity configuration must not be null");
        }
        if (dto.getMinMentees() == null || dto.getMinMentees() < 1) {
            throw new IllegalArgumentException("Minimum mentees must be at least 1");
        }
        if (dto.getMaxMentees() == null || dto.getMaxMentees() < dto.getMinMentees()) {
            throw new IllegalArgumentException("Maximum mentees cannot be less than minimum mentees (" + dto.getMinMentees() + ")");
        }
        if (dto.getMaxMentees() > MAX_PRODUCT_LIMIT) {
            throw new IllegalArgumentException("Maximum mentees cannot exceed product limit of " + MAX_PRODUCT_LIMIT);
        }

        int defaultVal = dto.getDefaultMentees() != null ? dto.getDefaultMentees() : dto.getMinMentees();
        if (defaultVal < dto.getMinMentees() || defaultVal > dto.getMaxMentees()) {
            throw new IllegalArgumentException("Default mentees must be between " + dto.getMinMentees() + " and " + dto.getMaxMentees());
        }

        int oldMin = getMinMentees();
        int oldMax = getMaxMentees();
        int oldDef = getDefaultMentees();

        String updater = adminEmail != null && !adminEmail.isBlank() ? adminEmail :
                (adminUserId != null ? "admin#" + adminUserId : "admin");

        if (platformSettingRepository != null) {
            saveOrUpdateSetting(KEY_MIN_MENTEES, String.valueOf(dto.getMinMentees()), "Minimum mentee capacity per mentor", updater);
            saveOrUpdateSetting(KEY_MAX_MENTEES, String.valueOf(dto.getMaxMentees()), "Maximum mentee capacity per mentor", updater);
            saveOrUpdateSetting(KEY_DEFAULT_MENTEES, String.valueOf(defaultVal), "Default mentee capacity assigned to approved mentors", updater);
        }

        minMentees.set(dto.getMinMentees());
        maxMentees.set(dto.getMaxMentees());
        defaultMentees.set(defaultVal);

        if (auditLogService != null) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("previousMin", oldMin);
            details.put("previousMax", oldMax);
            details.put("previousDefault", oldDef);
            details.put("newMin", dto.getMinMentees());
            details.put("newMax", dto.getMaxMentees());
            details.put("newDefault", defaultVal);

            auditLogService.recordAction(
                    adminUserId,
                    adminEmail,
                    null,
                    AdminAuditAction.PLATFORM_SETTING_UPDATED,
                    AdminAuditTargetType.PLATFORM_SETTING,
                    "mentorship_capacity",
                    "Updated mentorship capacity configuration: min=" + dto.getMinMentees() + ", max=" + dto.getMaxMentees() + ", default=" + defaultVal,
                    details
            );
        }

        return getConfig();
    }

    public MentorshipCapacityConfigDto updateConfig(Long adminUserId, MentorshipCapacityConfigDto dto) {
        return updateConfig(adminUserId, null, dto);
    }

    public MentorshipCapacityConfigDto updateConfig(MentorshipCapacityConfigDto dto) {
        return updateConfig(null, null, dto);
    }

    private void saveOrUpdateSetting(String key, String value, String description, String updater) {
        PlatformSetting setting = platformSettingRepository.findBySettingKey(key)
                .orElseGet(() -> PlatformSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());
        setting.setSettingValue(value);
        setting.setDescription(description);
        setting.setUpdatedBy(updater);
        setting.setUpdatedAt(LocalDateTime.now());
        platformSettingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public int validateAndResolveCapacity(Integer requestedCapacity) {
        int min = getMinMentees();
        int max = getMaxMentees();
        int def = getDefaultMentees();

        if (requestedCapacity == null) {
            return def;
        }
        if (requestedCapacity < min || requestedCapacity > max) {
            throw new IllegalArgumentException("Mentee capacity must be between " + min + " and " + max + " mentees");
        }
        return requestedCapacity;
    }

    @Transactional(readOnly = true)
    public Optional<PlatformSetting> getLatestSetting() {
        if (platformSettingRepository == null) {
            return Optional.empty();
        }
        return platformSettingRepository.findBySettingKey(KEY_MAX_MENTEES);
    }

    private int parseIntegerSafely(String val, int fallback) {
        if (val == null || val.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            logger.warn("Corrupt capacity setting value '{}', falling back to {}", val, fallback);
            return fallback;
        }
    }
}
