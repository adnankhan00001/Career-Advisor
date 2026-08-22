package com.careeradvisor.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAiSummaryDto {
    private Long resumeId;
    private String fileName;
    private LocalDateTime uploadTimestamp;
    private String parsingStatus;

    @Builder.Default
    private List<String> extractedSkills = new ArrayList<>();

    @Builder.Default
    private List<String> topCareerMatches = new ArrayList<>();

    @Builder.Default
    private List<String> identifiedSkillGaps = new ArrayList<>();

    private String executiveSummarySnippet;
}
