package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentSessionQuestionRepository sessionQuestionRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final TechnologyAssessmentResultRepository resultRepository;
    private final TechnologyRepository technologyRepository;
    private final CatalogService catalogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Default configuration limits
    public static final int DEFAULT_TIME_LIMIT_MINUTES = 20;
    public static final int DEFAULT_MIN_QUESTIONS_PER_TECH = 3;
    public static final int DEFAULT_MAX_QUESTIONS_PER_TECH = 6;

    public AssessmentService(AssessmentSessionRepository sessionRepository,
                             AssessmentQuestionRepository questionRepository,
                             AssessmentSessionQuestionRepository sessionQuestionRepository,
                             AssessmentAnswerRepository answerRepository,
                             TechnologyAssessmentResultRepository resultRepository,
                             TechnologyRepository technologyRepository,
                             CatalogService catalogService) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.sessionQuestionRepository = sessionQuestionRepository;
        this.answerRepository = answerRepository;
        this.resultRepository = resultRepository;
        this.technologyRepository = technologyRepository;
        this.catalogService = catalogService;
    }

    /**
     * Starts a combined assessment session for the selected technologies.
     */
    @Transactional
    public AssessmentSessionDto startAssessment(User user, StartAssessmentRequest request) {
        if (request == null || request.getTechnologySlugs() == null || request.getTechnologySlugs().isEmpty()) {
            throw new IllegalArgumentException("At least one technology must be selected");
        }

        // Validate technologies
        Set<Technology> selectedTechs = new HashSet<>();
        for (String slug : request.getTechnologySlugs()) {
            Technology tech = technologyRepository.findBySlug(slug.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Technology not found: " + slug));
            if (!tech.isActive()) {
                throw new IllegalArgumentException("Technology is inactive: " + slug);
            }
            selectedTechs.add(tech);
        }

        // Check if an existing IN_PROGRESS session exists
        Optional<AssessmentSession> activeOpt = sessionRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, AssessmentStatus.IN_PROGRESS);
        if (activeOpt.isPresent()) {
            AssessmentSession existing = activeOpt.get();
            if (LocalDateTime.now().isBefore(existing.getExpiresAt())) {
                logger.info("Resuming active in-progress assessment session #{} for user {}", existing.getId(), user.getEmail());
                return mapToSessionDto(existing);
            } else {
                // Auto-finalize expired session
                finalizeSessionInternal(existing, AssessmentStatus.TIMED_OUT);
            }
        }

        int timeLimit = request.getTimeLimitMinutes() != null && request.getTimeLimitMinutes() >= 5 && request.getTimeLimitMinutes() <= 60
                ? request.getTimeLimitMinutes()
                : DEFAULT_TIME_LIMIT_MINUTES;

        int minPerTech = request.getMinQuestionsPerTechnology() != null && request.getMinQuestionsPerTechnology() >= 2
                ? request.getMinQuestionsPerTechnology()
                : DEFAULT_MIN_QUESTIONS_PER_TECH;

        int maxPerTech = request.getMaxQuestionsPerTechnology() != null && request.getMaxQuestionsPerTechnology() >= minPerTech
                ? request.getMaxQuestionsPerTechnology()
                : DEFAULT_MAX_QUESTIONS_PER_TECH;

        LocalDateTime now = LocalDateTime.now();
        AssessmentSession session = AssessmentSession.builder()
                .user(user)
                .selectedTechnologies(selectedTechs)
                .status(AssessmentStatus.IN_PROGRESS)
                .startTime(now)
                .expiresAt(now.plusMinutes(timeLimit))
                .timeLimitMinutes(timeLimit)
                .assessmentVersion("v2.0-adaptive")
                .build();

        session = sessionRepository.save(session);

        // Seed initial questions: 1 beginner question per selected technology
        int order = 1;
        for (Technology tech : selectedTechs) {
            Optional<AssessmentQuestion> initialQ = selectAdaptiveQuestion(session, tech, AssessmentDifficulty.BEGINNER, Collections.emptySet());
            if (initialQ.isPresent()) {
                AssessmentQuestion q = initialQ.get();
                AssessmentSessionQuestion sq = AssessmentSessionQuestion.builder()
                        .session(session)
                        .question(q)
                        .technology(tech)
                        .topic(q.getTopic())
                        .difficulty(q.getDifficulty())
                        .presentationOrder(order++)
                        .scoringWeight(q.getScoringWeight())
                        .build();
                sq = sessionQuestionRepository.save(sq);

                AssessmentAnswer answer = AssessmentAnswer.builder()
                        .session(session)
                        .sessionQuestion(sq)
                        .status(AnswerStatus.UNANSWERED)
                        .build();
                answerRepository.save(answer);
            }
        }

        session.setTotalQuestionsCount(sessionQuestionRepository.countBySessionId(session.getId()));
        session = sessionRepository.save(session);

        logger.info("Started new assessment session #{} with {} initial questions for user {}",
                session.getId(), session.getTotalQuestionsCount(), user.getEmail());

        return mapToSessionDto(session);
    }

    /**
     * Starts a reassessment session for selected technologies/competencies, linking to previous baseline.
     */
    @Transactional
    public AssessmentSessionDto startReassessment(User user, StartReassessmentRequest request) {
        if (request == null || request.getTechnologySlugs() == null || request.getTechnologySlugs().isEmpty()) {
            throw new IllegalArgumentException("At least one technology must be selected for reassessment");
        }

        // Validate technologies
        Set<Technology> selectedTechs = new HashSet<>();
        for (String slug : request.getTechnologySlugs()) {
            Technology tech = technologyRepository.findBySlug(slug.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Technology not found: " + slug));
            if (!tech.isActive()) {
                throw new IllegalArgumentException("Technology is inactive: " + slug);
            }
            selectedTechs.add(tech);
        }

        // Discover baseline session if not provided
        Long baselineId = request.getBaselineSessionId();
        if (baselineId == null) {
            baselineId = sessionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                    .filter(s -> s.getStatus() == AssessmentStatus.SUBMITTED)
                    .map(AssessmentSession::getId)
                    .findFirst()
                    .orElse(null);
        } else {
            final Long lookupId = baselineId;
            AssessmentSession baseline = sessionRepository.findById(lookupId)
                    .orElseThrow(() -> new IllegalArgumentException("Baseline session not found: " + lookupId));
            if (!baseline.getUser().getId().equals(user.getId())) {
                throw new SecurityException("User does not own baseline assessment session");
            }
        }

        int timeLimit = request.getTimeLimitMinutes() != null && request.getTimeLimitMinutes() >= 5 && request.getTimeLimitMinutes() <= 60
                ? request.getTimeLimitMinutes()
                : DEFAULT_TIME_LIMIT_MINUTES;

        LocalDateTime now = LocalDateTime.now();
        AssessmentSession session = AssessmentSession.builder()
                .user(user)
                .selectedTechnologies(selectedTechs)
                .status(AssessmentStatus.IN_PROGRESS)
                .startTime(now)
                .expiresAt(now.plusMinutes(timeLimit))
                .timeLimitMinutes(timeLimit)
                .assessmentVersion("v2.0-adaptive")
                .isReassessment(true)
                .baselineSessionId(baselineId)
                .build();

        session = sessionRepository.save(session);

        // Seed initial questions: 1 beginner question per selected technology
        int order = 1;
        for (Technology tech : selectedTechs) {
            Optional<AssessmentQuestion> initialQ = selectAdaptiveQuestion(session, tech, AssessmentDifficulty.BEGINNER, Collections.emptySet());
            if (initialQ.isPresent()) {
                AssessmentQuestion q = initialQ.get();
                AssessmentSessionQuestion sq = AssessmentSessionQuestion.builder()
                        .session(session)
                        .question(q)
                        .technology(tech)
                        .topic(q.getTopic())
                        .difficulty(q.getDifficulty())
                        .presentationOrder(order++)
                        .scoringWeight(q.getScoringWeight())
                        .build();
                sq = sessionQuestionRepository.save(sq);

                AssessmentAnswer answer = AssessmentAnswer.builder()
                        .session(session)
                        .sessionQuestion(sq)
                        .status(AnswerStatus.UNANSWERED)
                        .build();
                answerRepository.save(answer);
            }
        }

        session.setTotalQuestionsCount(order - 1);
        session = sessionRepository.save(session);

        logger.info("Started reassessment session #{} for user {} on {} technologies (baseline: #{})",
                session.getId(), user.getEmail(), selectedTechs.size(), baselineId);

        return mapToSessionDto(session);
    }

    /**
     * Retrieves the current active assessment for the user, checking for timeout.
     */
    @Transactional
    public Optional<AssessmentSessionDto> getActiveSession(User user) {
        Optional<AssessmentSession> activeOpt = sessionRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, AssessmentStatus.IN_PROGRESS);
        if (activeOpt.isEmpty()) {
            return Optional.empty();
        }

        AssessmentSession session = activeOpt.get();
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            finalizeSessionInternal(session, AssessmentStatus.TIMED_OUT);
            return Optional.empty();
        }

        return Optional.of(mapToSessionDto(session));
    }

    /**
     * Retrieves session state by ID, verifying user ownership.
     */
    @Transactional
    public AssessmentSessionDto getSessionState(Long sessionId, User user) {
        AssessmentSession session = getSessionAndVerifyOwnership(sessionId, user);

        if (session.getStatus() == AssessmentStatus.IN_PROGRESS && LocalDateTime.now().isAfter(session.getExpiresAt())) {
            finalizeSessionInternal(session, AssessmentStatus.TIMED_OUT);
        }

        return mapToSessionDto(session);
    }

    /**
     * Submits an answer for a specific question within the assessment session.
     */
    @Transactional
    public AssessmentQuestionDto submitAnswer(Long sessionId, SubmitAnswerRequest request, User user) {
        AssessmentSession session = getSessionAndVerifyOwnership(sessionId, user);

        if (session.getStatus() != AssessmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Assessment session is not in progress (status: " + session.getStatus() + ")");
        }

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            finalizeSessionInternal(session, AssessmentStatus.TIMED_OUT);
            throw new IllegalStateException("Assessment time limit has expired");
        }

        AssessmentSessionQuestion sq = sessionQuestionRepository.findById(request.getSessionQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Session question not found: " + request.getSessionQuestionId()));

        if (!sq.getSession().getId().equals(session.getId())) {
            throw new IllegalArgumentException("Question does not belong to this assessment session");
        }

        AssessmentAnswer answer = answerRepository.findBySessionQuestionId(sq.getId())
                .orElseGet(() -> AssessmentAnswer.builder()
                        .session(session)
                        .sessionQuestion(sq)
                        .build());

        AssessmentQuestion question = sq.getQuestion();
        boolean isCorrect = (request.getSelectedOptionIndex() != null &&
                             request.getSelectedOptionIndex() == question.getCorrectOptionIndex());

        answer.setSelectedOptionIndex(request.getSelectedOptionIndex());
        answer.setStatus(AnswerStatus.ANSWERED);
        answer.setIsCorrect(isCorrect);
        answer.setAwardedMarks(isCorrect ? sq.getScoringWeight() : 0);
        answer.setAnsweredAt(LocalDateTime.now());
        answerRepository.save(answer);

        session.setAnsweredQuestionsCount(answerRepository.countBySessionIdAndStatus(session.getId(), AnswerStatus.ANSWERED));

        // Adaptively schedule the next question for this technology if count < max questions
        int currentTechCount = sessionQuestionRepository.countBySessionIdAndTechnologyId(session.getId(), sq.getTechnology().getId());
        if (currentTechCount < DEFAULT_MAX_QUESTIONS_PER_TECH) {
            maybeScheduleNextAdaptiveQuestion(session, sq.getTechnology(), isCorrect);
        }

        session.setTotalQuestionsCount(sessionQuestionRepository.countBySessionId(session.getId()));
        sessionRepository.save(session);

        return mapToQuestionDto(sq, answer);
    }

    /**
     * Marks a question as skipped so the student can return to it later.
     */
    @Transactional
    public AssessmentQuestionDto skipQuestion(Long sessionId, Long sessionQuestionId, User user) {
        AssessmentSession session = getSessionAndVerifyOwnership(sessionId, user);

        if (session.getStatus() != AssessmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Assessment session is not in progress");
        }

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            finalizeSessionInternal(session, AssessmentStatus.TIMED_OUT);
            throw new IllegalStateException("Assessment time limit has expired");
        }

        AssessmentSessionQuestion sq = sessionQuestionRepository.findById(sessionQuestionId)
                .orElseThrow(() -> new IllegalArgumentException("Session question not found: " + sessionQuestionId));

        if (!sq.getSession().getId().equals(session.getId())) {
            throw new IllegalArgumentException("Question does not belong to this assessment session");
        }

        AssessmentAnswer answer = answerRepository.findBySessionQuestionId(sq.getId())
                .orElseGet(() -> AssessmentAnswer.builder()
                        .session(session)
                        .sessionQuestion(sq)
                        .build());

        answer.setStatus(AnswerStatus.SKIPPED);
        answer.setSelectedOptionIndex(null);
        answer.setIsCorrect(false);
        answer.setAwardedMarks(0);
        answerRepository.save(answer);

        return mapToQuestionDto(sq, answer);
    }

    /**
     * Finalizes the assessment session, evaluates all answers, computes per-technology results and overall scores.
     */
    @Transactional
    public AssessmentReportDto submitAssessment(Long sessionId, User user) {
        AssessmentSession session = getSessionAndVerifyOwnership(sessionId, user);

        if (session.getStatus() == AssessmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot submit cancelled assessment session #" + sessionId);
        }

        if (session.getStatus() == AssessmentStatus.SUBMITTED || session.getStatus() == AssessmentStatus.TIMED_OUT) {
            return getAssessmentReport(sessionId, user);
        }

        AssessmentStatus finalStatus = LocalDateTime.now().isAfter(session.getExpiresAt())
                ? AssessmentStatus.TIMED_OUT
                : AssessmentStatus.SUBMITTED;

        return finalizeSessionInternal(session, finalStatus);
    }

    /**
     * Retrieves the completed assessment report with full review and explanations revealed.
     */
    @Transactional(readOnly = true)
    public AssessmentReportDto getAssessmentReport(Long sessionId, User user) {
        AssessmentSession session = getSessionAndVerifyOwnership(sessionId, user);

        if (session.getStatus() == AssessmentStatus.IN_PROGRESS) {
            if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
                // Handled in separate write txn if needed, but for read-only we proceed to format
            } else {
                throw new IllegalStateException("Assessment is still in progress. Submit the assessment to view the report.");
            }
        }

        List<TechnologyAssessmentResult> techResults = resultRepository.findBySessionIdWithTechnology(session.getId());
        List<TechnologyResultDto> techDtos = techResults.stream()
                .map(this::mapToTechnologyResultDto)
                .collect(Collectors.toList());

        List<AssessmentSessionQuestion> sessionQuestions = sessionQuestionRepository.findBySessionIdWithQuestionAndTech(session.getId());
        List<AssessmentAnswer> answers = answerRepository.findBySessionId(session.getId());
        Map<Long, AssessmentAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getSessionQuestion().getId(), a -> a, (a1, a2) -> a1));

        List<QuestionReviewDto> reviews = new ArrayList<>();
        for (AssessmentSessionQuestion sq : sessionQuestions) {
            AssessmentAnswer ans = answerMap.get(sq.getId());
            reviews.add(mapToQuestionReviewDto(sq, ans));
        }

        return AssessmentReportDto.builder()
                .sessionId(session.getId())
                .status(session.getStatus().name())
                .startTime(session.getStartTime())
                .submittedAt(session.getSubmittedAt())
                .overallScore(session.getOverallScore() != null ? session.getOverallScore() : 0)
                .overallProficiency(session.getOverallProficiency() != null ? session.getOverallProficiency() : "Beginner")
                .technologyResults(techDtos)
                .questionReviews(reviews)
                .build();
    }

    /**
     * Retrieves student's assessment history.
     */
    @Transactional(readOnly = true)
    public List<AssessmentSummaryDto> getAssessmentHistory(User user) {
        List<AssessmentSession> sessions = sessionRepository.findByUserOrderByCreatedAtDesc(user);
        return sessions.stream()
                .map(s -> AssessmentSummaryDto.builder()
                        .sessionId(s.getId())
                        .status(s.getStatus().name())
                        .startTime(s.getStartTime())
                        .submittedAt(s.getSubmittedAt())
                        .overallScore(s.getOverallScore())
                        .overallProficiency(s.getOverallProficiency())
                        .assessedTechnologies(s.getSelectedTechnologies().stream().map(Technology::getName).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Core evaluation and finalization logic.
     */
    @Transactional
    protected AssessmentReportDto finalizeSessionInternal(AssessmentSession session, AssessmentStatus finalStatus) {
        session.setStatus(finalStatus);
        session.setSubmittedAt(LocalDateTime.now());

        List<AssessmentSessionQuestion> sessionQuestions = sessionQuestionRepository.findBySessionIdWithQuestionAndTech(session.getId());
        List<AssessmentAnswer> answers = answerRepository.findBySessionId(session.getId());
        Map<Long, AssessmentAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getSessionQuestion().getId(), a -> a, (a1, a2) -> a1));

        // Evaluate any remaining unanswered/skipped questions
        for (AssessmentSessionQuestion sq : sessionQuestions) {
            AssessmentAnswer ans = answerMap.get(sq.getId());
            if (ans == null) {
                ans = AssessmentAnswer.builder()
                        .session(session)
                        .sessionQuestion(sq)
                        .status(AnswerStatus.UNANSWERED)
                        .isCorrect(false)
                        .awardedMarks(0)
                        .build();
                answerRepository.save(ans);
                answerMap.put(sq.getId(), ans);
            } else if (ans.getStatus() == AnswerStatus.UNANSWERED || ans.getStatus() == AnswerStatus.SKIPPED) {
                ans.setIsCorrect(false);
                ans.setAwardedMarks(0);
                answerRepository.save(ans);
            }
        }

        // Group by Technology and compute results
        Map<Technology, List<AssessmentSessionQuestion>> techQuestionsMap = sessionQuestions.stream()
                .collect(Collectors.groupingBy(AssessmentSessionQuestion::getTechnology));

        int totalSessionEarned = 0;
        int totalSessionAvailable = 0;
        List<TechnologyAssessmentResult> generatedResults = new ArrayList<>();

        for (Map.Entry<Technology, List<AssessmentSessionQuestion>> entry : techQuestionsMap.entrySet()) {
            Technology tech = entry.getKey();
            List<AssessmentSessionQuestion> techQuestions = entry.getValue();

            int marksEarned = 0;
            int marksAvailable = 0;
            int correctCount = 0;
            int incorrectCount = 0;
            int skippedCount = 0;
            int unansweredCount = 0;
            Set<String> topicsCovered = new HashSet<>();
            Map<String, Boolean> topicSuccessMap = new HashMap<>();

            for (AssessmentSessionQuestion sq : techQuestions) {
                marksAvailable += sq.getScoringWeight();
                topicsCovered.add(sq.getTopic());

                AssessmentAnswer ans = answerMap.get(sq.getId());
                if (ans != null && ans.getStatus() == AnswerStatus.ANSWERED) {
                    if (Boolean.TRUE.equals(ans.getIsCorrect())) {
                        correctCount++;
                        marksEarned += sq.getScoringWeight();
                        topicSuccessMap.put(sq.getTopic(), topicSuccessMap.getOrDefault(sq.getTopic(), true));
                    } else {
                        incorrectCount++;
                        topicSuccessMap.put(sq.getTopic(), false);
                    }
                } else if (ans != null && ans.getStatus() == AnswerStatus.SKIPPED) {
                    skippedCount++;
                    topicSuccessMap.put(sq.getTopic(), false);
                } else {
                    unansweredCount++;
                    topicSuccessMap.put(sq.getTopic(), false);
                }
            }

            int evaluatedCount = correctCount + incorrectCount;
            int rawScore = marksAvailable > 0 ? Math.round(((float) marksEarned / marksAvailable) * 100) : 0;

            // Estimated Proficiency Heuristic
            String proficiency;
            if (evaluatedCount < DEFAULT_MIN_QUESTIONS_PER_TECH) {
                proficiency = "INSUFFICIENT_EVIDENCE";
            } else if (rawScore >= 80 && hasAdvancedCorrect(techQuestions, answerMap)) {
                proficiency = rawScore >= 95 ? "EXPERT" : "ADVANCED";
            } else if (rawScore >= 60) {
                proficiency = "INTERMEDIATE";
            } else {
                proficiency = "BEGINNER";
            }

            // Confidence Level Heuristic
            String confidence;
            if (evaluatedCount < 3) {
                confidence = "LOW";
            } else if (evaluatedCount <= 4) {
                confidence = "MEDIUM";
            } else {
                confidence = "HIGH";
            }

            // Identify Strengths and Weaknesses
            List<String> strengths = new ArrayList<>();
            List<String> weaknesses = new ArrayList<>();
            for (Map.Entry<String, Boolean> topicEntry : topicSuccessMap.entrySet()) {
                if (Boolean.TRUE.equals(topicEntry.getValue())) {
                    strengths.add(topicEntry.getKey());
                } else {
                    weaknesses.add(topicEntry.getKey());
                }
            }

            TechnologyAssessmentResult result = TechnologyAssessmentResult.builder()
                    .session(session)
                    .technology(tech)
                    .rawScore(rawScore)
                    .marksEarned(marksEarned)
                    .marksAvailable(marksAvailable)
                    .evaluatedQuestionsCount(evaluatedCount)
                    .correctCount(correctCount)
                    .incorrectCount(incorrectCount)
                    .skippedCount(skippedCount)
                    .unansweredCount(unansweredCount)
                    .topicCoverageJson(toJson(new ArrayList<>(topicsCovered)))
                    .estimatedProficiency(proficiency)
                    .confidenceLevel(confidence)
                    .strengthsJson(toJson(strengths))
                    .weaknessesJson(toJson(weaknesses))
                    .scoringVersion("v2.0-heuristic")
                    .build();

            // Prevent duplicate result on retry
            resultRepository.findBySessionIdAndTechnologyId(session.getId(), tech.getId())
                    .ifPresent(existing -> result.setId(existing.getId()));

            generatedResults.add(resultRepository.save(result));

            totalSessionEarned += marksEarned;
            totalSessionAvailable += marksAvailable;
        }

        int overallScore = totalSessionAvailable > 0
                ? Math.round(((float) totalSessionEarned / totalSessionAvailable) * 100)
                : 0;

        String overallProficiency;
        if (overallScore >= 80) {
            overallProficiency = "Advanced";
        } else if (overallScore >= 55) {
            overallProficiency = "Intermediate";
        } else {
            overallProficiency = "Beginner";
        }

        session.setOverallScore(overallScore);
        session.setOverallProficiency(overallProficiency);
        AssessmentSession savedSession = sessionRepository.save(session);
        if (savedSession != null) {
            session = savedSession;
        }

        logger.info("Finalized assessment session #{} ({}) with overall score {}% ({})",
                session.getId(), finalStatus, overallScore, overallProficiency);

        return getAssessmentReport(session.getId(), session.getUser());
    }

    private boolean hasAdvancedCorrect(List<AssessmentSessionQuestion> questions, Map<Long, AssessmentAnswer> answerMap) {
        for (AssessmentSessionQuestion sq : questions) {
            if (sq.getDifficulty() == AssessmentDifficulty.ADVANCED) {
                AssessmentAnswer ans = answerMap.get(sq.getId());
                if (ans != null && Boolean.TRUE.equals(ans.getIsCorrect())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adaptive selector for the next question in a technology path.
     */
    private void maybeScheduleNextAdaptiveQuestion(AssessmentSession session, Technology tech, boolean lastWasCorrect) {
        // Collect IDs of questions already presented in this session
        Set<Long> usedQuestionIds = sessionQuestionRepository.findBySessionIdAndTechnologyIdOrderByPresentationOrderAsc(session.getId(), tech.getId())
                .stream()
                .map(sq -> sq.getQuestion().getId())
                .collect(Collectors.toSet());

        // Determine target difficulty based on performance
        AssessmentDifficulty targetDifficulty = lastWasCorrect
                ? AssessmentDifficulty.INTERMEDIATE
                : AssessmentDifficulty.BEGINNER;

        if (lastWasCorrect) {
            // Check if student already got intermediate correct
            List<AssessmentSessionQuestion> techQuestions = sessionQuestionRepository.findBySessionIdAndTechnologyIdOrderByPresentationOrderAsc(session.getId(), tech.getId());
            boolean hasCorrectIntermediate = techQuestions.stream()
                    .filter(q -> q.getDifficulty() == AssessmentDifficulty.INTERMEDIATE)
                    .anyMatch(q -> {
                        Optional<AssessmentAnswer> a = answerRepository.findBySessionQuestionId(q.getId());
                        return a.isPresent() && Boolean.TRUE.equals(a.get().getIsCorrect());
                    });
            if (hasCorrectIntermediate) {
                targetDifficulty = AssessmentDifficulty.ADVANCED;
            }
        }

        Optional<AssessmentQuestion> nextQ = selectAdaptiveQuestion(session, tech, targetDifficulty, usedQuestionIds);
        if (nextQ.isPresent()) {
            AssessmentQuestion q = nextQ.get();
            int nextOrder = sessionQuestionRepository.countBySessionId(session.getId()) + 1;

            AssessmentSessionQuestion sq = AssessmentSessionQuestion.builder()
                    .session(session)
                    .question(q)
                    .technology(tech)
                    .topic(q.getTopic())
                    .difficulty(q.getDifficulty())
                    .presentationOrder(nextOrder)
                    .scoringWeight(q.getScoringWeight())
                    .build();
            AssessmentSessionQuestion savedSq = sessionQuestionRepository.save(sq);
            if (savedSq != null) {
                sq = savedSq;
            }

            AssessmentAnswer answer = AssessmentAnswer.builder()
                    .session(session)
                    .sessionQuestion(sq)
                    .status(AnswerStatus.UNANSWERED)
                    .build();
            answerRepository.save(answer);

            logger.info("Adaptive engine scheduled question #{} ({}) for tech {} at difficulty {}",
                    sq.getPresentationOrder(), q.getQuestionKey(), tech.getName(), q.getDifficulty());
        }
    }

    private Optional<AssessmentQuestion> selectAdaptiveQuestion(AssessmentSession session, Technology tech,
                                                                 AssessmentDifficulty targetDiff, Set<Long> excludeQuestionIds) {
        List<AssessmentQuestion> pool = questionRepository.findByTechnologyAndActiveTrue(tech);

        // 1. Filter out already used questions
        List<AssessmentQuestion> available = pool.stream()
                .filter(q -> !excludeQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            return Optional.empty();
        }

        // 2. Try exact target difficulty
        List<AssessmentQuestion> atTarget = available.stream()
                .filter(q -> q.getDifficulty() == targetDiff)
                .collect(Collectors.toList());

        if (!atTarget.isEmpty()) {
            return Optional.of(atTarget.get(0));
        }

        // 3. Fall back to adjacent difficulty
        return Optional.of(available.get(0));
    }

    private AssessmentSession getSessionAndVerifyOwnership(Long sessionId, User user) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment session not found: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: student does not own this assessment session");
        }

        return session;
    }

    private AssessmentSessionDto mapToSessionDto(AssessmentSession session) {
        long remainingSeconds = 0;
        if (session.getStatus() == AssessmentStatus.IN_PROGRESS && session.getExpiresAt() != null) {
            remainingSeconds = Math.max(0, Duration.between(LocalDateTime.now(), session.getExpiresAt()).getSeconds());
        }

        List<AssessmentSessionQuestion> sessionQuestions = sessionQuestionRepository.findBySessionIdWithQuestionAndTech(session.getId());
        List<AssessmentAnswer> answers = answerRepository.findBySessionId(session.getId());
        Map<Long, AssessmentAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getSessionQuestion().getId(), a -> a, (a1, a2) -> a1));

        List<AssessmentQuestionDto> questionDtos = sessionQuestions.stream()
                .map(sq -> mapToQuestionDto(sq, answerMap.get(sq.getId())))
                .collect(Collectors.toList());

        List<CatalogTechnologyDto> techDtos = session.getSelectedTechnologies().stream()
                .map(catalogService::mapToTechnologyDto)
                .collect(Collectors.toList());

        int answeredCount = (int) questionDtos.stream().filter(q -> "ANSWERED".equalsIgnoreCase(q.getAnswerStatus())).count();
        int skippedCount = (int) questionDtos.stream().filter(q -> "SKIPPED".equalsIgnoreCase(q.getAnswerStatus())).count();

        return AssessmentSessionDto.builder()
                .sessionId(session.getId())
                .status(session.getStatus().name())
                .startTime(session.getStartTime())
                .expiresAt(session.getExpiresAt())
                .remainingSeconds(remainingSeconds)
                .timeLimitMinutes(session.getTimeLimitMinutes())
                .totalQuestionsCount(sessionQuestions.isEmpty() && session.getTotalQuestionsCount() > 0 ? session.getTotalQuestionsCount() : sessionQuestions.size())
                .answeredCount(answeredCount)
                .skippedCount(skippedCount)
                .currentOrder(questionDtos.isEmpty() ? 1 : questionDtos.size())
                .isReassessment(session.getIsReassessment())
                .baselineSessionId(session.getBaselineSessionId())
                .selectedTechnologies(techDtos)
                .questions(questionDtos)
                .build();
    }

    private AssessmentQuestionDto mapToQuestionDto(AssessmentSessionQuestion sq, AssessmentAnswer answer) {
        AssessmentQuestion q = sq.getQuestion();
        List<String> options = fromJsonList(q.getOptionsJson());

        Integer selectedIndex = answer != null ? answer.getSelectedOptionIndex() : null;
        String status = answer != null && answer.getStatus() != null ? answer.getStatus().name() : "UNANSWERED";

        return AssessmentQuestionDto.builder()
                .sessionQuestionId(sq.getId())
                .presentationOrder(sq.getPresentationOrder())
                .technologySlug(sq.getTechnology().getSlug())
                .technologyName(sq.getTechnology().getName())
                .topic(sq.getTopic())
                .difficulty(sq.getDifficulty() != null ? sq.getDifficulty().name() : "BEGINNER")
                .questionText(q.getQuestionText())
                .options(options)
                .scoringWeight(sq.getScoringWeight())
                .selectedOptionIndex(selectedIndex)
                .answerStatus(status)
                .build();
    }

    private QuestionReviewDto mapToQuestionReviewDto(AssessmentSessionQuestion sq, AssessmentAnswer ans) {
        AssessmentQuestion q = sq.getQuestion();
        List<String> options = fromJsonList(q.getOptionsJson());

        boolean correct = ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
        int awarded = ans != null ? ans.getAwardedMarks() : 0;
        Integer selected = ans != null ? ans.getSelectedOptionIndex() : null;
        String status = ans != null && ans.getStatus() != null ? ans.getStatus().name() : "UNANSWERED";

        return QuestionReviewDto.builder()
                .presentationOrder(sq.getPresentationOrder())
                .technologySlug(sq.getTechnology().getSlug())
                .technologyName(sq.getTechnology().getName())
                .topic(sq.getTopic())
                .difficulty(sq.getDifficulty() != null ? sq.getDifficulty().name() : "BEGINNER")
                .questionText(q.getQuestionText())
                .options(options)
                .selectedOptionIndex(selected)
                .correctOptionIndex(q.getCorrectOptionIndex())
                .isCorrect(correct)
                .explanation(q.getExplanation())
                .awardedMarks(awarded)
                .scoringWeight(sq.getScoringWeight())
                .answerStatus(status)
                .build();
    }

    private TechnologyResultDto mapToTechnologyResultDto(TechnologyAssessmentResult r) {
        return TechnologyResultDto.builder()
                .technologySlug(r.getTechnology().getSlug())
                .technologyName(r.getTechnology().getName())
                .rawScore(r.getRawScore())
                .marksEarned(r.getMarksEarned())
                .marksAvailable(r.getMarksAvailable())
                .evaluatedQuestionsCount(r.getEvaluatedQuestionsCount())
                .correctCount(r.getCorrectCount())
                .incorrectCount(r.getIncorrectCount())
                .skippedCount(r.getSkippedCount())
                .unansweredCount(r.getUnansweredCount())
                .topicCoverage(fromJsonList(r.getTopicCoverageJson()))
                .estimatedProficiency(r.getEstimatedProficiency())
                .confidenceLevel(r.getConfidenceLevel())
                .strengths(fromJsonList(r.getStrengthsJson()))
                .weaknesses(fromJsonList(r.getWeaknessesJson()))
                .build();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : Collections.emptyList());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
