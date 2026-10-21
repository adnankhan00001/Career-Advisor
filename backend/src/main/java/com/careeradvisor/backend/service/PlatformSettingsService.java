package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.MentorshipCapacityConfigDto;
import com.careeradvisor.backend.dto.PlatformSettingsDto;
import com.careeradvisor.backend.dto.UpdatePlatformSettingsRequest;
import com.careeradvisor.backend.model.PlatformSetting;
import com.careeradvisor.backend.repository.PlatformSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlatformSettingsService {

    private final MentorshipCapacityConfigService capacityConfigService;
    private final PlatformSettingRepository platformSettingRepository;

    public PlatformSettingsService(
            MentorshipCapacityConfigService capacityConfigService,
            PlatformSettingRepository platformSettingRepository) {
        this.capacityConfigService = capacityConfigService;
        this.platformSettingRepository = platformSettingRepository;
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
                .lastUpdatedAt(lastUpdatedAt)
                .lastUpdatedBy(lastUpdatedBy)
                .build();
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
