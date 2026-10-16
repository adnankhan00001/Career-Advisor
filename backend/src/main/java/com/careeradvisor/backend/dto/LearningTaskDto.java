package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.LearningTaskCategory;
import com.careeradvisor.backend.model.LearningTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningTaskDto {
    private Long id;
    private Long planId;
    private String title;
    private String description;
    private LearningTaskCategory category;
    private GapPriority priority;
    private LocalDate scheduledDate;
    private Integer estimatedMinutes;
    private String whyItMatters;
    private String expectedOutcome;
    private String actionUrl;
    private LearningTaskStatus status;
    private Integer sequenceNumber;
    private LocalDateTime completedAt;
}
