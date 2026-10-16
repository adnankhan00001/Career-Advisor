package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.MockInterviewRepository;
import com.careeradvisor.backend.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CareerIntelligenceService {

    private final CareerService careerService;
    private final UserSkillService userSkillService;
    private final QuizService quizService;
    private final UserProgressService userProgressService;
    private final ProblemService problemService;
    private final MockInterviewRepository interviewRepository;
    private final ResumeRepository resumeRepository;

    public CareerIntelligenceService(CareerService careerService,
                                     UserSkillService userSkillService,
                                     QuizService quizService,
                                     UserProgressService userProgressService,
                                     ProblemService problemService,
                                     MockInterviewRepository interviewRepository,
                                     ResumeRepository resumeRepository) {
        this.careerService = careerService;
        this.userSkillService = userSkillService;
        this.quizService = quizService;
        this.userProgressService = userProgressService;
        this.problemService = problemService;
        this.interviewRepository = interviewRepository;
        this.resumeRepository = resumeRepository;
    }

    /**
     * Calculates the deterministic unified career intelligence snapshot for the given user.
     */
    public CareerIntelligenceDto calculateCareerIntelligence(User user) {
        if (user == null) {
            return buildAnonymousIntelligence();
        }

        String targetGoal = (user.getCareerGoal() != null && !user.getCareerGoal().trim().isEmpty())
                ? user.getCareerGoal().trim() : "Java Backend Developer";

        List<CareerDto> allCareers = careerService.getAllCareers();
        CareerDto targetCareer = allCareers.stream()
                .filter(c -> c.getTitle().equalsIgnoreCase(targetGoal))
                .findFirst()
                .orElse(allCareers.isEmpty() ? null : allCareers.get(0));

        // 1. Fetch Authoritative Platform Records
        List<String> verifiedSkills = userSkillService.getUserSkills(user);
        Optional<Resume> latestResumeOpt = resumeRepository.findFirstByUserOrderByUploadTimestampDesc(user);
        Optional<QuizAttempt> latestQuizOpt = quizService.getLatestAttempt(user);
        ProgressSummaryResponse roadmapSummary = userProgressService.getProgressSummary(user);
        ProblemProgressSummaryDto dsaSummary = problemService.getProgressSummary(user);
        List<MockInterview> allInterviews = interviewRepository.findByUserOrderByStartedAtDesc(user);

        // 2. Compute 6 Dimension Scores
        Map<DimensionType, CareerDimensionScoreDto> dimensions = new LinkedHashMap<>();

        // Dimension 1: SKILLS (Weight: 20%)
        CareerDimensionScoreDto skillsDim = computeSkillsDimension(targetCareer, verifiedSkills);
        dimensions.put(DimensionType.SKILLS, skillsDim);

        // Dimension 2: ROADMAP (Weight: 20%)
        CareerDimensionScoreDto roadmapDim = computeRoadmapDimension(roadmapSummary);
        dimensions.put(DimensionType.ROADMAP, roadmapDim);

        // Dimension 3: ASSESSMENT (Weight: 15%)
        CareerDimensionScoreDto assessmentDim = computeAssessmentDimension(latestQuizOpt.orElse(null));
        dimensions.put(DimensionType.ASSESSMENT, assessmentDim);

        // Dimension 4: DSA (Weight: 15%)
        CareerDimensionScoreDto dsaDim = computeDsaDimension(dsaSummary);
        dimensions.put(DimensionType.DSA, dsaDim);

        // Dimension 5: MOCK_INTERVIEW (Weight: 15%)
        CareerDimensionScoreDto interviewDim = computeInterviewDimension(allInterviews);
        dimensions.put(DimensionType.MOCK_INTERVIEW, interviewDim);

        // Dimension 6: RESUME (Weight: 15%)
        CareerDimensionScoreDto resumeDim = computeResumeDimension(targetCareer, latestResumeOpt.orElse(null));
        dimensions.put(DimensionType.RESUME, resumeDim);

        // 3. Compute Overall Readiness Score (Weighted Sum Bounded 0-100)
        double totalWeighted = dimensions.values().stream()
                .mapToDouble(CareerDimensionScoreDto::getWeightedContribution)
                .sum();
        int overallScore = (int) Math.min(100, Math.max(0, Math.round(totalWeighted)));
        ReadinessState readinessState = ReadinessState.fromScore(overallScore);

        // 4. Identify Strongest & Weakest Dimensions
        List<CareerDimensionScoreDto> sortedDims = dimensions.values().stream()
                .sorted(Comparator.comparingInt(CareerDimensionScoreDto::getScore).reversed())
                .collect(Collectors.toList());

        List<String> strongest = sortedDims.stream()
                .filter(d -> d.getScore() >= 50)
                .limit(2)
                .map(CareerDimensionScoreDto::getDisplayName)
                .collect(Collectors.toList());

        List<String> weakest = sortedDims.stream()
                .sorted(Comparator.comparingInt(CareerDimensionScoreDto::getScore))
                .limit(2)
                .map(CareerDimensionScoreDto::getDisplayName)
                .collect(Collectors.toList());

        // 5. Compute Detailed Skill Gaps
        List<CareerGapDto> skillGaps = computeSkillGaps(targetCareer, verifiedSkills, latestResumeOpt.orElse(null), allInterviews);

        // 6. Compute Prioritized 3-5 Action Plan
        List<CareerActionDto> prioritizedActions = computePrioritizedActions(
                targetGoal,
                dimensions,
                skillGaps,
                roadmapSummary,
                dsaSummary,
                latestQuizOpt.orElse(null),
                latestResumeOpt.orElse(null),
                allInterviews
        );

        // 7. Executive Summary Narrative
        String summaryNarrative = String.format(
                "You are currently at the %s level (%d%% readiness) for %s. Your top strength is %s, while your primary growth opportunity lies in %s.",
                readinessState.getDisplayName(),
                overallScore,
                targetGoal,
                strongest.isEmpty() ? "Starting your learning journey" : strongest.get(0),
                weakest.isEmpty() ? "Core roadmap completion" : weakest.get(0)
        );

        return CareerIntelligenceDto.builder()
                .userId(user.getId())
                .userName(user.getName())
                .targetCareer(targetGoal)
                .userLevel(user.getUserLevel() != null ? user.getUserLevel() : "Intermediate")
                .overallReadinessScore(overallScore)
                .readinessState(readinessState)
                .readinessStateDisplayName(readinessState.getDisplayName())
                .summary(summaryNarrative)
                .dimensionScores(dimensions)
                .strongestDimensions(strongest)
                .weakestDimensions(weakest)
                .skillGaps(skillGaps)
                .prioritizedActions(prioritizedActions)
                .calculatedAt(Instant.now())
                .build();
    }

    public List<CareerActionDto> getPrioritizedActions(User user) {
        CareerIntelligenceDto intelligence = calculateCareerIntelligence(user);
        return intelligence.getPrioritizedActions();
    }

    private CareerDimensionScoreDto computeSkillsDimension(CareerDto targetCareer, List<String> verifiedSkills) {
        int weight = 20;
        if (verifiedSkills == null || verifiedSkills.isEmpty()) {
            return CareerDimensionScoreDto.builder()
                    .dimension(DimensionType.SKILLS)
                    .displayName("Verified Portfolio Skills")
                    .score(0)
                    .weightPercentage(weight)
                    .weightedContribution(0.0)
                    .attempted(false)
                    .statusDescription("No verified skills in portfolio yet")
                    .detailMetric("0 verified skills")
                    .build();
        }

        List<String> required = targetCareer != null && targetCareer.getRequiredSkills() != null
                ? targetCareer.getRequiredSkills() : Collections.emptyList();

        long matchedCount = required.stream()
                .filter(req -> verifiedSkills.stream().anyMatch(v -> v.equalsIgnoreCase(req)))
                .count();

        int score;
        if (required.isEmpty()) {
            score = Math.min(100, verifiedSkills.size() * 20);
        } else {
            int reqScore = (int) Math.round(((double) matchedCount / required.size()) * 80.0);
            int extraBonus = Math.min(20, (verifiedSkills.size() - (int) matchedCount) * 5);
            score = Math.min(100, reqScore + Math.max(0, extraBonus));
        }

        return CareerDimensionScoreDto.builder()
                .dimension(DimensionType.SKILLS)
                .displayName("Verified Portfolio Skills")
                .score(score)
                .weightPercentage(weight)
                .weightedContribution((score * weight) / 100.0)
                .attempted(true)
                .statusDescription(score >= 75 ? "Strong portfolio coverage" : "Skill gaps present")
                .detailMetric(String.format("%d verified (%d matched target)", verifiedSkills.size(), matchedCount))
                .build();
    }

    private CareerDimensionScoreDto computeRoadmapDimension(ProgressSummaryResponse roadmapSummary) {
        int weight = 20;
        if (roadmapSummary == null || roadmapSummary.getTotalStepsCount() == 0) {
            return CareerDimensionScoreDto.builder()
                    .dimension(DimensionType.ROADMAP)
                    .displayName("Career Roadmap Progress")
                    .score(0)
                    .weightPercentage(weight)
                    .weightedContribution(0.0)
                    .attempted(false)
                    .statusDescription("Roadmap not started")
                    .detailMetric("0 milestones completed")
                    .build();
        }

        int score = Math.min(100, Math.max(0, roadmapSummary.getRoadmapPercent()));
        boolean attempted = roadmapSummary.getCompletedStepsCount() > 0;

        return CareerDimensionScoreDto.builder()
                .dimension(DimensionType.ROADMAP)
                .displayName("Career Roadmap Progress")
                .score(score)
                .weightPercentage(weight)
                .weightedContribution((score * weight) / 100.0)
                .attempted(attempted)
                .statusDescription(score >= 70 ? "Advanced milestone progress" : attempted ? "In progress" : "Not started")
                .detailMetric(String.format("%d/%d milestones (%d%%)",
                        roadmapSummary.getCompletedStepsCount(),
                        roadmapSummary.getTotalStepsCount(),
                        score))
                .build();
    }

    private CareerDimensionScoreDto computeAssessmentDimension(QuizAttempt latestQuiz) {
        int weight = 15;
        if (latestQuiz == null) {
            return CareerDimensionScoreDto.builder()
                    .dimension(DimensionType.ASSESSMENT)
                    .displayName("Diagnostic Assessment")
                    .score(0)
                    .weightPercentage(weight)
                    .weightedContribution(0.0)
                    .attempted(false)
                    .statusDescription("Diagnostic quiz not yet attempted")
                    .detailMetric("No quiz records")
                    .build();
        }

        int score = Math.min(100, Math.max(0, latestQuiz.getPercentage()));

        return CareerDimensionScoreDto.builder()
                .dimension(DimensionType.ASSESSMENT)
                .displayName("Diagnostic Assessment")
                .score(score)
                .weightPercentage(weight)
                .weightedContribution((score * weight) / 100.0)
                .attempted(true)
                .statusDescription(String.format("Level: %s (%s)", latestQuiz.getLevel(), latestQuiz.getRecommendedCareer()))
                .detailMetric(String.format("%d/%d correct (%d%%)", latestQuiz.getScore(), latestQuiz.getTotalQuestions(), score))
                .build();
    }

    private CareerDimensionScoreDto computeDsaDimension(ProblemProgressSummaryDto dsaSummary) {
        int weight = 15;
        if (dsaSummary == null || dsaSummary.getSolvedProblems() == 0) {
            return CareerDimensionScoreDto.builder()
                    .dimension(DimensionType.DSA)
                    .displayName("DSA & Coding Practice")
                    .score(0)
                    .weightPercentage(weight)
                    .weightedContribution(0.0)
                    .attempted(false)
                    .statusDescription("No coding problems solved yet")
                    .detailMetric("0 solved")
                    .build();
        }

        // Benchmark: solving 10+ problems gives 100% DSA score in learning tier
        int solved = dsaSummary.getSolvedProblems();
        int score = Math.min(100, (int) Math.round((solved / 10.0) * 100.0));

        return CareerDimensionScoreDto.builder()
                .dimension(DimensionType.DSA)
                .displayName("DSA & Coding Practice")
                .score(score)
                .weightPercentage(weight)
                .weightedContribution((score * weight) / 100.0)
                .attempted(true)
                .statusDescription(score >= 70 ? "Consistent problem solver" : "Practicing fundamentals")
                .detailMetric(String.format("%d solved out of %d total", solved, dsaSummary.getTotalProblems()))
                .build();
    }

    private CareerDimensionScoreDto computeInterviewDimension(List<MockInterview> allInterviews) {
        int weight = 15;
        if (allInterviews == null || allInterviews.isEmpty()) {
            return CareerDimensionScoreDto.builder()
                    .dimension(DimensionType.MOCK_INTERVIEW)
                    .displayName("Mock Technical Interview")
                    .score(0)
                    .weightPercentage(weight)
                    .weightedContribution(0.0)
                    .attempted(false)
                    .statusDescription("No mock interviews completed")
                    .detailMetric("0 sessions")
                    .build();
        }

        List<MockInterview> completed = allInterviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completed.isEmpty()) {
            return CareerDimensionScoreDto.builder()
                    .dimension(DimensionType.MOCK_INTERVIEW)
                    .displayName("Mock Technical Interview")
                    .score(0)
                    .weightPercentage(weight)
                    .weightedContribution(0.0)
                    .attempted(true)
                    .statusDescription("Interview session in progress / incomplete")
                    .detailMetric(String.format("%d sessions started", allInterviews.size()))
                    .build();
        }

        int avgScore = (int) Math.round(completed.stream().mapToInt(MockInterview::getScore).average().orElse(0));
        int bestScore = completed.stream().mapToInt(MockInterview::getScore).max().orElse(0);

        return CareerDimensionScoreDto.builder()
                .dimension(DimensionType.MOCK_INTERVIEW)
                .displayName("Mock Technical Interview")
                .score(avgScore)
                .weightPercentage(weight)
                .weightedContribution((avgScore * weight) / 100.0)
                .attempted(true)
                .statusDescription(avgScore >= 70 ? "Strong technical communication" : "Requires additional review")
                .detailMetric(String.format("%d completed (Avg: %d%%, Best: %d%%)", completed.size(), avgScore, bestScore))
                .build();
    }

    private CareerDimensionScoreDto computeResumeDimension(CareerDto targetCareer, Resume resume) {
        int weight = 15;
        if (resume == null) {
            return CareerDimensionScoreDto.builder()
                    .dimension(DimensionType.RESUME)
                    .displayName("Resume Alignment & Strength")
                    .score(0)
                    .weightPercentage(weight)
                    .weightedContribution(0.0)
                    .attempted(false)
                    .statusDescription("No resume uploaded yet")
                    .detailMetric("0 resumes")
                    .build();
        }

        List<String> extracted = resume.getExtractedSkills() != null
                ? new ArrayList<>(resume.getExtractedSkills()) : Collections.emptyList();

        List<String> required = targetCareer != null && targetCareer.getRequiredSkills() != null
                ? targetCareer.getRequiredSkills() : Collections.emptyList();

        long matchedCount = required.stream()
                .filter(req -> extracted.stream().anyMatch(e -> e.equalsIgnoreCase(req)))
                .count();

        // Base 50 for having a parsed resume + up to 50 for skill match
        int matchScore = required.isEmpty() ? 50 : (int) Math.round(((double) matchedCount / required.size()) * 50.0);
        int score = Math.min(100, 50 + matchScore);

        return CareerDimensionScoreDto.builder()
                .dimension(DimensionType.RESUME)
                .displayName("Resume Alignment & Strength")
                .score(score)
                .weightPercentage(weight)
                .weightedContribution((score * weight) / 100.0)
                .attempted(true)
                .statusDescription(score >= 75 ? "Resume strongly aligned with target goal" : "Resume uploaded with minor gaps")
                .detailMetric(String.format("%d extracted skills (%d matched target)", extracted.size(), matchedCount))
                .build();
    }

    private List<CareerGapDto> computeSkillGaps(CareerDto targetCareer,
                                                List<String> verifiedSkills,
                                                Resume resume,
                                                List<MockInterview> interviews) {
        List<CareerGapDto> gaps = new ArrayList<>();
        if (targetCareer == null || targetCareer.getRequiredSkills() == null) {
            return gaps;
        }

        List<String> resumeSkills = (resume != null && resume.getExtractedSkills() != null)
                ? new ArrayList<>(resume.getExtractedSkills()) : Collections.emptyList();

        Set<String> interviewWeaknesses = new HashSet<>();
        if (interviews != null) {
            for (MockInterview mi : interviews) {
                if (mi.getWeakAreas() != null && !mi.getWeakAreas().trim().isEmpty()) {
                    for (String w : mi.getWeakAreas().split(",")) {
                        interviewWeaknesses.add(w.trim().toLowerCase());
                    }
                }
            }
        }

        for (String reqSkill : targetCareer.getRequiredSkills()) {
            boolean inVerified = verifiedSkills != null && verifiedSkills.stream().anyMatch(v -> v.equalsIgnoreCase(reqSkill));
            boolean inResume = resumeSkills.stream().anyMatch(r -> r.equalsIgnoreCase(reqSkill));
            boolean isWeak = interviewWeaknesses.stream().anyMatch(w -> w.contains(reqSkill.toLowerCase()));

            if (!inVerified || !inResume || isWeak) {
                GapPriority priority;
                List<String> evidence = new ArrayList<>();
                evidence.add("Required competency for " + targetCareer.getTitle());

                if (!inVerified && !inResume) {
                    priority = GapPriority.CRITICAL;
                    evidence.add("Missing from verified portfolio skills");
                    evidence.add("Not detected in uploaded resume");
                } else if (!inVerified) {
                    priority = GapPriority.HIGH;
                    evidence.add("Found on resume but not yet verified in platform portfolio");
                } else if (isWeak) {
                    priority = GapPriority.HIGH;
                    evidence.add("Flagged as improvement area in recent mock interview evaluation");
                } else {
                    priority = GapPriority.MEDIUM;
                    evidence.add("Verified in portfolio but missing on resume profile");
                }

                gaps.add(CareerGapDto.builder()
                        .skill(reqSkill)
                        .priority(priority)
                        .source("TARGET_CAREER_ALIGNMENT")
                        .evidence(evidence)
                        .recommendedAction(String.format("Study %s via roadmap milestones and solve related practice problems", reqSkill))
                        .relatedRoadmapTopic(reqSkill)
                        .build());
            }
        }

        // Sort by priority: CRITICAL -> HIGH -> MEDIUM -> LOW
        gaps.sort(Comparator.comparing(CareerGapDto::getPriority));
        return gaps;
    }

    private List<CareerActionDto> computePrioritizedActions(String targetCareerTitle,
                                                            Map<DimensionType, CareerDimensionScoreDto> dimensions,
                                                            List<CareerGapDto> skillGaps,
                                                            ProgressSummaryResponse roadmapSummary,
                                                            ProblemProgressSummaryDto dsaSummary,
                                                            QuizAttempt latestQuiz,
                                                            Resume latestResume,
                                                            List<MockInterview> allInterviews) {
        List<CareerActionDto> actions = new ArrayList<>();

        // 1. Diagnostic Quiz Action
        if (latestQuiz == null) {
            actions.add(CareerActionDto.builder()
                    .id("ACT_ASSESSMENT_1")
                    .title("Complete Baseline Diagnostic Quiz")
                    .description("Take the 10-question adaptive assessment to establish your verified proficiency tier.")
                    .category(ActionCategory.ASSESSMENT)
                    .priority(GapPriority.CRITICAL)
                    .reasoning("Essential first milestone: Unlocks tailored recommendations and baseline diagnostic scoring.")
                    .estimatedImpactPoints(15)
                    .actionUrl("/quiz")
                    .completed(false)
                    .build());
        }

        // 2. Resume Upload Action
        if (latestResume == null) {
            actions.add(CareerActionDto.builder()
                    .id("ACT_RESUME_1")
                    .title("Upload Resume for AI Parsing")
                    .description("Upload your PDF resume to extract skills, calculate ATS match, and synchronize your profile.")
                    .category(ActionCategory.RESUME)
                    .priority(GapPriority.HIGH)
                    .reasoning("Resume parsing directly activates resume readiness intelligence and detects missing keywords.")
                    .estimatedImpactPoints(15)
                    .actionUrl("/resume")
                    .completed(false)
                    .build());
        }

        // 3. Top Skill Gap Roadmap Action
        if (!skillGaps.isEmpty()) {
            CareerGapDto topGap = skillGaps.get(0);
            String nextTopic = (roadmapSummary != null && roadmapSummary.getNextTopicToLearn() != null)
                    ? roadmapSummary.getNextTopicToLearn() : topGap.getSkill();

            actions.add(CareerActionDto.builder()
                    .id("ACT_ROADMAP_1")
                    .title("Master " + topGap.getSkill() + " on Roadmap")
                    .description("Advance your " + targetCareerTitle + " journey by tackling: " + nextTopic)
                    .category(ActionCategory.ROADMAP)
                    .priority(topGap.getPriority())
                    .reasoning("Directly closes your highest-priority skill gap (" + topGap.getSkill() + ") for " + targetCareerTitle + ".")
                    .estimatedImpactPoints(12)
                    .actionUrl("/roadmap")
                    .completed(false)
                    .build());
        }

        // 4. DSA Coding Action
        int solvedCount = dsaSummary != null ? dsaSummary.getSolvedProblems() : 0;
        if (solvedCount < 5) {
            String recProb = (dsaSummary != null && dsaSummary.getNextRecommendedProblem() != null)
                    ? dsaSummary.getNextRecommendedProblem() : "Two Sum / Array Basics";

            actions.add(CareerActionDto.builder()
                    .id("ACT_DSA_1")
                    .title("Solve Practice Problem: " + recProb)
                    .description("Practice hands-on coding in the interactive workspace to build technical problem-solving muscle.")
                    .category(ActionCategory.DSA)
                    .priority(solvedCount == 0 ? GapPriority.HIGH : GapPriority.MEDIUM)
                    .reasoning("Coding practice validates algorithmic proficiency expected in technical interviews.")
                    .estimatedImpactPoints(10)
                    .actionUrl("/practice")
                    .completed(false)
                    .build());
        }

        // 5. Mock Interview Action
        boolean hasCompletedInterview = allInterviews != null && allInterviews.stream().anyMatch(i -> i.getStatus() == InterviewStatus.COMPLETED);
        if (!hasCompletedInterview) {
            actions.add(CareerActionDto.builder()
                    .id("ACT_INTERVIEW_1")
                    .title("Conduct First Mock Technical Interview")
                    .description("Simulate a real-world timed technical interview in " + targetCareerTitle + " to evaluate communication.")
                    .category(ActionCategory.INTERVIEW)
                    .priority(GapPriority.HIGH)
                    .reasoning("Evaluates conceptual clarity, problem articulation, and provides authoritative interview scoring.")
                    .estimatedImpactPoints(15)
                    .actionUrl("/mock-interview")
                    .completed(false)
                    .build());
        } else {
            // Suggest skill portfolio sync / update
            actions.add(CareerActionDto.builder()
                    .id("ACT_SKILL_1")
                    .title("Verify Additional Portfolio Skills")
                    .description("Review and add emerging technologies to your verified skill portfolio.")
                    .category(ActionCategory.SKILL)
                    .priority(GapPriority.MEDIUM)
                    .reasoning("Ensures full credit in portfolio coverage scoring for newly acquired skills.")
                    .estimatedImpactPoints(8)
                    .actionUrl("/skills")
                    .completed(false)
                    .build());
        }

        // Cap to top 3-5 actions
        return actions.stream().limit(5).collect(Collectors.toList());
    }

    private CareerIntelligenceDto buildAnonymousIntelligence() {
        Map<DimensionType, CareerDimensionScoreDto> dims = new LinkedHashMap<>();
        for (DimensionType type : DimensionType.values()) {
            dims.put(type, CareerDimensionScoreDto.builder()
                    .dimension(type)
                    .displayName(type.name())
                    .score(0)
                    .weightPercentage(0)
                    .weightedContribution(0.0)
                    .attempted(false)
                    .statusDescription("Unauthenticated")
                    .detailMetric("N/A")
                    .build());
        }

        return CareerIntelligenceDto.builder()
                .userId(null)
                .userName("Guest")
                .targetCareer("Java Backend Developer")
                .userLevel("Beginner")
                .overallReadinessScore(0)
                .readinessState(ReadinessState.BEGINNER)
                .readinessStateDisplayName(ReadinessState.BEGINNER.getDisplayName())
                .summary("Please sign in to view your personalized Career Intelligence.")
                .dimensionScores(dims)
                .strongestDimensions(Collections.emptyList())
                .weakestDimensions(Collections.emptyList())
                .skillGaps(Collections.emptyList())
                .prioritizedActions(Collections.emptyList())
                .calculatedAt(Instant.now())
                .build();
    }
}
