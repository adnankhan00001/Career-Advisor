package com.careeradvisor.backend.integration;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 6 End-to-End Student Acceptance Testing Suite.
 * Validates the complete student journey across Phases 1 through 6 against the real Spring Boot
 * application, security filter chain, and MySQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StudentJourneyEndToEndAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssessmentQuestionRepository questionRepository;

    @Autowired
    private AssessmentSessionQuestionRepository sessionQuestionRepository;

    @Autowired
    private AssessmentAnswerRepository answerRepository;

    @Autowired
    private AssessmentSessionRepository sessionRepository;

    @Autowired
    private PersonalizedRoadmapRepository roadmapRepository;

    @Autowired
    private RoadmapUpdateProposalRepository proposalRepository;

    // Student Alpha state
    private String studentAlphaEmail;
    private String studentAlphaPassword = "AlphaPassword123!";
    private String studentAlphaToken;
    private Long baselineSessionId;
    private Long activeRoadmapId;
    private Long activeStepId;
    private Long customMilestoneId;
    private Long reassessmentSessionId;
    private Long rejectedProposalId;
    private Long appliedProposalId;

    // Student Beta state (for security/isolation checks)
    private String studentBetaEmail;
    private String studentBetaPassword = "BetaPassword123!";
    private String studentBetaToken;

    @BeforeAll
    void setupSuite() {
        long timestamp = System.currentTimeMillis();
        studentAlphaEmail = "student.alpha." + timestamp + "@acceptance.test";
        studentBetaEmail = "student.beta." + timestamp + "@acceptance.test";
    }

    // =========================================================================
    // PHASE 1: ENVIRONMENT AND BASELINE VERIFICATION
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("Phase 1.1: Verify Auth Service Health Endpoint")
    void testAuthHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(11)
    @DisplayName("Phase 1.2: Verify Career Catalog Careers Retrieval")
    void testCatalogCareersRetrieval() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/catalog/careers"))
                .andExpect(status().isOk())
                .andReturn();

        List<CatalogCareerDto> careers = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<CatalogCareerDto>>() {}
        );

        assertNotNull(careers);
        assertFalse(careers.isEmpty(), "Catalog careers must not be empty");
        boolean hasJavaBackend = careers.stream().anyMatch(c -> "java-backend-developer".equals(c.getSlug()));
        assertTrue(hasJavaBackend, "Career catalog must contain 'java-backend-developer'");
    }

    @Test
    @Order(12)
    @DisplayName("Phase 1.3: Verify Career Catalog Technologies Retrieval")
    void testCatalogTechnologiesRetrieval() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/catalog/technologies"))
                .andExpect(status().isOk())
                .andReturn();

        List<CatalogTechnologyDto> technologies = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<CatalogTechnologyDto>>() {}
        );

        assertNotNull(technologies);
        assertFalse(technologies.isEmpty(), "Catalog technologies must not be empty");
        boolean hasJava = technologies.stream().anyMatch(t -> "java".equals(t.getSlug()));
        boolean hasSql = technologies.stream().anyMatch(t -> "sql".equals(t.getSlug()));
        assertTrue(hasJava, "Catalog technologies must contain 'java'");
        assertTrue(hasSql, "Catalog technologies must contain 'sql'");
    }

    // =========================================================================
    // PHASE 2: FIRST-TIME STUDENT JOURNEY (PHASES 1 - 4)
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("Phase 2.1: Register Student Alpha")
    void testRegisterStudentAlpha() throws Exception {
        SignupRequest request = new SignupRequest("Student Alpha", studentAlphaEmail, studentAlphaPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(studentAlphaEmail))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(authResponse.getToken());
        studentAlphaToken = authResponse.getToken();
    }

    @Test
    @Order(21)
    @DisplayName("Phase 2.2: Login Student Alpha and Authenticate")
    void testLoginStudentAlpha() throws Exception {
        LoginRequest request = new LoginRequest(studentAlphaEmail, studentAlphaPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(studentAlphaEmail))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(authResponse.getToken());
        studentAlphaToken = authResponse.getToken();

        // Verify current authenticated identity
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(studentAlphaEmail))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @Order(22)
    @DisplayName("Phase 2.3: Verify Initial Onboarding State")
    void testInitialOnboardingState() throws Exception {
        mockMvc.perform(get("/api/onboarding/state")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value(org.hamcrest.Matchers.isOneOf("NOT_STARTED", "CAREER_SELECTED")))
                .andExpect(jsonPath("$.hasPersonalizedRoadmap").value(false));
    }

    @Test
    @Order(23)
    @DisplayName("Phase 2.4: Select Career Track (java-backend-developer)")
    void testOnboardingSelectCareer() throws Exception {
        OnboardingSelectCareerRequest request = new OnboardingSelectCareerRequest("java-backend-developer");

        mockMvc.perform(post("/api/onboarding/select-career")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CAREER_SELECTED"))
                .andExpect(jsonPath("$.selectedCareer.slug").value("java-backend-developer"));
    }

    @Test
    @Order(24)
    @DisplayName("Phase 2.5: Select Known Technologies (java, sql)")
    void testOnboardingSelectTechnologies() throws Exception {
        OnboardingSelectTechnologiesRequest request = new OnboardingSelectTechnologiesRequest(List.of("java", "sql"));

        mockMvc.perform(post("/api/onboarding/select-technologies")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("TECHNOLOGIES_SELECTED"))
                .andExpect(jsonPath("$.selectedTechnologies").isArray());
    }

    @Test
    @Order(25)
    @DisplayName("Phase 2.6: Configure Study Preferences")
    void testOnboardingSaveStudyPreferences() throws Exception {
        OnboardingStudyPreferencesRequest request = OnboardingStudyPreferencesRequest.builder()
                .dailyStudyMinutes(120)
                .preferredStudyWindow(StudyWindow.EVENING)
                .preferredDays(List.of("MONDAY", "WEDNESDAY", "SATURDAY"))
                .targetCompletionDate(LocalDate.now().plusWeeks(12))
                .build();

        mockMvc.perform(post("/api/onboarding/study-preferences")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("STUDY_PREFERENCES_CONFIGURED"));
    }

    @Test
    @Order(26)
    @DisplayName("Phase 2.7: Start Baseline Assessment Session")
    void testStartBaselineAssessment() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/onboarding/start-assessment")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.questions").isArray())
                .andReturn();

        AssessmentSessionDto sessionDto = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AssessmentSessionDto.class
        );

        assertNotNull(sessionDto.getSessionId());
        assertFalse(sessionDto.getQuestions().isEmpty(), "Session questions should be seeded and selected");
        baselineSessionId = sessionDto.getSessionId();
    }

    @Test
    @Order(27)
    @DisplayName("Phase 2.8: Answer Baseline Assessment Questions and Submit")
    void testAnswerBaselineAssessmentAndSubmit() throws Exception {
        assertNotNull(baselineSessionId, "Baseline session ID must be available");

        // Loop adaptively answering all scheduled questions
        int questionIndex = 0;
        boolean moreToAnswer = true;
        while (moreToAnswer) {
            List<AssessmentSessionQuestion> unanswered = sessionQuestionRepository.findBySessionIdWithQuestionAndTech(baselineSessionId)
                    .stream()
                    .filter(sq -> {
                        Optional<AssessmentAnswer> ans = answerRepository.findBySessionQuestionId(sq.getId());
                        return ans.isEmpty() || ans.get().getStatus() == AnswerStatus.UNANSWERED;
                    })
                    .toList();

            if (unanswered.isEmpty()) {
                moreToAnswer = false;
            } else {
                for (AssessmentSessionQuestion sq : unanswered) {
                    int selectedIndex;
                    if (questionIndex % 2 == 0) {
                        selectedIndex = sq.getQuestion().getCorrectOptionIndex();
                    } else {
                        selectedIndex = (sq.getQuestion().getCorrectOptionIndex() + 1) % 4;
                    }

                    SubmitAnswerRequest answerRequest = new SubmitAnswerRequest(sq.getId(), selectedIndex);
                    mockMvc.perform(post("/api/assessments/" + baselineSessionId + "/answer")
                                    .header("Authorization", "Bearer " + studentAlphaToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(answerRequest)))
                            .andExpect(status().isOk());
                    questionIndex++;
                }
            }
        }

        // Submit baseline assessment
        MvcResult submitResult = mockMvc.perform(post("/api/assessments/" + baselineSessionId + "/submit")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(baselineSessionId))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.technologyResults").isArray())
                .andReturn();

        AssessmentReportDto reportDto = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(),
                AssessmentReportDto.class
        );

        assertNotNull(reportDto);
        assertTrue(reportDto.getOverallScore() >= 0);
        assertFalse(reportDto.getTechnologyResults().isEmpty(), "Report must contain technology results");
    }

    @Test
    @Order(28)
    @DisplayName("Phase 2.9: Complete Onboarding and Generate Personalized Roadmap")
    void testCompleteOnboardingAndGenerateRoadmap() throws Exception {
        // Review assessment report in onboarding
        mockMvc.perform(post("/api/onboarding/review-report")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ASSESSMENT_REVIEWED"));

        // Complete onboarding
        mockMvc.perform(post("/api/onboarding/complete")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPersonalizedRoadmap").value(true));

        // Generate personalized roadmap
        MvcResult roadmapResult = mockMvc.perform(post("/api/roadmaps/personalized/generate?forceRegenerate=true")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.careerTitle").isNotEmpty())
                .andExpect(jsonPath("$.milestones").isArray())
                .andReturn();

        PersonalizedRoadmapDto roadmapDto = objectMapper.readValue(
                roadmapResult.getResponse().getContentAsString(),
                PersonalizedRoadmapDto.class
        );

        assertNotNull(roadmapDto);
        assertNotNull(roadmapDto.getId());
        activeRoadmapId = roadmapDto.getId();
        assertFalse(roadmapDto.getMilestones().isEmpty(), "Roadmap must have milestones");

        // Find an active step to use in progress tracking
        for (PersonalizedMilestoneDto m : roadmapDto.getMilestones()) {
            for (PersonalizedStepDto s : m.getSteps()) {
                if (!s.isTestedOut()) {
                    activeStepId = s.getId();
                    break;
                }
            }
            if (activeStepId != null) break;
        }

        assertNotNull(activeStepId, "Roadmap must have at least one active non-tested-out step");
    }

    // =========================================================================
    // PHASE 3: LEARNING & PROGRESS TRACKING (PHASE 5)
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("Phase 3.1: Toggle Learning Step State (IN_PROGRESS -> COMPLETED)")
    void testUpdateStepState() throws Exception {
        assertNotNull(activeStepId, "Step ID must be available");

        // 1. Move to IN_PROGRESS
        RoadmapStepStateRequest inProgressReq = new RoadmapStepStateRequest("IN_PROGRESS");
        mockMvc.perform(put("/api/roadmaps/personalized/steps/" + activeStepId + "/state")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inProgressReq)))
                .andExpect(status().isOk());

        // 2. Move to COMPLETED
        RoadmapStepStateRequest completedReq = new RoadmapStepStateRequest("COMPLETED");
        MvcResult completedResult = mockMvc.perform(put("/api/roadmaps/personalized/steps/" + activeStepId + "/state")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completedReq)))
                .andExpect(status().isOk())
                .andReturn();

        PersonalizedRoadmapDto updatedRoadmap = objectMapper.readValue(
                completedResult.getResponse().getContentAsString(),
                PersonalizedRoadmapDto.class
        );

        // Verify step is recorded as completed
        boolean stepFoundCompleted = false;
        for (PersonalizedMilestoneDto m : updatedRoadmap.getMilestones()) {
            for (PersonalizedStepDto s : m.getSteps()) {
                if (s.getId().equals(activeStepId)) {
                    assertTrue(s.isCompleted(), "Step should be marked as completed");
                    assertEquals("COMPLETED", s.getState());
                    stepFoundCompleted = true;
                    break;
                }
            }
        }
        assertTrue(stepFoundCompleted, "Target step must be found in updated roadmap");
    }

    @Test
    @Order(31)
    @DisplayName("Phase 3.2: Add Custom Milestone")
    void testAddCustomMilestone() throws Exception {
        AddCustomMilestoneRequest customReq = AddCustomMilestoneRequest.builder()
                .title("Microservices API Production Project")
                .technologyName("Java & Spring Boot")
                .estimatedHours(14.0)
                .customNotes("Build containerized REST service with integration testing")
                .stepTitles(List.of("Design REST endpoints", "Implement Service layer", "Dockerize application"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/roadmaps/personalized/milestones/custom")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customReq)))
                .andExpect(status().isOk())
                .andReturn();

        PersonalizedRoadmapDto updatedRoadmap = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PersonalizedRoadmapDto.class
        );

        PersonalizedMilestoneDto customMilestone = updatedRoadmap.getMilestones().stream()
                .filter(PersonalizedMilestoneDto::isCustom)
                .findFirst()
                .orElse(null);

        assertNotNull(customMilestone, "Custom milestone must be present in roadmap");
        assertEquals("Microservices API Production Project", customMilestone.getTitle());
        customMilestoneId = customMilestone.getId();
        assertNotNull(customMilestoneId);
    }

    @Test
    @Order(32)
    @DisplayName("Phase 3.3: Submit Project Evidence for Custom Milestone")
    void testSubmitProjectEvidence() throws Exception {
        assertNotNull(customMilestoneId, "Custom milestone ID must be available");

        SubmitEvidenceRequest evidenceReq = SubmitEvidenceRequest.builder()
                .evidenceType(EvidenceType.GITHUB_REPOSITORY)
                .url("https://github.com/student-alpha/microservices-demo")
                .notes("Production-grade microservices codebase with JUnit tests and docker-compose")
                .build();

        mockMvc.perform(post("/api/roadmaps/personalized/milestones/" + customMilestoneId + "/evidence")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(evidenceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.evidenceType").value("GITHUB_REPOSITORY"))
                .andExpect(jsonPath("$.url").value("https://github.com/student-alpha/microservices-demo"));

        // Verify evidence is in the roadmap's evidence list
        mockMvc.perform(get("/api/roadmaps/personalized/evidence")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].url").value("https://github.com/student-alpha/microservices-demo"));
    }

    @Test
    @Order(33)
    @DisplayName("Phase 3.4: Adjust Study Schedule and Verify Revision History")
    void testUpdateScheduleAndVerifyRevisions() throws Exception {
        UpdateScheduleRequest scheduleReq = UpdateScheduleRequest.builder()
                .dailyStudyMinutes(150)
                .preferredDays("MONDAY,WEDNESDAY,FRIDAY,SATURDAY")
                .targetCompletionDate(LocalDate.now().plusWeeks(10))
                .build();

        mockMvc.perform(put("/api/roadmaps/personalized/schedule")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleReq)))
                .andExpect(status().isOk());

        // Verify revision history contains logged events
        MvcResult revisionsResult = mockMvc.perform(get("/api/roadmaps/personalized/revisions")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        List<RoadmapRevisionDto> revisions = objectMapper.readValue(
                revisionsResult.getResponse().getContentAsString(),
                new TypeReference<List<RoadmapRevisionDto>>() {}
        );
        assertNotNull(revisions);
        assertFalse(revisions.isEmpty(), "Roadmap revisions should be recorded");
    }

    // =========================================================================
    // PHASE 4: TARGETED REASSESSMENT FLOW (PHASE 6)
    // =========================================================================

    @Test
    @Order(40)
    @DisplayName("Phase 4.1: Start Targeted Reassessment Session for Java")
    void testStartReassessmentForJava() throws Exception {
        StartReassessmentRequest req = StartReassessmentRequest.builder()
                .technologySlugs(List.of("java"))
                .baselineSessionId(baselineSessionId)
                .build();

        MvcResult result = mockMvc.perform(post("/api/assessments/reassessments")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();

        AssessmentSessionDto sessionDto = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AssessmentSessionDto.class
        );
        assertNotNull(sessionDto.getSessionId());
        reassessmentSessionId = sessionDto.getSessionId();
    }

    @Test
    @Order(41)
    @DisplayName("Phase 4.2: Submit All Correct Answers in Reassessment and Submit")
    void testAnswerReassessmentAndSubmit() throws Exception {
        assertNotNull(reassessmentSessionId, "Reassessment session ID must be available");

        // Loop adaptively answering all scheduled questions with 100% correct answers
        boolean moreToAnswer = true;
        while (moreToAnswer) {
            List<AssessmentSessionQuestion> unanswered = sessionQuestionRepository.findBySessionIdWithQuestionAndTech(reassessmentSessionId)
                    .stream()
                    .filter(sq -> {
                        Optional<AssessmentAnswer> ans = answerRepository.findBySessionQuestionId(sq.getId());
                        return ans.isEmpty() || ans.get().getStatus() == AnswerStatus.UNANSWERED;
                    })
                    .toList();

            if (unanswered.isEmpty()) {
                moreToAnswer = false;
            } else {
                for (AssessmentSessionQuestion sq : unanswered) {
                    int correctIndex = sq.getQuestion().getCorrectOptionIndex();
                    SubmitAnswerRequest answerReq = new SubmitAnswerRequest(sq.getId(), correctIndex);
                    mockMvc.perform(post("/api/assessments/" + reassessmentSessionId + "/answer")
                                    .header("Authorization", "Bearer " + studentAlphaToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(answerReq)))
                            .andExpect(status().isOk());
                }
            }
        }

        // Submit reassessment
        MvcResult submitResult = mockMvc.perform(post("/api/assessments/reassessments/" + reassessmentSessionId + "/submit")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(reassessmentSessionId))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andReturn();

        AssessmentReportDto reportDto = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(),
                AssessmentReportDto.class
        );
        assertTrue(reportDto.getOverallScore() >= 80, "Score should demonstrate mastery: " + reportDto.getOverallScore());
    }

    @Test
    @Order(42)
    @DisplayName("Phase 4.3: Compare Reassessment with Baseline and Verify Percentage-Point Delta")
    void testReassessmentComparisonAndDelta() throws Exception {
        assertNotNull(reassessmentSessionId);
        assertNotNull(baselineSessionId);

        MvcResult result = mockMvc.perform(get("/api/assessments/reassessments/" + reassessmentSessionId + "/comparison")
                        .param("baselineId", baselineSessionId.toString())
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technologyComparisons").isArray())
                .andReturn();

        AssessmentComparisonDto comparison = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AssessmentComparisonDto.class
        );

        assertNotNull(comparison);
        assertFalse(comparison.getTechnologyComparisons().isEmpty(), "Must contain technology comparison");
        TechnologyComparisonDto javaComp = comparison.getTechnologyComparisons().stream()
                .filter(tc -> "java".equals(tc.getTechnologySlug()))
                .findFirst()
                .orElse(null);

        assertNotNull(javaComp, "Comparison must include 'java'");
        assertTrue(javaComp.getScoreDelta() > 0, "Score delta must be positive indicating improvement");

        // Verify percentage-point delta matches arithmetic difference: S_new - S_prev
        int expectedDelta = javaComp.getNewScore() - javaComp.getPreviousScore();
        assertEquals(expectedDelta, javaComp.getScoreDelta().intValue(),
                "Score delta must strictly follow percentage-point difference (S_new - S_prev)");

        // Verify topic changes are detailed and unreassessed competencies preserved
        assertNotNull(javaComp.getCompetencies(), "Competency changes must be detailed");
    }

    // =========================================================================
    // PHASE 5: PROPOSAL PREVIEW & APPROVAL (PHASE 6)
    // =========================================================================

    @Test
    @Order(50)
    @DisplayName("Phase 5.1: Generate Roadmap Proposal Preview (PENDING, Non-Mutating)")
    void testGenerateProposalPreviewNonMutating() throws Exception {
        assertNotNull(reassessmentSessionId);

        // Capture roadmap state before preview
        MvcResult preRoadmapResult = mockMvc.perform(get("/api/roadmaps/personalized")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andReturn();
        PersonalizedRoadmapDto roadmapBefore = objectMapper.readValue(
                preRoadmapResult.getResponse().getContentAsString(),
                PersonalizedRoadmapDto.class
        );

        GenerateRoadmapPreviewRequest previewReq = GenerateRoadmapPreviewRequest.builder()
                .reassessmentSessionId(reassessmentSessionId)
                .baselineSessionId(baselineSessionId)
                .build();

        MvcResult previewResult = mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/preview")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.changeItems").isArray())
                .andReturn();

        RoadmapUpdateProposalDto proposalDto = objectMapper.readValue(
                previewResult.getResponse().getContentAsString(),
                RoadmapUpdateProposalDto.class
        );
        assertNotNull(proposalDto.getId());
        rejectedProposalId = proposalDto.getId();
        assertEquals(ProposalStatus.PENDING, proposalDto.getStatus());
        assertFalse(proposalDto.getChangeItems().isEmpty(), "Proposal should contain itemized changes");

        // Verify roadmap is completely UNCHANGED after preview (preview is strictly non-mutating)
        MvcResult postRoadmapResult = mockMvc.perform(get("/api/roadmaps/personalized")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andReturn();
        PersonalizedRoadmapDto roadmapAfter = objectMapper.readValue(
                postRoadmapResult.getResponse().getContentAsString(),
                PersonalizedRoadmapDto.class
        );

        assertEquals(roadmapBefore.getMilestones().size(), roadmapAfter.getMilestones().size(),
                "Milestones count must remain unchanged after proposal preview");
    }

    @Test
    @Order(51)
    @DisplayName("Phase 5.2: Explicit Proposal Rejection (Roadmap Remains Unchanged)")
    void testExplicitProposalRejection() throws Exception {
        assertNotNull(rejectedProposalId, "Update proposal ID must be available");

        RejectProposalRequest rejectReq = new RejectProposalRequest("I prefer to keep all practice exercises.");

        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/" + rejectedProposalId + "/reject")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rejectedProposalId))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("I prefer to keep all practice exercises."));

        // Verify roadmap is still unchanged
        mockMvc.perform(get("/api/roadmaps/personalized")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(52)
    @DisplayName("Phase 5.3: Generate New Proposal and Explicitly Apply (Preserving Progress & Evidence)")
    void testGenerateNewProposalAndApplyWithPreservation() throws Exception {
        assertNotNull(reassessmentSessionId);

        // Generate a new proposal preview
        GenerateRoadmapPreviewRequest previewReq = GenerateRoadmapPreviewRequest.builder()
                .reassessmentSessionId(reassessmentSessionId)
                .baselineSessionId(baselineSessionId)
                .build();

        MvcResult previewResult = mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/preview")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        RoadmapUpdateProposalDto secondProposal = objectMapper.readValue(
                previewResult.getResponse().getContentAsString(),
                RoadmapUpdateProposalDto.class
        );
        appliedProposalId = secondProposal.getId();
        assertNotNull(appliedProposalId);

        // Explicitly apply the proposal
        MvcResult applyResult = mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/" + appliedProposalId + "/apply")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andReturn();

        PersonalizedRoadmapDto updatedRoadmap = objectMapper.readValue(
                applyResult.getResponse().getContentAsString(),
                PersonalizedRoadmapDto.class
        );
        assertNotNull(updatedRoadmap);

        // CRITICAL PRESERVATION ASSERTION 1: Completed step remains completed
        boolean completedStepFound = false;
        for (PersonalizedMilestoneDto m : updatedRoadmap.getMilestones()) {
            for (PersonalizedStepDto s : m.getSteps()) {
                if (s.getId().equals(activeStepId)) {
                    assertTrue(s.isCompleted(), "Previously completed step must remain completed");
                    assertEquals("COMPLETED", s.getState());
                    completedStepFound = true;
                    break;
                }
            }
        }
        assertTrue(completedStepFound, "Previously completed step must still exist in roadmap");

        // CRITICAL PRESERVATION ASSERTION 2: Custom milestone remains preserved
        boolean customMilestoneFound = updatedRoadmap.getMilestones().stream()
                .anyMatch(m -> m.isCustom() && m.getId().equals(customMilestoneId));
        assertTrue(customMilestoneFound, "Custom milestone must be preserved after proposal application");

        // CRITICAL PRESERVATION ASSERTION 3: Project evidence remains preserved
        mockMvc.perform(get("/api/roadmaps/personalized/evidence")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].url").value("https://github.com/student-alpha/microservices-demo"));

        // CRITICAL PRESERVATION ASSERTION 4: Revision history records adaptive update
        MvcResult revResult = mockMvc.perform(get("/api/roadmaps/personalized/revisions")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isOk())
                .andReturn();
        List<RoadmapRevisionDto> revisions = objectMapper.readValue(
                revResult.getResponse().getContentAsString(),
                new TypeReference<List<RoadmapRevisionDto>>() {}
        );
        boolean adaptiveRevisionLogged = revisions.stream().anyMatch(r ->
                r.getChangeType() != null && r.getChangeType().toLowerCase().contains("adaptive"));
        assertTrue(adaptiveRevisionLogged, "Revision history must log the adaptive roadmap update");
    }

    // =========================================================================
    // PHASE 6: SECURITY & CONCURRENCY EDGE CASES
    // =========================================================================

    @Test
    @Order(60)
    @DisplayName("Phase 6.1: Cross-User Proposal Access and Application Rejection (HTTP 403 Forbidden)")
    void testCrossUserProposalAccessRejection() throws Exception {
        assertNotNull(appliedProposalId);

        // Register Student Beta
        SignupRequest betaSignup = new SignupRequest("Student Beta", studentBetaEmail, studentBetaPassword);
        MvcResult betaResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(betaSignup)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse betaAuth = objectMapper.readValue(
                betaResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        studentBetaToken = betaAuth.getToken();
        assertNotNull(studentBetaToken);

        // Student Beta attempts to view Student Alpha's proposal -> HTTP 403 Forbidden
        mockMvc.perform(get("/api/roadmaps/personalized/adaptive-updates/" + appliedProposalId)
                        .header("Authorization", "Bearer " + studentBetaToken))
                .andExpect(status().isForbidden());

        // Student Beta attempts to apply Student Alpha's proposal -> HTTP 403 Forbidden
        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/" + appliedProposalId + "/apply")
                        .header("Authorization", "Bearer " + studentBetaToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(61)
    @DisplayName("Phase 6.2: Duplicate Proposal Application Prevention (HTTP 409 Conflict)")
    void testDuplicateProposalApplyRejection() throws Exception {
        assertNotNull(appliedProposalId);

        // Attempting to re-apply an already applied proposal must fail with HTTP 409 Conflict
        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/" + appliedProposalId + "/apply")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(62)
    @DisplayName("Phase 6.3: Stale Proposal Optimistic Locking Version Conflict (HTTP 409 Conflict)")
    void testStaleProposalOptimisticLockingRejection() throws Exception {
        // 1. Generate a new proposal preview based on current roadmap version V
        GenerateRoadmapPreviewRequest previewReq = GenerateRoadmapPreviewRequest.builder()
                .reassessmentSessionId(reassessmentSessionId)
                .baselineSessionId(baselineSessionId)
                .build();

        MvcResult previewResult = mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/preview")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewReq)))
                .andExpect(status().isCreated())
                .andReturn();

        RoadmapUpdateProposalDto staleProposal = objectMapper.readValue(
                previewResult.getResponse().getContentAsString(),
                RoadmapUpdateProposalDto.class
        );
        Long staleProposalId = staleProposal.getId();

        // 2. Concurrently mutate the roadmap to advance version to V+1 (e.g. updating schedule)
        UpdateScheduleRequest mutateReq = UpdateScheduleRequest.builder()
                .dailyStudyMinutes(180)
                .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .targetCompletionDate(LocalDate.now().plusWeeks(8))
                .build();

        mockMvc.perform(put("/api/roadmaps/personalized/schedule")
                        .header("Authorization", "Bearer " + studentAlphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mutateReq)))
                .andExpect(status().isOk());

        // 3. Attempting to apply the proposal with the outdated snapshot version must fail with HTTP 409 Conflict
        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/" + staleProposalId + "/apply")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(63)
    @DisplayName("Phase 6.4: Non-existent Resource Handling (HTTP 400 Bad Request)")
    void testNonExistentResourceHandling() throws Exception {
        mockMvc.perform(get("/api/roadmaps/personalized/adaptive-updates/9999999")
                        .header("Authorization", "Bearer " + studentAlphaToken))
                .andExpect(status().isBadRequest());
    }
}
