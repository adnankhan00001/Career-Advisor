package com.careeradvisor.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalAiContextDto {

    private UserSummaryDto userProfile;
    private String targetCareerGoal;

    @Builder.Default
    private List<String> verifiedSkills = new ArrayList<>();

    private ResumeAiSummaryDto resumeSummary;
    private QuizAiSummaryDto quizAssessment;
    private RoadmapAiProgressDto roadmapProgress;
    private DsaAiProgressDto dsaProgress;
    private MockInterviewAiSummaryDto mockInterviewPerformance;
    private RecommendationAiSummaryDto recommendations;

    @Builder.Default
    private Instant contextTimestamp = Instant.now();
}
