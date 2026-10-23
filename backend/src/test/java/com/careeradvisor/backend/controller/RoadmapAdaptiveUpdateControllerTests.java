package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.GlobalExceptionHandler;
import com.careeradvisor.backend.model.ProposalStatus;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.RoadmapAdaptiveUpdateService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoadmapAdaptiveUpdateControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RoadmapAdaptiveUpdateService updateService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoadmapAdaptiveUpdateController controller;

    private User testUser;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(501L)
                .email("student501@test.com")
                .role(Role.USER)
                .name("Alex Student")
                .build();

        testUserDetails = CustomUserDetails.build(testUser);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return testUserDetails;
                    }
                })
                .build();

        lenient().when(userRepository.findById(501L)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("POST /preview should return 201 Created with generated proposal")
    void testGenerateRoadmapPreview() throws Exception {
        GenerateRoadmapPreviewRequest request = GenerateRoadmapPreviewRequest.builder()
                .reassessmentSessionId(301L)
                .build();

        RoadmapUpdateProposalDto proposalDto = RoadmapUpdateProposalDto.builder()
                .id(88L)
                .roadmapId(10L)
                .status(ProposalStatus.PENDING)
                .testedOutCandidatesCount(2)
                .priorityUpdatesCount(1)
                .currentRemainingHours(20.0)
                .proposedRemainingHours(15.0)
                .build();

        when(updateService.generateProposalPreview(eq(testUser), any(GenerateRoadmapPreviewRequest.class)))
                .thenReturn(proposalDto);

        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(88))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.testedOutCandidatesCount").value(2));
    }

    @Test
    @DisplayName("GET /{proposalId} should return 200 OK with proposal details")
    void testGetProposal() throws Exception {
        RoadmapUpdateProposalDto proposalDto = RoadmapUpdateProposalDto.builder()
                .id(88L)
                .status(ProposalStatus.PENDING)
                .build();

        when(updateService.getProposal(eq(testUser), eq(88L))).thenReturn(proposalDto);

        mockMvc.perform(get("/api/roadmaps/personalized/adaptive-updates/88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(88))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /{proposalId}/apply should return 200 OK with updated roadmap")
    void testApplyProposal() throws Exception {
        PersonalizedRoadmapDto roadmapDto = PersonalizedRoadmapDto.builder()
                .id(10L)
                .careerTitle("Java Backend Developer")
                .progressPercentage(45)
                .build();

        when(updateService.applyProposal(eq(testUser), eq(88L))).thenReturn(roadmapDto);

        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/88/apply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.progressPercentage").value(45));
    }

    @Test
    @DisplayName("POST /{proposalId}/reject should return 200 OK with rejected proposal")
    void testRejectProposal() throws Exception {
        RejectProposalRequest request = RejectProposalRequest.builder()
                .reason("Prefer to study all topics")
                .build();

        RoadmapUpdateProposalDto rejectedDto = RoadmapUpdateProposalDto.builder()
                .id(88L)
                .status(ProposalStatus.REJECTED)
                .rejectionReason("Prefer to study all topics")
                .build();

        when(updateService.rejectProposal(eq(testUser), eq(88L), eq("Prefer to study all topics")))
                .thenReturn(rejectedDto);

        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/88/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(88))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Prefer to study all topics"));
    }

    @Test
    @DisplayName("GET /history should return 200 OK with list of proposal summaries")
    void testGetProposalHistory() throws Exception {
        ProposalSummaryDto summary = ProposalSummaryDto.builder()
                .id(88L)
                .sourceSessionId(301L)
                .status("APPLIED")
                .testedOutCandidatesCount(2)
                .createdAt(LocalDateTime.now())
                .build();

        when(updateService.getProposalHistory(eq(testUser))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/roadmaps/personalized/adaptive-updates/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(88))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));
    }

    @Test
    @DisplayName("POST /{proposalId}/apply should return 403 Forbidden when unauthorized")
    void testApplyProposal_unauthorized_returns403() throws Exception {
        when(updateService.applyProposal(eq(testUser), eq(999L)))
                .thenThrow(new SecurityException("User does not own roadmap update proposal #999"));

        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/999/apply"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("User does not own roadmap update proposal #999"));
    }

    @Test
    @DisplayName("POST /{proposalId}/apply should return 409 Conflict when version conflict occurs")
    void testApplyProposal_versionConflict_returns409() throws Exception {
        when(updateService.applyProposal(eq(testUser), eq(88L)))
                .thenThrow(new IllegalStateException("Roadmap has been modified since proposal #88 was generated (version conflict: proposal=1, current=2). Please preview changes again."));

        mockMvc.perform(post("/api/roadmaps/personalized/adaptive-updates/88/apply"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict / Invalid State"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("version conflict")));
    }
}
