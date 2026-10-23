package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.AssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private com.careeradvisor.backend.service.AssessmentComparisonService comparisonService;

    @Mock
    private com.careeradvisor.backend.repository.AssessmentSessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AssessmentController assessmentController;

    private User testUser;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(201L)
                .name("Alex Learner")
                .email("alex@learner.dev")
                .role(Role.USER)
                .build();

        testUserDetails = CustomUserDetails.build(testUser);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return testUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(assessmentController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();

        lenient().when(userRepository.findById(201L)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("POST /api/assessments/start returns 201 Created with AssessmentSessionDto")
    void testStartAssessment() throws Exception {
        StartAssessmentRequest request = StartAssessmentRequest.builder()
                .technologySlugs(List.of("java", "spring-boot"))
                .timeLimitMinutes(25)
                .build();

        AssessmentSessionDto sessionDto = AssessmentSessionDto.builder()
                .sessionId(701L)
                .status("IN_PROGRESS")
                .totalQuestionsCount(2)
                .answeredCount(0)
                .remainingSeconds(1500)
                .build();

        when(assessmentService.startAssessment(eq(testUser), any(StartAssessmentRequest.class)))
                .thenReturn(sessionDto);

        mockMvc.perform(post("/api/assessments/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(701))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.totalQuestionsCount").value(2));

        verify(assessmentService, times(1)).startAssessment(eq(testUser), any(StartAssessmentRequest.class));
    }

    @Test
    @DisplayName("GET /api/assessments/active returns 200 OK when active session exists")
    void testGetActiveAssessmentFound() throws Exception {
        AssessmentSessionDto sessionDto = AssessmentSessionDto.builder()
                .sessionId(702L)
                .status("IN_PROGRESS")
                .remainingSeconds(1200)
                .build();

        when(assessmentService.getActiveSession(testUser)).thenReturn(Optional.of(sessionDto));

        mockMvc.perform(get("/api/assessments/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(702))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("GET /api/assessments/active returns 204 No Content when no active session")
    void testGetActiveAssessmentNotFound() throws Exception {
        when(assessmentService.getActiveSession(testUser)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/assessments/active"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/assessments/{id}/answer records answer and returns updated question")
    void testSubmitAnswer() throws Exception {
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .sessionQuestionId(101L)
                .selectedOptionIndex(2)
                .build();

        AssessmentQuestionDto questionDto = AssessmentQuestionDto.builder()
                .sessionQuestionId(101L)
                .presentationOrder(1)
                .technologySlug("java")
                .selectedOptionIndex(2)
                .answerStatus("ANSWERED")
                .build();

        when(assessmentService.submitAnswer(eq(703L), any(SubmitAnswerRequest.class), eq(testUser)))
                .thenReturn(questionDto);

        mockMvc.perform(post("/api/assessments/703/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionQuestionId").value(101))
                .andExpect(jsonPath("$.answerStatus").value("ANSWERED"))
                .andExpect(jsonPath("$.selectedOptionIndex").value(2));
    }

    @Test
    @DisplayName("POST /api/assessments/{id}/skip marks question as skipped")
    void testSkipQuestion() throws Exception {
        SkipQuestionRequest request = SkipQuestionRequest.builder()
                .sessionQuestionId(102L)
                .build();

        AssessmentQuestionDto questionDto = AssessmentQuestionDto.builder()
                .sessionQuestionId(102L)
                .presentationOrder(2)
                .technologySlug("java")
                .answerStatus("SKIPPED")
                .build();

        when(assessmentService.skipQuestion(eq(704L), eq(102L), eq(testUser)))
                .thenReturn(questionDto);

        mockMvc.perform(post("/api/assessments/704/skip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionQuestionId").value(102))
                .andExpect(jsonPath("$.answerStatus").value("SKIPPED"));
    }

    @Test
    @DisplayName("POST /api/assessments/{id}/submit finalizes session and returns report")
    void testSubmitAssessment() throws Exception {
        AssessmentReportDto reportDto = AssessmentReportDto.builder()
                .sessionId(705L)
                .status("SUBMITTED")
                .overallScore(88)
                .overallProficiency("Advanced")
                .build();

        when(assessmentService.submitAssessment(705L, testUser)).thenReturn(reportDto);

        mockMvc.perform(post("/api/assessments/705/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(705))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.overallScore").value(88))
                .andExpect(jsonPath("$.overallProficiency").value("Advanced"));
    }

    @Test
    @DisplayName("GET /api/assessments/{id}/report returns full assessment report")
    void testGetAssessmentReport() throws Exception {
        AssessmentReportDto reportDto = AssessmentReportDto.builder()
                .sessionId(706L)
                .status("SUBMITTED")
                .overallScore(75)
                .overallProficiency("Intermediate")
                .technologyResults(List.of(
                        TechnologyResultDto.builder()
                                .technologySlug("java")
                                .technologyName("Java")
                                .rawScore(75)
                                .estimatedProficiency("INTERMEDIATE")
                                .confidenceLevel("HIGH")
                                .build()
                ))
                .build();

        when(assessmentService.getAssessmentReport(706L, testUser)).thenReturn(reportDto);

        mockMvc.perform(get("/api/assessments/706/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(706))
                .andExpect(jsonPath("$.technologyResults[0].technologySlug").value("java"))
                .andExpect(jsonPath("$.technologyResults[0].rawScore").value(75));
    }

    @Test
    @DisplayName("GET /api/assessments/history returns student assessment history")
    void testGetAssessmentHistory() throws Exception {
        AssessmentSummaryDto summary = AssessmentSummaryDto.builder()
                .sessionId(707L)
                .status("SUBMITTED")
                .overallScore(90)
                .overallProficiency("Advanced")
                .assessedTechnologies(List.of("Java", "MySQL"))
                .build();

        when(assessmentService.getAssessmentHistory(testUser)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/assessments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sessionId").value(707))
                .andExpect(jsonPath("$[0].overallScore").value(90))
                .andExpect(jsonPath("$[0].assessedTechnologies[0]").value("Java"));
    }

    @Test
    @DisplayName("POST /api/assessments/reassessments creates new reassessment session")
    void testStartReassessment() throws Exception {
        StartReassessmentRequest req = StartReassessmentRequest.builder()
                .technologySlugs(List.of("java"))
                .baselineSessionId(701L)
                .build();

        AssessmentSessionDto dto = AssessmentSessionDto.builder()
                .sessionId(708L)
                .status("IN_PROGRESS")
                .isReassessment(true)
                .baselineSessionId(701L)
                .build();

        when(assessmentService.startReassessment(eq(testUser), any(StartReassessmentRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/assessments/reassessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(708))
                .andExpect(jsonPath("$.isReassessment").value(true));
    }

    @Test
    @DisplayName("GET /api/assessments/reassessments/{id} returns reassessment session state")
    void testGetReassessmentAttempt() throws Exception {
        AssessmentSessionDto dto = AssessmentSessionDto.builder()
                .sessionId(708L)
                .status("IN_PROGRESS")
                .build();

        when(assessmentService.getSessionState(708L, testUser)).thenReturn(dto);

        mockMvc.perform(get("/api/assessments/reassessments/708"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(708));
    }

    @Test
    @DisplayName("POST /api/assessments/reassessments/{id}/submit finalizes reassessment")
    void testSubmitReassessment() throws Exception {
        AssessmentReportDto report = AssessmentReportDto.builder()
                .sessionId(708L)
                .status("SUBMITTED")
                .overallScore(85)
                .build();

        when(assessmentService.submitAssessment(708L, testUser)).thenReturn(report);

        mockMvc.perform(post("/api/assessments/reassessments/708/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(708))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.overallScore").value(85));
    }

    @Test
    @DisplayName("GET /api/assessments/reassessments/{id}/comparison returns before-and-after comparison")
    void testGetReassessmentComparison() throws Exception {
        com.careeradvisor.backend.model.AssessmentSession mockSession = com.careeradvisor.backend.model.AssessmentSession.builder()
                .id(708L)
                .user(testUser)
                .status(com.careeradvisor.backend.model.AssessmentStatus.SUBMITTED)
                .build();

        when(sessionRepository.findById(708L)).thenReturn(Optional.of(mockSession));

        AssessmentComparisonDto comparisonDto = AssessmentComparisonDto.builder()
                .reassessmentSessionId(708L)
                .baselineSessionId(701L)
                .improvedCompetenciesCount(2)
                .declinedCompetenciesCount(0)
                .build();

        when(comparisonService.compareSessions(eq(mockSession), isNull(), eq(testUser)))
                .thenReturn(comparisonDto);

        mockMvc.perform(get("/api/assessments/reassessments/708/comparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reassessmentSessionId").value(708))
                .andExpect(jsonPath("$.improvedCompetenciesCount").value(2));
    }
}
