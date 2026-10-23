package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AssessmentComparisonDto;
import com.careeradvisor.backend.dto.CompetencyChangeDto;
import com.careeradvisor.backend.dto.TechnologyComparisonDto;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.AssessmentSessionRepository;
import com.careeradvisor.backend.repository.CompetencyNodeRepository;
import com.careeradvisor.backend.repository.TechnologyAssessmentResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentComparisonServiceTests {

    @Mock
    private AssessmentSessionRepository sessionRepository;

    @Mock
    private TechnologyAssessmentResultRepository resultRepository;

    @Mock
    private CompetencyNodeRepository competencyNodeRepository;

    @InjectMocks
    private AssessmentComparisonService comparisonService;

    private User student;
    private User otherStudent;
    private Technology javaTech;
    private CompetencyNode syntaxNode;
    private CompetencyNode oopNode;
    private CompetencyNode streamsNode;

    @BeforeEach
    void setUp() {
        student = User.builder().id(101L).email("student@test.com").name("Alice Student").build();
        otherStudent = User.builder().id(999L).email("eve@test.com").name("Eve Hacker").build();

        javaTech = Technology.builder()
                .id(1L)
                .name("Java")
                .slug("java")
                .active(true)
                .build();

        syntaxNode = CompetencyNode.builder()
                .id(10L)
                .technology(javaTech)
                .competencyKey("java-syntax")
                .title("Java Syntax & Basics")
                .level("Beginner")
                .sequenceOrder(1)
                .active(true)
                .build();

        oopNode = CompetencyNode.builder()
                .id(11L)
                .technology(javaTech)
                .competencyKey("java-oop")
                .title("Java Object-Oriented Programming")
                .level("Intermediate")
                .sequenceOrder(2)
                .active(true)
                .build();

        streamsNode = CompetencyNode.builder()
                .id(12L)
                .technology(javaTech)
                .competencyKey("java-streams")
                .title("Java Streams & Functional")
                .level("Advanced")
                .sequenceOrder(3)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should compare higher score with marked competency improvement")
    void testCompareSessions_higherScoreWithImprovement() {
        // Baseline: Java score 50%, OOP was weak
        AssessmentSession baseline = AssessmentSession.builder()
                .id(201L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now().minusDays(7))
                .build();

        TechnologyAssessmentResult baselineResult = TechnologyAssessmentResult.builder()
                .session(baseline)
                .technology(javaTech)
                .rawScore(50)
                .evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH")
                .estimatedProficiency("INTERMEDIATE")
                .topicCoverageJson("[\"Java Syntax & Basics\", \"Java Object-Oriented Programming\"]")
                .strengthsJson("[\"Java Syntax & Basics\"]")
                .weaknessesJson("[\"Java Object-Oriented Programming\"]")
                .build();

        // Reassessment: Java score 85%, OOP now strength
        AssessmentSession reassessment = AssessmentSession.builder()
                .id(202L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now())
                .isReassessment(true)
                .baselineSessionId(baseline.getId())
                .build();

        TechnologyAssessmentResult reassessResult = TechnologyAssessmentResult.builder()
                .session(reassessment)
                .technology(javaTech)
                .rawScore(85)
                .evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH")
                .estimatedProficiency("ADVANCED")
                .topicCoverageJson("[\"Java Syntax & Basics\", \"Java Object-Oriented Programming\"]")
                .strengthsJson("[\"Java Syntax & Basics\", \"Java Object-Oriented Programming\"]")
                .weaknessesJson("[]")
                .build();

        when(resultRepository.findBySessionIdWithTechnology(202L)).thenReturn(List.of(reassessResult));
        when(resultRepository.findBySessionIdWithTechnology(201L)).thenReturn(List.of(baselineResult));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech))
                .thenReturn(List.of(syntaxNode, oopNode));

        AssessmentComparisonDto comparison = comparisonService.compareSessions(reassessment, baseline, student);

        assertNotNull(comparison);
        assertEquals(202L, comparison.getReassessmentSessionId());
        assertEquals(201L, comparison.getBaselineSessionId());
        assertEquals(1, comparison.getTechnologyComparisons().size());

        TechnologyComparisonDto techComp = comparison.getTechnologyComparisons().get(0);
        assertTrue(techComp.isComparable());
        assertEquals(50, techComp.getPreviousScore());
        assertEquals(85, techComp.getNewScore());
        assertEquals(35, techComp.getScoreDelta());

        // OOP improved from weakness to strength
        CompetencyChangeDto oopChange = techComp.getCompetencies().stream()
                .filter(c -> "java-oop".equals(c.getCompetencyKey()))
                .findFirst().orElseThrow();
        assertEquals(CompetencyChangeClassification.IMPROVED, oopChange.getClassification());
        assertEquals("WEAKNESS", oopChange.getPreviousStatus());
        assertEquals("STRENGTH", oopChange.getNewStatus());

        // Syntax remained unchanged (strength both times)
        CompetencyChangeDto syntaxChange = techComp.getCompetencies().stream()
                .filter(c -> "java-syntax".equals(c.getCompetencyKey()))
                .findFirst().orElseThrow();
        assertEquals(CompetencyChangeClassification.UNCHANGED, syntaxChange.getClassification());
    }

    @Test
    @DisplayName("Should detect lower score and competency performance decline")
    void testCompareSessions_lowerScoreAndDecline() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(203L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        TechnologyAssessmentResult baselineResult = TechnologyAssessmentResult.builder()
                .session(baseline)
                .technology(javaTech)
                .rawScore(80)
                .evaluatedQuestionsCount(3)
                .confidenceLevel("HIGH")
                .estimatedProficiency("ADVANCED")
                .topicCoverageJson("[\"Java Syntax & Basics\"]")
                .strengthsJson("[\"Java Syntax & Basics\"]")
                .weaknessesJson("[]")
                .build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(204L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now())
                .isReassessment(true)
                .baselineSessionId(baseline.getId())
                .build();

        TechnologyAssessmentResult reassessResult = TechnologyAssessmentResult.builder()
                .session(reassessment)
                .technology(javaTech)
                .rawScore(40)
                .evaluatedQuestionsCount(3)
                .confidenceLevel("HIGH")
                .estimatedProficiency("BEGINNER")
                .topicCoverageJson("[\"Java Syntax & Basics\"]")
                .strengthsJson("[]")
                .weaknessesJson("[\"Java Syntax & Basics\"]")
                .build();

        when(resultRepository.findBySessionIdWithTechnology(204L)).thenReturn(List.of(reassessResult));
        when(resultRepository.findBySessionIdWithTechnology(203L)).thenReturn(List.of(baselineResult));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech))
                .thenReturn(List.of(syntaxNode));

        AssessmentComparisonDto comparison = comparisonService.compareSessions(reassessment, baseline, student);

        TechnologyComparisonDto techComp = comparison.getTechnologyComparisons().get(0);
        assertTrue(techComp.isComparable());
        assertEquals(-40, techComp.getScoreDelta());

        CompetencyChangeDto syntaxChange = techComp.getCompetencies().get(0);
        assertEquals(CompetencyChangeClassification.DECLINED, syntaxChange.getClassification());
        assertEquals("STRENGTH", syntaxChange.getPreviousStatus());
        assertEquals("WEAKNESS", syntaxChange.getNewStatus());
    }

    @Test
    @DisplayName("Should handle first assessment where no previous baseline exists")
    void testCompareSessions_noPreviousBaseline() {
        AssessmentSession reassessment = AssessmentSession.builder()
                .id(205L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now())
                .isReassessment(true)
                .baselineSessionId(null)
                .build();

        TechnologyAssessmentResult reassessResult = TechnologyAssessmentResult.builder()
                .session(reassessment)
                .technology(javaTech)
                .rawScore(90)
                .evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH")
                .estimatedProficiency("ADVANCED")
                .topicCoverageJson("[\"Java Syntax & Basics\"]")
                .strengthsJson("[\"Java Syntax & Basics\"]")
                .weaknessesJson("[]")
                .build();

        when(resultRepository.findBySessionIdWithTechnology(205L)).thenReturn(List.of(reassessResult));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech))
                .thenReturn(List.of(syntaxNode, streamsNode));

        // No prior baseline found in DB
        when(sessionRepository.findByUserOrderByCreatedAtDesc(student)).thenReturn(Collections.emptyList());

        AssessmentComparisonDto comparison = comparisonService.compareSessions(reassessment, null, student);

        assertNull(comparison.getBaselineSessionId());
        TechnologyComparisonDto techComp = comparison.getTechnologyComparisons().get(0);
        assertFalse(techComp.isComparable());
        assertNull(techComp.getPreviousScore());
        assertEquals(90, techComp.getNewScore());
        assertNotNull(techComp.getIncomparableReason());

        // Assessed node is NEW_EVIDENCE
        CompetencyChangeDto syntaxChange = techComp.getCompetencies().stream()
                .filter(c -> "java-syntax".equals(c.getCompetencyKey()))
                .findFirst().orElseThrow();
        assertEquals(CompetencyChangeClassification.NEW_EVIDENCE, syntaxChange.getClassification());

        // Unassessed node is NOT_REASSESSED
        CompetencyChangeDto streamsChange = techComp.getCompetencies().stream()
                .filter(c -> "java-streams".equals(c.getCompetencyKey()))
                .findFirst().orElseThrow();
        assertEquals(CompetencyChangeClassification.NOT_REASSESSED, streamsChange.getClassification());
    }

    @Test
    @DisplayName("Should flag insufficient evidence when question count or confidence is low")
    void testCompareSessions_insufficientEvidence() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(206L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        TechnologyAssessmentResult baselineResult = TechnologyAssessmentResult.builder()
                .session(baseline)
                .technology(javaTech)
                .rawScore(60)
                .evaluatedQuestionsCount(3)
                .confidenceLevel("HIGH")
                .estimatedProficiency("INTERMEDIATE")
                .topicCoverageJson("[\"Java Syntax & Basics\"]")
                .strengthsJson("[\"Java Syntax & Basics\"]")
                .weaknessesJson("[]")
                .build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(207L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now())
                .isReassessment(true)
                .baselineSessionId(baseline.getId())
                .build();

        // Insufficient evidence result (evaluated only 1 question, confidence LOW)
        TechnologyAssessmentResult lowEvidenceResult = TechnologyAssessmentResult.builder()
                .session(reassessment)
                .technology(javaTech)
                .rawScore(100)
                .evaluatedQuestionsCount(1)
                .confidenceLevel("LOW")
                .estimatedProficiency("INSUFFICIENT_EVIDENCE")
                .topicCoverageJson("[\"Java Syntax & Basics\"]")
                .strengthsJson("[\"Java Syntax & Basics\"]")
                .weaknessesJson("[]")
                .build();

        when(resultRepository.findBySessionIdWithTechnology(207L)).thenReturn(List.of(lowEvidenceResult));
        when(resultRepository.findBySessionIdWithTechnology(206L)).thenReturn(List.of(baselineResult));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech))
                .thenReturn(List.of(syntaxNode));

        AssessmentComparisonDto comparison = comparisonService.compareSessions(reassessment, baseline, student);

        TechnologyComparisonDto techComp = comparison.getTechnologyComparisons().get(0);
        assertFalse(techComp.isComparable());
        assertTrue(techComp.getIncomparableReason().contains("insufficient"));

        CompetencyChangeDto syntaxChange = techComp.getCompetencies().get(0);
        assertEquals(CompetencyChangeClassification.INSUFFICIENT_EVIDENCE, syntaxChange.getClassification());
    }

    @Test
    @DisplayName("Should reject unauthorized cross-user session comparison")
    void testCompareSessions_unauthorizedUser() {
        AssessmentSession reassessment = AssessmentSession.builder()
                .id(208L)
                .user(otherStudent)
                .status(AssessmentStatus.SUBMITTED)
                .build();

        assertThrows(SecurityException.class, () ->
                comparisonService.compareSessions(reassessment, null, student));
    }

    @Test
    @DisplayName("Should reject comparison if reassessment is not SUBMITTED")
    void testCompareSessions_notSubmittedSession() {
        AssessmentSession inProgressReassessment = AssessmentSession.builder()
                .id(209L)
                .user(student)
                .status(AssessmentStatus.IN_PROGRESS)
                .build();

        assertThrows(IllegalStateException.class, () ->
                comparisonService.compareSessions(inProgressReassessment, null, student));
    }

    @Test
    @DisplayName("Scenario 1: Identical scoring scales and equivalent evidence yield 0 delta and UNCHANGED")
    void testCompare_identicalScalesAndEquivalentEvidence_unchanged() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(301L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now().minusDays(10)).build();
        TechnologyAssessmentResult baseRes = TechnologyAssessmentResult.builder()
                .session(baseline).technology(javaTech).rawScore(75).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("INTERMEDIATE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(302L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .createdAt(LocalDateTime.now()).isReassessment(true).baselineSessionId(301L).build();
        TechnologyAssessmentResult reassessRes = TechnologyAssessmentResult.builder()
                .session(reassessment).technology(javaTech).rawScore(75).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("INTERMEDIATE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        when(resultRepository.findBySessionIdWithTechnology(302L)).thenReturn(List.of(reassessRes));
        when(resultRepository.findBySessionIdWithTechnology(301L)).thenReturn(List.of(baseRes));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech)).thenReturn(List.of(syntaxNode));

        AssessmentComparisonDto comp = comparisonService.compareSessions(reassessment, baseline, student);
        TechnologyComparisonDto tc = comp.getTechnologyComparisons().get(0);

        assertTrue(tc.isComparable());
        assertEquals(0, tc.getScoreDelta()); // 0 percentage points
        assertEquals(CompetencyChangeClassification.UNCHANGED, tc.getCompetencies().get(0).getClassification());
    }

    @Test
    @DisplayName("Scenario 2: Different question sets evaluating same topic")
    void testCompare_differentQuestionSets() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(303L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech)).build();
        TechnologyAssessmentResult baseRes = TechnologyAssessmentResult.builder()
                .session(baseline).technology(javaTech).rawScore(50).evaluatedQuestionsCount(3)
                .confidenceLevel("MEDIUM").estimatedProficiency("INTERMEDIATE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[]").weaknessesJson("[\"Java Syntax & Basics\"]").build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(304L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .isReassessment(true).baselineSessionId(303L).build();
        TechnologyAssessmentResult reassessRes = TechnologyAssessmentResult.builder()
                .session(reassessment).technology(javaTech).rawScore(100).evaluatedQuestionsCount(5)
                .confidenceLevel("HIGH").estimatedProficiency("ADVANCED").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        when(resultRepository.findBySessionIdWithTechnology(304L)).thenReturn(List.of(reassessRes));
        when(resultRepository.findBySessionIdWithTechnology(303L)).thenReturn(List.of(baseRes));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech)).thenReturn(List.of(syntaxNode));

        AssessmentComparisonDto comp = comparisonService.compareSessions(reassessment, baseline, student);
        TechnologyComparisonDto tc = comp.getTechnologyComparisons().get(0);

        assertTrue(tc.isComparable());
        assertEquals(50, tc.getScoreDelta()); // +50 percentage points
        assertEquals(CompetencyChangeClassification.IMPROVED, tc.getCompetencies().get(0).getClassification());
    }

    @Test
    @DisplayName("Scenario 3: Different difficulty levels and proficiency tier promotion")
    void testCompare_differentDifficultyLevels() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(305L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech)).build();
        TechnologyAssessmentResult baseRes = TechnologyAssessmentResult.builder()
                .session(baseline).technology(javaTech).rawScore(60).evaluatedQuestionsCount(3)
                .confidenceLevel("MEDIUM").estimatedProficiency("BEGINNER").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Object-Oriented Programming\"]").strengthsJson("[]").weaknessesJson("[\"Java Object-Oriented Programming\"]").build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(306L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .isReassessment(true).baselineSessionId(305L).build();
        TechnologyAssessmentResult reassessRes = TechnologyAssessmentResult.builder()
                .session(reassessment).technology(javaTech).rawScore(90).evaluatedQuestionsCount(5)
                .confidenceLevel("HIGH").estimatedProficiency("ADVANCED").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Object-Oriented Programming\"]").strengthsJson("[\"Java Object-Oriented Programming\"]").weaknessesJson("[]").build();

        when(resultRepository.findBySessionIdWithTechnology(306L)).thenReturn(List.of(reassessRes));
        when(resultRepository.findBySessionIdWithTechnology(305L)).thenReturn(List.of(baseRes));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech)).thenReturn(List.of(oopNode));

        AssessmentComparisonDto comp = comparisonService.compareSessions(reassessment, baseline, student);
        TechnologyComparisonDto tc = comp.getTechnologyComparisons().get(0);

        assertTrue(tc.isComparable());
        assertEquals("BEGINNER", tc.getPreviousProficiency());
        assertEquals("ADVANCED", tc.getNewProficiency());
        assertEquals(30, tc.getScoreDelta()); // +30 percentage points
        assertEquals(CompetencyChangeClassification.IMPROVED, tc.getCompetencies().get(0).getClassification());
    }

    @Test
    @DisplayName("Scenario 4: Different topic coverage - newly assessed topics get NEW_EVIDENCE, unassessed get NOT_REASSESSED")
    void testCompare_differentTopicCoverage() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(307L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech)).build();
        TechnologyAssessmentResult baseRes = TechnologyAssessmentResult.builder()
                .session(baseline).technology(javaTech).rawScore(70).evaluatedQuestionsCount(3)
                .confidenceLevel("MEDIUM").estimatedProficiency("INTERMEDIATE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(308L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .isReassessment(true).baselineSessionId(307L).build();
        TechnologyAssessmentResult reassessRes = TechnologyAssessmentResult.builder()
                .session(reassessment).technology(javaTech).rawScore(80).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("INTERMEDIATE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Streams & Functional\"]").strengthsJson("[\"Java Streams & Functional\"]").weaknessesJson("[]").build();

        when(resultRepository.findBySessionIdWithTechnology(308L)).thenReturn(List.of(reassessRes));
        when(resultRepository.findBySessionIdWithTechnology(307L)).thenReturn(List.of(baseRes));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech))
                .thenReturn(List.of(syntaxNode, streamsNode));

        AssessmentComparisonDto comp = comparisonService.compareSessions(reassessment, baseline, student);
        TechnologyComparisonDto tc = comp.getTechnologyComparisons().get(0);

        // Syntax was not in reassessment -> NOT_REASSESSED
        CompetencyChangeDto syntaxC = tc.getCompetencies().stream().filter(c -> "java-syntax".equals(c.getCompetencyKey())).findFirst().orElseThrow();
        assertEquals(CompetencyChangeClassification.NOT_REASSESSED, syntaxC.getClassification());

        // Streams was not in baseline but in reassessment -> NEW_EVIDENCE
        CompetencyChangeDto streamsC = tc.getCompetencies().stream().filter(c -> "java-streams".equals(c.getCompetencyKey())).findFirst().orElseThrow();
        assertEquals(CompetencyChangeClassification.NEW_EVIDENCE, streamsC.getClassification());
    }

    @Test
    @DisplayName("Scenario 5: Baseline with insufficient evidence does NOT claim improvement or decline")
    void testCompare_baselineInsufficientEvidence_doesNotClaimImprovementOrDecline() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(309L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech)).build();
        TechnologyAssessmentResult baseRes = TechnologyAssessmentResult.builder()
                .session(baseline).technology(javaTech).rawScore(30).evaluatedQuestionsCount(1) // only 1 question!
                .confidenceLevel("LOW").estimatedProficiency("INSUFFICIENT_EVIDENCE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[]").weaknessesJson("[\"Java Syntax & Basics\"]").build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(310L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .isReassessment(true).baselineSessionId(309L).build();
        TechnologyAssessmentResult reassessRes = TechnologyAssessmentResult.builder()
                .session(reassessment).technology(javaTech).rawScore(95).evaluatedQuestionsCount(5)
                .confidenceLevel("HIGH").estimatedProficiency("ADVANCED").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        when(resultRepository.findBySessionIdWithTechnology(310L)).thenReturn(List.of(reassessRes));
        when(resultRepository.findBySessionIdWithTechnology(309L)).thenReturn(List.of(baseRes));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech)).thenReturn(List.of(syntaxNode));

        AssessmentComparisonDto comp = comparisonService.compareSessions(reassessment, baseline, student);
        TechnologyComparisonDto tc = comp.getTechnologyComparisons().get(0);

        // Comparability is false due to baseline insufficient evidence
        assertFalse(tc.isComparable());
        assertTrue(tc.getIncomparableReason().contains("Baseline"));
        // Strictly does NOT claim IMPROVED!
        CompetencyChangeDto syntaxC = tc.getCompetencies().get(0);
        assertNotEquals(CompetencyChangeClassification.IMPROVED, syntaxC.getClassification());
        assertEquals(CompetencyChangeClassification.INSUFFICIENT_EVIDENCE, syntaxC.getClassification());
    }

    @Test
    @DisplayName("Scenario 6: Incompatible scoring versions prevent comparison and avoid false progression")
    void testCompare_incompatibleProficiencyScales_scoringVersionMismatch() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(311L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech)).build();
        TechnologyAssessmentResult baseRes = TechnologyAssessmentResult.builder()
                .session(baseline).technology(javaTech).rawScore(50).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("INTERMEDIATE").scoringVersion("v1.0-legacy") // incompatible version
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[]").weaknessesJson("[\"Java Syntax & Basics\"]").build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(312L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .isReassessment(true).baselineSessionId(311L).build();
        TechnologyAssessmentResult reassessRes = TechnologyAssessmentResult.builder()
                .session(reassessment).technology(javaTech).rawScore(90).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("ADVANCED").scoringVersion("v2.0-heuristic") // v2 version
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        when(resultRepository.findBySessionIdWithTechnology(312L)).thenReturn(List.of(reassessRes));
        when(resultRepository.findBySessionIdWithTechnology(311L)).thenReturn(List.of(baseRes));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech)).thenReturn(List.of(syntaxNode));

        AssessmentComparisonDto comp = comparisonService.compareSessions(reassessment, baseline, student);
        TechnologyComparisonDto tc = comp.getTechnologyComparisons().get(0);

        assertFalse(tc.isComparable());
        assertTrue(tc.getIncomparableReason().contains("incompatible"));
        assertEquals(CompetencyChangeClassification.INSUFFICIENT_EVIDENCE, tc.getCompetencies().get(0).getClassification());
    }

    @Test
    @DisplayName("Scenario 7: Score improvement without proficiency tier change")
    void testCompare_scoreImprovementWithoutProficiencyTierChange() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(313L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech)).build();
        TechnologyAssessmentResult baseRes = TechnologyAssessmentResult.builder()
                .session(baseline).technology(javaTech).rawScore(62).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("INTERMEDIATE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(314L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .isReassessment(true).baselineSessionId(313L).build();
        TechnologyAssessmentResult reassessRes = TechnologyAssessmentResult.builder()
                .session(reassessment).technology(javaTech).rawScore(75).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("INTERMEDIATE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        when(resultRepository.findBySessionIdWithTechnology(314L)).thenReturn(List.of(reassessRes));
        when(resultRepository.findBySessionIdWithTechnology(313L)).thenReturn(List.of(baseRes));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech)).thenReturn(List.of(syntaxNode));

        AssessmentComparisonDto comp = comparisonService.compareSessions(reassessment, baseline, student);
        TechnologyComparisonDto tc = comp.getTechnologyComparisons().get(0);

        assertTrue(tc.isComparable());
        assertEquals("INTERMEDIATE", tc.getPreviousProficiency());
        assertEquals("INTERMEDIATE", tc.getNewProficiency()); // Tier unchanged
        assertEquals(13, tc.getScoreDelta()); // +13 percentage points
    }

    @Test
    @DisplayName("Scenario 8: Tier change with small score difference (59% BEGINNER to 61% INTERMEDIATE)")
    void testCompare_tierChangeWithSmallScoreDifference() {
        AssessmentSession baseline = AssessmentSession.builder()
                .id(315L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech)).build();
        TechnologyAssessmentResult baseRes = TechnologyAssessmentResult.builder()
                .session(baseline).technology(javaTech).rawScore(59).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("BEGINNER").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[]").weaknessesJson("[\"Java Syntax & Basics\"]").build();

        AssessmentSession reassessment = AssessmentSession.builder()
                .id(316L).user(student).status(AssessmentStatus.SUBMITTED).selectedTechnologies(Set.of(javaTech))
                .isReassessment(true).baselineSessionId(315L).build();
        TechnologyAssessmentResult reassessRes = TechnologyAssessmentResult.builder()
                .session(reassessment).technology(javaTech).rawScore(61).evaluatedQuestionsCount(4)
                .confidenceLevel("HIGH").estimatedProficiency("INTERMEDIATE").scoringVersion("v2.0-heuristic")
                .topicCoverageJson("[\"Java Syntax & Basics\"]").strengthsJson("[\"Java Syntax & Basics\"]").weaknessesJson("[]").build();

        when(resultRepository.findBySessionIdWithTechnology(316L)).thenReturn(List.of(reassessRes));
        when(resultRepository.findBySessionIdWithTechnology(315L)).thenReturn(List.of(baseRes));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(javaTech)).thenReturn(List.of(syntaxNode));

        AssessmentComparisonDto comp = comparisonService.compareSessions(reassessment, baseline, student);
        TechnologyComparisonDto tc = comp.getTechnologyComparisons().get(0);

        assertTrue(tc.isComparable());
        assertEquals("BEGINNER", tc.getPreviousProficiency());
        assertEquals("INTERMEDIATE", tc.getNewProficiency());
        assertEquals(2, tc.getScoreDelta()); // +2 percentage points
    }
}
