package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.MockInterviewRepository;
import com.careeradvisor.backend.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeterministicProgressEngine {

    private final CareerIntelligenceService careerIntelligenceService;
    private final UserProgressService userProgressService;
    private final ProblemService problemService;
    private final MockInterviewRepository interviewRepository;
    private final PlannerService plannerService;
    private final ResumeRepository resumeRepository;
    private final QuizService quizService;

    public DeterministicProgressEngine(CareerIntelligenceService careerIntelligenceService,
                                       UserProgressService userProgressService,
                                       ProblemService problemService,
                                       MockInterviewRepository interviewRepository,
                                       PlannerService plannerService,
                                       ResumeRepository resumeRepository,
                                       QuizService quizService) {
        this.careerIntelligenceService = careerIntelligenceService;
        this.userProgressService = userProgressService;
        this.problemService = problemService;
        this.interviewRepository = interviewRepository;
        this.plannerService = plannerService;
        this.resumeRepository = resumeRepository;
        this.quizService = quizService;
    }

    public WeeklyProgressReportDto generateDeterministicReport(User user) {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);

        // 1. Career Intelligence
        int careerReadinessScore = 0;
        String targetCareer = user.getCareerGoal() != null ? user.getCareerGoal() : "Software Engineer";
        List<CareerGapDto> skillGaps = new ArrayList<>();
        List<CareerActionDto> prioritizedActions = new ArrayList<>();
        try {
            CareerIntelligenceDto intel = careerIntelligenceService.calculateCareerIntelligence(user);
            if (intel != null) {
                careerReadinessScore = intel.getOverallReadinessScore();
                if (intel.getTargetCareer() != null) targetCareer = intel.getTargetCareer();
                if (intel.getSkillGaps() != null) skillGaps = intel.getSkillGaps();
                if (intel.getPrioritizedActions() != null) prioritizedActions = intel.getPrioritizedActions();
            }
        } catch (Exception ignored) {}

        // 2. Roadmap
        int roadmapProgress = 0;
        int completedSteps = 0;
        int totalSteps = 0;
        String nextRoadmapStep = "Continue next roadmap checkpoint";
        try {
            ProgressSummaryResponse rm = userProgressService.getProgressSummary(user);
            if (rm != null) {
                roadmapProgress = rm.getRoadmapPercent();
                completedSteps = rm.getCompletedStepsCount();
                totalSteps = rm.getTotalStepsCount();
                if (rm.getNextTopicToLearn() != null && !rm.getNextTopicToLearn().isEmpty()) {
                    nextRoadmapStep = rm.getNextTopicToLearn();
                }
            }
        } catch (Exception ignored) {}

        // 3. DSA
        int dsaSolved = 0;
        int dsaTotal = 0;
        int dsaPercent = 0;
        String nextProblem = "Solve Arrays & Two-Pointer challenge";
        try {
            ProblemProgressSummaryDto dsa = problemService.getProgressSummary(user);
            if (dsa != null) {
                dsaSolved = dsa.getSolvedProblems();
                dsaTotal = dsa.getTotalProblems();
                dsaPercent = dsa.getCompletionPercentage();
                if (dsa.getNextRecommendedProblem() != null && !dsa.getNextRecommendedProblem().isEmpty()) {
                    nextProblem = dsa.getNextRecommendedProblem();
                }
            }
        } catch (Exception ignored) {}

        // 4. Mock Interviews
        int interviewAvg = 0;
        int interviewCount = 0;
        int latestScore = 0;
        int previousScore = 0;
        try {
            List<MockInterview> allInterviews = interviewRepository.findByUserOrderByStartedAtDesc(user);
            List<MockInterview> completed = allInterviews.stream()
                    .filter(i -> i.getStatus() == InterviewStatus.COMPLETED)
                    .collect(Collectors.toList());
            interviewCount = completed.size();
            if (interviewCount > 0) {
                int totalScore = completed.stream().mapToInt(MockInterview::getScore).sum();
                interviewAvg = totalScore / interviewCount;
                latestScore = completed.get(0).getScore();
                if (interviewCount > 1) {
                    previousScore = completed.get(1).getScore();
                }
            }
        } catch (Exception ignored) {}

        // 5. Planner
        int plannerConsistency = 0;
        int completedTasks = 0;
        int skippedTasks = 0;
        int studyMinutes = 0;
        int streakDays = 0;
        try {
            PlannerProgressDto p = plannerService.getProgress(user);
            if (p != null) {
                plannerConsistency = p.getCompletionPercentage();
                completedTasks = p.getCompletedTasks();
                skippedTasks = p.getSkippedTasks();
                studyMinutes = p.getWeeklyMinutesCompleted();
                streakDays = p.getCurrentStreak();
            }
        } catch (Exception ignored) {}

        // 6. Resume & Assessment presence
        boolean hasResume = false;
        try {
            hasResume = resumeRepository.findFirstByUserOrderByUploadTimestampDesc(user).isPresent();
        } catch (Exception ignored) {}

        boolean hasQuiz = false;
        try {
            hasQuiz = quizService.getLatestAttempt(user).isPresent();
        } catch (Exception ignored) {}

        // 7. Deterministic Overall Progress Score (Weighted formula: 0-100)
        int overallScore = (int) Math.round(
                (careerReadinessScore * 0.30) +
                (plannerConsistency * 0.25) +
                (roadmapProgress * 0.20) +
                (dsaPercent * 0.15) +
                (interviewAvg * 0.10)
        );
        overallScore = Math.max(0, Math.min(100, overallScore));

        // 8. Trends
        TrendDirection roadmapTrend = completedSteps > 0 ? TrendDirection.IMPROVING : (totalSteps > 0 ? TrendDirection.STABLE : TrendDirection.INSUFFICIENT_DATA);
        TrendDirection dsaTrend = dsaSolved > 0 ? TrendDirection.IMPROVING : (dsaTotal > 0 ? TrendDirection.STABLE : TrendDirection.INSUFFICIENT_DATA);
        
        TrendDirection interviewTrend;
        if (interviewCount >= 2) {
            if (latestScore > previousScore) interviewTrend = TrendDirection.IMPROVING;
            else if (latestScore < previousScore) interviewTrend = TrendDirection.DECLINING;
            else interviewTrend = TrendDirection.STABLE;
        } else if (interviewCount == 1) {
            interviewTrend = TrendDirection.STABLE;
        } else {
            interviewTrend = TrendDirection.INSUFFICIENT_DATA;
        }

        TrendDirection plannerTrend;
        if (streakDays >= 2 || plannerConsistency >= 50) {
            plannerTrend = TrendDirection.IMPROVING;
        } else if (completedTasks > 0) {
            plannerTrend = TrendDirection.STABLE;
        } else if (skippedTasks > 0) {
            plannerTrend = TrendDirection.DECLINING;
        } else {
            plannerTrend = TrendDirection.INSUFFICIENT_DATA;
        }

        TrendDirection overallTrend;
        if (plannerTrend == TrendDirection.IMPROVING || roadmapTrend == TrendDirection.IMPROVING || dsaTrend == TrendDirection.IMPROVING || interviewTrend == TrendDirection.IMPROVING) {
            overallTrend = TrendDirection.IMPROVING;
        } else if (plannerTrend == TrendDirection.DECLINING || interviewTrend == TrendDirection.DECLINING) {
            overallTrend = TrendDirection.DECLINING;
        } else {
            overallTrend = TrendDirection.STABLE;
        }

        // 9. Top Achievements (This Week's Wins)
        List<String> achievements = new ArrayList<>();
        if (completedTasks > 0) {
            achievements.add("Completed " + completedTasks + " scheduled learning milestones in Weekly Planner");
        }
        if (streakDays > 0) {
            achievements.add("Maintained active " + streakDays + "-day daily learning streak");
        }
        if (completedSteps > 0) {
            achievements.add("Achieved " + completedSteps + " of " + totalSteps + " milestones in " + targetCareer + " roadmap");
        }
        if (dsaSolved > 0) {
            achievements.add("Solved " + dsaSolved + " curated DSA challenges with working solutions");
        }
        if (interviewCount > 0) {
            achievements.add("Completed " + interviewCount + " mock technical interview sessions (Avg Score: " + interviewAvg + "%)");
        }
        if (studyMinutes > 0) {
            achievements.add("Logged " + studyMinutes + " minutes of dedicated active career study time");
        }
        if (achievements.isEmpty()) {
            achievements.add("Registered target career goal for " + targetCareer);
            achievements.add("Initialized personalized Career Intelligence diagnostics");
        }

        // 10. Priority Areas (Needs Attention)
        List<String> priorityAreas = new ArrayList<>();
        if (!skillGaps.isEmpty()) {
            priorityAreas.add("Address top critical skill gap: " + skillGaps.get(0).getSkill());
            if (skillGaps.size() > 1) {
                priorityAreas.add("Review missing target competency: " + skillGaps.get(1).getSkill());
            }
        }
        if (interviewCount == 0) {
            priorityAreas.add("No mock interview completed yet — schedule a diagnostic technical session");
        } else if (interviewAvg < 60) {
            priorityAreas.add("Mock interview average (" + interviewAvg + "%) is below recommended 70% threshold");
        }
        if (skippedTasks > 0) {
            priorityAreas.add(skippedTasks + " planner tasks were skipped this week — review weekly study budget");
        }
        if (!hasResume) {
            priorityAreas.add("Upload resume to enable AI ATS parsing and extract verified profile skills");
        }
        if (!hasQuiz) {
            priorityAreas.add("Take diagnostic skill quiz to benchmark foundational competencies");
        }

        // 11. Strengths, Weaknesses, Improvements
        List<String> strengths = new ArrayList<>();
        if (streakDays >= 2) strengths.add("Strong daily learning consistency and adherence");
        if (roadmapProgress >= 30) strengths.add("Solid curriculum progression through career roadmap");
        if (dsaSolved >= 5) strengths.add("Active hands-on problem solving habits");
        if (interviewAvg >= 70) strengths.add("Confident articulation in technical interview simulations");
        if (strengths.isEmpty()) strengths.add("Clear target career goal established for " + targetCareer);

        List<String> weaknesses = new ArrayList<>();
        if (!skillGaps.isEmpty()) weaknesses.add("Missing key industry requirements: " + skillGaps.stream().limit(3).map(CareerGapDto::getSkill).collect(Collectors.joining(", ")));
        if (interviewCount == 0 || interviewAvg < 60) weaknesses.add("Under-developed interview screen confidence under timed conditions");
        if (dsaSolved < 3) weaknesses.add("Low DSA volume in problem solving catalog");
        if (weaknesses.isEmpty()) weaknesses.add("Need to expand hands-on portfolio projects for recruiter visibility");

        List<String> improvements = new ArrayList<>();
        if (streakDays > 0) improvements.add("Study habit formation is trending positively (+ " + streakDays + " streak days)");
        if (completedSteps > 0) improvements.add("Roadmap mastery increased to " + roadmapProgress + "%");
        if (interviewTrend == TrendDirection.IMPROVING) improvements.add("Recent interview performance scored above historical average");
        if (improvements.isEmpty()) improvements.add("Systematic execution will unlock rapid readiness point gains");

        // 12. Next Week Focus (3-4 Action items)
        List<ProgressActionDto> nextWeekFocus = new ArrayList<>();
        nextWeekFocus.add(ProgressActionDto.builder()
                .title("Complete Next Roadmap Checkpoint: " + nextRoadmapStep)
                .category("ROADMAP")
                .whyItMatters("Closes foundational curriculum gaps in " + targetCareer + " track.")
                .actionUrl("/roadmap")
                .estimatedEffort("45 mins")
                .build());

        nextWeekFocus.add(ProgressActionDto.builder()
                .title("Solve Practice Challenge: " + nextProblem)
                .category("DSA")
                .whyItMatters("Sharpens algorithmic problem solving and coding fluency.")
                .actionUrl("/practice")
                .estimatedEffort("30 mins")
                .build());

        if (interviewCount == 0 || interviewAvg < 65) {
            nextWeekFocus.add(ProgressActionDto.builder()
                    .title("Complete Full 5-Question Mock Interview")
                    .category("MOCK_INTERVIEW")
                    .whyItMatters("Validates concept explanations and scores under timed conditions.")
                    .actionUrl("/mock-interview")
                    .estimatedEffort("30 mins")
                    .build());
        }

        if (!hasResume) {
            nextWeekFocus.add(ProgressActionDto.builder()
                    .title("Upload & Analyze Technical Resume")
                    .category("RESUME")
                    .whyItMatters("Enables ATS intelligence and synchronizes verified skills to your profile.")
                    .actionUrl("/resume")
                    .estimatedEffort("15 mins")
                    .build());
        } else if (!skillGaps.isEmpty()) {
            nextWeekFocus.add(ProgressActionDto.builder()
                    .title("Verify & Add Skill: " + skillGaps.get(0).getSkill())
                    .category("SKILL")
                    .whyItMatters("Directly satisfies recruiter screening filters for " + targetCareer + ".")
                    .actionUrl("/skills")
                    .estimatedEffort("15 mins")
                    .build());
        }

        // 13. Deterministic Coach Summary
        String coachSummary = "Your overall progress score is " + overallScore + "% with an overall " + overallTrend.name().toLowerCase() + " trend. " +
                (streakDays > 0 ? "You have an active " + streakDays + "-day streak. " : "") +
                (!skillGaps.isEmpty() ? "Prioritize closing " + skillGaps.get(0).getSkill() + " and advancing roadmap checkpoints next week." : "Keep up daily practice to reach candidate interview readiness.");

        return WeeklyProgressReportDto.builder()
                .userId(user.getId())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .overallProgressScore(overallScore)
                .careerReadinessScore(careerReadinessScore)
                .roadmapProgress(roadmapProgress)
                .dsaProgress(dsaPercent)
                .dsaSolvedCount(dsaSolved)
                .dsaTotalCount(dsaTotal)
                .interviewProgress(interviewAvg)
                .interviewCompletedCount(interviewCount)
                .plannerConsistency(plannerConsistency)
                .completedTasks(completedTasks)
                .skippedTasks(skippedTasks)
                .studyMinutes(studyMinutes)
                .streakDays(streakDays)
                .roadmapTrend(roadmapTrend)
                .dsaTrend(dsaTrend)
                .interviewTrend(interviewTrend)
                .plannerTrend(plannerTrend)
                .overallTrend(overallTrend)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .improvements(improvements)
                .topAchievements(achievements)
                .priorityAreas(priorityAreas)
                .nextWeekFocus(nextWeekFocus.stream().limit(4).collect(Collectors.toList()))
                .coachSummary(coachSummary)
                .source("DETERMINISTIC")
                .generatedAt(java.time.Instant.now())
                .build();
    }
}
