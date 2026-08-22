package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.ProblemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingProblemDto {
    private Long id;
    private String slug;
    private String title;
    private String description;
    private Difficulty difficulty;
    private ProblemCategory category;
    private String topic;
    private String externalUrl;
    private List<String> tags;
    private String acceptanceRate;
    private Integer orderIndex;
    private String starterCode;
    private String sampleInput;
    private String sampleOutput;
    private String constraints;
    private String explanation;
    private boolean solved;
    private LocalDateTime solvedAt;
}
