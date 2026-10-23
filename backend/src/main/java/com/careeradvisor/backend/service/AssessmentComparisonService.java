package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AssessmentComparisonDto;
import com.careeradvisor.backend.dto.CompetencyChangeDto;
import com.careeradvisor.backend.dto.TechnologyComparisonDto;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.AssessmentSessionRepository;
import com.careeradvisor.backend.repository.CompetencyNodeRepository;
import com.careeradvisor.backend.repository.TechnologyAssessmentResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentComparisonService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentComparisonService.class);

    private final AssessmentSessionRepository sessionRepository;
    private final TechnologyAssessmentResultRepository resultRepository;
    private final CompetencyNodeRepository competencyNodeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public AssessmentComparisonService(AssessmentSessionRepository sessionRepository,
                                       TechnologyAssessmentResultRepository resultRepository,
                                       CompetencyNodeRepository competencyNodeRepository) {
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.competencyNodeRepository = competencyNodeRepository;
    }

    /**
     * Compares a submitted reassessment session against a baseline session for a user.
     */
    @Transactional(readOnly = true)
    public AssessmentComparisonDto compareSessions(AssessmentSession reassessment, AssessmentSession baseline, User user) {
        if (reassessment == null) {
            throw new IllegalArgumentException("Reassessment session must not be null");
        }
        if (!reassessment.getUser().getId().equals(user.getId())) {
            throw new SecurityException("User does not own this reassessment session");
        }
        if (reassessment.getStatus() != AssessmentStatus.SUBMITTED) {
            throw new IllegalStateException("Reassessment session #" + reassessment.getId() + " is not SUBMITTED");
        }
        if (baseline != null && !baseline.getUser().getId().equals(user.getId())) {
            throw new SecurityException("User does not own the baseline assessment session");
        }

        // Ensure reassessment has selectedTechnologies initialized within transaction
        if (reassessment.getId() != null) {
            reassessment = sessionRepository.findByIdWithTechnologies(reassessment.getId()).orElse(reassessment);
        }

        // If baseline is not provided, try to discover the closest prior submitted session
        if (baseline == null) {
            if (reassessment.getBaselineSessionId() != null) {
                baseline = sessionRepository.findByIdWithTechnologies(reassessment.getBaselineSessionId()).orElse(null);
            }
            if (baseline == null) {
                List<AssessmentSession> priorSessions = sessionRepository.findByUserOrderByCreatedAtDesc(user);
                for (AssessmentSession s : priorSessions) {
                    if (!s.getId().equals(reassessment.getId()) && s.getStatus() == AssessmentStatus.SUBMITTED
                            && s.getCreatedAt().isBefore(reassessment.getCreatedAt())) {
                        baseline = sessionRepository.findByIdWithTechnologies(s.getId()).orElse(s);
                        break;
                    }
                }
            }
        } else if (baseline.getId() != null) {
            baseline = sessionRepository.findByIdWithTechnologies(baseline.getId()).orElse(baseline);
        }

        List<TechnologyAssessmentResult> reassessmentResults = resultRepository.findBySessionIdWithTechnology(reassessment.getId());
        Map<String, TechnologyAssessmentResult> reassessmentResultMap = new HashMap<>();
        for (TechnologyAssessmentResult r : reassessmentResults) {
            if (r.getTechnology() != null) {
                reassessmentResultMap.put(r.getTechnology().getSlug(), r);
            }
        }

        Map<String, TechnologyAssessmentResult> baselineResultMap = new HashMap<>();
        if (baseline != null) {
            List<TechnologyAssessmentResult> baselineResults = resultRepository.findBySessionIdWithTechnology(baseline.getId());
            for (TechnologyAssessmentResult r : baselineResults) {
                if (r.getTechnology() != null) {
                    baselineResultMap.put(r.getTechnology().getSlug(), r);
                }
            }
        }

        List<TechnologyComparisonDto> techComparisons = new ArrayList<>();
        int totalImproved = 0;
        int totalDeclined = 0;
        int totalUnchanged = 0;
        int totalNewEvidence = 0;
        int totalInsufficient = 0;
        int totalUnassessed = 0;

        for (Technology tech : reassessment.getSelectedTechnologies()) {
            TechnologyAssessmentResult newResult = reassessmentResultMap.get(tech.getSlug());
            TechnologyAssessmentResult prevResult = baselineResultMap.get(tech.getSlug());

            if (newResult == null) {
                continue;
            }

            boolean isComparable = false;
            String incomparableReason = null;
            Integer scoreDelta = null;

            if (prevResult == null) {
                isComparable = false;
                incomparableReason = "No previous baseline assessment found for " + tech.getName() + " to establish a delta.";
            } else if (newResult.getEvaluatedQuestionsCount() < 2 ||
                    "Low".equalsIgnoreCase(newResult.getConfidenceLevel()) ||
                    "INSUFFICIENT_EVIDENCE".equalsIgnoreCase(newResult.getEstimatedProficiency())) {
                isComparable = false;
                incomparableReason = "Reassessment session has insufficient topic coverage or low confidence level (" + newResult.getConfidenceLevel() + ").";
            } else if (prevResult.getEvaluatedQuestionsCount() < 2 ||
                    "Low".equalsIgnoreCase(prevResult.getConfidenceLevel()) ||
                    "INSUFFICIENT_EVIDENCE".equalsIgnoreCase(prevResult.getEstimatedProficiency())) {
                isComparable = false;
                incomparableReason = "Baseline assessment for " + tech.getName() + " has insufficient evidence or low confidence (" + prevResult.getConfidenceLevel() + ") to establish a reliable comparison.";
            } else if (prevResult.getScoringVersion() != null && newResult.getScoringVersion() != null &&
                    !prevResult.getScoringVersion().equals(newResult.getScoringVersion())) {
                isComparable = false;
                incomparableReason = "Scoring versions/scales are incompatible (" + prevResult.getScoringVersion() + " vs " + newResult.getScoringVersion() + ").";
            } else {
                isComparable = true;
                // Rule: scoreDelta represents the absolute percentage-point difference (new - prev), e.g. +15 percentage points.
                scoreDelta = newResult.getRawScore() - prevResult.getRawScore();
            }

            Set<String> newStrengths = parseJsonSet(newResult.getStrengthsJson());
            Set<String> newWeaknesses = parseJsonSet(newResult.getWeaknessesJson());
            Set<String> newCoverage = parseJsonSet(newResult.getTopicCoverageJson());

            Set<String> prevStrengths = prevResult != null ? parseJsonSet(prevResult.getStrengthsJson()) : Collections.emptySet();
            Set<String> prevWeaknesses = prevResult != null ? parseJsonSet(prevResult.getWeaknessesJson()) : Collections.emptySet();
            Set<String> prevCoverage = prevResult != null ? parseJsonSet(prevResult.getTopicCoverageJson()) : Collections.emptySet();

            List<CompetencyNode> nodes = competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(tech);
            List<CompetencyChangeDto> compChanges = new ArrayList<>();

            for (CompetencyNode node : nodes) {
                boolean wasCovered = matchesTopic(node, prevCoverage);
                boolean wasStrength = matchesTopic(node, prevStrengths);
                boolean wasWeakness = matchesTopic(node, prevWeaknesses);

                boolean isCoveredNow = matchesTopic(node, newCoverage);
                boolean isStrengthNow = matchesTopic(node, newStrengths);
                boolean isWeaknessNow = matchesTopic(node, newWeaknesses);

                String prevStatus = wasStrength ? "STRENGTH" : (wasWeakness ? "WEAKNESS" : (wasCovered ? "ASSESSED" : "UNASSESSED"));
                String currentStatus = isStrengthNow ? "STRENGTH" : (isWeaknessNow ? "WEAKNESS" : (isCoveredNow ? "ASSESSED" : "NOT_REASSESSED"));

                Integer prevCompScore = prevResult != null && wasCovered ? (wasStrength ? prevResult.getRawScore() : (wasWeakness ? 0 : prevResult.getRawScore())) : null;
                Integer currentCompScore = isCoveredNow ? (isStrengthNow ? newResult.getRawScore() : (isWeaknessNow ? 0 : newResult.getRawScore())) : null;
                Integer compScoreDelta = (prevCompScore != null && currentCompScore != null) ? (currentCompScore - prevCompScore) : null;

                CompetencyChangeClassification classification;
                String explanation;

                if (!isCoveredNow) {
                    classification = CompetencyChangeClassification.NOT_REASSESSED;
                    explanation = "Competency was not reassessed in this session: previous standing preserved.";
                    totalUnassessed++;
                } else if (!isComparable) {
                    // When evidence is not comparable, NEVER claim improvement or decline!
                    if (prevResult == null || !wasCovered) {
                        classification = CompetencyChangeClassification.NEW_EVIDENCE;
                        explanation = isStrengthNow ? "Newly demonstrated strength via reassessment." : "Identified learning growth opportunity.";
                        totalNewEvidence++;
                    } else {
                        classification = CompetencyChangeClassification.INSUFFICIENT_EVIDENCE;
                        explanation = incomparableReason != null ? incomparableReason : "Evidence is not comparable to establish verified progression.";
                        totalInsufficient++;
                    }
                } else if (prevResult == null || !wasCovered) {
                    classification = CompetencyChangeClassification.NEW_EVIDENCE;
                    explanation = isStrengthNow ? "Newly demonstrated strength via reassessment." : "Identified learning growth opportunity.";
                    totalNewEvidence++;
                } else if ((wasWeakness || !wasStrength) && isStrengthNow) {
                    classification = CompetencyChangeClassification.IMPROVED;
                    explanation = "Demonstrated marked improvement from prior weakness to confirmed competency.";
                    totalImproved++;
                } else if (wasStrength && isWeaknessNow) {
                    classification = CompetencyChangeClassification.DECLINED;
                    explanation = "Performance declined on this topic in reassessment compared to baseline.";
                    totalDeclined++;
                } else if ((wasStrength && isStrengthNow) || (wasWeakness && isWeaknessNow)) {
                    classification = CompetencyChangeClassification.UNCHANGED;
                    explanation = isStrengthNow ? "Consistently demonstrated competency." : "Persisting gap: recommended for focused review.";
                    totalUnchanged++;
                } else {
                    classification = CompetencyChangeClassification.UNCHANGED;
                    explanation = "Competency score consistent with baseline evidence.";
                    totalUnchanged++;
                }

                compChanges.add(CompetencyChangeDto.builder()
                        .competencyKey(node.getCompetencyKey())
                        .competencyTitle(node.getTitle())
                        .technologySlug(tech.getSlug())
                        .level(node.getLevel() != null ? node.getLevel() : "Intermediate")
                        .classification(classification)
                        .previousStatus(prevStatus)
                        .newStatus(currentStatus)
                        .previousScore(prevCompScore)
                        .newScore(currentCompScore)
                        .scoreDelta(compScoreDelta)
                        .isComparable(isComparable && prevCompScore != null && currentCompScore != null)
                        .explanation(explanation)
                        .build());
            }

            techComparisons.add(TechnologyComparisonDto.builder()
                    .technologySlug(tech.getSlug())
                    .technologyName(tech.getName())
                    .previousScore(prevResult != null ? prevResult.getRawScore() : null)
                    .newScore(newResult.getRawScore())
                    .scoreDelta(scoreDelta)
                    .previousProficiency(prevResult != null ? prevResult.getEstimatedProficiency() : null)
                    .newProficiency(newResult.getEstimatedProficiency())
                    .previousConfidence(prevResult != null ? prevResult.getConfidenceLevel() : null)
                    .newConfidence(newResult.getConfidenceLevel())
                    .isComparable(isComparable)
                    .incomparableReason(incomparableReason)
                    .competencies(compChanges)
                    .build());
        }

        String summary = String.format("Reassessment compared: %d improved, %d declined, %d unchanged, %d new evidence, %d insufficient evidence, %d unassessed.",
                totalImproved, totalDeclined, totalUnchanged, totalNewEvidence, totalInsufficient, totalUnassessed);

        return AssessmentComparisonDto.builder()
                .reassessmentSessionId(reassessment.getId())
                .baselineSessionId(baseline != null ? baseline.getId() : null)
                .reassessmentDate(reassessment.getSubmittedAt() != null ? reassessment.getSubmittedAt() : reassessment.getCreatedAt())
                .baselineDate(baseline != null ? (baseline.getSubmittedAt() != null ? baseline.getSubmittedAt() : baseline.getCreatedAt()) : null)
                .technologyComparisons(techComparisons)
                .improvedCompetenciesCount(totalImproved)
                .declinedCompetenciesCount(totalDeclined)
                .unchangedCompetenciesCount(totalUnchanged)
                .newEvidenceCompetenciesCount(totalNewEvidence)
                .insufficientEvidenceCount(totalInsufficient)
                .unassessedCompetenciesCount(totalUnassessed)
                .summary(summary)
                .build();
    }

    public static boolean matchesTopic(CompetencyNode node, Set<String> topics) {
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

    private Set<String> parseJsonSet(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptySet();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return new HashSet<>(list);
        } catch (Exception e) {
            logger.warn("Failed to parse JSON set: {}", json, e);
            return Collections.emptySet();
        }
    }
}
