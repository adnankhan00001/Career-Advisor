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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalizedRoadmapServiceTests {

    @Mock
    private PersonalizedRoadmapRepository personalizedRoadmapRepository;

    @Mock
    private PersonalizedRoadmapStepRepository stepRepository;

    @Mock
    private PersonalizedRoadmapMilestoneRepository milestoneRepository;

    @Mock
    private RoadmapProjectEvidenceRepository evidenceRepository;

    @Mock
    private RoadmapRevisionRepository revisionRepository;

    @Mock
    private StudentOnboardingRepository studentOnboardingRepository;

    @Mock
    private CareerTrackRepository careerTrackRepository;

    @Mock
    private CompetencyNodeRepository competencyNodeRepository;

    @Mock
    private CompetencyPrerequisiteRepository competencyPrerequisiteRepository;

    @Mock
    private TechnologyAssessmentResultRepository resultRepository;

    @Mock
    private AssessmentSessionRepository assessmentSessionRepository;

    @Mock
    private UserRoadmapProgressRepository userRoadmapProgressRepository;

    @Mock
    private RoadmapService legacyRoadmapService;

    @InjectMocks
    private PersonalizedRoadmapService roadmapService;

    private User student;
    private CareerTrack careerTrack;
    private Technology techJava;
    private Technology techSpring;
    private CompetencyNode compJavaSyntax;
    private CompetencyNode compJavaOop;
    private CompetencyNode compSpringDi;
    private AssessmentSession assessmentSession;

    @BeforeEach
    void setUp() {
        student = User.builder().id(101L).name("Sam Student").email("sam@test.com").careerGoal("Java Backend Developer").build();

        techJava = Technology.builder().id(1L).name("Java").slug("java").active(true).build();
        techSpring = Technology.builder().id(2L).name("Spring Boot").slug("spring-boot").active(true).build();

        Set<Technology> techs = new HashSet<>();
        techs.add(techJava);
        techs.add(techSpring);

        careerTrack = CareerTrack.builder()
                .id(10L)
                .title("Java Backend Developer")
                .slug("java-backend-developer")
                .active(true)
                .technologies(techs)
                .build();

        compJavaSyntax = CompetencyNode.builder()
                .id(100L)
                .competencyKey("java-syntax")
                .title("Java Syntax & Basics")
                .level("Beginner")
                .sequenceOrder(1)
                .technology(techJava)
                .active(true)
                .build();

        compJavaOop = CompetencyNode.builder()
                .id(101L)
                .competencyKey("java-oop")
                .title("Java Object-Oriented Programming")
                .level("Intermediate")
                .sequenceOrder(2)
                .technology(techJava)
                .active(true)
                .build();

        compSpringDi = CompetencyNode.builder()
                .id(102L)
                .competencyKey("spring-di")
                .title("Spring Dependency Injection")
                .level("Intermediate")
                .sequenceOrder(1)
                .technology(techSpring)
                .active(true)
                .build();

        assessmentSession = AssessmentSession.builder()
                .id(500L)
                .user(student)
                .status(AssessmentStatus.SUBMITTED)
                .build();
    }

    @Test
    @DisplayName("Generate roadmap with assessment results marks tested-out skills and focus areas")
    void testGenerateRoadmapWithAssessmentResults() {
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .id(1L)
                .user(student)
                .selectedCareer(careerTrack)
                .assessmentSession(assessmentSession)
                .dailyStudyMinutes(60)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId()))
                .thenReturn(Optional.of(onboarding));

        // High score in Java (80%) -> tests out Beginner competencies
        TechnologyAssessmentResult resultJava = TechnologyAssessmentResult.builder()
                .session(assessmentSession)
                .technology(techJava)
                .rawScore(80)
                .evaluatedQuestionsCount(5)
                .estimatedProficiency("Advanced")
                .confidenceLevel("High")
                .build();

        // Low score in Spring Boot (40%) -> marks HIGH_PRIORITY focus area
        TechnologyAssessmentResult resultSpring = TechnologyAssessmentResult.builder()
                .session(assessmentSession)
                .technology(techSpring)
                .rawScore(40)
                .evaluatedQuestionsCount(5)
                .estimatedProficiency("Beginner")
                .confidenceLevel("Medium")
                .build();

        when(resultRepository.findBySessionIdWithTechnology(500L))
                .thenReturn(List.of(resultJava, resultSpring));

        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techJava))
                .thenReturn(List.of(compJavaSyntax, compJavaOop));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techSpring))
                .thenReturn(List.of(compSpringDi));

        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class)))
                .thenAnswer(i -> {
                    PersonalizedRoadmap r = i.getArgument(0);
                    r.setId(999L);
                    return r;
                });

        PersonalizedRoadmapDto result = roadmapService.generateRoadmap(student, false);

        assertNotNull(result);
        assertEquals("Java Backend Developer", result.getCareerTitle());
        assertEquals(3, result.getTotalStepsCount());
        // 1 step tested out (Java Syntax), but not marked completed at generation time
        assertEquals(1, result.getTestedOutCount());
        assertEquals(0, result.getCompletedStepsCount());
        assertEquals(0, result.getProgressPercentage());
        assertEquals(1, result.getFocusAreasCount()); // Spring Boot step is HIGH_PRIORITY

        // Milestone statuses at generation
        assertEquals("IN_PROGRESS", result.getMilestones().get(0).getStatus());
        assertEquals("UPCOMING", result.getMilestones().get(1).getStatus());

        // Step evidence status
        assertEquals("ASSESSED", result.getMilestones().get(0).getSteps().get(0).getEvidenceStatus());

        verify(studentOnboardingRepository).save(onboarding);
        assertTrue(onboarding.isHasPersonalizedRoadmap());
        assertEquals(OnboardingState.ROADMAP_GENERATED, onboarding.getState());
    }

    @Test
    @DisplayName("Generate roadmap throws IllegalStateException when user has no selected career")
    void testGenerateRoadmapWithoutSelectedCareerThrowsIllegalStateException() {
        User unassignedUser = User.builder()
                .id(99L)
                .name("Unassigned User")
                .email("unassigned@test.com")
                .careerGoal(null)
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(unassignedUser, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(unassignedUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> roadmapService.generateRoadmap(unassignedUser, false));
    }

    @Test
    @DisplayName("Topologically sorts competencies respecting prerequisite dependencies")
    void testSortCompetenciesTopologically() {
        // compJavaOop depends on compJavaSyntax
        CompetencyPrerequisite prereq = CompetencyPrerequisite.builder()
                .id(1L)
                .competency(compJavaOop)
                .prerequisite(compJavaSyntax)
                .build();

        when(competencyPrerequisiteRepository.findByCompetency(compJavaOop)).thenReturn(List.of(prereq));
        when(competencyPrerequisiteRepository.findByCompetency(compJavaSyntax)).thenReturn(Collections.emptyList());

        // Provide list in reverse order
        List<CompetencyNode> unordered = List.of(compJavaOop, compJavaSyntax);
        List<CompetencyNode> sorted = roadmapService.sortCompetenciesTopologically(unordered);

        assertEquals(2, sorted.size());
        assertEquals(compJavaSyntax.getId(), sorted.get(0).getId());
        assertEquals(compJavaOop.getId(), sorted.get(1).getId());
    }

    @Test
    @DisplayName("Topological sort detects cycles in competency prerequisites and throws IllegalStateException")
    void testSortCompetenciesTopologically_CycleDetected() {
        // Syntax depends on OOP and OOP depends on Syntax (Cycle)
        CompetencyPrerequisite p1 = CompetencyPrerequisite.builder()
                .id(1L)
                .competency(compJavaOop)
                .prerequisite(compJavaSyntax)
                .build();
        CompetencyPrerequisite p2 = CompetencyPrerequisite.builder()
                .id(2L)
                .competency(compJavaSyntax)
                .prerequisite(compJavaOop)
                .build();

        when(competencyPrerequisiteRepository.findByCompetency(compJavaOop)).thenReturn(List.of(p1));
        when(competencyPrerequisiteRepository.findByCompetency(compJavaSyntax)).thenReturn(List.of(p2));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                roadmapService.sortCompetenciesTopologically(List.of(compJavaSyntax, compJavaOop)));

        assertTrue(ex.getMessage().contains("Cycle detected in prerequisite graph"));
    }

    @Test
    @DisplayName("Target date realism flags unrealistic deadlines based on capacity")
    void testTargetDateRealism_Unrealistic() {
        // User sets target date to tomorrow with 3 hours of study remaining at 15m/day
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .id(2L)
                .user(student)
                .selectedCareer(careerTrack)
                .dailyStudyMinutes(15)
                .preferredDays("MONDAY")
                .targetCompletionDate(LocalDate.now().plusDays(1))
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId()))
                .thenReturn(Optional.of(onboarding));

        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techJava))
                .thenReturn(List.of(compJavaSyntax, compJavaOop));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techSpring))
                .thenReturn(List.of(compSpringDi));

        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class)))
                .thenAnswer(i -> {
                    PersonalizedRoadmap r = i.getArgument(0);
                    r.setId(888L);
                    return r;
                });

        PersonalizedRoadmapDto result = roadmapService.generateRoadmap(student, false);

        assertNotNull(result);
        assertFalse(result.getIsTargetDateRealistic());
        assertNotNull(result.getProjectedCompletionDate());
        assertTrue(result.getProjectedCompletionDate().isAfter(LocalDate.now().plusWeeks(1)));
    }

    @Test
    @DisplayName("Evidence status is INSUFFICIENT_EVIDENCE when assessed question count is zero")
    void testEvidenceStatus_InsufficientEvidence() {
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .id(3L)
                .user(student)
                .selectedCareer(careerTrack)
                .assessmentSession(assessmentSession)
                .dailyStudyMinutes(60)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId()))
                .thenReturn(Optional.of(onboarding));

        TechnologyAssessmentResult resultJava = TechnologyAssessmentResult.builder()
                .session(assessmentSession)
                .technology(techJava)
                .rawScore(0)
                .evaluatedQuestionsCount(0)
                .confidenceLevel("Low")
                .build();

        when(resultRepository.findBySessionIdWithTechnology(500L))
                .thenReturn(List.of(resultJava));

        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techJava))
                .thenReturn(List.of(compJavaSyntax));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techSpring))
                .thenReturn(Collections.emptyList());

        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class)))
                .thenAnswer(i -> {
                    PersonalizedRoadmap r = i.getArgument(0);
                    r.setId(777L);
                    return r;
                });

        PersonalizedRoadmapDto result = roadmapService.generateRoadmap(student, false);

        assertNotNull(result);
        assertEquals("INSUFFICIENT_EVIDENCE", result.getMilestones().get(0).getSteps().get(0).getEvidenceStatus());
    }

    @Test
    @DisplayName("Get roadmap returns existing active roadmap if available")
    void testGetPersonalizedRoadmapExisting() {
        PersonalizedRoadmap existing = PersonalizedRoadmap.builder()
                .id(100L)
                .user(student)
                .careerTitle("Java Backend Developer")
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(existing));

        PersonalizedRoadmapDto dto = roadmapService.getPersonalizedRoadmap(student);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals("Java Backend Developer", dto.getCareerTitle());
        verify(personalizedRoadmapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Toggle step completes an uncompleted step and updates progress")
    void testToggleStep() {
        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .id(200L)
                .user(student)
                .careerTitle("Java Backend Developer")
                .status("ACTIVE")
                .totalStepsCount(2)
                .completedStepsCount(0)
                .progressPercentage(0)
                .milestones(new ArrayList<>())
                .build();

        PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                .id(20L)
                .roadmap(roadmap)
                .title("Java Milestone")
                .status("IN_PROGRESS")
                .steps(new ArrayList<>())
                .build();
        roadmap.addMilestone(milestone);

        PersonalizedRoadmapStep step = PersonalizedRoadmapStep.builder()
                .id(201L)
                .milestone(milestone)
                .title("Java OOP")
                .isCompleted(false)
                .isTestedOut(false)
                .build();
        milestone.addStep(step);

        when(stepRepository.findByIdWithMilestoneAndRoadmap(201L)).thenReturn(Optional.of(step));
        when(stepRepository.save(any(PersonalizedRoadmapStep.class))).thenAnswer(i -> i.getArgument(0));
        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class))).thenAnswer(i -> i.getArgument(0));

        PersonalizedRoadmapDto result = roadmapService.toggleStep(student, 201L);

        assertNotNull(result);
        assertTrue(step.isCompleted());
        assertNotNull(step.getCompletedAt());
        assertEquals(1, roadmap.getCompletedStepsCount());
        assertEquals("COMPLETED", milestone.getStatus());
        verify(userRoadmapProgressRepository).save(any(UserRoadmapProgress.class));
    }

    @Test
    @DisplayName("Specific weakness cannot test out even if technology raw score was high")
    void testTestedOutCompetencies_SpecificWeaknessCannotTestOutEvenWithHighRawScore() {
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .id(1L)
                .user(student)
                .selectedCareer(careerTrack)
                .assessmentSession(assessmentSession)
                .dailyStudyMinutes(60)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId()))
                .thenReturn(Optional.of(onboarding));

        TechnologyAssessmentResult resultJava = TechnologyAssessmentResult.builder()
                .session(assessmentSession)
                .technology(techJava)
                .rawScore(85)
                .evaluatedQuestionsCount(5)
                .estimatedProficiency("Advanced")
                .confidenceLevel("High")
                .strengthsJson("[\"java-oop\"]")
                .weaknessesJson("[\"java-syntax\"]")
                .topicCoverageJson("[\"java-syntax\",\"java-oop\"]")
                .build();

        when(resultRepository.findBySessionIdWithTechnology(500L))
                .thenReturn(List.of(resultJava));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techJava))
                .thenReturn(List.of(compJavaSyntax, compJavaOop));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techSpring))
                .thenReturn(Collections.emptyList());

        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class)))
                .thenAnswer(i -> {
                    PersonalizedRoadmap r = i.getArgument(0);
                    r.setId(999L);
                    return r;
                });

        PersonalizedRoadmapDto result = roadmapService.generateRoadmap(student, false);

        assertNotNull(result);
        PersonalizedStepDto syntaxStep = result.getMilestones().get(0).getSteps().get(0);
        assertFalse(syntaxStep.isTestedOut());
        assertEquals("HIGH_PRIORITY", syntaxStep.getPriority());
        assertEquals(0, syntaxStep.getAssessmentScore());
    }

    @Test
    @DisplayName("Unassessed competencies do not have fabricated scores and receive score null")
    void testUnassessedCompetencies_DoNotFabricateScores() {
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .id(1L)
                .user(student)
                .selectedCareer(careerTrack)
                .assessmentSession(assessmentSession)
                .dailyStudyMinutes(60)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId()))
                .thenReturn(Optional.of(onboarding));

        TechnologyAssessmentResult resultJava = TechnologyAssessmentResult.builder()
                .session(assessmentSession)
                .technology(techJava)
                .rawScore(80)
                .evaluatedQuestionsCount(4)
                .estimatedProficiency("Intermediate")
                .confidenceLevel("Medium")
                .strengthsJson("[]")
                .weaknessesJson("[]")
                .topicCoverageJson("[\"java-syntax\"]")
                .build();

        when(resultRepository.findBySessionIdWithTechnology(500L))
                .thenReturn(List.of(resultJava));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techJava))
                .thenReturn(List.of(compJavaSyntax, compJavaOop));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techSpring))
                .thenReturn(Collections.emptyList());

        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class)))
                .thenAnswer(i -> {
                    PersonalizedRoadmap r = i.getArgument(0);
                    r.setId(999L);
                    return r;
                });

        PersonalizedRoadmapDto result = roadmapService.generateRoadmap(student, false);

        assertNotNull(result);
        PersonalizedStepDto oopStep = result.getMilestones().get(0).getSteps().get(1);
        assertEquals("NOT_ASSESSED", oopStep.getEvidenceStatus());
        assertNull(oopStep.getAssessmentScore(), "Unassessed competency should not have fabricated score");
        assertFalse(oopStep.isTestedOut());
    }

    @Test
    @DisplayName("Toggle step throws AccessDeniedException when step belongs to another user")
    void testToggleStep_AccessDenied_WhenStudentDoesNotOwnStep() {
        User anotherUser = User.builder().id(999L).name("Other").email("other@test.com").build();
        PersonalizedRoadmap otherRoadmap = PersonalizedRoadmap.builder()
                .id(300L)
                .user(anotherUser)
                .careerTitle("Java Backend Developer")
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                .id(30L)
                .roadmap(otherRoadmap)
                .steps(new ArrayList<>())
                .build();
        otherRoadmap.addMilestone(milestone);

        PersonalizedRoadmapStep step = PersonalizedRoadmapStep.builder()
                .id(301L)
                .milestone(milestone)
                .title("Java Collections")
                .isCompleted(false)
                .build();
        milestone.addStep(step);

        when(stepRepository.findByIdWithMilestoneAndRoadmap(301L)).thenReturn(Optional.of(step));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                roadmapService.toggleStep(student, 301L));
    }

    @Test
    @DisplayName("Get roadmap returns null when no active roadmap exists without mutating DB")
    void testGetPersonalizedRoadmap_ReturnsNull_WhenNoActiveRoadmap() {
        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.empty());

        PersonalizedRoadmapDto dto = roadmapService.getPersonalizedRoadmap(student);

        assertNull(dto);
        verify(personalizedRoadmapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Target date in past is flagged as unrealistic")
    void testTargetDateRealism_PastDateFlaggedUnrealistic() {
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .id(5L)
                .user(student)
                .selectedCareer(careerTrack)
                .dailyStudyMinutes(60)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .targetCompletionDate(LocalDate.now().minusDays(1))
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId()))
                .thenReturn(Optional.of(onboarding));

        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techJava))
                .thenReturn(List.of(compJavaSyntax));
        when(competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(techSpring))
                .thenReturn(Collections.emptyList());

        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class)))
                .thenAnswer(i -> {
                    PersonalizedRoadmap r = i.getArgument(0);
                    r.setId(666L);
                    return r;
                });

        PersonalizedRoadmapDto result = roadmapService.generateRoadmap(student, false);

        assertNotNull(result);
        assertFalse(result.getIsTargetDateRealistic(), "Past target completion date must be marked unrealistic");
    }

    @Test
    @DisplayName("Update step state handles learning states and syncs UserRoadmapProgress")
    void testUpdateStepState_ProgressTransitions() {
        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .id(200L)
                .user(student)
                .careerTitle("Java Backend Developer")
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                .id(20L)
                .roadmap(roadmap)
                .title("Java Milestone")
                .status("UPCOMING")
                .steps(new ArrayList<>())
                .build();
        roadmap.addMilestone(milestone);

        PersonalizedRoadmapStep step = PersonalizedRoadmapStep.builder()
                .id(201L)
                .milestone(milestone)
                .title("Java Syntax & Basics")
                .isCompleted(false)
                .state("NOT_STARTED")
                .build();
        milestone.addStep(step);

        when(stepRepository.findByIdWithMilestoneAndRoadmap(201L)).thenReturn(Optional.of(step));
        when(stepRepository.save(any(PersonalizedRoadmapStep.class))).thenAnswer(i -> i.getArgument(0));
        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class))).thenAnswer(i -> i.getArgument(0));

        // 1. Transition to IN_PROGRESS
        PersonalizedRoadmapDto resInProg = roadmapService.updateStepState(student, 201L, "IN_PROGRESS");
        assertEquals("IN_PROGRESS", step.getState());
        assertFalse(step.isCompleted());
        assertEquals("IN_PROGRESS", milestone.getStatus());
        verify(userRoadmapProgressRepository, never()).save(any());

        // 2. Transition to COMPLETED
        PersonalizedRoadmapDto resComp = roadmapService.updateStepState(student, 201L, "COMPLETED");
        assertEquals("COMPLETED", step.getState());
        assertTrue(step.isCompleted());
        assertEquals("COMPLETED", milestone.getStatus());
        verify(userRoadmapProgressRepository).save(any(UserRoadmapProgress.class));

        // 3. Transition to NOT_STARTED (Reset)
        when(userRoadmapProgressRepository.findByUserAndCareerTitleAndStepTitle(student, "Java Backend Developer", step.getTitle()))
                .thenReturn(Optional.of(new UserRoadmapProgress(student, "Java Backend Developer", step.getTitle())));

        PersonalizedRoadmapDto resReset = roadmapService.updateStepState(student, 201L, "NOT_STARTED");
        assertEquals("NOT_STARTED", step.getState());
        assertFalse(step.isCompleted());
        assertEquals("UPCOMING", milestone.getStatus());
        verify(userRoadmapProgressRepository).delete(any(UserRoadmapProgress.class));
    }

    @Test
    @DisplayName("Add custom milestone appends milestone with isCustom true and records revision")
    void testAddCustomMilestone() {
        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .id(100L)
                .user(student)
                .careerTitle("Java Backend Developer")
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(roadmap));
        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class))).thenAnswer(i -> i.getArgument(0));

        AddCustomMilestoneRequest request = AddCustomMilestoneRequest.builder()
                .title("Docker & Microservices")
                .technologyName("Docker")
                .estimatedHours(6.0)
                .customNotes("Deploying containers")
                .stepTitles(List.of("Container Basics", "Docker Compose"))
                .build();

        PersonalizedRoadmapDto dto = roadmapService.addCustomMilestone(student, request);

        assertNotNull(dto);
        assertEquals(1, roadmap.getMilestones().size());
        PersonalizedRoadmapMilestone added = roadmap.getMilestones().get(0);
        assertTrue(added.isCustom());
        assertEquals("Docker & Microservices", added.getTitle());
        assertEquals(2, added.getSteps().size());
        verify(revisionRepository).save(any(RoadmapRevision.class));
    }

    @Test
    @DisplayName("Delete custom milestone rejects canonical required milestones")
    void testDeleteCustomMilestone_CanonicalRejected() {
        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .id(100L)
                .user(student)
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        PersonalizedRoadmapMilestone canonicalMilestone = PersonalizedRoadmapMilestone.builder()
                .id(10L)
                .roadmap(roadmap)
                .title("Core Java")
                .isCustom(false)
                .build();
        roadmap.addMilestone(canonicalMilestone);

        when(milestoneRepository.findByIdWithRoadmap(10L)).thenReturn(Optional.of(canonicalMilestone));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                roadmapService.deleteCustomMilestone(student, 10L));

        assertTrue(ex.getMessage().contains("Cannot delete canonical career milestone"));
        verify(milestoneRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Skip and restore milestone updates isSkipped and adjusts schedule")
    void testSkipAndRestoreMilestone() {
        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .id(100L)
                .user(student)
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                .id(25L)
                .roadmap(roadmap)
                .title("Optional Cloud Tools")
                .isSkipped(false)
                .status("UPCOMING")
                .steps(new ArrayList<>())
                .build();
        roadmap.addMilestone(milestone);

        when(milestoneRepository.findByIdWithRoadmap(25L)).thenReturn(Optional.of(milestone));
        when(milestoneRepository.save(any(PersonalizedRoadmapMilestone.class))).thenAnswer(i -> i.getArgument(0));
        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class))).thenAnswer(i -> i.getArgument(0));

        // 1. Skip
        PersonalizedRoadmapDto skippedDto = roadmapService.skipMilestone(student, 25L, true);
        assertTrue(milestone.isSkipped());
        assertEquals("SKIPPED", milestone.getStatus());

        // 2. Restore
        PersonalizedRoadmapDto restoredDto = roadmapService.skipMilestone(student, 25L, false);
        assertFalse(milestone.isSkipped());
        assertEquals("UPCOMING", milestone.getStatus());
        verify(revisionRepository, atLeast(2)).save(any(RoadmapRevision.class));
    }

    @Test
    @DisplayName("Reorder milestones validates prerequisites and rejects dependency violations")
    void testReorderMilestones_PrerequisiteViolation() {
        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .id(100L)
                .user(student)
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        PersonalizedRoadmapMilestone mJava = PersonalizedRoadmapMilestone.builder()
                .id(1L)
                .roadmap(roadmap)
                .title("Java Milestone")
                .technologySlug("java")
                .sequenceOrder(1)
                .steps(new ArrayList<>())
                .build();

        PersonalizedRoadmapMilestone mSpring = PersonalizedRoadmapMilestone.builder()
                .id(2L)
                .roadmap(roadmap)
                .title("Spring Boot Milestone")
                .technologySlug("spring-boot")
                .sequenceOrder(2)
                .steps(new ArrayList<>())
                .build();

        roadmap.addMilestone(mJava);
        roadmap.addMilestone(mSpring);

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(roadmap));

        // Nodes: Spring DI depends on Java Syntax
        when(competencyNodeRepository.findByTechnologySlug("spring-boot"))
                .thenReturn(List.of(compSpringDi));
        when(competencyNodeRepository.findByTechnologySlug("java"))
                .thenReturn(List.of(compJavaSyntax));

        CompetencyPrerequisite prereq = CompetencyPrerequisite.builder()
                .id(50L)
                .competency(compSpringDi)
                .prerequisite(compJavaSyntax)
                .build();
        when(competencyPrerequisiteRepository.findByCompetency(compSpringDi))
                .thenReturn(List.of(prereq));

        // Proposed order: put Spring Boot (ID 2) BEFORE Java (ID 1)
        List<Long> invalidOrder = List.of(2L, 1L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                roadmapService.reorderMilestones(student, invalidOrder));

        assertTrue(ex.getMessage().contains("Prerequisite violation"));
    }

    @Test
    @DisplayName("Submit evidence validates URLs with SSRF protections")
    void testSubmitEvidence_SsrfProtections() {
        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .id(100L)
                .user(student)
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        PersonalizedRoadmapMilestone milestone = PersonalizedRoadmapMilestone.builder()
                .id(40L)
                .roadmap(roadmap)
                .title("Spring Boot Milestone")
                .steps(new ArrayList<>())
                .build();

        when(milestoneRepository.findByIdWithRoadmap(40L)).thenReturn(Optional.of(milestone));

        // 1. SSRF Localhost rejection
        SubmitEvidenceRequest localhostReq = SubmitEvidenceRequest.builder()
                .evidenceType(EvidenceType.DEPLOYED_PROJECT)
                .url("http://localhost:8080/admin")
                .build();
        assertThrows(IllegalArgumentException.class, () ->
                roadmapService.submitEvidence(student, 40L, localhostReq));

        // 2. SSRF 127.0.0.1 loopback rejection
        SubmitEvidenceRequest loopbackReq = SubmitEvidenceRequest.builder()
                .evidenceType(EvidenceType.DEPLOYED_PROJECT)
                .url("http://127.0.0.1:3000/api")
                .build();
        assertThrows(IllegalArgumentException.class, () ->
                roadmapService.submitEvidence(student, 40L, loopbackReq));

        // 3. Invalid scheme rejection (ftp)
        SubmitEvidenceRequest ftpReq = SubmitEvidenceRequest.builder()
                .evidenceType(EvidenceType.OTHER_LINK)
                .url("ftp://files.example.com/project.zip")
                .build();
        assertThrows(IllegalArgumentException.class, () ->
                roadmapService.submitEvidence(student, 40L, ftpReq));

        // 4. Valid HTTPS repository acceptance
        SubmitEvidenceRequest validReq = SubmitEvidenceRequest.builder()
                .evidenceType(EvidenceType.GITHUB_REPOSITORY)
                .url("https://github.com/sam/spring-boot-api")
                .notes("Production ready CRUD API")
                .build();

        when(evidenceRepository.save(any(RoadmapProjectEvidence.class))).thenAnswer(i -> {
            RoadmapProjectEvidence e = i.getArgument(0);
            e.setId(88L);
            return e;
        });

        ProjectEvidenceDto savedEvidence = roadmapService.submitEvidence(student, 40L, validReq);
        assertNotNull(savedEvidence);
        assertEquals("SELF_REPORTED", savedEvidence.getSubmissionStatus());
        assertEquals("GITHUB_REPOSITORY", savedEvidence.getEvidenceType());
        verify(evidenceRepository).save(any(RoadmapProjectEvidence.class));
    }

    @Test
    @DisplayName("Dynamic scheduling recalculates workload when study preferences change")
    void testDynamicScheduleUpdate() {
        PersonalizedRoadmap roadmap = PersonalizedRoadmap.builder()
                .id(100L)
                .user(student)
                .dailyStudyMinutes(30)
                .status("ACTIVE")
                .milestones(new ArrayList<>())
                .build();

        when(personalizedRoadmapRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student, "ACTIVE"))
                .thenReturn(Optional.of(roadmap));
        when(personalizedRoadmapRepository.save(any(PersonalizedRoadmap.class))).thenAnswer(i -> i.getArgument(0));
        when(studentOnboardingRepository.findByUserIdWithCareerAndTech(student.getId())).thenReturn(Optional.empty());

        UpdateScheduleRequest req = UpdateScheduleRequest.builder()
                .dailyStudyMinutes(120)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY")
                .targetCompletionDate(LocalDate.now().plusMonths(3))
                .build();

        PersonalizedRoadmapDto updated = roadmapService.updateSchedule(student, req);

        assertNotNull(updated);
        assertEquals(120, updated.getDailyStudyMinutes());
        assertTrue(updated.getIsTargetDateRealistic());
        verify(revisionRepository).save(any(RoadmapRevision.class));
    }
}


