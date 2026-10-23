package com.careeradvisor.backend.ai.context;

import com.careeradvisor.backend.ai.dto.*;
import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.MockInterviewRepository;
import com.careeradvisor.backend.repository.ResumeRepository;
import com.careeradvisor.backend.service.*;
import com.careeradvisor.backend.service.PlannerService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserAiContextService {

    private final UserSkillService userSkillService;
    private final QuizService quizService;
    private final UserProgressService userProgressService;
    private final ProblemService problemService;
    private final MockInterviewRepository interviewRepository;
    private final ResumeRepository resumeRepository;
    private final RecommendationService recommendationService;
    private final CareerIntelligenceService careerIntelligenceService;
    private final PlannerService plannerService;
    private final DeterministicProgressEngine deterministicProgressEngine;

    public UserAiContextService(UserSkillService userSkillService,
                                QuizService quizService,
                                UserProgressService userProgressService,
                                ProblemService problemService,
                                MockInterviewRepository interviewRepository,
                                ResumeRepository resumeRepository,
                                RecommendationService recommendationService,
                                CareerIntelligenceService careerIntelligenceService,
                                PlannerService plannerService,
                                DeterministicProgressEngine deterministicProgressEngine) {
        this.userSkillService = userSkillService;
        this.quizService = quizService;
        this.userProgressService = userProgressService;
        this.problemService = problemService;
        this.interviewRepository = interviewRepository;
        this.resumeRepository = resumeRepository;
        this.recommendationService = recommendationService;
        this.careerIntelligenceService = careerIntelligenceService;
        this.plannerService = plannerService;
        this.deterministicProgressEngine = deterministicProgressEngine;
    }

    /**
     * Aggregates the authoritative personal context for the specified authenticated user.
     * Guarantees zero sensitive data (passwords, salts, hashes, tokens, admin secrets) is exposed.
     */
    public PersonalAiContextDto buildUserContext(User user) {
        if (user == null) {
            return null;
        }

        // 1. User Profile Summary
        UserSummaryDto profile = UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .careerGoal(user.getCareerGoal() != null ? user.getCareerGoal() : null)
                .userLevel(user.getUserLevel() != null ? user.getUserLevel() : "Intermediate")
                .build();

        // 2. Verified Skills
        List<String> verifiedSkills = userSkillService.getUserSkills(user);

        // 3. Latest Resume Summary
        ResumeAiSummaryDto resumeSummary = null;
        Optional<Resume> latestResumeOpt = resumeRepository.findFirstByUserOrderByUploadTimestampDesc(user);
        if (latestResumeOpt.isPresent()) {
            Resume resume = latestResumeOpt.get();
            resumeSummary = ResumeAiSummaryDto.builder()
                    .resumeId(resume.getId())
                    .fileName(resume.getOriginalFileName())
                    .uploadTimestamp(resume.getUploadTimestamp())
                    .parsingStatus(resume.getParsingStatus() != null ? resume.getParsingStatus().name() : "PARSED")
                    .extractedSkills(resume.getExtractedSkills() != null ? new ArrayList<>(resume.getExtractedSkills()) : new ArrayList<>())
                    .executiveSummarySnippet(resume.getExtractedSummary() != null && resume.getExtractedSummary().length() > 200
                            ? resume.getExtractedSummary().substring(0, 200) + "..."
                            : resume.getExtractedSummary())
                    .build();
        }

        // 4. Quiz Assessment Summary
        QuizAiSummaryDto quizSummary = null;
        Optional<QuizAttempt> latestQuizOpt = quizService.getLatestAttempt(user);
        if (latestQuizOpt.isPresent()) {
            QuizAttempt quiz = latestQuizOpt.get();
            quizSummary = QuizAiSummaryDto.builder()
                    .score(quiz.getScore())
                    .totalQuestions(quiz.getTotalQuestions())
                    .percentage(quiz.getPercentage())
                    .evaluatedLevel(quiz.getLevel())
                    .recommendedCareer(quiz.getRecommendedCareer())
                    .attemptDate(quiz.getCreatedAt())
                    .build();
        }

        // 5. Roadmap Progress Summary
        RoadmapAiProgressDto roadmapProgress = null;
        ProgressSummaryResponse progressSummary = userProgressService.getProgressSummary(user);
        if (progressSummary != null) {
            roadmapProgress = RoadmapAiProgressDto.builder()
                    .careerGoal(progressSummary.getCareerGoal())
                    .completedStepsCount(progressSummary.getCompletedStepsCount())
                    .totalStepsCount(progressSummary.getTotalStepsCount())
                    .completionPercentage(progressSummary.getRoadmapPercent())
                    .completedStepTitles(progressSummary.getCompletedSteps() != null ? new ArrayList<>(progressSummary.getCompletedSteps()) : new ArrayList<>())
                    .nextRecommendedStep(progressSummary.getNextTopicToLearn() != null ? progressSummary.getNextTopicToLearn() : "Continue next roadmap checkpoint")
                    .build();
        }

        // 6. DSA / Problem Progress Summary
        DsaAiProgressDto dsaProgress = null;
        ProblemProgressSummaryDto dsaSummary = problemService.getProgressSummary(user);
        if (dsaSummary != null) {
            Map<String, Integer> categoryDistribution = new HashMap<>();
            if (dsaSummary.getCategoryStats() != null) {
                for (CategoryProgressStat stat : dsaSummary.getCategoryStats()) {
                    String catName = stat.getDisplayName() != null ? stat.getDisplayName() : (stat.getCategory() != null ? stat.getCategory().name() : "Other");
                    categoryDistribution.put(catName, stat.getSolved());
                }
            }

            dsaProgress = DsaAiProgressDto.builder()
                    .solvedCount(dsaSummary.getSolvedProblems())
                    .totalCount(dsaSummary.getTotalProblems())
                    .completionPercentage(dsaSummary.getCompletionPercentage())
                    .categoryDistribution(categoryDistribution)
                    .nextRecommendedProblem(dsaSummary.getNextRecommendedProblem())
                    .build();
        }

        // 7. Mock Interview Performance
        MockInterviewAiSummaryDto interviewSummary = null;
        List<MockInterview> allInterviews = interviewRepository.findByUserOrderByStartedAtDesc(user);
        if (!allInterviews.isEmpty()) {
            int total = allInterviews.size();
            List<MockInterview> completed = allInterviews.stream()
                    .filter(i -> i.getStatus() == InterviewStatus.COMPLETED)
                    .collect(Collectors.toList());

            int avgScore = 0;
            int bestScore = 0;
            if (!completed.isEmpty()) {
                avgScore = (int) Math.round(completed.stream().mapToInt(MockInterview::getScore).average().orElse(0));
                bestScore = completed.stream().mapToInt(MockInterview::getScore).max().orElse(0);
            }

            MockInterview latest = allInterviews.get(0);
            List<String> strongList = latest.getStrongAreas() != null ? Arrays.asList(latest.getStrongAreas().split(",")) : new ArrayList<>();
            List<String> weakList = latest.getWeakAreas() != null ? Arrays.asList(latest.getWeakAreas().split(",")) : new ArrayList<>();

            interviewSummary = MockInterviewAiSummaryDto.builder()
                    .totalSessions(total)
                    .completedSessions(completed.size())
                    .averageScore(avgScore)
                    .bestScore(bestScore)
                    .latestScore(latest.getScore())
                    .latestCategory(latest.getCategory() != null ? latest.getCategory().name() : null)
                    .strongAreas(strongList)
                    .weakAreas(weakList)
                    .build();
        }

        // 8. Recommendation Intelligence Summary
        RecommendationAiSummaryDto recommendationSummary = null;
        PersonalizedIntelligenceResponse recs = recommendationService.getPersonalizedIntelligence(user);
        if (recs != null) {
            List<String> actions = new ArrayList<>();
            if (recs.getActionPlan() != null) {
                actions = recs.getActionPlan().stream()
                        .map(RecommendationItemDto::getTitle)
                        .collect(Collectors.toList());
            }

            List<String> missingSkills = recs.getSkillGaps() != null && recs.getSkillGaps().getMissingSkills() != null
                    ? new ArrayList<>(recs.getSkillGaps().getMissingSkills())
                    : new ArrayList<>();

            String topMatchTitle = profile.getCareerGoal();
            int topMatchScore = 0;
            if (recs.getCareerMatches() != null && !recs.getCareerMatches().isEmpty()) {
                CareerMatchDto topMatch = recs.getCareerMatches().get(0);
                topMatchTitle = topMatch.getTitle();
                topMatchScore = topMatch.getMatchScore();
            }

            recommendationSummary = RecommendationAiSummaryDto.builder()
                    .overallReadinessScore(recs.getOverallReadinessScore())
                    .userLifecycleState(recs.getUserState() != null ? recs.getUserState() : "BEGINNER")
                    .topCareerMatch(topMatchTitle)
                    .topCareerMatchPercentage(topMatchScore)
                    .missingSkills(missingSkills)
                    .topActionableRecommendations(actions)
                    .build();
        }

        // 9. Unified Career Intelligence Engine Snapshot
        CareerIntelligenceAiSummaryDto careerIntelligence = null;
        try {
            CareerIntelligenceDto intel = careerIntelligenceService.calculateCareerIntelligence(user);
            if (intel != null) {
                List<String> topGaps = intel.getSkillGaps() != null
                        ? intel.getSkillGaps().stream().limit(5).map(CareerGapDto::getSkill).collect(Collectors.toList())
                        : new ArrayList<>();

                List<String> actionTitles = intel.getPrioritizedActions() != null
                        ? intel.getPrioritizedActions().stream().limit(4).map(CareerActionDto::getTitle).collect(Collectors.toList())
                        : new ArrayList<>();

                careerIntelligence = CareerIntelligenceAiSummaryDto.builder()
                        .overallReadinessScore(intel.getOverallReadinessScore())
                        .readinessState(intel.getReadinessStateDisplayName())
                        .targetCareer(intel.getTargetCareer())
                        .strongestDimensions(intel.getStrongestDimensions() != null ? intel.getStrongestDimensions() : new ArrayList<>())
                        .weakestDimensions(intel.getWeakestDimensions() != null ? intel.getWeakestDimensions() : new ArrayList<>())
                        .topSkillGaps(topGaps)
                        .prioritizedActionTitles(actionTitles)
                        .build();
            }
        } catch (Exception e) {
            // Safe fallback
        }

        // 10. Planner Summary (Phase 15C)
        PlannerAiSummaryDto plannerSummary = null;
        try {
            var progress = plannerService.getProgress(user);
            var todayTasks = plannerService.getTodayTasks(user);
            var pref = plannerService.getOrCreatePreferences(user);
            plannerSummary = PlannerAiSummaryDto.builder()
                    .weeklyGoal(pref.getWeeklyGoal())
                    .completionPercentage(progress.getCompletionPercentage())
                    .totalTasks(progress.getTotalTasks())
                    .completedTasks(progress.getCompletedTasks())
                    .dailyStudyMinutes(pref.getDailyStudyMinutes())
                    .currentStreak(progress.getCurrentStreak())
                    .todayScheduledTasks(todayTasks != null ? todayTasks.stream().map(t -> t.getTitle() + " (" + t.getEstimatedMinutes() + "m, " + t.getPriority() + ")").collect(Collectors.toList()) : new ArrayList<>())
                    .build();
        } catch (Exception ignored) {}

        // 11. Progress Intelligence Coach Summary (Phase 15D)
        ProgressAiSummaryDto progressReport = null;
        try {
            WeeklyProgressReportDto rep = deterministicProgressEngine.generateDeterministicReport(user);
            if (rep != null) {
                progressReport = ProgressAiSummaryDto.builder()
                        .overallProgressScore(rep.getOverallProgressScore())
                        .careerReadinessScore(rep.getCareerReadinessScore())
                        .roadmapProgress(rep.getRoadmapProgress())
                        .dsaSolvedCount(rep.getDsaSolvedCount())
                        .interviewAvgScore(rep.getInterviewProgress())
                        .plannerConsistency(rep.getPlannerConsistency())
                        .currentStreak(rep.getStreakDays())
                        .overallTrend(rep.getOverallTrend())
                        .topAchievements(rep.getTopAchievements() != null ? rep.getTopAchievements() : new ArrayList<>())
                        .priorityAreas(rep.getPriorityAreas() != null ? rep.getPriorityAreas() : new ArrayList<>())
                        .coachSummary(rep.getCoachSummary())
                        .build();
            }
        } catch (Exception ignored) {}

        return PersonalAiContextDto.builder()
                .userProfile(profile)
                .targetCareerGoal(profile.getCareerGoal())
                .verifiedSkills(verifiedSkills)
                .resumeSummary(resumeSummary)
                .quizAssessment(quizSummary)
                .roadmapProgress(roadmapProgress)
                .dsaProgress(dsaProgress)
                .mockInterviewPerformance(interviewSummary)
                .recommendations(recommendationSummary)
                .careerIntelligence(careerIntelligence)
                .plannerSummary(plannerSummary)
                .progressReport(progressReport)
                .contextTimestamp(Instant.now())
                .build();
    }
}
