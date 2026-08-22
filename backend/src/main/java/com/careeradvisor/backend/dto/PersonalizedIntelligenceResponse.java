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
public class PersonalizedIntelligenceResponse {
    private String userState; // ONBOARDING, BEGINNER, INTERMEDIATE, ADVANCED
    private String summaryHeadline;
    private String careerGoal;
    private int overallReadinessScore; // 0 - 100%
    private List<CareerMatchDto> careerMatches;
    private SkillGapDto skillGaps;
    private RoadmapActionDto nextRoadmapAction;
    private PracticeActionDto practiceAction;
    private InterviewFocusDto interviewFocusAction;
    private List<RecommendationItemDto> actionPlan;
}
