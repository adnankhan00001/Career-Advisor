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
public class RecentActivityDto {
    private String id;
    private String activityType; // ASSESSMENT, ROADMAP, INTERVIEW, RESUME, MENTORSHIP, PLANNER
    private String title;
    private String subtitle;
    private LocalDateTime timestamp;
    private String actionUrl;
    private String status;
}
