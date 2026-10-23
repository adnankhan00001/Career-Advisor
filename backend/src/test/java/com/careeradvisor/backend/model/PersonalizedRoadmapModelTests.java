package com.careeradvisor.backend.model;

import com.careeradvisor.backend.dto.PersonalizedMilestoneDto;
import com.careeradvisor.backend.dto.PersonalizedRoadmapDto;
import com.careeradvisor.backend.dto.PersonalizedStepDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonalizedRoadmapModelTests {

    @Test
    @DisplayName("PersonalizedRoadmap entity defaults and milestone cascading")
    void testPersonalizedRoadmapDefaults() {
        User user = User.builder().id(10L).email("student@test.com").name("Alex").build();

        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .user(user)
                .careerTitle("Java Backend Developer")
                .careerSlug("java-backend-developer")
                .dailyStudyMinutes(60)
                .milestones(new ArrayList<>())
                .build();

        assertEquals("ACTIVE", roadmap.getStatus());
        assertEquals(0, roadmap.getCompletedStepsCount());
        assertEquals(0, roadmap.getProgressPercentage());
        assertNotNull(roadmap.getCreatedAt());
        assertNotNull(roadmap.getUpdatedAt());

        PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                .title("Milestone 1: Java Foundations")
                .technologyName("Java")
                .sequenceOrder(1)
                .steps(new ArrayList<>())
                .build();

        roadmap.addMilestone(milestone);
        assertEquals(roadmap, milestone.getRoadmap());
        assertEquals(1, roadmap.getMilestones().size());

        PersonalizedRoadmapStep step = PersonalizedRoadmapStep.builder()
                .title("Java Syntax")
                .sequenceOrder(1)
                .build();

        milestone.addStep(step);
        assertEquals(milestone, step.getMilestone());
        assertEquals(1, milestone.getSteps().size());
        assertFalse(step.isCompleted());
        assertFalse(step.isTestedOut());
        assertEquals("CORE", step.getPriority());
        assertEquals("NOT_ASSESSED", step.getEvidenceStatus());
    }

    @Test
    @DisplayName("PersonalizedRoadmap DTO mapping and metrics calculation")
    void testDtoMapping() {
        PersonalizedStepDto stepDto = PersonalizedStepDto.builder()
                .id(1L)
                .title("Java OOP")
                .isCompleted(true)
                .isTestedOut(true)
                .priority("TESTED_OUT")
                .evidenceStatus("ASSESSED")
                .estimatedMinutes(60)
                .assessmentScore(85)
                .build();

        PersonalizedMilestoneDto milestoneDto = PersonalizedMilestoneDto.builder()
                .id(10L)
                .title("Java Milestone")
                .targetWeekNumber(1)
                .estimatedHours(1.0)
                .status("COMPLETED")
                .steps(List.of(stepDto))
                .build();

        PersonalizedRoadmapDto roadmapDto = PersonalizedRoadmapDto.builder()
                .id(100L)
                .careerTitle("Java Backend Developer")
                .totalEstimatedHours(40.0)
                .completedStepsCount(1)
                .totalStepsCount(10)
                .progressPercentage(10)
                .testedOutCount(1)
                .focusAreasCount(2)
                .targetCompletionDate(LocalDate.now().plusMonths(3))
                .isTargetDateRealistic(true)
                .projectedCompletionDate(LocalDate.now().plusMonths(2))
                .milestones(List.of(milestoneDto))
                .build();

        assertEquals(100L, roadmapDto.getId());
        assertEquals(1, roadmapDto.getTestedOutCount());
        assertEquals(2, roadmapDto.getFocusAreasCount());
        assertEquals(10, roadmapDto.getProgressPercentage());
        assertTrue(roadmapDto.getIsTargetDateRealistic());
        assertNotNull(roadmapDto.getProjectedCompletionDate());
        assertEquals(1, roadmapDto.getMilestones().size());
        assertEquals(1, roadmapDto.getMilestones().get(0).getSteps().size());
        assertTrue(roadmapDto.getMilestones().get(0).getSteps().get(0).isTestedOut());
        assertEquals("ASSESSED", roadmapDto.getMilestones().get(0).getSteps().get(0).getEvidenceStatus());
    }
}
