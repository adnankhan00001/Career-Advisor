package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsDto {

    private MentorshipCapacityConfigDto mentorshipCapacity;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;
}
