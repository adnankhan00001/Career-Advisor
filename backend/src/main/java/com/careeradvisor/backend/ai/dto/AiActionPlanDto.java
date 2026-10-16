package com.careeradvisor.backend.ai.dto;

import com.careeradvisor.backend.dto.GapPriority;
import com.careeradvisor.backend.dto.ReadinessState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiActionPlanDto {
    private Long userId;
    private String targetCareer;
    private String title;
    private String summary;
    private ReadinessState currentState;
    private String currentStateDisplayName;
    private int overallReadinessScore;
    private GapPriority primaryPriority;

    @Builder.Default
    private List<AiActionPlanItemDto> actions = new ArrayList<>();

    private String estimatedTimeframe;
    private String motivationalNote;
    private String source; // "AI_GENERATED" or "DETERMINISTIC_FALLBACK"
    private String provider;
    private String model;
    private long latencyMs;

    @Builder.Default
    private Instant generatedAt = Instant.now();
}
