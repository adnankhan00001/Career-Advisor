package com.careeradvisor.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewAiSummaryDto {
    private int totalSessions;
    private int completedSessions;
    private int averageScore;
    private int bestScore;

    @Builder.Default
    private List<String> strongAreas = new ArrayList<>();

    @Builder.Default
    private List<String> weakAreas = new ArrayList<>();

    private Integer latestScore;
    private String latestCategory;
}
