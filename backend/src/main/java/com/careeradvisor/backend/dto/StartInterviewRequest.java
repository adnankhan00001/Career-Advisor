package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.ProblemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartInterviewRequest {
    private ProblemCategory category;
    private Difficulty difficulty;
    private Integer durationMinutes;
    private Integer questionCount;
}
