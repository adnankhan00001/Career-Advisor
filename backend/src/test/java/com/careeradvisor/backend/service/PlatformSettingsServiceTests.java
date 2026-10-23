package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.MentorshipCapacityConfigDto;
import com.careeradvisor.backend.dto.PlatformSettingsDto;
import com.careeradvisor.backend.dto.UpdatePlatformSettingsRequest;
import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import com.careeradvisor.backend.model.PlatformSetting;
import com.careeradvisor.backend.repository.PlatformSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformSettingsServiceTests {

    @Mock
    private PlatformSettingRepository platformSettingRepository;

    @Mock
    private AdminAuditLogService auditLogService;

    private MentorshipCapacityConfigService capacityConfigService;
    private PlatformSettingsService platformSettingsService;

    @BeforeEach
    void setUp() {
        capacityConfigService = new MentorshipCapacityConfigService(platformSettingRepository, 1, 10, 3);
        capacityConfigService.setAuditLogService(auditLogService);
        platformSettingsService = new PlatformSettingsService(capacityConfigService, platformSettingRepository);
    }

    @Test
    @DisplayName("getPlatformSettings returns persistent values and latest metadata")
    void testGetPlatformSettings() {
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_MIN_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingValue("2").build()));
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_MAX_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingValue("12").build()));
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_DEFAULT_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingValue("4").build()));

        LocalDateTime now = LocalDateTime.now();
        PlatformSetting latestSetting = PlatformSetting.builder()
                .settingKey(MentorshipCapacityConfigService.KEY_MAX_MENTEES)
                .settingValue("12")
                .updatedBy("admin@careeradvisor.dev")
                .updatedAt(now)
                .build();
        when(platformSettingRepository.findAllByOrderByUpdatedAtDesc())
                .thenReturn(List.of(latestSetting));

        PlatformSettingsDto settings = platformSettingsService.getPlatformSettings();

        assertNotNull(settings);
        assertEquals(2, settings.getMentorshipCapacity().getMinMentees());
        assertEquals(12, settings.getMentorshipCapacity().getMaxMentees());
        assertEquals(4, settings.getMentorshipCapacity().getDefaultMentees());
        assertEquals("admin@careeradvisor.dev", settings.getLastUpdatedBy());
        assertEquals(now, settings.getLastUpdatedAt());
        assertNotNull(settings.getPlatformConfig());
        assertEquals("gemini-1.5-flash", settings.getPlatformConfig().get("aiModel"));
        assertNotNull(settings.getSecurityPolicy());
        assertEquals("Stateless JWT (HMAC-SHA256)", settings.getSecurityPolicy().get("authenticationMethod"));
        assertNotNull(settings.getSystemInfo());
        assertEquals("Career Advisor", settings.getSystemInfo().get("applicationName"));
    }

    @Test
    @DisplayName("updatePlatformSettings persists valid updates and triggers audit logging")
    void testUpdatePlatformSettingsValid() {
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_MIN_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingKey(MentorshipCapacityConfigService.KEY_MIN_MENTEES).settingValue("1").build()));
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_MAX_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingKey(MentorshipCapacityConfigService.KEY_MAX_MENTEES).settingValue("10").build()));
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_DEFAULT_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingKey(MentorshipCapacityConfigService.KEY_DEFAULT_MENTEES).settingValue("3").build()));

        MentorshipCapacityConfigDto newCapacity = MentorshipCapacityConfigDto.builder()
                .minMentees(2)
                .maxMentees(15)
                .defaultMentees(5)
                .build();
        UpdatePlatformSettingsRequest request = new UpdatePlatformSettingsRequest(newCapacity);

        PlatformSettingsDto updated = platformSettingsService.updatePlatformSettings(1L, "superadmin@careeradvisor.dev", request);

        assertNotNull(updated);
        // Verify repository saved settings
        verify(platformSettingRepository, atLeastOnce()).save(any(PlatformSetting.class));

        // Verify audit log recorded PLATFORM_SETTING_UPDATED
        ArgumentCaptor<AdminAuditAction> actionCaptor = ArgumentCaptor.forClass(AdminAuditAction.class);
        ArgumentCaptor<AdminAuditTargetType> targetTypeCaptor = ArgumentCaptor.forClass(AdminAuditTargetType.class);
        verify(auditLogService).recordAction(
                eq(1L),
                eq("superadmin@careeradvisor.dev"),
                isNull(),
                actionCaptor.capture(),
                targetTypeCaptor.capture(),
                eq("mentorship_capacity"),
                anyString(),
                anyMap()
        );
        assertEquals(AdminAuditAction.PLATFORM_SETTING_UPDATED, actionCaptor.getValue());
        assertEquals(AdminAuditTargetType.PLATFORM_SETTING, targetTypeCaptor.getValue());
    }

    @Test
    @DisplayName("updatePlatformSettings rejects min greater than max")
    void testUpdatePlatformSettingsMinGreaterThanMax() {
        MentorshipCapacityConfigDto invalidCapacity = MentorshipCapacityConfigDto.builder()
                .minMentees(10)
                .maxMentees(5)
                .defaultMentees(7)
                .build();
        UpdatePlatformSettingsRequest request = new UpdatePlatformSettingsRequest(invalidCapacity);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platformSettingsService.updatePlatformSettings(1L, "admin@test.com", request)
        );
        assertTrue(ex.getMessage().contains("cannot be less than minimum mentees"));
        verify(platformSettingRepository, never()).save(any(PlatformSetting.class));
    }

    @Test
    @DisplayName("updatePlatformSettings rejects default outside range")
    void testUpdatePlatformSettingsDefaultOutOfRange() {
        MentorshipCapacityConfigDto invalidCapacity = MentorshipCapacityConfigDto.builder()
                .minMentees(2)
                .maxMentees(10)
                .defaultMentees(12)
                .build();
        UpdatePlatformSettingsRequest request = new UpdatePlatformSettingsRequest(invalidCapacity);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platformSettingsService.updatePlatformSettings(1L, "admin@test.com", request)
        );
        assertTrue(ex.getMessage().contains("Default mentees must be between"));
        verify(platformSettingRepository, never()).save(any(PlatformSetting.class));
    }

    @Test
    @DisplayName("updatePlatformSettings rejects max capacity exceeding product safety limit")
    void testUpdatePlatformSettingsExceedsProductLimit() {
        MentorshipCapacityConfigDto invalidCapacity = MentorshipCapacityConfigDto.builder()
                .minMentees(1)
                .maxMentees(150) // Product limit is 100
                .defaultMentees(5)
                .build();
        UpdatePlatformSettingsRequest request = new UpdatePlatformSettingsRequest(invalidCapacity);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platformSettingsService.updatePlatformSettings(1L, "admin@test.com", request)
        );
        assertTrue(ex.getMessage().contains("cannot exceed product limit"));
        verify(platformSettingRepository, never()).save(any(PlatformSetting.class));
    }

    @Test
    @DisplayName("validateAndResolveCapacity enforces current persistent boundaries")
    void testValidateAndResolveCapacity() {
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_MIN_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingValue("2").build()));
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_MAX_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingValue("8").build()));
        when(platformSettingRepository.findBySettingKey(MentorshipCapacityConfigService.KEY_DEFAULT_MENTEES))
                .thenReturn(Optional.of(PlatformSetting.builder().settingValue("4").build()));

        // Null requested capacity resolves to default (4)
        assertEquals(4, capacityConfigService.validateAndResolveCapacity(null));

        // Within range is accepted
        assertEquals(5, capacityConfigService.validateAndResolveCapacity(5));

        // Below min throws
        assertThrows(IllegalArgumentException.class, () -> capacityConfigService.validateAndResolveCapacity(1));

        // Above max throws
        assertThrows(IllegalArgumentException.class, () -> capacityConfigService.validateAndResolveCapacity(9));
    }
}
