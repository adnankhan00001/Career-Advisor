package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.ResumeStatus;
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
public class ResumeAnalysisResponse {

    private Long resumeId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private ResumeStatus parsingStatus;
    private LocalDateTime uploadTimestamp;

    private String summary;
    private String extractedEmail;
    private String extractedPhone;

    @Builder.Default
    private List<ExtractedSkillDto> extractedSkills = new ArrayList<>();

    @Builder.Default
    private List<String> extractedEducation = new ArrayList<>();

    @Builder.Default
    private List<String> extractedExperience = new ArrayList<>();

    @Builder.Default
    private List<String> extractedProjects = new ArrayList<>();

    @Builder.Default
    private List<CareerMatchDto> matchedCareers = new ArrayList<>();

    private SkillGapDto skillGaps;

    @Builder.Default
    private List<RecommendationItemDto> recommendations = new ArrayList<>();
}
