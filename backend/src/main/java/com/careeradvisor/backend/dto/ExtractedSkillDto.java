package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedSkillDto {
    private String skillName;
    private String category;
    private int confidence;
    private boolean alreadyInProfile;
}
