package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerGapDto {
    private String skill;
    private GapPriority priority;
    private String source;
    private List<String> evidence;
    private String recommendedAction;
    private String relatedRoadmapTopic;
}
