package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonalizedRoadmapService {

    private static final Logger logger = LoggerFactory.getLogger(PersonalizedRoadmapService.class);

    private final PersonalizedRoadmapRepository personalizedRoadmapRepository;
    private final PersonalizedRoadmapStepRepository stepRepository;
    private final PersonalizedRoadmapMilestoneRepository milestoneRepository;
    private final RoadmapProjectEvidenceRepository evidenceRepository;
    private final RoadmapRevisionRepository revisionRepository;
    private final StudentOnboardingRepository studentOnboardingRepository;
    private final CareerTrackRepository careerTrackRepository;
    private final CompetencyNodeRepository competencyNodeRepository;
    private final CompetencyPrerequisiteRepository competencyPrerequisiteRepository;
    private final TechnologyAssessmentResultRepository resultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UserRoadmapProgressRepository userRoadmapProgressRepository;
    private final RoadmapService legacyRoadmapService;

    public PersonalizedRoadmapService(PersonalizedRoadmapRepository personalizedRoadmapRepository,
                                      PersonalizedRoadmapStepRepository stepRepository,
                                      PersonalizedRoadmapMilestoneRepository milestoneRepository,
                                      RoadmapProjectEvidenceRepository evidenceRepository,
                                      RoadmapRevisionRepository revisionRepository,
                                      StudentOnboardingRepository studentOnboardingRepository,
                                      CareerTrackRepository careerTrackRepository,
                                      CompetencyNodeRepository competencyNodeRepository,
                                      CompetencyPrerequisiteRepository competencyPrerequisiteRepository,
                                      TechnologyAssessmentResultRepository resultRepository,
                                      AssessmentSessionRepository assessmentSessionRepository,
                                      UserRoadmapProgressRepository userRoadmapProgressRepository,
                                      RoadmapService legacyRoadmapService) {
        this.personalizedRoadmapRepository = personalizedRoadmapRepository;
        this.stepRepository = stepRepository;
        this.milestoneRepository = milestoneRepository;
        this.evidenceRepository = evidenceRepository;
        this.revisionRepository = revisionRepository;
        this.studentOnboardingRepository = studentOnboardingRepository;
        this.careerTrackRepository = careerTrackRepository;
        this.competencyNodeRepository = competencyNodeRepository;
        this.competencyPrerequisiteRepository = competencyPrerequisiteRepository;
        this.resultRepository = resultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.userRoadmapProgressRepository = userRoadmapProgressRepository;
        this.legacyRoadmapService = legacyRoadmapService;
    }

    /**
     * Retrieves the candidate's active personalized roadmap without unexpected mutation.
     * Returns null if no active roadmap exists.
     */
    @Transactional(readOnly = true)
    public PersonalizedRoadmapDto getPersonalizedRoadmap(User user) {
        return personalizedRoadmapRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE")
                .map(this::mapToDto)
                .orElse(null);
    }

    /**
     * Generates a tailored learning roadmap using catalog competencies, assessment calibration, and study schedule.
     */
    @Transactional
    public PersonalizedRoadmapDto generateRoadmap(User user, boolean forceRegenerate) {
        synchronized (("user-roadmap-" + user.getId()).intern()) {
            if (!forceRegenerate) {
                Optional<PersonalizedRoadmap> existing = personalizedRoadmapRepository
                        .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE");
                if (existing.isPresent()) {
                    return mapToDto(existing.get());
                }
            }

            // 1. Fetch StudentOnboarding if available
            Optional<StudentOnboarding> onboardingOpt = studentOnboardingRepository.findByUserIdWithCareerAndTech(user.getId());

            CareerTrack careerTrack = null;
            int dailyMinutes = 60;
            int studyDaysPerWeek = 5;
            LocalDate targetDate = null;
            AssessmentSession assessmentSession = null;

            if (onboardingOpt.isPresent()) {
                StudentOnboarding onboarding = onboardingOpt.get();
                careerTrack = onboarding.getSelectedCareer();
                if (onboarding.getDailyStudyMinutes() != null) {
                    dailyMinutes = Math.min(480, Math.max(15, onboarding.getDailyStudyMinutes()));
                }
                if (onboarding.getPreferredDays() != null && !onboarding.getPreferredDays().trim().isEmpty()) {
                    studyDaysPerWeek = Math.min(7, Math.max(1, onboarding.getPreferredDays().split(",").length));
                }
                targetDate = onboarding.getTargetCompletionDate();
                assessmentSession = onboarding.getAssessmentSession();
            }

            // 2. Resolve Career Track
            if (careerTrack == null) {
                String goal = user.getCareerGoal();
                if (goal != null && !goal.trim().isEmpty()) {
                    careerTrack = careerTrackRepository.findByTitleIgnoreCase(goal.trim()).orElse(null);
                }
            }

            if (careerTrack == null) {
                List<CareerTrack> allTracks = careerTrackRepository.findAllActiveWithTechnologies();
                if (!allTracks.isEmpty()) {
                    careerTrack = allTracks.get(0);
                } else {
                    throw new IllegalStateException("No active career tracks available in catalog to generate roadmap");
                }
            }

            // Fallback to student's latest submitted assessment session if onboarding didn't record one
            if (assessmentSession == null) {
                assessmentSession = assessmentSessionRepository
                        .findFirstByUserAndStatusOrderByCreatedAtDesc(user, AssessmentStatus.SUBMITTED)
                        .orElse(null);
            }

            // 3. Build diagnostic assessment calibration map
            Map<String, TechnologyAssessmentResult> assessmentMap = new HashMap<>();
            if (assessmentSession != null) {
                List<TechnologyAssessmentResult> results = resultRepository.findBySessionIdWithTechnology(assessmentSession.getId());
                for (TechnologyAssessmentResult r : results) {
                    if (r.getTechnology() != null) {
                        assessmentMap.put(r.getTechnology().getSlug(), r);
                    }
                }
            }

            // 4. Construct Milestones and Steps from Career Track Technologies
            PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                    .user(user)
                    .careerTrack(careerTrack)
                    .careerTitle(careerTrack.getTitle())
                    .careerSlug(careerTrack.getSlug())
                    .dailyStudyMinutes(dailyMinutes)
                    .status("ACTIVE")
                    .milestones(new ArrayList<>())
                    .build();

            int weeklyCapacityMinutes = Math.max(75, dailyMinutes * studyDaysPerWeek);
            int cumulativeTotalMinutes = 0;
            int cumulativeRequiredMinutes = 0;
            int milestoneSeq = 1;

            List<Technology> technologies = careerTrack.getTechnologies().stream()
                    .filter(Technology::isActive)
                    .sorted(Comparator.comparing(Technology::getName))
                    .collect(Collectors.toList());

            // If career track has no associated technologies, fallback to legacy roadmap sections
            if (technologies.isEmpty()) {
                List<RoadmapSectionDto> sections = legacyRoadmapService.getRoadmapForCareer(careerTrack.getTitle());
                for (RoadmapSectionDto sec : sections) {
                    PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                            .roadmap(roadmap)
                            .title(sec.getTitle())
                            .sequenceOrder(milestoneSeq++)
                            .isCustom(false)
                            .isSkipped(false)
                            .steps(new ArrayList<>())
                            .build();

                    int stepSeq = 1;
                    int milestoneMinutes = 0;
                    for (String stepTitle : sec.getSteps()) {
                        PersonalizedRoadmapStep step = PersonalizedRoadmapStep.builder()
                                .milestone(milestone)
                                .title(stepTitle)
                                .description("Master core competency: " + stepTitle)
                                .level("Intermediate")
                                .estimatedMinutes(60)
                                .sequenceOrder(stepSeq++)
                                .priority("CORE")
                                .evidenceStatus("NOT_ASSESSED")
                                .isCompleted(false)
                                .state("NOT_STARTED")
                                .isTestedOut(false)
                                .build();

                        milestone.addStep(step);
                        milestoneMinutes += 60;
                    }

                    cumulativeTotalMinutes += milestoneMinutes;
                    cumulativeRequiredMinutes += milestoneMinutes;
                    int weekNum = Math.max(1, (int) Math.ceil((double) cumulativeRequiredMinutes / weeklyCapacityMinutes));
                    milestone.setTargetWeekNumber(weekNum);
                    milestone.setEstimatedHours(milestoneMinutes / 60.0);
                    milestone.setStatus("UPCOMING");
                    roadmap.addMilestone(milestone);
                }
            } else {
                for (Technology tech : technologies) {
                    List<CompetencyNode> nodes = competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(tech);
                    List<CompetencyNode> sortedNodes = sortCompetenciesTopologically(nodes);

                    PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                            .roadmap(roadmap)
                            .title("Milestone " + milestoneSeq + ": " + tech.getName() + " Mastery")
                            .technologyName(tech.getName())
                            .technologySlug(tech.getSlug())
                            .sequenceOrder(milestoneSeq++)
                            .isCustom(false)
                            .isSkipped(false)
                            .steps(new ArrayList<>())
                            .build();

                    TechnologyAssessmentResult assessment = assessmentMap.get(tech.getSlug());
                    Set<String> strengths = assessment != null ? parseJsonArray(assessment.getStrengthsJson()) : Collections.emptySet();
                    Set<String> weaknesses = assessment != null ? parseJsonArray(assessment.getWeaknessesJson()) : Collections.emptySet();
                    Set<String> coveredTopics = assessment != null ? parseJsonArray(assessment.getTopicCoverageJson()) : Collections.emptySet();

                    int milestoneTotalMinutes = 0;
                    int milestoneRequiredMinutes = 0;
                    int stepSeq = 1;

                    if (sortedNodes.isEmpty()) {
                        List<String> defaultSteps = List.of(
                                tech.getName() + " Fundamentals & Setup",
                                tech.getName() + " Core Principles & Practice",
                                tech.getName() + " Real-World Project Application"
                        );
                        for (String ds : defaultSteps) {
                            String priority = assessment != null ? "CORE" : "RECOMMENDED";
                            String evidenceStatus = assessment != null ? "ASSESSED" : "NOT_ASSESSED";
                            PersonalizedRoadmapStep step = PersonalizedRoadmapStep.builder()
                                    .milestone(milestone)
                                    .title(ds)
                                    .description("Build foundational competency in " + tech.getName())
                                    .level("Intermediate")
                                    .estimatedMinutes(60)
                                    .sequenceOrder(stepSeq++)
                                    .priority(priority)
                                    .evidenceStatus(evidenceStatus)
                                    .isCompleted(false)
                                    .state("NOT_STARTED")
                                    .isTestedOut(false)
                                    .assessmentScore(null)
                                    .build();
                            milestone.addStep(step);
                            milestoneTotalMinutes += 60;
                            milestoneRequiredMinutes += 60;
                        }
                    } else {
                        for (CompetencyNode node : sortedNodes) {
                            boolean isTestedOut = false;
                            String priority = "RECOMMENDED";
                            String evidenceStatus = "NOT_ASSESSED";
                            Integer score = null;

                            if (assessment != null) {
                                if (assessment.getEvaluatedQuestionsCount() < 2 ||
                                    "Low".equalsIgnoreCase(assessment.getConfidenceLevel()) ||
                                    "INSUFFICIENT_EVIDENCE".equalsIgnoreCase(assessment.getEstimatedProficiency())) {
                                    evidenceStatus = "INSUFFICIENT_EVIDENCE";
                                    priority = "CORE";
                                    score = null;
                                } else {
                                    boolean hasTopicCoverage = !coveredTopics.isEmpty();
                                    boolean isSpecificWeakness = matchesTopic(node, weaknesses);
                                    boolean isSpecificStrength = matchesTopic(node, strengths);
                                    boolean isCovered = matchesTopic(node, coveredTopics);

                                    if (isSpecificWeakness) {
                                        evidenceStatus = "ASSESSED";
                                        priority = "HIGH_PRIORITY";
                                        score = 0;
                                        isTestedOut = false;
                                    } else if (isSpecificStrength) {
                                        evidenceStatus = "ASSESSED";
                                        score = assessment.getRawScore();
                                        if ("Beginner".equalsIgnoreCase(node.getLevel()) && assessment.getRawScore() >= 75) {
                                            isTestedOut = true;
                                            priority = "TESTED_OUT";
                                        } else {
                                            priority = "CORE";
                                        }
                                    } else if (hasTopicCoverage && !isCovered) {
                                        evidenceStatus = "NOT_ASSESSED";
                                        priority = "CORE";
                                        score = null;
                                        isTestedOut = false;
                                    } else {
                                        evidenceStatus = "ASSESSED";
                                        if (assessment.getRawScore() >= 75 ||
                                            "Advanced".equalsIgnoreCase(assessment.getEstimatedProficiency()) ||
                                            "Expert".equalsIgnoreCase(assessment.getEstimatedProficiency())) {
                                            score = assessment.getRawScore();
                                            if ("Beginner".equalsIgnoreCase(node.getLevel())) {
                                                isTestedOut = true;
                                                priority = "TESTED_OUT";
                                            } else {
                                                priority = "CORE";
                                            }
                                        } else if (assessment.getRawScore() < 50) {
                                            priority = "HIGH_PRIORITY";
                                            score = assessment.getRawScore();
                                        } else {
                                            priority = "CORE";
                                            score = assessment.getRawScore();
                                        }
                                    }
                                }
                            }

                            PersonalizedRoadmapStep step = PersonalizedRoadmapStep.builder()
                                    .milestone(milestone)
                                    .title(node.getTitle())
                                    .description(node.getDescription())
                                    .competencyKey(node.getCompetencyKey())
                                    .level(node.getLevel() != null ? node.getLevel() : "Intermediate")
                                    .estimatedMinutes(60)
                                    .sequenceOrder(stepSeq++)
                                    .priority(priority)
                                    .evidenceStatus(evidenceStatus)
                                    .isCompleted(false)
                                    .state("NOT_STARTED")
                                    .completedAt(null)
                                    .isTestedOut(isTestedOut)
                                    .assessmentScore(score)
                                    .build();

                            milestone.addStep(step);
                            milestoneTotalMinutes += 60;
                            if (!isTestedOut) {
                                milestoneRequiredMinutes += 60;
                            }
                        }
                    }

                    cumulativeTotalMinutes += milestoneTotalMinutes;
                    cumulativeRequiredMinutes += milestoneRequiredMinutes;

                    int weekNum = Math.max(1, (int) Math.ceil((double) Math.max(milestoneRequiredMinutes, cumulativeRequiredMinutes) / weeklyCapacityMinutes));
                    milestone.setTargetWeekNumber(weekNum);
                    milestone.setEstimatedHours(milestoneTotalMinutes / 60.0);
                    milestone.setStatus("UPCOMING");
                    roadmap.addMilestone(milestone);
                }
            }

            // Set first milestone to IN_PROGRESS
            if (!roadmap.getMilestones().isEmpty()) {
                roadmap.getMilestones().get(0).setStatus("IN_PROGRESS");
            }

            // 5. Compute roadmap totals and progress
            int totalSteps = 0;
            int completedSteps = 0;
            for (PersonalizedRoadmapMilestone m : roadmap.getMilestones()) {
                totalSteps += m.getSteps().size();
                completedSteps += (int) m.getSteps().stream().filter(PersonalizedRoadmapStep::isCompleted).count();
            }

            roadmap.setTotalStepsCount(totalSteps);
            roadmap.setCompletedStepsCount(completedSteps);
            roadmap.setProgressPercentage(totalSteps > 0 ? (completedSteps * 100) / totalSteps : 0);
            roadmap.setTotalEstimatedHours(cumulativeRequiredMinutes / 60.0);

            int totalWeeks = Math.max(1, (int) Math.ceil((double) cumulativeRequiredMinutes / weeklyCapacityMinutes));
            LocalDate realisticDate = LocalDate.now().plusWeeks(totalWeeks);
            roadmap.setProjectedCompletionDate(realisticDate);

            if (targetDate == null) {
                roadmap.setTargetCompletionDate(realisticDate);
                roadmap.setIsTargetDateRealistic(true);
            } else {
                roadmap.setTargetCompletionDate(targetDate);
                if (targetDate.isBefore(LocalDate.now()) || targetDate.isBefore(realisticDate)) {
                    roadmap.setIsTargetDateRealistic(false);
                } else {
                    roadmap.setIsTargetDateRealistic(true);
                }
            }

            // Safely archive existing active roadmaps only on successful generation
            if (forceRegenerate) {
                List<PersonalizedRoadmap> activeRoadmaps = personalizedRoadmapRepository.findActiveRoadmapsForUser(user);
                for (PersonalizedRoadmap r : activeRoadmaps) {
                    r.setStatus("ARCHIVED");
                    personalizedRoadmapRepository.save(r);
                }
            }

            roadmap = personalizedRoadmapRepository.save(roadmap);

            // 6. Synchronize any already-completed steps into user_roadmap_progress
            for (PersonalizedRoadmapMilestone m : roadmap.getMilestones()) {
                for (PersonalizedRoadmapStep step : m.getSteps()) {
                    if (step.isCompleted()) {
                        if (!userRoadmapProgressRepository.existsByUserAndCareerTitleAndStepTitle(user, careerTrack.getTitle(), step.getTitle())) {
                            userRoadmapProgressRepository.save(new UserRoadmapProgress(user, careerTrack.getTitle(), step.getTitle()));
                        }
                    }
                }
            }

            // 7. Update onboarding state
            if (onboardingOpt.isPresent()) {
                StudentOnboarding onboarding = onboardingOpt.get();
                onboarding.setHasPersonalizedRoadmap(true);
                onboarding.setState(OnboardingState.ROADMAP_GENERATED);
                studentOnboardingRepository.save(onboarding);
            }

            recordRevision(roadmap, "ROADMAP_GENERATED", null, null,
                    "Generated personalized roadmap for " + roadmap.getCareerTitle(),
                    null, "milestones=" + roadmap.getMilestones().size(), user.getEmail());

            logger.info("Generated personalized roadmap #{} for user {} with {} milestones and {} steps ({} tested out)",
                    roadmap.getId(), user.getEmail(), roadmap.getMilestones().size(), totalSteps, completedSteps);

            return mapToDto(roadmap);
        }
    }

    /**
     * Toggles completion status of a personalized learning step and recalculates progress.
     */
    @Transactional
    public PersonalizedRoadmapDto toggleStep(User user, Long stepId) {
        PersonalizedRoadmapStep step = stepRepository.findByIdWithMilestoneAndRoadmap(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap step not found: " + stepId));

        PersonalizedRoadmap roadmap = step.getMilestone().getRoadmap();
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User does not own this roadmap step");
        }

        boolean newStatus = !step.isCompleted();
        step.setCompleted(newStatus);
        step.setState(newStatus ? "COMPLETED" : "NOT_STARTED");
        step.setCompletedAt(newStatus ? LocalDateTime.now() : null);
        if (!newStatus && step.isTestedOut()) {
            step.setTestedOut(false);
        }
        stepRepository.save(step);

        recalculateMilestoneAndRoadmapProgress(roadmap, step.getMilestone());

        if (newStatus) {
            if (!userRoadmapProgressRepository.existsByUserAndCareerTitleAndStepTitle(user, roadmap.getCareerTitle(), step.getTitle())) {
                userRoadmapProgressRepository.save(new UserRoadmapProgress(user, roadmap.getCareerTitle(), step.getTitle()));
            }
        } else {
            userRoadmapProgressRepository.findByUserAndCareerTitleAndStepTitle(user, roadmap.getCareerTitle(), step.getTitle())
                    .ifPresent(userRoadmapProgressRepository::delete);
        }

        recordRevision(roadmap, "STEP_TOGGLED", step.getMilestone().getId(), step.getId(),
                "Toggled step '" + step.getTitle() + "' to " + (newStatus ? "COMPLETED" : "NOT_STARTED"),
                !newStatus ? "COMPLETED" : "NOT_STARTED", newStatus ? "COMPLETED" : "NOT_STARTED", user.getEmail());

        logger.info("User {} toggled step '{}' to {}", user.getEmail(), step.getTitle(), newStatus);

        return mapToDto(roadmap);
    }

    /**
     * Updates learning state of a step: NOT_STARTED, IN_PROGRESS, COMPLETED, or VERIFICATION_PENDING.
     */
    @Transactional
    public PersonalizedRoadmapDto updateStepState(User user, Long stepId, String newState) {
        PersonalizedRoadmapStep step = stepRepository.findByIdWithMilestoneAndRoadmap(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap step not found: " + stepId));

        PersonalizedRoadmap roadmap = step.getMilestone().getRoadmap();
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User does not own this roadmap step");
        }

        String validState = switch (newState.toUpperCase()) {
            case "NOT_STARTED" -> "NOT_STARTED";
            case "IN_PROGRESS" -> "IN_PROGRESS";
            case "COMPLETED" -> "COMPLETED";
            case "VERIFICATION_PENDING" -> "VERIFICATION_PENDING";
            default -> throw new IllegalArgumentException("Invalid step state: " + newState);
        };

        String prevState = step.getState() != null ? step.getState() : (step.isCompleted() ? "COMPLETED" : "NOT_STARTED");
        boolean isNowCompleted = "COMPLETED".equals(validState);
        step.setState(validState);
        step.setCompleted(isNowCompleted);
        step.setCompletedAt(isNowCompleted ? LocalDateTime.now() : null);
        if (!isNowCompleted && step.isTestedOut()) {
            step.setTestedOut(false);
        }
        stepRepository.save(step);

        recalculateMilestoneAndRoadmapProgress(roadmap, step.getMilestone());

        if (isNowCompleted) {
            if (!userRoadmapProgressRepository.existsByUserAndCareerTitleAndStepTitle(user, roadmap.getCareerTitle(), step.getTitle())) {
                userRoadmapProgressRepository.save(new UserRoadmapProgress(user, roadmap.getCareerTitle(), step.getTitle()));
            }
        } else {
            userRoadmapProgressRepository.findByUserAndCareerTitleAndStepTitle(user, roadmap.getCareerTitle(), step.getTitle())
                    .ifPresent(userRoadmapProgressRepository::delete);
        }

        recordRevision(roadmap, "STEP_STATE_CHANGED", step.getMilestone().getId(), step.getId(),
                "Changed step '" + step.getTitle() + "' state to " + validState,
                prevState, validState, user.getEmail());

        return mapToDto(roadmap);
    }

    /**
     * Updates priority of a learning step (CORE, HIGH_PRIORITY, RECOMMENDED, TESTED_OUT).
     */
    @Transactional
    public PersonalizedRoadmapDto updateStepPriority(User user, Long stepId, String priority) {
        PersonalizedRoadmapStep step = stepRepository.findByIdWithMilestoneAndRoadmap(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap step not found: " + stepId));

        PersonalizedRoadmap roadmap = step.getMilestone().getRoadmap();
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User does not own this roadmap step");
        }

        String validPriority = switch (priority.toUpperCase()) {
            case "CORE" -> "CORE";
            case "HIGH_PRIORITY" -> "HIGH_PRIORITY";
            case "RECOMMENDED" -> "RECOMMENDED";
            case "TESTED_OUT" -> "TESTED_OUT";
            default -> throw new IllegalArgumentException("Invalid priority: " + priority);
        };

        String prevPriority = step.getPriority();
        step.setPriority(validPriority);
        if ("TESTED_OUT".equals(validPriority)) {
            step.setTestedOut(true);
        } else if (step.isTestedOut()) {
            step.setTestedOut(false);
        }
        stepRepository.save(step);

        recalculateDynamicSchedule(roadmap);
        roadmap = personalizedRoadmapRepository.save(roadmap);

        recordRevision(roadmap, "PRIORITY_CHANGED", step.getMilestone().getId(), step.getId(),
                "Changed priority of '" + step.getTitle() + "' to " + validPriority,
                prevPriority, validPriority, user.getEmail());

        return mapToDto(roadmap);
    }

    /**
     * Adds an eligible custom milestone to the candidate's active personalized roadmap.
     */
    @Transactional
    public PersonalizedRoadmapDto addCustomMilestone(User user, AddCustomMilestoneRequest request) {
        PersonalizedRoadmap roadmap = personalizedRoadmapRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active personalized roadmap found for user"));

        int nextSeq = roadmap.getMilestones().stream()
                .mapToInt(PersonalizedRoadmapMilestone::getSequenceOrder)
                .max().orElse(0) + 1;

        double hours = request.getEstimatedHours() != null ? Math.max(0.5, Math.min(100.0, request.getEstimatedHours())) : 3.0;

        PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                .roadmap(roadmap)
                .title(request.getTitle().trim())
                .technologyName(request.getTechnologyName() != null ? request.getTechnologyName().trim() : "Custom Track")
                .technologySlug(request.getTechnologyName() != null ? request.getTechnologyName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-") : "custom")
                .sequenceOrder(nextSeq)
                .estimatedHours(hours)
                .isCustom(true)
                .isSkipped(false)
                .customNotes(request.getCustomNotes())
                .status("UPCOMING")
                .steps(new ArrayList<>())
                .build();

        List<String> titles = (request.getStepTitles() != null && !request.getStepTitles().isEmpty())
                ? request.getStepTitles()
                : List.of(request.getTitle().trim() + " Practical Application");

        int stepSeq = 1;
        for (String st : titles) {
            if (st != null && !st.trim().isEmpty()) {
                PersonalizedRoadmapStep step = PersonalizedRoadmapStep.builder()
                        .milestone(milestone)
                        .title(st.trim())
                        .description("Custom learning objective: " + st.trim())
                        .level("Intermediate")
                        .estimatedMinutes((int) ((hours / titles.size()) * 60))
                        .sequenceOrder(stepSeq++)
                        .priority("RECOMMENDED")
                        .evidenceStatus("NOT_ASSESSED")
                        .isCompleted(false)
                        .state("NOT_STARTED")
                        .isTestedOut(false)
                        .build();
                milestone.addStep(step);
            }
        }

        roadmap.addMilestone(milestone);
        recalculateDynamicSchedule(roadmap);
        roadmap = personalizedRoadmapRepository.save(roadmap);

        recordRevision(roadmap, "MILESTONE_ADDED", milestone.getId(), null,
                "Added custom milestone: " + milestone.getTitle(),
                null, milestone.getTitle(), user.getEmail());

        return mapToDto(roadmap);
    }

    /**
     * Edits permitted fields of a milestone (title if custom, estimated hours, custom notes).
     */
    @Transactional
    public PersonalizedRoadmapDto updateMilestone(User user, Long milestoneId, UpdateMilestoneRequest request) {
        PersonalizedRoadmapMilestone milestone = milestoneRepository.findByIdWithRoadmap(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

        PersonalizedRoadmap roadmap = milestone.getRoadmap();
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User does not own this milestone");
        }

        String prevSummary = "title=" + milestone.getTitle() + ", hours=" + milestone.getEstimatedHours();

        if (milestone.isCustom() && request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            milestone.setTitle(request.getTitle().trim());
        }

        if (request.getEstimatedHours() != null) {
            milestone.setEstimatedHours(Math.max(0.5, Math.min(100.0, request.getEstimatedHours())));
        }

        if (request.getCustomNotes() != null) {
            milestone.setCustomNotes(request.getCustomNotes());
        }

        if (request.getIsSkipped() != null) {
            return skipMilestone(user, milestoneId, request.getIsSkipped());
        }

        milestoneRepository.save(milestone);
        recalculateDynamicSchedule(roadmap);
        roadmap = personalizedRoadmapRepository.save(roadmap);

        recordRevision(roadmap, "MILESTONE_EDITED", milestone.getId(), null,
                "Updated details for milestone: " + milestone.getTitle(),
                prevSummary, "title=" + milestone.getTitle() + ", hours=" + milestone.getEstimatedHours(), user.getEmail());

        return mapToDto(roadmap);
    }

    /**
     * Removes a user-added custom milestone. Canonical required career milestones cannot be deleted.
     */
    @Transactional
    public PersonalizedRoadmapDto deleteCustomMilestone(User user, Long milestoneId) {
        PersonalizedRoadmapMilestone milestone = milestoneRepository.findByIdWithRoadmap(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

        PersonalizedRoadmap roadmap = milestone.getRoadmap();
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User does not own this milestone");
        }

        // Canonical career requirements protection
        if (!milestone.isCustom()) {
            throw new IllegalArgumentException("Cannot delete canonical career milestone. Required competencies must remain part of the career track.");
        }

        String deletedTitle = milestone.getTitle();
        roadmap.getMilestones().remove(milestone);
        milestoneRepository.delete(milestone);

        int seq = 1;
        for (PersonalizedRoadmapMilestone m : roadmap.getMilestones()) {
            m.setSequenceOrder(seq++);
        }

        recalculateDynamicSchedule(roadmap);
        roadmap = personalizedRoadmapRepository.save(roadmap);

        recordRevision(roadmap, "MILESTONE_REMOVED", milestoneId, null,
                "Removed custom milestone: " + deletedTitle,
                deletedTitle, null, user.getEmail());

        return mapToDto(roadmap);
    }

    /**
     * Skips or restores an eligible milestone, recalculating workload and checking prerequisite dependencies.
     */
    @Transactional
    public PersonalizedRoadmapDto skipMilestone(User user, Long milestoneId, boolean skip) {
        PersonalizedRoadmapMilestone milestone = milestoneRepository.findByIdWithRoadmap(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

        PersonalizedRoadmap roadmap = milestone.getRoadmap();
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User does not own this milestone");
        }

        if (skip) {
            validatePrerequisiteDependentsForSkip(roadmap, milestone);
            milestone.setSkipped(true);
            milestone.setStatus("SKIPPED");
        } else {
            milestone.setSkipped(false);
            boolean allDone = milestone.getSteps().stream().allMatch(PersonalizedRoadmapStep::isCompleted);
            boolean anyDone = milestone.getSteps().stream().anyMatch(PersonalizedRoadmapStep::isCompleted);
            if (allDone && !milestone.getSteps().isEmpty()) {
                milestone.setStatus("COMPLETED");
            } else if (anyDone) {
                milestone.setStatus("IN_PROGRESS");
            } else {
                milestone.setStatus("UPCOMING");
            }
        }

        milestoneRepository.save(milestone);
        recalculateDynamicSchedule(roadmap);
        roadmap = personalizedRoadmapRepository.save(roadmap);

        recordRevision(roadmap, skip ? "MILESTONE_SKIPPED" : "MILESTONE_RESTORED", milestone.getId(), null,
                (skip ? "Skipped" : "Restored") + " milestone: " + milestone.getTitle(),
                skip ? "ACTIVE" : "SKIPPED", skip ? "SKIPPED" : "ACTIVE", user.getEmail());

        return mapToDto(roadmap);
    }

    private void validatePrerequisiteDependentsForSkip(PersonalizedRoadmap roadmap, PersonalizedRoadmapMilestone milestoneToSkip) {
        if (milestoneToSkip.getTechnologySlug() == null) return;

        List<CompetencyNode> nodesInSkipped = competencyNodeRepository.findByTechnologySlug(milestoneToSkip.getTechnologySlug());
        Set<Long> nodeIds = nodesInSkipped.stream().map(CompetencyNode::getId).collect(Collectors.toSet());

        for (PersonalizedRoadmapMilestone other : roadmap.getMilestones()) {
            if (other.getId().equals(milestoneToSkip.getId()) || other.isSkipped() || other.getTechnologySlug() == null) {
                continue;
            }
            if (other.getSequenceOrder() > milestoneToSkip.getSequenceOrder()) {
                List<CompetencyNode> otherNodes = competencyNodeRepository.findByTechnologySlug(other.getTechnologySlug());
                for (CompetencyNode on : otherNodes) {
                    List<CompetencyPrerequisite> prereqs = competencyPrerequisiteRepository.findByCompetency(on);
                    for (CompetencyPrerequisite cp : prereqs) {
                        if (cp.getPrerequisite() != null && nodeIds.contains(cp.getPrerequisite().getId())) {
                            logger.warn("Prerequisite advisory: upcoming milestone '{}' depends on skipped milestone '{}'",
                                    other.getTitle(), milestoneToSkip.getTitle());
                        }
                    }
                }
            }
        }
    }

    /**
     * Reorders milestones in a personalized roadmap while verifying all prerequisite constraints.
     * Rejects invalid sequences that violate competency dependencies.
     */
    @Transactional
    public PersonalizedRoadmapDto reorderMilestones(User user, List<Long> milestoneIds) {
        PersonalizedRoadmap roadmap = personalizedRoadmapRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active personalized roadmap found for user"));

        if (milestoneIds == null || milestoneIds.size() != roadmap.getMilestones().size()) {
            throw new IllegalArgumentException("Reorder request must include all " + roadmap.getMilestones().size() + " milestone IDs");
        }

        Map<Long, PersonalizedRoadmapMilestone> milestoneMap = roadmap.getMilestones().stream()
                .collect(Collectors.toMap(PersonalizedRoadmapMilestone::getId, m -> m));

        for (Long id : milestoneIds) {
            if (!milestoneMap.containsKey(id)) {
                throw new IllegalArgumentException("Milestone ID " + id + " does not belong to active roadmap");
            }
        }

        // PREREQUISITE SAFEGUARD: Ensure no dependency cycle or reverse prerequisite ordering
        for (int i = 0; i < milestoneIds.size(); i++) {
            PersonalizedRoadmapMilestone earlierMilestone = milestoneMap.get(milestoneIds.get(i));
            if (earlierMilestone.getTechnologySlug() == null) continue;

            List<CompetencyNode> earlierNodes = competencyNodeRepository.findByTechnologySlug(earlierMilestone.getTechnologySlug());

            for (int j = i + 1; j < milestoneIds.size(); j++) {
                PersonalizedRoadmapMilestone laterMilestone = milestoneMap.get(milestoneIds.get(j));
                if (laterMilestone.getTechnologySlug() == null) continue;

                List<CompetencyNode> laterNodes = competencyNodeRepository.findByTechnologySlug(laterMilestone.getTechnologySlug());
                Set<Long> laterNodeIds = laterNodes.stream().map(CompetencyNode::getId).collect(Collectors.toSet());

                for (CompetencyNode en : earlierNodes) {
                    List<CompetencyPrerequisite> prereqs = competencyPrerequisiteRepository.findByCompetency(en);
                    for (CompetencyPrerequisite cp : prereqs) {
                        if (cp.getPrerequisite() != null && laterNodeIds.contains(cp.getPrerequisite().getId())) {
                            throw new IllegalArgumentException(
                                    "Prerequisite violation: Milestone '" + earlierMilestone.getTitle() +
                                    "' requires '" + cp.getPrerequisite().getTitle() + "' in '" +
                                    laterMilestone.getTitle() + "', which is placed after it in sequence.");
                        }
                    }
                }
            }
        }

        for (int i = 0; i < milestoneIds.size(); i++) {
            PersonalizedRoadmapMilestone m = milestoneMap.get(milestoneIds.get(i));
            m.setSequenceOrder(i + 1);
        }

        roadmap.getMilestones().sort(Comparator.comparingInt(PersonalizedRoadmapMilestone::getSequenceOrder));

        recalculateDynamicSchedule(roadmap);
        roadmap = personalizedRoadmapRepository.save(roadmap);

        recordRevision(roadmap, "MILESTONES_REORDERED", null, null,
                "Reordered roadmap milestones",
                null, milestoneIds.toString(), user.getEmail());

        return mapToDto(roadmap);
    }

    /**
     * Submits project evidence for an eligible milestone or step with SSRF protection.
     */
    @Transactional
    public ProjectEvidenceDto submitEvidence(User user, Long milestoneId, SubmitEvidenceRequest request) {
        PersonalizedRoadmapMilestone milestone = milestoneRepository.findByIdWithRoadmap(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

        PersonalizedRoadmap roadmap = milestone.getRoadmap();
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User does not own this milestone");
        }

        if (request.getUrl() != null && !request.getUrl().trim().isEmpty()) {
            validateUrlSsrfSafe(request.getUrl().trim());
        }

        PersonalizedRoadmapStep step = null;
        if (request.getStepId() != null) {
            step = stepRepository.findById(request.getStepId())
                    .orElseThrow(() -> new IllegalArgumentException("Step not found: " + request.getStepId()));
            if (!step.getMilestone().getId().equals(milestone.getId())) {
                throw new IllegalArgumentException("Step does not belong to specified milestone");
            }
            if ("NOT_STARTED".equals(step.getState()) || "IN_PROGRESS".equals(step.getState())) {
                step.setState("VERIFICATION_PENDING");
                stepRepository.save(step);
            }
        }

        RoadmapProjectEvidence evidence = RoadmapProjectEvidence.builder()
                .user(user)
                .roadmap(roadmap)
                .milestone(milestone)
                .step(step)
                .evidenceType(request.getEvidenceType())
                .url(request.getUrl() != null ? request.getUrl().trim() : null)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .submissionStatus("SELF_REPORTED")
                .build();

        evidence = evidenceRepository.save(evidence);

        recordRevision(roadmap, "EVIDENCE_SUBMITTED", milestone.getId(), step != null ? step.getId() : null,
                "Submitted " + request.getEvidenceType() + " evidence for " + milestone.getTitle(),
                null, request.getUrl() != null ? request.getUrl() : "Notes attached", user.getEmail());

        return mapEvidenceToDto(evidence);
    }

    private void validateUrlSsrfSafe(String urlString) {
        try {
            URI uri = URI.create(urlString);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("Invalid URL scheme: only HTTP and HTTPS are permitted");
            }
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid URL host");
            }
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost") || lowerHost.equals("127.0.0.1") || lowerHost.equals("0.0.0.0") ||
                lowerHost.equals("::1") || lowerHost.equals("169.254.169.254") ||
                lowerHost.startsWith("10.") || lowerHost.startsWith("192.168.") ||
                lowerHost.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
                throw new IllegalArgumentException("Submitted URL destination is restricted or private");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed URL: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectEvidenceDto> getMilestoneEvidence(User user, Long milestoneId) {
        PersonalizedRoadmapMilestone milestone = milestoneRepository.findByIdWithRoadmap(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

        if (!milestone.getRoadmap().getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("User does not own this milestone");
        }

        return evidenceRepository.findByMilestoneOrderByCreatedAtDesc(milestone).stream()
                .map(this::mapEvidenceToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectEvidenceDto> getRoadmapEvidence(User user) {
        PersonalizedRoadmap roadmap = personalizedRoadmapRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active personalized roadmap found for user"));

        return evidenceRepository.findByRoadmapOrderByCreatedAtDesc(roadmap).stream()
                .map(this::mapEvidenceToDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates candidate study availability preferences and dynamically recalculates roadmap schedule.
     */
    @Transactional
    public PersonalizedRoadmapDto updateSchedule(User user, UpdateScheduleRequest request) {
        PersonalizedRoadmap roadmap = personalizedRoadmapRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active personalized roadmap found for user"));

        int dailyMinutes = roadmap.getDailyStudyMinutes() != null ? roadmap.getDailyStudyMinutes() : 60;
        if (request.getDailyStudyMinutes() != null) {
            dailyMinutes = Math.min(480, Math.max(15, request.getDailyStudyMinutes()));
            roadmap.setDailyStudyMinutes(dailyMinutes);
        }

        int studyDays = 5;
        if (request.getPreferredDays() != null && !request.getPreferredDays().trim().isEmpty()) {
            studyDays = Math.min(7, Math.max(1, request.getPreferredDays().split(",").length));
        }

        if (request.getTargetCompletionDate() != null) {
            roadmap.setTargetCompletionDate(request.getTargetCompletionDate());
        }

        recalculateDynamicSchedule(roadmap, dailyMinutes, studyDays);
        PersonalizedRoadmap savedRoadmap = personalizedRoadmapRepository.save(roadmap);

        studentOnboardingRepository.findByUserIdWithCareerAndTech(user.getId()).ifPresent(so -> {
            so.setDailyStudyMinutes(savedRoadmap.getDailyStudyMinutes());
            if (request.getPreferredDays() != null) {
                so.setPreferredDays(request.getPreferredDays());
            }
            if (request.getTargetCompletionDate() != null) {
                so.setTargetCompletionDate(request.getTargetCompletionDate());
            }
            studentOnboardingRepository.save(so);
        });

        recordRevision(savedRoadmap, "SCHEDULE_UPDATED", null, null,
                "Updated study availability to " + dailyMinutes + "m/day across " + studyDays + " days/week",
                null, "dailyMinutes=" + dailyMinutes + ", target=" + savedRoadmap.getTargetCompletionDate(), user.getEmail());

        return mapToDto(savedRoadmap);
    }

    public void recalculateDynamicSchedule(PersonalizedRoadmap roadmap) {
        int dailyMinutes = roadmap.getDailyStudyMinutes() != null ? Math.min(480, Math.max(15, roadmap.getDailyStudyMinutes())) : 60;
        int studyDays = 5;
        recalculateDynamicSchedule(roadmap, dailyMinutes, studyDays);
    }

    public void recalculateDynamicSchedule(PersonalizedRoadmap roadmap, int dailyMinutes, int studyDays) {
        int weeklyCapacityMinutes = Math.max(75, dailyMinutes * studyDays);
        int cumulativeRequiredMinutes = 0;

        for (PersonalizedRoadmapMilestone milestone : roadmap.getMilestones()) {
            if (milestone.isSkipped()) {
                milestone.setStatus("SKIPPED");
                continue;
            }

            int milestoneRequiredMinutes = 0;
            int milestoneTotalMinutes = 0;
            for (PersonalizedRoadmapStep step : milestone.getSteps()) {
                milestoneTotalMinutes += step.getEstimatedMinutes();
                if (!step.isCompleted() && !step.isTestedOut()) {
                    milestoneRequiredMinutes += step.getEstimatedMinutes();
                }
            }

            cumulativeRequiredMinutes += milestoneRequiredMinutes;
            int targetWeek = Math.max(1, (int) Math.ceil((double) Math.max(milestoneRequiredMinutes, cumulativeRequiredMinutes) / weeklyCapacityMinutes));
            milestone.setTargetWeekNumber(targetWeek);
        }

        roadmap.setTotalEstimatedHours(cumulativeRequiredMinutes / 60.0);
        int totalWeeks = Math.max(1, (int) Math.ceil((double) cumulativeRequiredMinutes / weeklyCapacityMinutes));
        LocalDate realisticDate = LocalDate.now().plusWeeks(totalWeeks);
        roadmap.setProjectedCompletionDate(realisticDate);

        LocalDate targetDate = roadmap.getTargetCompletionDate();
        if (targetDate == null) {
            roadmap.setTargetCompletionDate(realisticDate);
            roadmap.setIsTargetDateRealistic(true);
        } else {
            if (targetDate.isBefore(LocalDate.now()) || targetDate.isBefore(realisticDate)) {
                roadmap.setIsTargetDateRealistic(false);
            } else {
                roadmap.setIsTargetDateRealistic(true);
            }
        }
    }

    public void recalculateMilestoneAndRoadmapProgress(PersonalizedRoadmap roadmap, PersonalizedRoadmapMilestone milestone) {
        boolean allMilestoneStepsDone = !milestone.getSteps().isEmpty() &&
                milestone.getSteps().stream().allMatch(PersonalizedRoadmapStep::isCompleted);
        boolean anyStepStarted = milestone.getSteps().stream().anyMatch(s ->
                s.isCompleted() || "IN_PROGRESS".equalsIgnoreCase(s.getState()) || "VERIFICATION_PENDING".equalsIgnoreCase(s.getState()));

        if (allMilestoneStepsDone) {
            milestone.setStatus("COMPLETED");
        } else if (anyStepStarted) {
            milestone.setStatus("IN_PROGRESS");
        } else {
            milestone.setStatus("UPCOMING");
        }
        milestoneRepository.save(milestone);

        int total = 0;
        int completed = 0;
        for (PersonalizedRoadmapMilestone m : roadmap.getMilestones()) {
            if (!m.isSkipped()) {
                total += m.getSteps().size();
                completed += (int) m.getSteps().stream().filter(PersonalizedRoadmapStep::isCompleted).count();
            }
        }

        roadmap.setTotalStepsCount(total);
        roadmap.setCompletedStepsCount(completed);
        roadmap.setProgressPercentage(total > 0 ? (completed * 100) / total : 0);

        if (completed == total && total > 0) {
            roadmap.setStatus("COMPLETED");
        } else {
            roadmap.setStatus("ACTIVE");
        }

        recalculateDynamicSchedule(roadmap);
        personalizedRoadmapRepository.save(roadmap);
    }

    /**
     * Records a lightweight revision history record for roadmap modifications.
     */
    @Transactional
    public void recordRevision(PersonalizedRoadmap roadmap, String changeType, Long milestoneId, Long stepId,
                               String summary, String prevVal, String newVal, String actor) {
        int nextRev = revisionRepository.findFirstByRoadmapOrderByRevisionNumberDesc(roadmap)
                .map(r -> r.getRevisionNumber() + 1)
                .orElse(1);

        RoadmapRevision revision = RoadmapRevision.builder()
                .roadmap(roadmap)
                .revisionNumber(nextRev)
                .changeType(changeType)
                .affectedMilestoneId(milestoneId)
                .affectedStepId(stepId)
                .summary(summary)
                .previousValue(prevVal)
                .newValue(newVal)
                .actorEmail(actor)
                .build();

        revisionRepository.save(revision);
    }

    /**
     * Retrieves revision history for the candidate's active roadmap.
     */
    @Transactional(readOnly = true)
    public List<RoadmapRevisionDto> getRevisionHistory(User user) {
        PersonalizedRoadmap roadmap = personalizedRoadmapRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active personalized roadmap found for user"));

        return revisionRepository.findByRoadmapOrderByRevisionNumberDesc(roadmap).stream()
                .map(r -> RoadmapRevisionDto.builder()
                        .id(r.getId())
                        .revisionNumber(r.getRevisionNumber())
                        .changeType(r.getChangeType())
                        .affectedMilestoneId(r.getAffectedMilestoneId())
                        .affectedStepId(r.getAffectedStepId())
                        .summary(r.getSummary())
                        .previousValue(r.getPreviousValue())
                        .newValue(r.getNewValue())
                        .actorEmail(r.getActorEmail())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Topologically sorts competencies respecting their prerequisite dependencies.
     */
    public List<CompetencyNode> sortCompetenciesTopologically(List<CompetencyNode> nodes) {
        if (nodes == null || nodes.size() <= 1) {
            return nodes != null ? new ArrayList<>(nodes) : Collections.emptyList();
        }

        Map<Long, CompetencyNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(CompetencyNode::getId, n -> n));
        Set<Long> nodeIds = nodeMap.keySet();

        Map<Long, Integer> inDegree = new HashMap<>();
        Map<Long, List<Long>> adjacencyList = new HashMap<>();

        for (Long id : nodeIds) {
            inDegree.put(id, 0);
            adjacencyList.put(id, new ArrayList<>());
        }

        for (CompetencyNode node : nodes) {
            List<CompetencyPrerequisite> prereqs = competencyPrerequisiteRepository.findByCompetency(node);
            for (CompetencyPrerequisite cp : prereqs) {
                if (cp.getPrerequisite() != null && nodeIds.contains(cp.getPrerequisite().getId())) {
                    Long prereqId = cp.getPrerequisite().getId();
                    adjacencyList.get(prereqId).add(node.getId());
                    inDegree.put(node.getId(), inDegree.get(node.getId()) + 1);
                }
            }
        }

        PriorityQueue<CompetencyNode> queue = new PriorityQueue<>(Comparator.comparing(
                CompetencyNode::getSequenceOrder, Comparator.nullsLast(Integer::compareTo)));

        for (Long id : nodeIds) {
            if (inDegree.get(id) == 0) {
                queue.add(nodeMap.get(id));
            }
        }

        List<CompetencyNode> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            CompetencyNode current = queue.poll();
            sorted.add(current);

            for (Long neighborId : adjacencyList.get(current.getId())) {
                int remaining = inDegree.get(neighborId) - 1;
                inDegree.put(neighborId, remaining);
                if (remaining == 0) {
                    queue.add(nodeMap.get(neighborId));
                }
            }
        }

        if (sorted.size() != nodes.size()) {
            List<String> cycleNodeNames = nodes.stream()
                    .filter(n -> !sorted.contains(n))
                    .map(CompetencyNode::getTitle)
                    .collect(Collectors.toList());
            logger.error("Cycle detected in competency prerequisite graph for nodes: {}", cycleNodeNames);
            throw new IllegalStateException("Cycle detected in prerequisite graph for competencies: " + cycleNodeNames);
        }

        return sorted;
    }

    public PersonalizedRoadmapDto mapToDto(PersonalizedRoadmap roadmap) {
        int testedOutCount = 0;
        int focusAreasCount = 0;

        List<PersonalizedMilestoneDto> milestoneDtos = new ArrayList<>();
        for (PersonalizedRoadmapMilestone m : roadmap.getMilestones()) {
            List<PersonalizedStepDto> stepDtos = new ArrayList<>();
            for (PersonalizedRoadmapStep s : m.getSteps()) {
                if (s.isTestedOut()) testedOutCount++;
                if ("HIGH_PRIORITY".equalsIgnoreCase(s.getPriority())) focusAreasCount++;

                stepDtos.add(PersonalizedStepDto.builder()
                        .id(s.getId())
                        .title(s.getTitle())
                        .description(s.getDescription())
                        .competencyKey(s.getCompetencyKey())
                        .level(s.getLevel())
                        .estimatedMinutes(s.getEstimatedMinutes())
                        .sequenceOrder(s.getSequenceOrder())
                        .isCompleted(s.isCompleted())
                        .state(s.getState() != null ? s.getState() : (s.isCompleted() ? "COMPLETED" : "NOT_STARTED"))
                        .completedAt(s.getCompletedAt())
                        .isTestedOut(s.isTestedOut())
                        .priority(s.getPriority())
                        .evidenceStatus(s.getEvidenceStatus())
                        .assessmentScore(s.getAssessmentScore())
                        .build());
            }

            milestoneDtos.add(PersonalizedMilestoneDto.builder()
                    .id(m.getId())
                    .title(m.getTitle())
                    .technologyName(m.getTechnologyName())
                    .technologySlug(m.getTechnologySlug())
                    .sequenceOrder(m.getSequenceOrder())
                    .targetWeekNumber(m.getTargetWeekNumber())
                    .estimatedHours(m.getEstimatedHours())
                    .status(m.getStatus())
                    .isCustom(m.isCustom())
                    .isSkipped(m.isSkipped())
                    .customNotes(m.getCustomNotes())
                    .steps(stepDtos)
                    .build());
        }

        return PersonalizedRoadmapDto.builder()
                .id(roadmap.getId())
                .careerTitle(roadmap.getCareerTitle())
                .careerSlug(roadmap.getCareerSlug())
                .totalEstimatedHours(roadmap.getTotalEstimatedHours())
                .completedStepsCount(roadmap.getCompletedStepsCount())
                .totalStepsCount(roadmap.getTotalStepsCount())
                .progressPercentage(roadmap.getProgressPercentage())
                .dailyStudyMinutes(roadmap.getDailyStudyMinutes())
                .targetCompletionDate(roadmap.getTargetCompletionDate())
                .isTargetDateRealistic(roadmap.getIsTargetDateRealistic())
                .projectedCompletionDate(roadmap.getProjectedCompletionDate())
                .testedOutCount(testedOutCount)
                .focusAreasCount(focusAreasCount)
                .status(roadmap.getStatus())
                .milestones(milestoneDtos)
                .build();
    }

    private ProjectEvidenceDto mapEvidenceToDto(RoadmapProjectEvidence evidence) {
        return ProjectEvidenceDto.builder()
                .id(evidence.getId())
                .milestoneId(evidence.getMilestone() != null ? evidence.getMilestone().getId() : null)
                .milestoneTitle(evidence.getMilestone() != null ? evidence.getMilestone().getTitle() : null)
                .stepId(evidence.getStep() != null ? evidence.getStep().getId() : null)
                .stepTitle(evidence.getStep() != null ? evidence.getStep().getTitle() : null)
                .evidenceType(evidence.getEvidenceType() != null ? evidence.getEvidenceType().name() : null)
                .url(evidence.getUrl())
                .notes(evidence.getNotes())
                .submissionStatus(evidence.getSubmissionStatus())
                .reviewFeedback(evidence.getReviewFeedback())
                .reviewedBy(evidence.getReviewedBy())
                .reviewedAt(evidence.getReviewedAt())
                .createdAt(evidence.getCreatedAt())
                .updatedAt(evidence.getUpdatedAt())
                .build();
    }

    private Set<String> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return Collections.emptySet();
        }
        try {
            Set<String> result = new HashSet<>();
            String cleaned = json.replaceAll("[\\[\\]\"]", "");
            for (String part : cleaned.split(",")) {
                String trimmed = part.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        } catch (Exception e) {
            logger.warn("Failed to parse JSON array: {}", json, e);
            return Collections.emptySet();
        }
    }

    private boolean matchesTopic(CompetencyNode node, Set<String> topics) {
        if (node == null || topics == null || topics.isEmpty()) {
            return false;
        }
        String key = node.getCompetencyKey() != null ? node.getCompetencyKey().toLowerCase() : "";
        String title = node.getTitle() != null ? node.getTitle().toLowerCase() : "";
        String desc = node.getDescription() != null ? node.getDescription().toLowerCase() : "";

        for (String topic : topics) {
            String t = topic.toLowerCase();
            if ((!key.isEmpty() && (key.contains(t) || t.contains(key))) ||
                (!title.isEmpty() && (title.contains(t) || t.contains(title))) ||
                (!desc.isEmpty() && desc.contains(t))) {
                return true;
            }
        }
        return false;
    }
}
