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
public class ProjectEvidenceDto {
    private Long id;
    private Long milestoneId;
    private String milestoneTitle;
    private Long stepId;
    private String stepTitle;
    private String evidenceType;
    private String url;
    private String notes;
    private String submissionStatus;
    private String reviewFeedback;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
