package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.MockInterviewRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final CareerService careerService;
    private final RoadmapService roadmapService;
    private final UserSkillService userSkillService;
    private final QuizService quizService;
    private final UserProgressService userProgressService;
    private final ProblemService problemService;
    private final MockInterviewRepository interviewRepository;
    private final com.careeradvisor.backend.repository.ResumeRepository resumeRepository;

    public RecommendationService(CareerService careerService,
                                  RoadmapService roadmapService,
                                  UserSkillService userSkillService,
                                  QuizService quizService,
                                  UserProgressService userProgressService,
                                  ProblemService problemService,
                                  MockInterviewRepository interviewRepository,
                                  com.careeradvisor.backend.repository.ResumeRepository resumeRepository) {
        this.careerService = careerService;
        this.roadmapService = roadmapService;
        this.userSkillService = userSkillService;
        this.quizService = quizService;
        this.userProgressService = userProgressService;
        this.problemService = problemService;
        this.interviewRepository = interviewRepository;
        this.resumeRepository = resumeRepository;
    }

    public PersonalizedIntelligenceResponse getPersonalizedIntelligence(User user) {
        if (user == null) {
            return buildAnonymousIntelligence();
        }

        List<String> userSkills = userSkillService.getUserSkills(user);
        String targetGoal = user.getCareerGoal() != null && !user.getCareerGoal().trim().isEmpty()
                ? user.getCareerGoal() : "Java Backend Developer";

        QuizAttempt latestQuiz = quizService.getLatestAttempt(user).orElse(null);
        ProgressSummaryResponse roadmapSummary = userProgressService.getProgressSummary(user);
        ProblemProgressSummaryDto practiceSummary = problemService.getProgressSummary(user);
        List<CareerDto> allCareers = careerService.getAllCareers();
        List<CodingProblemDto> allProblems = problemService.getProblems(null, null, null, user);

        MockInterview latestInterview = interviewRepository.findFirstByUserAndStatusOrderByStartedAtDesc(user, InterviewStatus.COMPLETED)
                .orElse(null);

        boolean isNewUser = userSkills.isEmpty()
                && latestQuiz == null
                && latestInterview == null
                && (roadmapSummary == null || roadmapSummary.getCompletedStepsCount() == 0)
                && (practiceSummary == null || practiceSummary.getSolvedProblems() == 0);

        if (isNewUser) {
            return buildNewUserIntelligence(user, targetGoal);
        }

        // 1. Career Matches
        List<CareerMatchDto> careerMatches = calculateCareerMatches(allCareers, userSkills, targetGoal, latestQuiz);
        CareerDto targetCareer = allCareers.stream()
                .filter(c -> c.getTitle().equalsIgnoreCase(targetGoal))
                .findFirst()
                .orElse(allCareers.get(0));

        // 2. Skill Gap Analysis
        SkillGapDto skillGaps = calculateSkillGaps(targetCareer, userSkills);

        // 3. Next Roadmap Action
        RoadmapActionDto roadmapAction = calculateRoadmapAction(targetGoal, roadmapSummary);

        // 4. Practice / DSA Action
        PracticeActionDto practiceAction = calculatePracticeAction(allProblems, practiceSummary);

        // 5. Interview Focus Action
        InterviewFocusDto interviewAction = calculateInterviewFocus(latestQuiz, latestInterview, allProblems, targetCareer);

        // 6. Calculate Overall Readiness Score (0-100%)
        int skillMatchPct = skillGaps.getMatchPercentage();
        int roadmapPct = roadmapSummary != null ? roadmapSummary.getRoadmapPercent() : 0;
        int practicePct = practiceSummary != null ? practiceSummary.getCompletionPercentage() : 0;
        int quizPct = latestQuiz != null ? latestQuiz.getPercentage() : (user.getUserLevel() != null ? 60 : 40);
        int interviewPct = latestInterview != null ? latestInterview.getScore() : quizPct;

        int overallReadiness = Math.min(100, Math.round(
                (0.30f * skillMatchPct) +
                (0.25f * roadmapPct) +
                (0.20f * practicePct) +
                (0.15f * quizPct) +
                (0.10f * interviewPct)
        ));

        // Determine User Tier
        String userState = overallReadiness >= 75 ? "ADVANCED"
                : overallReadiness >= 40 ? "INTERMEDIATE" : "BEGINNER";

        // Headline Summary
        String headline = generateSummaryHeadline(targetGoal, roadmapAction, skillGaps, practiceAction);

        // 7. Action Plan
        List<RecommendationItemDto> actionPlan = generateActionPlan(
                user, targetGoal, skillGaps, roadmapAction, practiceAction, interviewAction, latestQuiz, userSkills);

        return PersonalizedIntelligenceResponse.builder()
                .userState(userState)
                .summaryHeadline(headline)
                .careerGoal(targetGoal)
                .overallReadinessScore(overallReadiness)
                .careerMatches(careerMatches)
                .skillGaps(skillGaps)
                .nextRoadmapAction(roadmapAction)
                .practiceAction(practiceAction)
                .interviewFocusAction(interviewAction)
                .actionPlan(actionPlan)
                .build();
    }

    private List<CareerMatchDto> calculateCareerMatches(List<CareerDto> allCareers,
                                                       List<String> userSkills,
                                                       String targetGoal,
                                                       QuizAttempt latestQuiz) {
        Set<String> normalizedSkills = userSkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<CareerMatchDto> matches = new ArrayList<>();

        for (CareerDto career : allCareers) {
            List<String> matched = new ArrayList<>();
            List<String> missing = new ArrayList<>();

            for (String required : career.getRequiredSkills()) {
                if (normalizedSkills.contains(required.toLowerCase())) {
                    matched.add(required);
                } else {
                    missing.add(required);
                }
            }

            int totalReq = career.getRequiredSkills().size();
            int skillScore = totalReq > 0 ? Math.round(((float) matched.size() / totalReq) * 100) : 0;

            boolean isTarget = career.getTitle().equalsIgnoreCase(targetGoal);
            int goalScore = isTarget ? 100 : 50;

            int quizScore = 50;
            if (latestQuiz != null) {
                if (latestQuiz.getRecommendedCareer() != null &&
                        latestQuiz.getRecommendedCareer().equalsIgnoreCase(career.getTitle())) {
                    quizScore = 95;
                } else {
                    quizScore = Math.max(30, latestQuiz.getPercentage());
                }
            }

            int matchScore = Math.min(100, Math.round(
                    (0.45f * skillScore) + (0.35f * goalScore) + (0.20f * quizScore)
            ));

            matches.add(CareerMatchDto.builder()
                    .title(career.getTitle())
                    .category(career.getCategory())
                    .matchScore(matchScore)
                    .skillScore(skillScore)
                    .goalScore(goalScore)
                    .quizScore(quizScore)
                    .matchedSkills(matched)
                    .missingSkills(missing)
                    .targetGoal(isTarget)
                    .build());
        }

        matches.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return matches;
    }

    private SkillGapDto calculateSkillGaps(CareerDto targetCareer, List<String> userSkills) {
        Set<String> userSet = userSkills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String req : targetCareer.getRequiredSkills()) {
            if (userSet.contains(req.toLowerCase())) {
                matched.add(req);
            } else {
                missing.add(req);
            }
        }

        int total = targetCareer.getRequiredSkills().size();
        int matchPct = total > 0 ? Math.round(((float) matched.size() / total) * 100) : 0;
        List<String> highPriority = missing.stream().limit(3).collect(Collectors.toList());

        return SkillGapDto.builder()
                .targetCareer(targetCareer.getTitle())
                .matchPercentage(matchPct)
                .acquiredSkills(matched)
                .missingSkills(missing)
                .highPriorityMissing(highPriority)
                .build();
    }

    private RoadmapActionDto calculateRoadmapAction(String targetGoal, ProgressSummaryResponse summary) {
        String nextTopic = "Language Syntax & Basics";
        String sectionTitle = "Stage 1: Foundations";
        String reason = "Establish foundational syntax, types, and core runtime concepts.";
        int total = 10;
        int completed = 0;
        int pct = 0;

        if (summary != null && summary.getCompletedSteps() != null) {
            completed = summary.getCompletedStepsCount();
            pct = summary.getRoadmapPercent();

            if (completed == 0) {
                nextTopic = "Language Syntax & Basics";
                sectionTitle = "Stage 1: Foundations";
                reason = "Begin by mastering basic syntax, classes, and types.";
            } else if (completed == 1) {
                nextTopic = "Object-Oriented Programming (OOP)";
                sectionTitle = "Stage 1: Foundations";
                reason = "Solidify classes, inheritance, encapsulation, and polymorphism.";
            } else if (pct < 40) {
                nextTopic = "Collections Framework & Generics";
                sectionTitle = "Stage 2: Core Engineering";
                reason = "Deepen understanding of List, Map, Set implementations and internal mechanics.";
            } else if (pct < 75) {
                nextTopic = "RESTful APIs & Database Integration";
                sectionTitle = "Stage 3: Frameworks & Persistence";
                reason = "Build production-grade REST controllers and integrate Hibernate/JPA ORM.";
            } else {
                nextTopic = "System Design & Deployment Architecture";
                sectionTitle = "Stage 4: Advanced Systems";
                reason = "Explore caching, distributed transactions, message queues, and CI/CD pipelines.";
            }
        }

        return RoadmapActionDto.builder()
                .careerTitle(targetGoal)
                .nextTopic(nextTopic)
                .sectionTitle(sectionTitle)
                .completedTopics(completed)
                .totalTopics(Math.max(1, total))
                .progressPercentage(pct)
                .reason(reason)
                .build();
    }

    private PracticeActionDto calculatePracticeAction(List<CodingProblemDto> allProblems,
                                                      ProblemProgressSummaryDto summary) {
        CodingProblemDto recommended = null;

        for (CodingProblemDto prob : allProblems) {
            if (!prob.isSolved()) {
                recommended = prob;
                break;
            }
        }

        if (recommended == null && !allProblems.isEmpty()) {
            recommended = allProblems.get(0);
        }

        String topic = recommended != null ? recommended.getTopic() : "Arrays";
        String slug = recommended != null ? recommended.getSlug() : "two-sum";
        String title = recommended != null ? recommended.getTitle() : "Two Sum";
        Difficulty diff = recommended != null ? recommended.getDifficulty() : Difficulty.EASY;

        int topicSolved = 0;
        int topicTotal = 0;

        if (summary != null && summary.getTopicStats() != null) {
            for (TopicProgressStat stat : summary.getTopicStats()) {
                if (stat.getTopic().equalsIgnoreCase(topic)) {
                    topicSolved = stat.getSolved();
                    topicTotal = stat.getTotal();
                    break;
                }
            }
        }

        int overallSolved = summary != null ? summary.getSolvedProblems() : 0;
        int overallTotal = summary != null ? summary.getTotalProblems() : allProblems.size();

        String reason = overallSolved == 0
                ? "Start building problem-solving momentum with high-frequency interview questions in " + topic + "."
                : "Practice " + topic + " to strengthen technical coding interviews. Next recommended problem: " + title + ".";

        return PracticeActionDto.builder()
                .recommendedTopic(topic)
                .problemSlug(slug)
                .problemTitle(title)
                .difficulty(diff)
                .topicSolved(topicSolved)
                .topicTotal(topicTotal)
                .overallSolved(overallSolved)
                .overallTotal(overallTotal)
                .reason(reason)
                .build();
    }

    private InterviewFocusDto calculateInterviewFocus(QuizAttempt latestQuiz,
                                                      MockInterview latestInterview,
                                                      List<CodingProblemDto> allProblems,
                                                      CareerDto targetCareer) {
        String subject = "DBMS & SQL";
        String category = "DBMS";
        String reason = "Core database indexing, normalization, and ACID properties are tested across technical interview loops.";
        List<String> topics = List.of("Transactions & ACID", "Indexing & Query Optimization", "Normalization & Normal Forms");
        String problemSlug = "dbms-acid-properties-and-isolation-levels";
        int score = latestQuiz != null ? latestQuiz.getPercentage() : 70;

        if (latestInterview != null) {
            score = latestInterview.getScore();
            if (latestInterview.getWeakAreas() != null && !latestInterview.getWeakAreas().isEmpty()) {
                subject = latestInterview.getCategory().name() + " Revision";
                category = latestInterview.getCategory().name();
                reason = "Your recent mock interview identified weak areas: " + latestInterview.getWeakAreas() + ". Prioritize these conceptual topics.";
                topics = Arrays.asList(latestInterview.getWeakAreas().split(", "));
            }
        } else if (targetCareer.getTitle().toLowerCase().contains("java")) {
            subject = "Core Java & Concurrency";
            category = "JAVA";
            reason = "Mastering Java Memory Model, HashMap internal working, and thread lifecycles is critical for backend engineering interviews.";
            topics = List.of("HashMap Internal Architecture", "Multithreading & Synchronization", "JVM Garbage Collection");
            problemSlug = "java-hashmap-internal-working";
        } else if (targetCareer.getTitle().toLowerCase().contains("frontend")) {
            subject = "JavaScript & React Internals";
            category = "DSA";
            reason = "Strengthen DOM reconciliation, asynchronous event loops, and state management lifecycle patterns.";
            topics = List.of("Event Loop & Microtasks", "React Fiber & Reconciliation", "Closures & Prototypes");
            problemSlug = "longest-substring-without-repeating-characters";
        }

        return InterviewFocusDto.builder()
                .subject(subject)
                .category(category)
                .reason(reason)
                .scoreOrAccuracy(score)
                .keyTopicsToReview(topics)
                .suggestedPracticeProblemSlug(problemSlug)
                .build();
    }

    private String generateSummaryHeadline(String targetGoal,
                                           RoadmapActionDto roadmapAction,
                                           SkillGapDto skillGaps,
                                           PracticeActionDto practiceAction) {
        String missingFirst = !skillGaps.getHighPriorityMissing().isEmpty()
                ? skillGaps.getHighPriorityMissing().get(0) : "Core Stack";

        return "Focus on " + missingFirst + " and " + practiceAction.getProblemTitle() +
                " to accelerate your " + targetGoal + " interview readiness.";
    }

    private List<RecommendationItemDto> generateActionPlan(User user,
                                                          String targetGoal,
                                                          SkillGapDto skillGaps,
                                                          RoadmapActionDto roadmapAction,
                                                          PracticeActionDto practiceAction,
                                                          InterviewFocusDto interviewAction,
                                                          QuizAttempt latestQuiz,
                                                          List<String> userSkills) {
        List<RecommendationItemDto> items = new ArrayList<>();

        // 1. Roadmap Step
        items.add(RecommendationItemDto.builder()
                .id("roadmap-" + UUID.randomUUID().toString().substring(0, 8))
                .priority("HIGH")
                .type("ROADMAP")
                .title("Next Roadmap Milestone: " + roadmapAction.getNextTopic())
                .description("Advance your " + targetGoal + " curriculum by completing '" + roadmapAction.getSectionTitle() + "'.")
                .actionUrl("/roadmap")
                .actionText("Open Roadmap")
                .score(roadmapAction.getProgressPercentage())
                .build());

        // 2. Skill Acquisition
        if (!skillGaps.getHighPriorityMissing().isEmpty()) {
            items.add(RecommendationItemDto.builder()
                    .id("skill-" + UUID.randomUUID().toString().substring(0, 8))
                    .priority("HIGH")
                    .type("SKILL_GAP")
                    .title("Acquire Missing Skill: " + skillGaps.getHighPriorityMissing().get(0))
                    .description("Bridging this prerequisite gap increases your " + targetGoal + " match to " +
                            Math.min(100, skillGaps.getMatchPercentage() + 15) + "%.")
                    .actionUrl("/skills")
                    .actionText("Add to Skills")
                    .score(skillGaps.getMatchPercentage())
                    .build());
        }

        // 3. Resume Integration Item
        Optional<Resume> latestResume = resumeRepository.findFirstByUserOrderByUploadTimestampDesc(user);
        if (latestResume.isPresent()) {
            Resume r = latestResume.get();
            Set<String> userSkillSet = userSkills.stream().map(String::toLowerCase).collect(Collectors.toSet());
            long unsyncedCount = r.getExtractedSkills() != null
                    ? r.getExtractedSkills().stream().filter(s -> !userSkillSet.contains(s.toLowerCase())).count()
                    : 0;

            if (unsyncedCount > 0) {
                items.add(RecommendationItemDto.builder()
                        .id("resume-sync-" + UUID.randomUUID().toString().substring(0, 8))
                        .priority("HIGH")
                        .type("RESUME_SYNC")
                        .title("Sync " + unsyncedCount + " Detected Skills from Resume")
                        .description("You have " + unsyncedCount + " skills detected in your resume that are not yet in your profile.")
                        .actionUrl("/resume")
                        .actionText("Sync Skills 📄")
                        .score((int) unsyncedCount)
                        .build());
            }
        } else {
            items.add(RecommendationItemDto.builder()
                    .id("resume-upload-" + UUID.randomUUID().toString().substring(0, 8))
                    .priority("MEDIUM")
                    .type("RESUME")
                    .title("Upload Resume for Instant Analysis")
                    .description("Extract technical skills, analyze skill gaps, and match with industry career tracks.")
                    .actionUrl("/resume")
                    .actionText("Upload Resume 📄")
                    .score(0)
                    .build());
        }

        // 4. Coding Problem
        items.add(RecommendationItemDto.builder()
                .id("practice-" + UUID.randomUUID().toString().substring(0, 8))
                .priority("MEDIUM")
                .type("PRACTICE")
                .title("Solve Challenge: " + practiceAction.getProblemTitle())
                .description("Practice " + practiceAction.getRecommendedTopic() + " (" + practiceAction.getDifficulty() + ") to strengthen coding interviews.")
                .actionUrl("/practice/" + practiceAction.getProblemSlug())
                .actionText("Practice in IDE 💻")
                .score(practiceAction.getOverallSolved())
                .build());

        // 5. Interview Preparation
        items.add(RecommendationItemDto.builder()
                .id("interview-" + UUID.randomUUID().toString().substring(0, 8))
                .priority("MEDIUM")
                .type("INTERVIEW")
                .title("Interview Revision: " + interviewAction.getSubject())
                .description(interviewAction.getReason())
                .actionUrl("/mock-interview")
                .actionText("Start Mock Interview")
                .score(interviewAction.getScoreOrAccuracy())
                .build());

        return items;
    }

    private PersonalizedIntelligenceResponse buildAnonymousIntelligence() {
        return PersonalizedIntelligenceResponse.builder()
                .userState("ANONYMOUS")
                .summaryHeadline("Sign in or take the Career Assessment Quiz to generate your personalized learning plan.")
                .careerGoal("Java Backend Developer")
                .overallReadinessScore(0)
                .careerMatches(Collections.emptyList())
                .skillGaps(SkillGapDto.builder()
                        .targetCareer("Java Backend Developer")
                        .acquiredSkills(Collections.emptyList())
                        .missingSkills(List.of("Java", "Spring Boot", "MySQL", "REST APIs"))
                        .highPriorityMissing(List.of("Java", "Spring Boot"))
                        .matchPercentage(0)
                        .build())
                .nextRoadmapAction(RoadmapActionDto.builder()
                        .careerTitle("Java Backend Developer")
                        .nextTopic("Start Backend Path")
                        .sectionTitle("Introduction")
                        .reason("Select your target career to generate your milestone roadmap.")
                        .totalTopics(10)
                        .completedTopics(0)
                        .progressPercentage(0)
                        .build())
                .practiceAction(PracticeActionDto.builder()
                        .recommendedTopic("Arrays & Hashing")
                        .problemSlug("two-sum")
                        .problemTitle("Two Sum")
                        .difficulty(Difficulty.EASY)
                        .topicSolved(0)
                        .topicTotal(5)
                        .overallSolved(0)
                        .overallTotal(22)
                        .reason("Start by practicing fundamental Two Sum array problem.")
                        .build())
                .interviewFocusAction(InterviewFocusDto.builder()
                        .subject("Core Technical Concepts")
                        .category("JAVA")
                        .reason("Brush up on Core Java, OOP principles, and basic SQL.")
                        .scoreOrAccuracy(0)
                        .keyTopicsToReview(List.of("Java Basics", "OOP Principles", "Basic SQL"))
                        .suggestedPracticeProblemSlug("two-sum")
                        .build())
                .actionPlan(List.of(
                        RecommendationItemDto.builder()
                                .id("anon-quiz")
                                .priority("HIGH")
                                .type("ONBOARDING")
                                .title("Take Career Assessment Quiz")
                                .description("Evaluate your baseline knowledge to calibrate career matches and skill recommendations.")
                                .actionUrl("/quiz")
                                .actionText("Take Quiz")
                                .score(0)
                                .build()
                ))
                .build();
    }

    private PersonalizedIntelligenceResponse buildNewUserIntelligence(User user, String targetGoal) {
        return PersonalizedIntelligenceResponse.builder()
                .userState("ONBOARDING")
                .summaryHeadline("Welcome " + (user.getName() != null ? user.getName() : "Explorer") +
                        "! Complete these 4 setup steps to generate your personalized career plan.")
                .careerGoal(targetGoal)
                .overallReadinessScore(0)
                .careerMatches(Collections.emptyList())
                .skillGaps(SkillGapDto.builder()
                        .targetCareer(targetGoal)
                        .acquiredSkills(Collections.emptyList())
                        .missingSkills(List.of("Java", "Spring Boot", "MySQL", "Git"))
                        .highPriorityMissing(List.of("Java", "Spring Boot"))
                        .matchPercentage(0)
                        .build())
                .nextRoadmapAction(RoadmapActionDto.builder()
                        .careerTitle(targetGoal)
                        .nextTopic("Language Syntax & Basics")
                        .sectionTitle("Stage 1: Foundations")
                        .reason("Begin your learning journey by marking milestone steps in your roadmap.")
                        .totalTopics(10)
                        .completedTopics(0)
                        .progressPercentage(0)
                        .build())
                .practiceAction(PracticeActionDto.builder()
                        .recommendedTopic("Arrays")
                        .problemSlug("two-sum")
                        .problemTitle("Two Sum")
                        .difficulty(Difficulty.EASY)
                        .topicSolved(0)
                        .topicTotal(5)
                        .overallSolved(0)
                        .overallTotal(22)
                        .reason("Start problem-solving momentum with standard easy coding challenge.")
                        .build())
                .interviewFocusAction(InterviewFocusDto.builder()
                        .subject("Baseline Interview Preparation")
                        .category("JAVA")
                        .reason("Take your first mock technical interview to establish benchmark accuracy.")
                        .scoreOrAccuracy(0)
                        .keyTopicsToReview(List.of("Core Java", "OOP Principles", "Data Structures"))
                        .suggestedPracticeProblemSlug("two-sum")
                        .build())
                .actionPlan(List.of(
                        RecommendationItemDto.builder()
                                .id("onboard-1")
                                .priority("HIGH")
                                .type("ONBOARDING")
                                .title("Take Career Assessment Quiz")
                                .description("Calibrate your knowledge level across programming, frameworks, and architecture.")
                                .actionUrl("/quiz")
                                .actionText("Take Quiz")
                                .score(0)
                                .build(),
                        RecommendationItemDto.builder()
                                .id("onboard-2")
                                .priority("HIGH")
                                .type("ONBOARDING")
                                .title("Select Target Career Track")
                                .description("Confirm your goal to specialize your customized learning milestones.")
                                .actionUrl("/careers")
                                .actionText("Explore Tracks")
                                .score(0)
                                .build(),
                        RecommendationItemDto.builder()
                                .id("onboard-3")
                                .priority("MEDIUM")
                                .type("ONBOARDING")
                                .title("Add Your Known Skills")
                                .description("Add programming languages and frameworks you have worked with.")
                                .actionUrl("/skills")
                                .actionText("Add Skills")
                                .score(0)
                                .build(),
                        RecommendationItemDto.builder()
                                .id("onboard-4")
                                .priority("MEDIUM")
                                .type("ONBOARDING")
                                .title("Solve First Coding Challenge")
                                .description("Start with fundamental array and hashing problem-solving.")
                                .actionUrl("/practice/two-sum")
                                .actionText("Solve Problem")
                                .score(0)
                                .build(),
                        RecommendationItemDto.builder()
                                .id("onboard-5")
                                .priority("MEDIUM")
                                .type("ONBOARDING")
                                .title("Upload Resume for Skill Matching")
                                .description("Automatically extract skills and see how your experience aligns with career tracks.")
                                .actionUrl("/resume")
                                .actionText("Upload Resume")
                                .score(0)
                                .build()
                ))
                .build();
    }
}
