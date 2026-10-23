package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsDto {

    private MentorshipCapacityConfigDto mentorshipCapacity;
    private Map<String, Object> platformConfig;
    private Map<String, Object> securityPolicy;
    private Map<String, Object> systemInfo;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;
}
