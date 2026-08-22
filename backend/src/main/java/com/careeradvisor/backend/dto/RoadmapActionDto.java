package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapActionDto {
    private String careerTitle;
    private String nextTopic;
    private String sectionTitle;
    private int completedTopics;
    private int totalTopics;
    private int progressPercentage;
    private String reason;
}
