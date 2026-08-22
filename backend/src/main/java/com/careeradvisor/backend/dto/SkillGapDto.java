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
public class SkillGapDto {
    private String targetCareer;
    private int matchPercentage;
    private List<String> acquiredSkills;
    private List<String> missingSkills;
    private List<String> highPriorityMissing;
}
