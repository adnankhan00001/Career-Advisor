package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.CareerActionDto;
import com.careeradvisor.backend.dto.CareerGapDto;
import com.careeradvisor.backend.dto.CareerIntelligenceDto;
import com.careeradvisor.backend.dto.GapPriority;
import com.careeradvisor.backend.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Component
public class DeterministicPlannerEngine {

    private static final Logger logger = LoggerFactory.getLogger(DeterministicPlannerEngine.class);

    public List<LearningTask> generateWeeklyTasks(LearningPlan plan,
                                                   CareerIntelligenceDto intel,
                                                   PlannerPreference preference) {
        List<LearningTask> tasks = new ArrayList<>();
        LocalDate start = plan.getWeekStartDate();
        int dailyBudget = preference.getDailyStudyMinutes() != null ? preference.getDailyStudyMinutes() : 60;
        Set<DayOfWeek> activeDays = parsePreferredDays(preference.getPreferredDays());

        // Pool of potential task candidates prioritized by impact
        List<TaskCandidate> candidates = assembleTaskCandidates(intel, plan.getTargetCareer());

        // Distribute candidates across the 7 days
        int candidateIdx = 0;
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = start.plusDays(dayOffset);
            boolean isPreferred = activeDays.contains(date.getDayOfWeek());

            if (!isPreferred && !activeDays.isEmpty()) {
                // If not a preferred day, schedule light 15-20 min review only if candidates remain
                if (candidateIdx < candidates.size() && dailyBudget >= 45) {
                    TaskCandidate c = candidates.get(candidateIdx % candidates.size());
                    tasks.add(buildTask(plan, date, "Quick Review: " + c.title, c.description,
                            LearningTaskCategory.REVIEW, GapPriority.LOW, 15,
                            "Light consolidation on off-days keeps momentum active.",
                            "Retain core concepts without mental fatigue.", c.actionUrl, 1));
                }
                continue;
            }

            int dayMinutesAllocated = 0;
            int sequence = 1;

            while (dayMinutesAllocated < dailyBudget && candidateIdx < candidates.size()) {
                TaskCandidate c = candidates.get(candidateIdx);
                int duration = Math.min(c.durationMinutes, dailyBudget - dayMinutesAllocated + 15);
                if (duration < 15) duration = 15;

                tasks.add(buildTask(plan, date, c.title, c.description, c.category, c.priority,
                        duration, c.whyItMatters, c.expectedOutcome, c.actionUrl, sequence++));

                dayMinutesAllocated += duration;
                candidateIdx++;
            }

            // If day is still under-allocated, add a supplemental practice/revision task
            if (dayMinutesAllocated < dailyBudget) {
                int remaining = Math.max(15, dailyBudget - dayMinutesAllocated);
                tasks.add(buildTask(plan, date, "Target Track Deep Dive & Notes",
                        "Review key design decisions and syntax nuances for " + plan.getTargetCareer(),
                        LearningTaskCategory.REVIEW, GapPriority.MEDIUM, remaining,
                        "Reinforces retention of complex backend concepts.",
                        "Solid conceptual foundations for interview rounds.", "/roadmap", sequence++));
            }
        }

        return tasks;
    }

    private List<TaskCandidate> assembleTaskCandidates(CareerIntelligenceDto intel, String targetCareer) {
        List<TaskCandidate> pool = new ArrayList<>();

        // 1. Critical and High Skill Gaps
        if (intel.getSkillGaps() != null) {
            for (CareerGapDto gap : intel.getSkillGaps()) {
                String actionUrl = gap.getRelatedRoadmapTopic() != null ? "/roadmap" : "/skills";
                pool.add(new TaskCandidate(
                        "Master Core Competency: " + gap.getSkill(),
                        "Targeted learning and milestone completion for " + gap.getSkill() + " (" + gap.getPriority() + " priority).",
                        LearningTaskCategory.SKILL,
                        gap.getPriority(),
                        35,
                        String.join("; ", gap.getEvidence()),
                        "Close verified competency gap in " + gap.getSkill(),
                        actionUrl
                ));
            }
        }

        // 2. High-ROI Prioritized Actions from Intelligence Engine
        if (intel.getPrioritizedActions() != null) {
            for (CareerActionDto act : intel.getPrioritizedActions()) {
                LearningTaskCategory cat = mapCategory(act.getCategory().name());
                pool.add(new TaskCandidate(
                        act.getTitle(),
                        act.getDescription(),
                        cat,
                        act.getPriority(),
                        40,
                        act.getReasoning(),
                        "Directly improves your career readiness score (+ " + act.getEstimatedImpactPoints() + " pts).",
                        act.getActionUrl()
                ));
            }
        }

        // 3. Roadmap Milestones
        pool.add(new TaskCandidate(
                "Execute Next Roadmap Milestone",
                "Complete the next sequential learning checkpoint for " + targetCareer,
                LearningTaskCategory.ROADMAP,
                GapPriority.HIGH,
                45,
                "Ensures systematic end-to-end curriculum progression.",
                "Advance overall roadmap completion percentage.",
                "/roadmap"
        ));

        // 4. DSA Algorithmic Problem Solving
        pool.add(new TaskCandidate(
                "Solve 2 Curated DSA Problems",
                "Practice problem solving in the in-browser workspace (Arrays, Strings, HashMaps).",
                LearningTaskCategory.DSA,
                GapPriority.HIGH,
                45,
                "Crucial for passing live coding and technical screening rounds.",
                "Clean, optimal O(N) algorithmic implementation.",
                "/practice"
        ));

        // 5. Technical Mock Interview
        pool.add(new TaskCandidate(
                "Simulate Timed Technical Mock Interview",
                "Complete a 5-question timed mock interview session.",
                LearningTaskCategory.MOCK_INTERVIEW,
                GapPriority.MEDIUM,
                30,
                "Tests technical articulation under real exam constraints.",
                "Receive detailed feedback on strong and weak areas.",
                "/mock-interview"
        ));

        // 6. Diagnostic Assessment / Resume Check
        pool.add(new TaskCandidate(
                "Career Readiness & Resume Sync",
                "Review parsed skills and update verified portfolio competencies.",
                LearningTaskCategory.RESUME,
                GapPriority.MEDIUM,
                25,
                "Ensures platform intelligence aligns with your latest achievements.",
                "Accurate, up-to-date career readiness tracking.",
                "/resume"
        ));

        return pool;
    }

    private LearningTask buildTask(LearningPlan plan, LocalDate date, String title, String description,
                                   LearningTaskCategory category, GapPriority priority, int minutes,
                                   String why, String outcome, String url, int seq) {
        return LearningTask.builder()
                .learningPlan(plan)
                .scheduledDate(date)
                .title(title)
                .description(description)
                .category(category)
                .priority(priority)
                .estimatedMinutes(minutes)
                .whyItMatters(why)
                .expectedOutcome(outcome)
                .actionUrl(url)
                .status(LearningTaskStatus.PENDING)
                .sequenceNumber(seq)
                .build();
    }

    private Set<DayOfWeek> parsePreferredDays(String preferredDaysStr) {
        Set<DayOfWeek> days = new HashSet<>();
        if (preferredDaysStr == null || preferredDaysStr.trim().isEmpty()) {
            days.addAll(Arrays.asList(DayOfWeek.values()));
            return days;
        }
        for (String part : preferredDaysStr.split(",")) {
            try {
                days.add(DayOfWeek.valueOf(part.trim().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (days.isEmpty()) {
            days.addAll(Arrays.asList(DayOfWeek.values()));
        }
        return days;
    }

    private LearningTaskCategory mapCategory(String cat) {
        try {
            return LearningTaskCategory.valueOf(cat.toUpperCase());
        } catch (Exception e) {
            return LearningTaskCategory.ROADMAP;
        }
    }

    private static class TaskCandidate {
        final String title;
        final String description;
        final LearningTaskCategory category;
        final GapPriority priority;
        final int durationMinutes;
        final String whyItMatters;
        final String expectedOutcome;
        final String actionUrl;

        TaskCandidate(String title, String description, LearningTaskCategory category,
                      GapPriority priority, int durationMinutes, String whyItMatters,
                      String expectedOutcome, String actionUrl) {
            this.title = title;
            this.description = description;
            this.category = category;
            this.priority = priority;
            this.durationMinutes = durationMinutes;
            this.whyItMatters = whyItMatters;
            this.expectedOutcome = expectedOutcome;
            this.actionUrl = actionUrl;
        }
    }
}
