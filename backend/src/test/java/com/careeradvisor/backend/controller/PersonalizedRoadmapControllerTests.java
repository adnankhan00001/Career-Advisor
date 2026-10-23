package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.EvidenceType;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.PersonalizedRoadmapService;
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
class PersonalizedRoadmapControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PersonalizedRoadmapService personalizedRoadmapService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PersonalizedRoadmapController roadmapController;

    private User testUser;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(401L)
                .name("Jordan Student")
                .email("jordan@student.dev")
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

        mockMvc = MockMvcBuilders.standaloneSetup(roadmapController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();

        lenient().when(userRepository.findById(401L)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("GET /api/roadmaps/personalized returns 200 and roadmap DTO")
    void testGetPersonalizedRoadmap() throws Exception {
        PersonalizedRoadmapDto dto = PersonalizedRoadmapDto.builder()
                .id(1L)
                .careerTitle("Java Backend Developer")
                .progressPercentage(25)
                .totalEstimatedHours(50.0)
                .build();

        when(personalizedRoadmapService.getPersonalizedRoadmap(any(User.class))).thenReturn(dto);

        mockMvc.perform(get("/api/roadmaps/personalized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.careerTitle").value("Java Backend Developer"))
                .andExpect(jsonPath("$.progressPercentage").value(25));

        verify(personalizedRoadmapService).getPersonalizedRoadmap(any(User.class));
    }

    @Test
    @DisplayName("GET /api/roadmaps/personalized returns 204 No Content when no active roadmap exists")
    void testGetPersonalizedRoadmap_NoContent() throws Exception {
        when(personalizedRoadmapService.getPersonalizedRoadmap(any(User.class))).thenReturn(null);

        mockMvc.perform(get("/api/roadmaps/personalized"))
                .andExpect(status().isNoContent());

        verify(personalizedRoadmapService).getPersonalizedRoadmap(any(User.class));
    }

    @Test
    @DisplayName("POST /api/roadmaps/personalized/generate returns 200 with generated roadmap")
    void testGenerateRoadmap() throws Exception {
        PersonalizedRoadmapDto dto = PersonalizedRoadmapDto.builder()
                .id(2L)
                .careerTitle("Frontend Developer")
                .progressPercentage(0)
                .build();

        when(personalizedRoadmapService.generateRoadmap(any(User.class), eq(true))).thenReturn(dto);

        mockMvc.perform(post("/api/roadmaps/personalized/generate?forceRegenerate=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.careerTitle").value("Frontend Developer"));

        verify(personalizedRoadmapService).generateRoadmap(any(User.class), eq(true));
    }

    @Test
    @DisplayName("POST /api/roadmaps/personalized/steps/{stepId}/toggle returns 200 with updated roadmap")
    void testToggleStep() throws Exception {
        PersonalizedRoadmapDto dto = PersonalizedRoadmapDto.builder()
                .id(1L)
                .careerTitle("Java Backend Developer")
                .completedStepsCount(1)
                .progressPercentage(50)
                .build();

        when(personalizedRoadmapService.toggleStep(any(User.class), eq(99L))).thenReturn(dto);

        mockMvc.perform(post("/api/roadmaps/personalized/steps/99/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedStepsCount").value(1))
                .andExpect(jsonPath("$.progressPercentage").value(50));

        verify(personalizedRoadmapService).toggleStep(any(User.class), eq(99L));
    }

    @Test
    @DisplayName("PUT /api/roadmaps/personalized/steps/{stepId}/state updates learning state")
    void testUpdateStepState() throws Exception {
        PersonalizedRoadmapDto dto = PersonalizedRoadmapDto.builder()
                .id(1L)
                .careerTitle("Java Backend Developer")
                .build();

        when(personalizedRoadmapService.updateStepState(any(User.class), eq(99L), eq("IN_PROGRESS"))).thenReturn(dto);

        RoadmapStepStateRequest req = new RoadmapStepStateRequest("IN_PROGRESS");

        mockMvc.perform(put("/api/roadmaps/personalized/steps/99/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(personalizedRoadmapService).updateStepState(any(User.class), eq(99L), eq("IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /api/roadmaps/personalized/milestones/custom creates custom milestone")
    void testAddCustomMilestone() throws Exception {
        PersonalizedRoadmapDto dto = PersonalizedRoadmapDto.builder()
                .id(1L)
                .careerTitle("Java Backend Developer")
                .build();

        when(personalizedRoadmapService.addCustomMilestone(any(User.class), any(AddCustomMilestoneRequest.class)))
                .thenReturn(dto);

        AddCustomMilestoneRequest req = AddCustomMilestoneRequest.builder()
                .title("Advanced Microservices")
                .estimatedHours(5.0)
                .build();

        mockMvc.perform(post("/api/roadmaps/personalized/milestones/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(personalizedRoadmapService).addCustomMilestone(any(User.class), any(AddCustomMilestoneRequest.class));
    }

    @Test
    @DisplayName("POST /api/roadmaps/personalized/milestones/{id}/evidence submits project evidence")
    void testSubmitEvidence() throws Exception {
        ProjectEvidenceDto evidenceDto = ProjectEvidenceDto.builder()
                .id(50L)
                .milestoneId(10L)
                .evidenceType("GITHUB_REPOSITORY")
                .url("https://github.com/jordan/test")
                .submissionStatus("SELF_REPORTED")
                .build();

        when(personalizedRoadmapService.submitEvidence(any(User.class), eq(10L), any(SubmitEvidenceRequest.class)))
                .thenReturn(evidenceDto);

        SubmitEvidenceRequest req = SubmitEvidenceRequest.builder()
                .evidenceType(EvidenceType.GITHUB_REPOSITORY)
                .url("https://github.com/jordan/test")
                .notes("Demo project")
                .build();

        mockMvc.perform(post("/api/roadmaps/personalized/milestones/10/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.submissionStatus").value("SELF_REPORTED"));

        verify(personalizedRoadmapService).submitEvidence(any(User.class), eq(10L), any(SubmitEvidenceRequest.class));
    }

    @Test
    @DisplayName("GET /api/roadmaps/personalized/revisions returns revision history")
    void testGetRevisionHistory() throws Exception {
        RoadmapRevisionDto rev = RoadmapRevisionDto.builder()
                .id(1L)
                .revisionNumber(1)
                .changeType("ROADMAP_GENERATED")
                .summary("Generated roadmap")
                .createdAt(LocalDateTime.now())
                .build();

        when(personalizedRoadmapService.getRevisionHistory(any(User.class))).thenReturn(List.of(rev));

        mockMvc.perform(get("/api/roadmaps/personalized/revisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revisionNumber").value(1))
                .andExpect(jsonPath("$[0].changeType").value("ROADMAP_GENERATED"));

        verify(personalizedRoadmapService).getRevisionHistory(any(User.class));
    }
}
