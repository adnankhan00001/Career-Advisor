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
public class PersonalizedStepDto {
    private Long id;
    private String title;
    private String description;
    private String competencyKey;
    private String level;
    private int estimatedMinutes;
    private int sequenceOrder;
    private boolean isCompleted;
    private String state;
    private LocalDateTime completedAt;
    private boolean isTestedOut;
    private String priority;
    private String evidenceStatus;
    private Integer assessmentScore;
}
