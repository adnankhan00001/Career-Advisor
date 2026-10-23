package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadmapAdaptiveUpdateServiceTests {

    @Mock
    private RoadmapUpdateProposalRepository proposalRepository;

    @Mock
    private RoadmapProposalChangeItemRepository changeItemRepository;

    @Mock
    private PersonalizedRoadmapRepository personalizedRoadmapRepository;

    @Mock
    private PersonalizedRoadmapStepRepository stepRepository;

    @Mock
    private PersonalizedRoadmapMilestoneRepository milestoneRepository;

    @Mock
    private AssessmentSessionRepository assessmentSessionRepository;

    @Mock
    private CompetencyNodeRepository competencyNodeRepository;

    @Mock
    private CompetencyPrerequisiteRepository competencyPrerequisiteRepository;

    @Mock
    private AssessmentComparisonService comparisonService;

    @Mock
    private PersonalizedRoadmapService personalizedRoadmapService;

    @InjectMocks
    private RoadmapAdaptiveUpdateService updateService;

    private User student;
    private User otherStudent;
    private PersonalizedRoadmap roadmap;
    private PersonalizedRoadmapMilestone milestone1;
    private PersonalizedRoadmapMilestone milestone2;
    private PersonalizedRoadmapStep stepSyntax;
    private PersonalizedRoadmapStep stepOop;
    private PersonalizedRoadmapStep stepCompleted;
    private PersonalizedRoadmapStep stepEvidencePending;
    private AssessmentSession reassessmentSession;

    @BeforeEach
    void setUp() {
        student = User.builder().id(100L).email("student@test.com").name("Alice").build();
        otherStudent = User.builder().id(200L).email("eve@test.com").name("Eve").build();

        roadmap = PersonalizedRoadmap.builder()
                .id(500L)
                .user(student)
                .careerTitle("Java Backend Developer")
                .status("ACTIVE")
                .dailyStudyMinutes(60)
                .version(1L)
                .milestones(new ArrayList<>())
                .build();

        milestone1 = PersonalizedRoadmapMilestone.builder()
                .id(10L)
                .roadmap(roadmap)
                .title("Milestone 1: Java Basics")
                .technologySlug("java")
                .isCustom(false)
                .isSkipped(false)
                .steps(new ArrayList<>())
                .build();

        stepSyntax = PersonalizedRoadmapStep.builder()
                .id(101L)
                .milestone(milestone1)
                .title("Java Syntax & Basics")
                .competencyKey("java-syntax")
                .level("Beginner")
                .estimatedMinutes(60)
                .priority("CORE")
                .isCompleted(false)
                .state("NOT_STARTED")
                .isTestedOut(false)
                .build();

        stepOop = PersonalizedRoadmapStep.builder()
                .id(102L)
                .milestone(milestone1)
                .title("Java Object-Oriented Programming")
                .competencyKey("java-oop")
                .level("Intermediate")
                .estimatedMinutes(60)
                .priority("CORE")
                .isCompleted(false)
                .state("NOT_STARTED")
                .isTestedOut(false)
                .build();

        milestone1.addStep(stepSyntax);
        milestone1.addStep(stepOop);
        roadmap.addMilestone(milestone1);

        milestone2 = PersonalizedRoadmapMilestone.builder()
                .id(20L)
                .roadmap(roadmap)
                .title("Milestone 2: Practice & Portfolio")
                .isCustom(false)
                .isSkipped(false)
                .steps(new ArrayList<>())
                .build();

        stepCompleted = PersonalizedRoadmapStep.builder()
                .id(201L)
                .milestone(milestone2)
                .title("Complete Coding Exercises")
                .competencyKey("java-exercises")
                .level("Intermediate")
                .estimatedMinutes(60)
                .priority("CORE")
                .isCompleted(true)
                .state("COMPLETED")
                .isTestedOut(false)
                .build();

        stepEvidencePending = PersonalizedRoadmapStep.builder()
                .id(202L)
                .milestone(milestone2)
                .title("Build Full-Stack Capstone")
                .competencyKey("capstone-project")
                .level("Advanced")
                .estimatedMinutes(120)
                .priority("CORE")
                .isCompleted(false)
                .state("VERIFICATION_PENDING")
                .isTestedOut(false)
                .build();

        milestone2.addStep(stepCompleted);
        milestone2.addStep(stepEvidencePending);
        roadmap.addMilestone(milestone2);

        reassessmentSession = AssessmentSession.builder()
                .id(777L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .isReassessment(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should generate preview without mutating active roadmap and mark beginner strength as tested out")
    void testGenerateProposalPreview_success() {
        GenerateRoadmapPreviewRequest request = GenerateRoadmapPreviewRequest.builder()
                .reassessmentSessionId(777L)
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(roadmap));
        when(assessmentSessionRepository.findById(777L))
                .thenReturn(Optional.of(reassessmentSession));

        // Mock comparison: syntax is IMPROVED to strength, oop is DECLINED
        AssessmentComparisonDto mockComparison = AssessmentComparisonDto.builder()
                .reassessmentSessionId(777L)
                .technologyComparisons(List.of(
                        TechnologyComparisonDto.builder()
                                .technologySlug("java")
                                .technologyName("Java")
                                .previousScore(50)
                                .newScore(85)
                                .scoreDelta(35)
                                .isComparable(true)
                                .competencies(List.of(
                                        CompetencyChangeDto.builder()
                                                .competencyKey("java-syntax")
                                                .competencyTitle("Java Syntax & Basics")
                                                .level("Beginner")
                                                .classification(CompetencyChangeClassification.IMPROVED)
                                                .newStatus("STRENGTH")
                                                .newScore(85)
                                                .build(),
                                        CompetencyChangeDto.builder()
                                                .competencyKey("java-oop")
                                                .competencyTitle("Java Object-Oriented Programming")
                                                .level("Intermediate")
                                                .classification(CompetencyChangeClassification.DECLINED)
                                                .newStatus("WEAKNESS")
                                                .newScore(40)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(comparisonService.compareSessions(eq(reassessmentSession), any(), eq(student)))
                .thenReturn(mockComparison);
        when(proposalRepository.save(any(RoadmapUpdateProposal.class)))
                .thenAnswer(invocation -> {
                    RoadmapUpdateProposal p = invocation.getArgument(0);
                    p.setId(888L);
                    return p;
                });

        RoadmapUpdateProposalDto preview = updateService.generateProposalPreview(student, request);

        assertNotNull(preview);
        assertEquals(ProposalStatus.PENDING, preview.getStatus());
        assertEquals(888L, preview.getId());
        assertEquals(1L, preview.getRoadmapVersion());

        // Verify active roadmap is NOT mutated
        assertFalse(stepSyntax.isTestedOut());
        assertEquals("CORE", stepSyntax.getPriority());

        // Check proposed items
        List<RoadmapProposalChangeItemDto> items = preview.getChangeItems();

        // 1. Syntax is Beginner and improved -> MARK_AS_POTENTIALLY_TESTED_OUT
        RoadmapProposalChangeItemDto syntaxItem = items.stream()
                .filter(i -> i.getStepId().equals(101L))
                .findFirst().orElseThrow();
        assertEquals(RoadmapChangeAction.MARK_AS_POTENTIALLY_TESTED_OUT, syntaxItem.getActionType());
        assertTrue(syntaxItem.getProposedTestedOut());
        assertEquals("TESTED_OUT", syntaxItem.getProposedPriority());

        // 2. OOP declined -> UPDATE_RECOMMENDED_PRIORITY to HIGH_PRIORITY
        RoadmapProposalChangeItemDto oopItem = items.stream()
                .filter(i -> i.getStepId().equals(102L))
                .findFirst().orElseThrow();
        assertEquals(RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY, oopItem.getActionType());
        assertEquals("HIGH_PRIORITY", oopItem.getProposedPriority());

        // 3. Completed step -> KEEP_EXISTING_STEP
        RoadmapProposalChangeItemDto completedItem = items.stream()
                .filter(i -> i.getStepId().equals(201L))
                .findFirst().orElseThrow();
        assertEquals(RoadmapChangeAction.KEEP_EXISTING_STEP, completedItem.getActionType());
        assertTrue(completedItem.getReasoning().contains("Completed step"));

        // 4. Verification pending step -> KEEP_EXISTING_STEP
        RoadmapProposalChangeItemDto pendingItem = items.stream()
                .filter(i -> i.getStepId().equals(202L))
                .findFirst().orElseThrow();
        assertEquals(RoadmapChangeAction.KEEP_EXISTING_STEP, pendingItem.getActionType());
        assertTrue(pendingItem.getReasoning().contains("awaiting verification"));
    }

    @Test
    @DisplayName("Should protect prerequisite dependencies: retain as foundational prerequisite instead of removing")
    void testGenerateProposalPreview_protectsPrerequisiteDependencies() {
        GenerateRoadmapPreviewRequest request = GenerateRoadmapPreviewRequest.builder()
                .reassessmentSessionId(777L)
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(roadmap));
        when(assessmentSessionRepository.findById(777L))
                .thenReturn(Optional.of(reassessmentSession));

        // Mock comparison: syntax is IMPROVED
        AssessmentComparisonDto mockComparison = AssessmentComparisonDto.builder()
                .reassessmentSessionId(777L)
                .technologyComparisons(List.of(
                        TechnologyComparisonDto.builder()
                                .technologySlug("java")
                                .technologyName("Java")
                                .competencies(List.of(
                                        CompetencyChangeDto.builder()
                                                .competencyKey("java-syntax")
                                                .competencyTitle("Java Syntax & Basics")
                                                .level("Beginner")
                                                .classification(CompetencyChangeClassification.IMPROVED)
                                                .newStatus("STRENGTH")
                                                .newScore(90)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(comparisonService.compareSessions(eq(reassessmentSession), any(), eq(student)))
                .thenReturn(mockComparison);

        // Prerequisite mock: Java OOP depends on Java Syntax! And OOP is uncompleted in the roadmap!
        CompetencyNode syntaxNode = CompetencyNode.builder().id(10L).competencyKey("java-syntax").build();
        CompetencyNode oopNode = CompetencyNode.builder().id(11L).competencyKey("java-oop").build();
        when(competencyNodeRepository.findByCompetencyKey("java-syntax")).thenReturn(Optional.of(syntaxNode));
        when(competencyPrerequisiteRepository.findByPrerequisite(syntaxNode)).thenReturn(List.of(
                CompetencyPrerequisite.builder().competency(oopNode).prerequisite(syntaxNode).build()
        ));

        when(proposalRepository.save(any(RoadmapUpdateProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoadmapUpdateProposalDto preview = updateService.generateProposalPreview(student, request);

        RoadmapProposalChangeItemDto syntaxItem = preview.getChangeItems().stream()
                .filter(i -> i.getStepId().equals(101L))
                .findFirst().orElseThrow();

        // Must NOT mark as tested out because OOP still requires it as a foundational prerequisite!
        assertEquals(RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY, syntaxItem.getActionType());
        assertEquals("RECOMMENDED", syntaxItem.getProposedPriority());
        assertFalse(syntaxItem.getProposedTestedOut());
        assertTrue(syntaxItem.getReasoning().contains("retained as foundational prerequisite"));
    }

    @Test
    @DisplayName("Should apply proposal transactionally, idempotently, and log revision history")
    void testApplyProposal_success() {
        RoadmapProposalChangeItem item1 = RoadmapProposalChangeItem.builder()
                .actionType(RoadmapChangeAction.MARK_AS_POTENTIALLY_TESTED_OUT)
                .stepId(101L)
                .stepTitle("Java Syntax & Basics")
                .proposedPriority("TESTED_OUT")
                .proposedTestedOut(true)
                .build();

        RoadmapProposalChangeItem item2 = RoadmapProposalChangeItem.builder()
                .actionType(RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY)
                .stepId(102L)
                .stepTitle("Java Object-Oriented Programming")
                .proposedPriority("HIGH_PRIORITY")
                .proposedTestedOut(false)
                .build();

        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .id(999L)
                .user(student)
                .roadmap(roadmap)
                .roadmapVersion(1L)
                .sourceSession(reassessmentSession)
                .status(ProposalStatus.PENDING)
                .changeItems(List.of(item1, item2))
                .proposedTargetCompletionDate(LocalDate.now().plusWeeks(4))
                .build();

        when(proposalRepository.findByIdWithChangeItems(999L)).thenReturn(Optional.of(proposal));
        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(roadmap));
        when(personalizedRoadmapRepository.save(roadmap)).thenReturn(roadmap);
        when(personalizedRoadmapService.mapToDto(roadmap)).thenReturn(PersonalizedRoadmapDto.builder().id(500L).build());

        PersonalizedRoadmapDto result = updateService.applyProposal(student, 999L);

        assertNotNull(result);
        assertEquals(ProposalStatus.APPLIED, proposal.getStatus());
        assertNotNull(proposal.getAppliedAt());

        // Step mutations applied
        assertTrue(stepSyntax.isTestedOut());
        assertEquals("TESTED_OUT", stepSyntax.getPriority());
        assertEquals("HIGH_PRIORITY", stepOop.getPriority());

        // Completed step preserved intact
        assertTrue(stepCompleted.isCompleted());

        // Verify revision recorded
        verify(personalizedRoadmapService).recordRevision(eq(roadmap), eq("ADAPTIVE_REASSESSMENT_UPDATE"),
                isNull(), isNull(), any(), any(), any(), eq(student.getEmail()));
    }

    @Test
    @DisplayName("Should reject applying proposal when roadmap status is inactive")
    void testApplyProposal_rejectsInactiveRoadmap() {
        roadmap.setStatus("ARCHIVED");

        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .id(999L)
                .user(student)
                .roadmap(roadmap)
                .roadmapVersion(1L)
                .status(ProposalStatus.PENDING)
                .build();

        when(proposalRepository.findByIdWithChangeItems(999L)).thenReturn(Optional.of(proposal));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                updateService.applyProposal(student, 999L));
        assertTrue(ex.getMessage().contains("inactive roadmap"));
    }

    @Test
    @DisplayName("Should reject applying proposal when roadmap is no longer the current active roadmap")
    void testApplyProposal_rejectsMismatchedActiveRoadmap() {
        PersonalizedRoadmap newerRoadmap = PersonalizedRoadmap.builder()
                .id(600L)
                .user(student)
                .careerTitle("Java Backend Developer")
                .status("ACTIVE")
                .build();

        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .id(999L)
                .user(student)
                .roadmap(roadmap) // roadmap id = 500L
                .roadmapVersion(1L)
                .status(ProposalStatus.PENDING)
                .build();

        when(proposalRepository.findByIdWithChangeItems(999L)).thenReturn(Optional.of(proposal));
        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(newerRoadmap));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                updateService.applyProposal(student, 999L));
        assertTrue(ex.getMessage().contains("does not reference your current active roadmap"));
    }

    @Test
    @DisplayName("Should strictly preserve steps in IN_PROGRESS or VERIFICATION_PENDING states")
    void testApplyProposal_preservesInProgressAndVerificationPending() {
        stepSyntax.setState("IN_PROGRESS");
        stepOop.setState("VERIFICATION_PENDING");

        RoadmapProposalChangeItem item1 = RoadmapProposalChangeItem.builder()
                .stepId(stepSyntax.getId())
                .actionType(RoadmapChangeAction.MARK_AS_POTENTIALLY_TESTED_OUT)
                .proposedTestedOut(true)
                .proposedPriority("TESTED_OUT")
                .build();

        RoadmapProposalChangeItem item2 = RoadmapProposalChangeItem.builder()
                .stepId(stepOop.getId())
                .actionType(RoadmapChangeAction.UPDATE_RECOMMENDED_PRIORITY)
                .proposedPriority("RECOMMENDED")
                .build();

        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .id(999L)
                .user(student)
                .roadmap(roadmap)
                .roadmapVersion(1L)
                .sourceSession(reassessmentSession)
                .status(ProposalStatus.PENDING)
                .changeItems(List.of(item1, item2))
                .build();

        when(proposalRepository.findByIdWithChangeItems(999L)).thenReturn(Optional.of(proposal));
        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(roadmap));
        when(personalizedRoadmapRepository.save(roadmap)).thenReturn(roadmap);
        when(personalizedRoadmapService.mapToDto(roadmap)).thenReturn(PersonalizedRoadmapDto.builder().id(500L).build());

        updateService.applyProposal(student, 999L);

        // Neither step was modified because one is IN_PROGRESS and the other is VERIFICATION_PENDING
        assertFalse(stepSyntax.isTestedOut());
        assertEquals("CORE", stepSyntax.getPriority());
        assertEquals("IN_PROGRESS", stepSyntax.getState());

        assertEquals("CORE", stepOop.getPriority());
        assertEquals("VERIFICATION_PENDING", stepOop.getState());
    }

    @Test
    @DisplayName("Should reject stale proposal when roadmap version has changed")
    void testApplyProposal_staleVersionMismatch() {
        roadmap.setVersion(2L); // Incremented since proposal creation

        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .id(999L)
                .user(student)
                .roadmap(roadmap)
                .roadmapVersion(1L) // Older version token
                .status(ProposalStatus.PENDING)
                .build();

        when(proposalRepository.findByIdWithChangeItems(999L)).thenReturn(Optional.of(proposal));
        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(roadmap));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                updateService.applyProposal(student, 999L));

        assertTrue(ex.getMessage().contains("version conflict"));
    }

    @Test
    @DisplayName("Should reject applying proposal if status is not PENDING")
    void testApplyProposal_alreadyApplied() {
        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .id(999L)
                .user(student)
                .roadmap(roadmap)
                .status(ProposalStatus.APPLIED)
                .build();

        when(proposalRepository.findByIdWithChangeItems(999L)).thenReturn(Optional.of(proposal));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                updateService.applyProposal(student, 999L));

        assertTrue(ex.getMessage().contains("is not PENDING"));
    }

    @Test
    @DisplayName("Should reject applying proposal owned by another student")
    void testApplyProposal_unauthorizedOwnership() {
        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .id(999L)
                .user(otherStudent)
                .roadmap(roadmap)
                .status(ProposalStatus.PENDING)
                .build();

        when(proposalRepository.findByIdWithChangeItems(999L)).thenReturn(Optional.of(proposal));

        assertThrows(SecurityException.class, () ->
                updateService.applyProposal(student, 999L));
    }

    @Test
    @DisplayName("Should explicitly reject proposal and leave active roadmap completely untouched")
    void testRejectProposal_success() {
        RoadmapUpdateProposal proposal = RoadmapUpdateProposal.builder()
                .id(888L)
                .user(student)
                .roadmap(roadmap)
                .status(ProposalStatus.PENDING)
                .changeItems(new ArrayList<>())
                .build();

        when(proposalRepository.findByIdWithChangeItems(888L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        RoadmapUpdateProposalDto result = updateService.rejectProposal(student, 888L, "Not ready to skip topics");

        assertNotNull(result);
        assertEquals(ProposalStatus.REJECTED, proposal.getStatus());
        assertEquals("Not ready to skip topics", proposal.getRejectionReason());
        assertNotNull(proposal.getRejectedAt());

        // Roadmap was never modified or saved
        verify(personalizedRoadmapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve proposal history ordered by creation date")
    void testGetProposalHistory() {
        RoadmapUpdateProposal p1 = RoadmapUpdateProposal.builder()
                .id(1L)
                .user(student)
                .sourceSession(reassessmentSession)
                .status(ProposalStatus.APPLIED)
                .testedOutCandidatesCount(2)
                .priorityUpdatesCount(1)
                .preservedCompletedCount(3)
                .proposedRemainingHours(10.0)
                .createdAt(LocalDateTime.now().minusDays(1))
                .appliedAt(LocalDateTime.now())
                .build();

        when(proposalRepository.findByUserOrderByCreatedAtDesc(student)).thenReturn(List.of(p1));

        List<ProposalSummaryDto> history = updateService.getProposalHistory(student);

        assertEquals(1, history.size());
        assertEquals(1L, history.get(0).getId());
        assertEquals("APPLIED", history.get(0).getStatus());
        assertEquals(2, history.get(0).getTestedOutCandidatesCount());
    }
}
