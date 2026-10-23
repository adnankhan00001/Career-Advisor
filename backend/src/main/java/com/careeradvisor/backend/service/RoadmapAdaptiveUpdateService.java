package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoadmapAdaptiveUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(RoadmapAdaptiveUpdateService.class);

    private final RoadmapUpdateProposalRepository proposalRepository;
    private final RoadmapProposalChangeItemRepository changeItemRepository;
    private final PersonalizedRoadmapRepository personalizedRoadmapRepository;
    private final PersonalizedRoadmapStepRepository stepRepository;
    private final PersonalizedRoadmapMilestoneRepository milestoneRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final CompetencyNodeRepository competencyNodeRepository;
    private final CompetencyPrerequisiteRepository competencyPrerequisiteRepository;
    private final AssessmentComparisonService comparisonService;
    private final PersonalizedRoadmapService personalizedRoadmapService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public RoadmapAdaptiveUpdateService(RoadmapUpdateProposalRepository proposalRepository,
                                         RoadmapProposalChangeItemRepository changeItemRepository,
                                         PersonalizedRoadmapRepository personalizedRoadmapRepository,
                                         PersonalizedRoadmapStepRepository stepRepository,
                                         PersonalizedRoadmapMilestoneRepository milestoneRepository,
                                         AssessmentSessionRepository assessmentSessionRepository,
                                         CompetencyNodeRepository competencyNodeRepository,
                                         CompetencyPrerequisiteRepository competencyPrerequisiteRepository,
                                         AssessmentComparisonService comparisonService,
                                         PersonalizedRoadmapService personalizedRoadmapService) {
        this.proposalRepository = proposalRepository;
        this.changeItemRepository = changeItemRepository;
        this.personalizedRoadmapRepository = personalizedRoadmapRepository;
        this.stepRepository = stepRepository;
        this.milestoneRepository = milestoneRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.competencyNodeRepository = competencyNodeRepository;
        this.competencyPrerequisiteRepository = competencyPrerequisiteRepository;
        this.comparisonService = comparisonService;
        this.personalizedRoadmapService = personalizedRoadmapService;
    }

    /**
     * Evaluates new reassessment evidence against current roadmap and generates a proposal preview.
     * Does NOT mutate active roadmap.
     */
    @Transactional
    public RoadmapUpdateProposalDto generateProposalPreview(User user, GenerateRoadmapPreviewRequest request) {
        if (request == null || request.getReassessmentSessionId() == null) {
            throw new IllegalArgumentException("Reassessment session ID is required");
        }

        // 1. Fetch active roadmap
        PersonalizedRoadmap roadmap = personalizedRoadmapRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active personalized roadmap found for user. Please generate a roadmap first."));

        // 2. Fetch and validate reassessment session
        AssessmentSession reassessment = assessmentSessionRepository.findById(request.getReassessmentSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Assessment session not found: " + request.getReassessmentSessionId()));

        if (!reassessment.getUser().getId().equals(user.getId())) {
            throw new SecurityException("User does not own assessment session #" + request.getReassessmentSessionId());
        }

        if (reassessment.getStatus() != AssessmentStatus.SUBMITTED) {
            throw new IllegalStateException("Assessment session #" + reassessment.getId() + " is not SUBMITTED. Current status: " + reassessment.getStatus());
        }

        // 3. Fetch optional baseline session
        AssessmentSession baseline = null;
        if (request.getBaselineSessionId() != null) {
            baseline = assessmentSessionRepository.findById(request.getBaselineSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Baseline session not found: " + request.getBaselineSessionId()));
            if (!baseline.getUser().getId().equals(user.getId())) {
                throw new SecurityException("User does not own baseline assessment session");
            }
        }

        // 4. Compare sessions
        AssessmentComparisonDto comparison = comparisonService.compareSessions(reassessment, baseline, user);

        // 5. Index competency changes for lookup
        Map<String, CompetencyChangeDto> compChangeByKey = new HashMap<>();
        Map<String, CompetencyChangeDto> compChangeByTitle = new HashMap<>();
        for (TechnologyComparisonDto tc : comparison.getTechnologyComparisons()) {
            for (CompetencyChangeDto cc : tc.getCompetencies()) {
                if (cc.getCompetencyKey() != null) {
                    compChangeByKey.put(cc.getCompetencyKey().toLowerCase(), cc);
                }
                if (cc.getCompetencyTitle() != null) {
                    compChangeByTitle.put(cc.getCompetencyTitle().toLowerCase(), cc);
                }
            }
        }

        // 6. Prerequisite dependency graph analysis: identify uncompleted steps in roadmap
        Set<String> uncompletedStepCompetencyKeys = new HashSet<>();
        for (PersonalizedRoadmapMilestone m : roadmap.getMilestones()) {
            if (!m.isSkipped()) {
                for (PersonalizedRoadmapStep s : m.getSteps()) {
                    if (!s.isCompleted() && s.getCompetencyKey() != null) {
                        uncompletedStepCompetencyKeys.add(s.getCompetencyKey().toLowerCase());
                    }
                }
            }
        }

        // 7. Generate change items
        List<RoadmapProposalChangeItem> changeItems = new ArrayList<>();
        int testedOutCandidatesCount = 0;
        int priorityUpdatesCount = 0;
        int preservedCompletedCount = 0;
        int seq = 1;

        int dailyMinutes = roadmap.getDailyStudyMinutes() != null ? Math.min(480, Math.max(15, roadmap.getDailyStudyMinutes())) : 60;
        int weeklyCapacityMinutes = Math.max(75, dailyMinutes * 5);

        int currentRemainingMinutes = 0;
        int proposedRemainingMinutes = 0;

        for (PersonalizedRoadmapMilestone milestone : roadmap.getMilestones()) {
            boolean isMilestoneSkipped = milestone.isSkipped();

            for (PersonalizedRoadmapStep step : milestone.getSteps()) {
                boolean isCompleted = step.isCompleted() || "COMPLETED".equalsIgnoreCase(step.getState());
                boolean isTestedOut = step.isTestedOut();
                int estMinutes = step.getEstimatedMinutes();

                if (!isMilestoneSkipped && !isCompleted && !isTestedOut) {
                    currentRemainingMinutes += estMinutes;
                }

                RoadmapChangeAction action;
                String proposedPriority = step.getPriority();
                Integer proposedMinutes = estMinutes;
                Boolean proposedTestedOut = isTestedOut;
                String reasoning;

                if (milestone.isCustom()) {
                    action = RoadmapChangeAction.KEEP_EXISTING_STEP;
                    reasoning = "Custom milestone created by student: preserved intact.";
                } else if (isCompleted) {
                    action = RoadmapChangeAction.KEEP_EXISTING_STEP;
                    preservedCompletedCount++;
                    reasoning = "Completed step: preserved as historical achievement.";
                } else if ("VERIFICATION_PENDING".equalsIgnoreCase(step.getState())) {
                    action = RoadmapChangeAction.KEEP_EXISTING_STEP;
                    reasoning = "Project evidence submitted and awaiting verification: preserved intact.";
                } else if ("IN_PROGRESS".equalsIgnoreCase(step.getState())) {
                    action = RoadmapChangeAction.KEEP_EXISTING_STEP;
                    reasoning = "Topic actively in-progress: preserved.";
                } else {
                    // Canonical uncompleted step: match with comparison evidence
                    CompetencyChangeDto change = null;
                    if (step.getCompetencyKey() != null) {
                        change = compChangeByKey.get(step.getCompetencyKey().toLowerCase());
                    }
                    if (change == null && step.getTitle() != null) {
                        change = compChangeByTitle.get(step.getTitle().toLowerCase());
                    }

                    if (change == null) {
                        action = RoadmapChangeAction.NO_CHANGE_REQUIRED;
                        reasoning = "Technology was not in reassessment scope: current configuration preserved.";
                    } else if (change.getClassification() == CompetencyChangeClassification.NOT_REASSESSED) {
                        action = RoadmapChangeAction.NO_CHANGE_REQUIRED;
                        reasoning = "Topic was not reassessed in this session: current roadmap configuration retained.";
                    } else if (change.getClassification() == CompetencyChangeClassification.INSUFFICIENT_EVIDENCE) {
                        action = RoadmapChangeAction.RECOMMEND_REASSESSMENT;
                        reasoning = "Reassessment had insufficient evidence to alter curriculum. Recommend focused reassessment.";
                    } else if (change.getClassification() == CompetencyChangeClassification.IMPROVED) {
                        // Check if candidate for testing out
                        boolean isBeginnerLevel = "Beginner".equalsIgnoreCase(step.getLevel());
                        int score = change.getNewScore() != null ? change.getNewScore() : 0;

                        if (isBeginnerLevel && score >= 75 && !step.isTestedOut()) {
                            // Prerequisite check: does an uncompleted step depend on this competency?
                            boolean isPrerequisiteForUncompleted = isPrerequisiteForActiveWork(step.getCompetencyKey(), uncompletedStepCompetencyKeys);
                            if (isPrerequisiteForUncompleted) {
                                action = RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY;
                                proposedPriority = "RECOMMENDED";
                                priorityUpdatesCount++;
                                reasoning = "Demonstrated proficiency in reassessment (" + score + "%); retained as foundational prerequisite for downstream topics.";
                            } else {
                                action = RoadmapChangeAction.MARK_AS_POTENTIALLY_TESTED_OUT;
                                proposedPriority = "TESTED_OUT";
                                proposedTestedOut = true;
                                testedOutCandidatesCount++;
                                reasoning = "Demonstrated verified competency in reassessment (" + score + "%). Recommended to test out.";
                            }
                        } else {
                            action = RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY;
                            proposedPriority = "RECOMMENDED";
                            priorityUpdatesCount++;
                            reasoning = "Proficiency verified via reassessment: priority lowered from CORE to RECOMMENDED.";
                        }
                    } else if (change.getClassification() == CompetencyChangeClassification.DECLINED) {
                        action = RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY;
                        proposedPriority = "HIGH_PRIORITY";
                        priorityUpdatesCount++;
                        reasoning = "Reassessment identified this topic as an active growth opportunity: elevated to HIGH_PRIORITY.";
                    } else if (change.getClassification() == CompetencyChangeClassification.NEW_EVIDENCE && "WEAKNESS".equalsIgnoreCase(change.getNewStatus())) {
                        action = RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY;
                        proposedPriority = "HIGH_PRIORITY";
                        priorityUpdatesCount++;
                        reasoning = "New assessment evidence highlighted this topic as a learning opportunity: elevated to HIGH_PRIORITY.";
                    } else {
                        action = RoadmapChangeAction.NO_CHANGE_REQUIRED;
                        reasoning = "Reassessment result consistent with current roadmap assignment.";
                    }
                }

                if (!isMilestoneSkipped && !isCompleted && !Boolean.TRUE.equals(proposedTestedOut)) {
                    proposedRemainingMinutes += proposedMinutes;
                }

                RoadmapProposalChangeItem item = RoadmapProposalChangeItem.builder()
                        .actionType(action)
                        .milestoneId(milestone.getId())
                        .milestoneTitle(milestone.getTitle())
                        .technologySlug(milestone.getTechnologySlug())
                        .stepId(step.getId())
                        .stepTitle(step.getTitle())
                        .competencyKey(step.getCompetencyKey())
                        .currentPriority(step.getPriority())
                        .proposedPriority(proposedPriority)
                        .currentEstimatedMinutes(estMinutes)
                        .proposedEstimatedMinutes(proposedMinutes)
                        .currentTestedOut(isTestedOut)
                        .proposedTestedOut(proposedTestedOut)
                        .isCompleted(isCompleted)
                        .state(step.getState())
                        .reasoning(reasoning)
                        .sequenceOrder(seq++)
                        .build();

                changeItems.add(item);
            }
        }

        double currentRemainingHours = currentRemainingMinutes / 60.0;
        double proposedRemainingHours = proposedRemainingMinutes / 60.0;
        int currentWeeks = Math.max(1, (int) Math.ceil((double) currentRemainingMinutes / weeklyCapacityMinutes));
        int proposedWeeks = Math.max(1, (int) Math.ceil((double) proposedRemainingMinutes / weeklyCapacityMinutes));
        LocalDate proposedTargetDate = LocalDate.now().plusWeeks(proposedWeeks);

        // 8. Capture roadmap concurrency version
        Long roadmapVersion = roadmap.getVersion() != null ? roadmap.getVersion() : 0L;

        // 9. Check if an existing PENDING proposal exists for this session & roadmap; supersede it
        List<RoadmapUpdateProposal> existingPending = proposalRepository.findByRoadmapAndStatus(roadmap, ProposalStatus.PENDING);
        for (RoadmapUpdateProposal p : existingPending) {
            p.setStatus(ProposalStatus.SUPERSEDED);
            proposalRepository.save(p);
        }

        String comparisonJson = null;
        try {
            comparisonJson = objectMapper.writeValueAsString(comparison);
        } catch (Exception e) {
            logger.warn("Failed to serialize comparison JSON", e);
        }

        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .user(user)
                .roadmap(roadmap)
                .roadmapVersion(roadmapVersion)
                .sourceSession(reassessment)
                .baselineSession(baseline)
                .status(ProposalStatus.PENDING)
                .comparisonSummaryJson(comparisonJson)
                .proposedEstimatedWeeks(proposedWeeks)
                .proposedTargetCompletionDate(proposedTargetDate)
                .currentRemainingHours(currentRemainingHours)
                .proposedRemainingHours(proposedRemainingHours)
                .testedOutCandidatesCount(testedOutCandidatesCount)
                .priorityUpdatesCount(priorityUpdatesCount)
                .preservedCompletedCount(preservedCompletedCount)
                .changeItems(new ArrayList<>())
                .build();

        for (RoadmapProposalChangeItem item : changeItems) {
            proposal.addChangeItem(item);
        }

        proposal = proposalRepository.save(proposal);

        logger.info("Generated roadmap update proposal #{} for user {} from session #{}: {} tested out candidates, {} reprioritized",
                proposal.getId(), user.getEmail(), reassessment.getId(), testedOutCandidatesCount, priorityUpdatesCount);

        return mapToProposalDto(proposal, comparison, roadmap);
    }

    /**
     * Retrieves an existing proposal by ID.
     */
    @Transactional(readOnly = true)
    public RoadmapUpdateProposalDto getProposal(User user, Long proposalId) {
        RoadmapUpdateProposal proposal = proposalRepository.findByIdWithChangeItems(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap proposal not found: " + proposalId));

        if (!proposal.getUser().getId().equals(user.getId())) {
            throw new SecurityException("User does not own roadmap update proposal #" + proposalId);
        }

        PersonalizedRoadmap roadmap = proposal.getRoadmap();
        boolean isStale = proposal.getStatus() == ProposalStatus.PENDING &&
                proposal.getRoadmapVersion() != null &&
                !proposal.getRoadmapVersion().equals(roadmap.getVersion());

        AssessmentComparisonDto comparison = null;
        if (proposal.getComparisonSummaryJson() != null) {
            try {
                comparison = objectMapper.readValue(proposal.getComparisonSummaryJson(), AssessmentComparisonDto.class);
            } catch (Exception e) {
                logger.warn("Failed to deserialize comparison JSON from proposal #{}", proposalId, e);
            }
        }
        if (comparison == null) {
            comparison = comparisonService.compareSessions(proposal.getSourceSession(), proposal.getBaselineSession(), user);
        }

        RoadmapUpdateProposalDto dto = mapToProposalDto(proposal, comparison, roadmap);
        dto.setStale(isStale);
        return dto;
    }

    /**
     * Applies approved roadmap update proposal transactionally.
     * Prevents stale proposals, duplicates, and unowned proposals.
     */
    @Transactional
    public PersonalizedRoadmapDto applyProposal(User user, Long proposalId) {
        RoadmapUpdateProposal proposal = proposalRepository.findByIdWithChangeItems(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap proposal not found: " + proposalId));

        if (!proposal.getUser().getId().equals(user.getId())) {
            throw new SecurityException("User does not own roadmap update proposal #" + proposalId);
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException("Proposal #" + proposalId + " is not PENDING. Current status: " + proposal.getStatus());
        }

        PersonalizedRoadmap roadmap = proposal.getRoadmap();

        // 1. Verify that roadmap is active and matches user's current active roadmap
        if (!"ACTIVE".equalsIgnoreCase(roadmap.getStatus())) {
            throw new IllegalStateException("Proposal references an inactive roadmap #" + roadmap.getId());
        }

        PersonalizedRoadmap activeRoadmap = personalizedRoadmapRepository
                .findFirstByUserAndStatusOrderByCreatedAtDesc(user, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active personalized roadmap found for user"));

        if (!activeRoadmap.getId().equals(roadmap.getId())) {
            throw new IllegalStateException("Proposal does not reference your current active roadmap");
        }

        // 2. Concurrency token validation
        if (proposal.getRoadmapVersion() != null && !proposal.getRoadmapVersion().equals(roadmap.getVersion())) {
            throw new IllegalStateException("Roadmap has been modified since proposal #" + proposalId +
                    " was generated (version conflict: proposal=" + proposal.getRoadmapVersion() +
                    ", current=" + roadmap.getVersion() + "). Please preview changes again.");
        }

        // Index roadmap steps by ID
        Map<Long, PersonalizedRoadmapStep> stepMap = new HashMap<>();
        for (PersonalizedRoadmapMilestone m : roadmap.getMilestones()) {
            for (PersonalizedRoadmapStep s : m.getSteps()) {
                stepMap.put(s.getId(), s);
            }
        }

        // Apply approved changes
        for (RoadmapProposalChangeItem item : proposal.getChangeItems()) {
            PersonalizedRoadmapStep step = stepMap.get(item.getStepId());
            if (step == null) {
                continue;
            }

            // Always preserve completed work, in-progress topics, and verification-pending steps
            if (step.isCompleted() ||
                "COMPLETED".equalsIgnoreCase(step.getState()) ||
                "IN_PROGRESS".equalsIgnoreCase(step.getState()) ||
                "VERIFICATION_PENDING".equalsIgnoreCase(step.getState())) {
                continue;
            }

            if (item.getActionType() == RoadmapChangeAction.MARK_AS_POTENTIALLY_TESTED_OUT) {
                step.setTestedOut(true);
                step.setPriority("TESTED_OUT");
            } else if (item.getActionType() == RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY) {
                step.setPriority(item.getProposedPriority());
            }

            if (item.getProposedEstimatedMinutes() != null && item.getProposedEstimatedMinutes() > 0) {
                step.setEstimatedMinutes(item.getProposedEstimatedMinutes());
            }

            stepRepository.save(step);
        }

        // Recalculate milestones and dynamic schedule
        for (PersonalizedRoadmapMilestone m : new ArrayList<>(roadmap.getMilestones())) {
            personalizedRoadmapService.recalculateMilestoneAndRoadmapProgress(roadmap, m);
        }

        personalizedRoadmapService.recalculateDynamicSchedule(roadmap);
        PersonalizedRoadmap savedRoadmap = personalizedRoadmapRepository.save(roadmap);

        // Record auditable revision history
        personalizedRoadmapService.recordRevision(savedRoadmap, "ADAPTIVE_REASSESSMENT_UPDATE", null, null,
                "Applied adaptive update from reassessment session #" + proposal.getSourceSession().getId() + " (" + proposal.getSourceSession().getOverallScore() + "%)",
                "remainingHours=" + proposal.getCurrentRemainingHours(),
                "remainingHours=" + proposal.getProposedRemainingHours() + ", testedOut=" + proposal.getTestedOutCandidatesCount() + ", reprioritized=" + proposal.getPriorityUpdatesCount(),
                user.getEmail());

        // Update proposal status
        proposal.setStatus(ProposalStatus.APPLIED);
        proposal.setAppliedAt(LocalDateTime.now());
        proposalRepository.save(proposal);

        // Mark any remaining pending proposals for this roadmap as superseded
        List<RoadmapUpdateProposal> otherPending = proposalRepository.findByRoadmapAndStatus(roadmap, ProposalStatus.PENDING);
        for (RoadmapUpdateProposal other : otherPending) {
            if (!other.getId().equals(proposal.getId())) {
                other.setStatus(ProposalStatus.SUPERSEDED);
                proposalRepository.save(other);
            }
        }

        logger.info("Successfully applied roadmap update proposal #{} for user {}", proposalId, user.getEmail());

        return personalizedRoadmapService.mapToDto(savedRoadmap);
    }

    /**
     * Explicitly rejects proposed roadmap update. Active roadmap is completely untouched.
     */
    @Transactional
    public RoadmapUpdateProposalDto rejectProposal(User user, Long proposalId, String reason) {
        RoadmapUpdateProposal proposal = proposalRepository.findByIdWithChangeItems(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Roadmap proposal not found: " + proposalId));

        if (!proposal.getUser().getId().equals(user.getId())) {
            throw new SecurityException("User does not own roadmap update proposal #" + proposalId);
        }

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException("Proposal #" + proposalId + " is not PENDING. Current status: " + proposal.getStatus());
        }

        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setRejectedAt(LocalDateTime.now());
        proposal.setRejectionReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Rejected by student");

        proposal = proposalRepository.save(proposal);

        logger.info("Rejected roadmap update proposal #{} by user {}: {}", proposalId, user.getEmail(), proposal.getRejectionReason());

        AssessmentComparisonDto comparison = null;
        if (proposal.getComparisonSummaryJson() != null) {
            try {
                comparison = objectMapper.readValue(proposal.getComparisonSummaryJson(), AssessmentComparisonDto.class);
            } catch (Exception ignored) {}
        }

        return mapToProposalDto(proposal, comparison, proposal.getRoadmap());
    }

    /**
     * Retrieves proposal history for user.
     */
    @Transactional(readOnly = true)
    public List<ProposalSummaryDto> getProposalHistory(User user) {
        List<RoadmapUpdateProposal> proposals = proposalRepository.findByUserOrderByCreatedAtDesc(user);
        return proposals.stream()
                .map(p -> ProposalSummaryDto.builder()
                        .id(p.getId())
                        .sourceSessionId(p.getSourceSession() != null ? p.getSourceSession().getId() : null)
                        .status(p.getStatus().name())
                        .testedOutCandidatesCount(p.getTestedOutCandidatesCount())
                        .priorityUpdatesCount(p.getPriorityUpdatesCount())
                        .preservedCompletedCount(p.getPreservedCompletedCount())
                        .proposedRemainingHours(p.getProposedRemainingHours())
                        .createdAt(p.getCreatedAt())
                        .appliedAt(p.getAppliedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isPrerequisiteForActiveWork(String competencyKey, Set<String> uncompletedStepCompetencyKeys) {
        if (competencyKey == null || uncompletedStepCompetencyKeys.isEmpty()) {
            return false;
        }
        Optional<CompetencyNode> nodeOpt = competencyNodeRepository.findByCompetencyKey(competencyKey);
        if (nodeOpt.isEmpty()) {
            return false;
        }
        CompetencyNode node = nodeOpt.get();
        List<CompetencyPrerequisite> dependents = competencyPrerequisiteRepository.findByPrerequisite(node);
        for (CompetencyPrerequisite dep : dependents) {
            if (dep.getCompetency() != null && dep.getCompetency().getCompetencyKey() != null) {
                if (uncompletedStepCompetencyKeys.contains(dep.getCompetency().getCompetencyKey().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private RoadmapUpdateProposalDto mapToProposalDto(RoadmapUpdateProposal proposal,
                                                      AssessmentComparisonDto comparison,
                                                      PersonalizedRoadmap roadmap) {
        List<RoadmapProposalChangeItemDto> changeItemDtos = proposal.getChangeItems().stream()
                .map(i -> RoadmapProposalChangeItemDto.builder()
                        .id(i.getId())
                        .actionType(i.getActionType())
                        .milestoneId(i.getMilestoneId())
                        .milestoneTitle(i.getMilestoneTitle())
                        .technologySlug(i.getTechnologySlug())
                        .stepId(i.getStepId())
                        .stepTitle(i.getStepTitle())
                        .competencyKey(i.getCompetencyKey())
                        .currentPriority(i.getCurrentPriority())
                        .proposedPriority(i.getProposedPriority())
                        .currentEstimatedMinutes(i.getCurrentEstimatedMinutes())
                        .proposedEstimatedMinutes(i.getProposedEstimatedMinutes())
                        .currentTestedOut(i.getCurrentTestedOut())
                        .proposedTestedOut(i.getProposedTestedOut())
                        .isCompleted(i.getIsCompleted())
                        .state(i.getState())
                        .reasoning(i.getReasoning())
                        .sequenceOrder(i.getSequenceOrder())
                        .build())
                .collect(Collectors.toList());

        int dailyMinutes = roadmap != null && roadmap.getDailyStudyMinutes() != null ? roadmap.getDailyStudyMinutes() : 60;
        int currentWeeks = roadmap != null ? Math.max(1, (int) Math.ceil((roadmap.getTotalEstimatedHours() * 60) / (dailyMinutes * 5.0))) : 1;

        return RoadmapUpdateProposalDto.builder()
                .id(proposal.getId())
                .roadmapId(roadmap != null ? roadmap.getId() : null)
                .roadmapVersion(proposal.getRoadmapVersion())
                .sourceSessionId(proposal.getSourceSession() != null ? proposal.getSourceSession().getId() : null)
                .baselineSessionId(proposal.getBaselineSession() != null ? proposal.getBaselineSession().getId() : null)
                .status(proposal.getStatus())
                .currentRoadmapTitle(roadmap != null ? roadmap.getCareerTitle() : "")
                .currentCompletionPercentage(roadmap != null ? roadmap.getProgressPercentage() : 0)
                .currentRemainingHours(proposal.getCurrentRemainingHours())
                .proposedRemainingHours(proposal.getProposedRemainingHours())
                .currentEstimatedWeeks(currentWeeks)
                .proposedEstimatedWeeks(proposal.getProposedEstimatedWeeks() != null ? proposal.getProposedEstimatedWeeks() : currentWeeks)
                .currentDailyStudyMinutes(dailyMinutes)
                .proposedTargetCompletionDate(proposal.getProposedTargetCompletionDate())
                .testedOutCandidatesCount(proposal.getTestedOutCandidatesCount())
                .priorityUpdatesCount(proposal.getPriorityUpdatesCount())
                .preservedCompletedCount(proposal.getPreservedCompletedCount())
                .comparison(comparison)
                .changeItems(changeItemDtos)
                .preservationNotice("Your completed work and existing project evidence will be preserved.")
                .createdAt(proposal.getCreatedAt())
                .appliedAt(proposal.getAppliedAt())
                .rejectedAt(proposal.getRejectedAt())
                .rejectionReason(proposal.getRejectionReason())
                .isStale(false)
                .build();
    }
}
