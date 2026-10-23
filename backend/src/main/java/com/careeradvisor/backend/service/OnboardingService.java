package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);

    private final StudentOnboardingRepository studentOnboardingRepository;
    private final CareerTrackRepository careerTrackRepository;
    private final TechnologyRepository technologyRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UserRoadmapProgressRepository userRoadmapProgressRepository;
    private final PersonalizedRoadmapRepository personalizedRoadmapRepository;
    private final UserRepository userRepository;
    private final AssessmentService assessmentService;
    private final PlannerService plannerService;
    private final CatalogService catalogService;

    public OnboardingService(StudentOnboardingRepository studentOnboardingRepository,
                             CareerTrackRepository careerTrackRepository,
                             TechnologyRepository technologyRepository,
                             AssessmentSessionRepository assessmentSessionRepository,
                             UserRoadmapProgressRepository userRoadmapProgressRepository,
                             PersonalizedRoadmapRepository personalizedRoadmapRepository,
                             UserRepository userRepository,
                             AssessmentService assessmentService,
                             PlannerService plannerService,
                             CatalogService catalogService) {
        this.studentOnboardingRepository = studentOnboardingRepository;
        this.careerTrackRepository = careerTrackRepository;
        this.technologyRepository = technologyRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.userRoadmapProgressRepository = userRoadmapProgressRepository;
        this.personalizedRoadmapRepository = personalizedRoadmapRepository;
        this.userRepository = userRepository;
        this.assessmentService = assessmentService;
        this.plannerService = plannerService;
        this.catalogService = catalogService;
    }

    /**
     * Retrieves or initializes the onboarding record for the student and returns state DTO.
     */
    @Transactional
    public OnboardingStateDto getOnboardingState(User user) {
        StudentOnboarding onboarding = getOrCreateOnboardingEntity(user);

        // Check if user has an active personalized roadmap in the authoritative repository
        boolean hasActivePersonalizedRoadmap = personalizedRoadmapRepository != null &&
                personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE").isPresent();

        if (hasActivePersonalizedRoadmap) {
            if (!onboarding.isHasPersonalizedRoadmap()) {
                onboarding.setHasPersonalizedRoadmap(true);
                if (onboarding.getState() == OnboardingState.NOT_STARTED || onboarding.getState() == OnboardingState.ASSESSMENT_COMPLETED) {
                    onboarding.setState(OnboardingState.ROADMAP_GENERATED);
                }
                onboarding = studentOnboardingRepository.save(onboarding);
            }
        } else if (onboarding.isHasPersonalizedRoadmap()) {
            // Reconcile stale flag if no active personalized roadmap actually exists
            onboarding.setHasPersonalizedRoadmap(false);
            if (onboarding.getState() == OnboardingState.ROADMAP_GENERATED) {
                onboarding.setState(onboarding.getSelectedCareer() != null ? OnboardingState.CAREER_SELECTED : OnboardingState.NOT_STARTED);
            }
            onboarding = studentOnboardingRepository.save(onboarding);
        }

        // Synchronize assessment session status if active or expired
        if (onboarding.getAssessmentSession() != null) {
            AssessmentSession session = onboarding.getAssessmentSession();
            if (onboarding.getState() == OnboardingState.ASSESSMENT_IN_PROGRESS) {
                if (session.getStatus() == AssessmentStatus.SUBMITTED || session.getStatus() == AssessmentStatus.TIMED_OUT) {
                    onboarding.setState(OnboardingState.ASSESSMENT_COMPLETED);
                    onboarding = studentOnboardingRepository.save(onboarding);
                } else if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
                    assessmentService.getSessionState(session.getId(), user);
                    onboarding.setState(OnboardingState.ASSESSMENT_COMPLETED);
                    onboarding = studentOnboardingRepository.save(onboarding);
                }
            }
        }

        return mapToStateDto(onboarding);
    }

    /**
     * Persists career selection for the student and updates user career goal.
     */
    @Transactional
    public OnboardingStateDto selectCareer(User user, String careerSlug) {
        if (careerSlug == null || careerSlug.trim().isEmpty()) {
            throw new IllegalArgumentException("Career slug cannot be blank");
        }

        CareerTrack track = careerTrackRepository.findBySlugWithTechnologies(careerSlug.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Career track not found: " + careerSlug));

        if (!track.isActive()) {
            throw new IllegalArgumentException("Career track is inactive: " + careerSlug);
        }

        StudentOnboarding onboarding = getOrCreateOnboardingEntity(user);

        // If career changed, clear previously selected technologies
        if (onboarding.getSelectedCareer() == null || !onboarding.getSelectedCareer().getId().equals(track.getId())) {
            onboarding.getSelectedTechnologies().clear();
        }

        onboarding.setSelectedCareer(track);
        if (onboarding.getState() == OnboardingState.NOT_STARTED) {
            onboarding.setState(OnboardingState.CAREER_SELECTED);
        }

        // Sync with User entity careerGoal
        user.setCareerGoal(track.getTitle());
        userRepository.save(user);

        onboarding = studentOnboardingRepository.save(onboarding);
        logger.info("User {} selected career track '{}'", user.getEmail(), track.getTitle());

        return mapToStateDto(onboarding);
    }

    /**
     * Persists known technology selection for the student after validating they belong to chosen career.
     */
    @Transactional
    public OnboardingStateDto selectTechnologies(User user, List<String> technologySlugs) {
        StudentOnboarding onboarding = getOrCreateOnboardingEntity(user);

        if (onboarding.getSelectedCareer() == null) {
            throw new IllegalStateException("Career track must be selected before choosing technologies");
        }

        if (technologySlugs == null || technologySlugs.isEmpty()) {
            throw new IllegalArgumentException("At least one technology must be selected");
        }

        Set<String> validCareerSlugs = onboarding.getSelectedCareer().getTechnologies().stream()
                .filter(Technology::isActive)
                .map(Technology::getSlug)
                .collect(Collectors.toSet());

        Set<Technology> selectedTechnologies = new HashSet<>();
        for (String slug : technologySlugs) {
            String cleanSlug = slug.trim().toLowerCase();
            if (!validCareerSlugs.contains(cleanSlug)) {
                throw new IllegalArgumentException("Selected technology '" + slug + "' is not part of career track '"
                        + onboarding.getSelectedCareer().getTitle() + "'");
            }

            Technology tech = technologyRepository.findBySlug(cleanSlug)
                    .orElseThrow(() -> new IllegalArgumentException("Technology not found: " + slug));

            if (!tech.isActive()) {
                throw new IllegalArgumentException("Technology is inactive: " + slug);
            }

            selectedTechnologies.add(tech);
        }

        onboarding.setSelectedTechnologies(selectedTechnologies);

        if (onboarding.getState() == OnboardingState.NOT_STARTED || onboarding.getState() == OnboardingState.CAREER_SELECTED) {
            onboarding.setState(OnboardingState.TECHNOLOGIES_SELECTED);
        }

        onboarding = studentOnboardingRepository.save(onboarding);
        logger.info("User {} selected {} technologies for career '{}'",
                user.getEmail(), selectedTechnologies.size(), onboarding.getSelectedCareer().getTitle());

        return mapToStateDto(onboarding);
    }

    /**
     * Persists study availability preferences and synchronizes with PlannerPreference.
     */
    @Transactional
    public OnboardingStateDto saveStudyPreferences(User user, OnboardingStudyPreferencesRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Study preferences cannot be null");
        }

        if (request.getDailyStudyMinutes() == null || request.getDailyStudyMinutes() < 15 || request.getDailyStudyMinutes() > 480) {
            throw new IllegalArgumentException("Daily study time must be between 15 and 480 minutes");
        }

        if (request.getPreferredDays() == null || request.getPreferredDays().isEmpty()) {
            throw new IllegalArgumentException("Preferred days list cannot be empty");
        }

        StudentOnboarding onboarding = getOrCreateOnboardingEntity(user);

        StudyWindow window = request.getPreferredStudyWindow() != null ? request.getPreferredStudyWindow() : StudyWindow.FLEXIBLE;

        onboarding.setDailyStudyMinutes(request.getDailyStudyMinutes());
        onboarding.setPreferredStudyWindow(window);
        onboarding.setPreferredDays(String.join(",", request.getPreferredDays()));
        onboarding.setTargetCompletionDate(request.getTargetCompletionDate());

        if (onboarding.getState() == OnboardingState.NOT_STARTED ||
            onboarding.getState() == OnboardingState.CAREER_SELECTED ||
            onboarding.getState() == OnboardingState.TECHNOLOGIES_SELECTED) {
            onboarding.setState(OnboardingState.STUDY_PREFERENCES_CONFIGURED);
        }

        // Synchronize with PlannerService
        PlannerPreferenceDto plannerDto = PlannerPreferenceDto.builder()
                .dailyStudyMinutes(request.getDailyStudyMinutes())
                .preferredStudyWindow(window)
                .preferredDays(request.getPreferredDays())
                .weeklyGoal(onboarding.getSelectedCareer() != null ? "Master " + onboarding.getSelectedCareer().getTitle() : null)
                .build();
        plannerService.updatePreferences(user, plannerDto);

        onboarding = studentOnboardingRepository.save(onboarding);
        logger.info("User {} configured study preferences: {} mins/day", user.getEmail(), request.getDailyStudyMinutes());

        return mapToStateDto(onboarding);
    }

    /**
     * Starts or resumes an adaptive assessment session using the selected technologies.
     */
    @Transactional
    public AssessmentSessionDto startOnboardingAssessment(User user) {
        StudentOnboarding onboarding = getOrCreateOnboardingEntity(user);

        if (onboarding.getSelectedTechnologies() == null || onboarding.getSelectedTechnologies().isEmpty()) {
            throw new IllegalStateException("At least one technology must be selected before starting the assessment");
        }

        List<String> slugs = onboarding.getSelectedTechnologies().stream()
                .map(Technology::getSlug)
                .sorted()
                .collect(Collectors.toList());

        StartAssessmentRequest startReq = StartAssessmentRequest.builder()
                .technologySlugs(slugs)
                .timeLimitMinutes(20)
                .build();

        AssessmentSessionDto sessionDto = assessmentService.startAssessment(user, startReq);

        AssessmentSession session = assessmentSessionRepository.findById(sessionDto.getSessionId())
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve created assessment session"));

        onboarding.setAssessmentSession(session);
        if (onboarding.getState().ordinal() < OnboardingState.ASSESSMENT_IN_PROGRESS.ordinal()) {
            onboarding.setState(OnboardingState.ASSESSMENT_IN_PROGRESS);
        }

        studentOnboardingRepository.save(onboarding);
        logger.info("User {} started onboarding assessment session #{}", user.getEmail(), session.getId());

        return sessionDto;
    }

    /**
     * Marks the assessment report as reviewed by the student.
     */
    @Transactional
    public OnboardingStateDto reviewAssessmentReport(User user) {
        StudentOnboarding onboarding = getOrCreateOnboardingEntity(user);

        if (onboarding.getAssessmentSession() == null) {
            throw new IllegalStateException("No assessment session found to review");
        }

        AssessmentSession session = onboarding.getAssessmentSession();
        if (session.getStatus() == AssessmentStatus.IN_PROGRESS) {
            assessmentService.submitAssessment(session.getId(), user);
        }

        onboarding.setState(OnboardingState.ASSESSMENT_REVIEWED);
        onboarding = studentOnboardingRepository.save(onboarding);
        logger.info("User {} reviewed assessment report for session #{}", user.getEmail(), session.getId());

        return mapToStateDto(onboarding);
    }

    /**
     * Explicit continuation to roadmap generation.
     */
    @Transactional
    public OnboardingStateDto completeOnboarding(User user) {
        StudentOnboarding onboarding = getOrCreateOnboardingEntity(user);
        onboarding.setState(OnboardingState.ROADMAP_GENERATED);
        onboarding.setHasPersonalizedRoadmap(true);
        onboarding = studentOnboardingRepository.save(onboarding);
        logger.info("User {} completed onboarding and advanced to roadmap generation", user.getEmail());

        return mapToStateDto(onboarding);
    }

    /**
     * Resets the onboarding configuration, allowing the student to restart setup.
     */
    @Transactional
    public OnboardingStateDto resetOnboarding(User user) {
        StudentOnboarding onboarding = getOrCreateOnboardingEntity(user);
        onboarding.setSelectedTechnologies(new HashSet<>());
        onboarding.setAssessmentSession(null);
        onboarding.setState(OnboardingState.NOT_STARTED);
        onboarding.setHasPersonalizedRoadmap(false);

        onboarding = studentOnboardingRepository.save(onboarding);
        logger.info("User {} reset onboarding state", user.getEmail());

        return mapToStateDto(onboarding);
    }

    /**
     * Helper to get or initialize StudentOnboarding entity.
     */
    @Transactional
    public StudentOnboarding getOrCreateOnboardingEntity(User user) {
        Optional<StudentOnboarding> opt = studentOnboardingRepository.findByUserIdWithCareerAndTech(user.getId());
        if (opt.isPresent()) {
            return opt.get();
        }

        boolean hasRoadmap = userRoadmapProgressRepository.countByUser(user) > 0;
        CareerTrack career = null;
        if (user.getCareerGoal() != null && !user.getCareerGoal().trim().isEmpty()) {
            career = careerTrackRepository.findByTitleIgnoreCase(user.getCareerGoal().trim()).orElse(null);
        }

        StudentOnboarding onboarding = StudentOnboarding.builder()
                .user(user)
                .selectedCareer(career)
                .state(hasRoadmap ? OnboardingState.ROADMAP_GENERATED : (career != null ? OnboardingState.CAREER_SELECTED : OnboardingState.NOT_STARTED))
                .hasPersonalizedRoadmap(hasRoadmap)
                .dailyStudyMinutes(60)
                .preferredStudyWindow(StudyWindow.FLEXIBLE)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .build();

        return studentOnboardingRepository.save(onboarding);
    }

    private OnboardingStateDto mapToStateDto(StudentOnboarding onboarding) {
        CatalogCareerDto careerDto = onboarding.getSelectedCareer() != null
                ? catalogService.mapToCareerDto(onboarding.getSelectedCareer())
                : null;

        List<CatalogTechnologyDto> techDtos = onboarding.getSelectedTechnologies() != null
                ? onboarding.getSelectedTechnologies().stream()
                .filter(Technology::isActive)
                .sorted(Comparator.comparing(Technology::getName))
                .map(catalogService::mapToTechnologyDto)
                .collect(Collectors.toList())
                : Collections.emptyList();

        List<String> days = (onboarding.getPreferredDays() != null && !onboarding.getPreferredDays().trim().isEmpty())
                ? Arrays.asList(onboarding.getPreferredDays().split(","))
                : Collections.emptyList();

        OnboardingStudyPreferencesDto prefDto = OnboardingStudyPreferencesDto.builder()
                .dailyStudyMinutes(onboarding.getDailyStudyMinutes())
                .preferredStudyWindow(onboarding.getPreferredStudyWindow())
                .preferredDays(days)
                .targetCompletionDate(onboarding.getTargetCompletionDate())
                .build();

        Long currentSessionId = onboarding.getAssessmentSession() != null ? onboarding.getAssessmentSession().getId() : null;
        String assessmentStatus = onboarding.getAssessmentSession() != null ? onboarding.getAssessmentSession().getStatus().name() : null;

        boolean canResume = onboarding.getAssessmentSession() != null
                && onboarding.getAssessmentSession().getStatus() == AssessmentStatus.IN_PROGRESS
                && LocalDateTime.now().isBefore(onboarding.getAssessmentSession().getExpiresAt());

        boolean canGenerate = onboarding.getState() == OnboardingState.ASSESSMENT_REVIEWED
                || onboarding.getState() == OnboardingState.ASSESSMENT_COMPLETED
                || onboarding.getState() == OnboardingState.ROADMAP_GENERATED
                || onboarding.isHasPersonalizedRoadmap();

        return OnboardingStateDto.builder()
                .state(onboarding.getState())
                .selectedCareer(careerDto)
                .selectedTechnologies(techDtos)
                .studyPreferences(prefDto)
                .currentAssessmentSessionId(currentSessionId)
                .assessmentStatus(assessmentStatus)
                .hasPersonalizedRoadmap(onboarding.isHasPersonalizedRoadmap())
                .canResumeAssessment(canResume)
                .canGenerateRoadmap(canGenerate)
                .build();
    }
}
