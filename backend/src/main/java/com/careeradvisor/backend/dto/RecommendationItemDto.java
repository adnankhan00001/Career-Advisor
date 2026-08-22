package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItemDto {
    private String id;
    private String type; // CAREER, SKILL_GAP, ROADMAP, PRACTICE, INTERVIEW, ONBOARDING
    private String priority; // HIGH, MEDIUM, LOW
    private String title;
    private String description;
    private String reason;
    private String actionText;
    private String actionUrl;
    private Integer score;
}
