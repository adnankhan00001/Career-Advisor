package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.GlobalExceptionHandler;
import com.careeradvisor.backend.model.MentorVerificationStatus;
import com.careeradvisor.backend.model.MentorshipRequestStatus;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.MentorProfileService;
import com.careeradvisor.backend.service.MentorshipRequestService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MentorControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MentorProfileService mentorProfileService;

    @Mock
    private MentorshipRequestService mentorshipRequestService;

    @InjectMocks
    private MentorController mentorController;

    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(101L)
                .name("Alex Student")
                .email("alex@example.com")
                .role(Role.USER)
                .build();
        mockUserDetails = CustomUserDetails.build(user);

        // Custom resolver to inject @AuthenticationPrincipal CustomUserDetails
        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(mentorController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/mentors/apply returns 201 Created on valid input")
    void testApplyAsMentor_Success() throws Exception {
        MentorApplicationDto input = MentorApplicationDto.builder()
                .jobTitle("Principal Engineer")
                .company("Stripe")
                .yearsOfExperience(8)
                .bio("Fintech mentor")
                .expertiseAreas("Distributed Systems, Payments")
                .maxMentees(3)
                .build();

        MentorProfileDto responseDto = MentorProfileDto.builder()
                .id(10L)
                .userId(101L)
                .name("Alex Student")
                .jobTitle("Principal Engineer")
                .company("Stripe")
                .verificationStatus(MentorVerificationStatus.APPLIED)
                .build();

        when(mentorProfileService.submitApplication(eq(101L), any(MentorApplicationDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/mentors/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.verificationStatus").value("APPLIED"));
    }

    @Test
    @DisplayName("POST /api/mentors/apply returns 400 Bad Request on missing required fields")
    void testApplyAsMentor_ValidationFailure() throws Exception {
        MentorApplicationDto invalid = MentorApplicationDto.builder()
                .jobTitle("") // Blank
                .company(null) // Null
                .yearsOfExperience(-2) // Negative
                .build();

        mockMvc.perform(post("/api/mentors/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    @DisplayName("GET /api/mentors returns 200 OK with approved mentors list")
    void testGetApprovedMentors_Success() throws Exception {
        MentorProfileDto mentor = MentorProfileDto.builder()
                .id(1L)
                .name("Jane Mentor")
                .company("Google")
                .jobTitle("Cloud Architect")
                .verificationStatus(MentorVerificationStatus.APPROVED)
                .build();

        when(mentorProfileService.getApprovedMentors(null, null)).thenReturn(List.of(mentor));

        mockMvc.perform(get("/api/mentors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Jane Mentor"))
                .andExpect(jsonPath("$[0].company").value("Google"));
    }

    @Test
    @DisplayName("POST /api/mentors/{mentorId}/requests returns 201 Created on valid goal description")
    void testCreateMentorshipRequest_Success() throws Exception {
        CreateMentorshipRequestDto input = new CreateMentorshipRequestDto("Looking for system design mock interviews.");

        MentorshipRequestDto responseDto = MentorshipRequestDto.builder()
                .id(200L)
                .studentId(101L)
                .mentorProfileId(1L)
                .status(MentorshipRequestStatus.PENDING)
                .goalDescription("Looking for system design mock interviews.")
                .build();

        when(mentorshipRequestService.createRequest(eq(101L), eq(1L), any(CreateMentorshipRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/mentors/1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(200L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/mentors/requests/{requestId}/accept returns 200 OK")
    void testAcceptRequest_Success() throws Exception {
        MentorshipRequestDto responseDto = MentorshipRequestDto.builder()
                .id(300L)
                .status(MentorshipRequestStatus.ACCEPTED)
                .conversationId(45L)
                .responseNote("Accepted!")
                .build();

        when(mentorshipRequestService.acceptRequest(eq(101L), eq(300L), eq("Accepted!")))
                .thenReturn(responseDto);

        MentorshipActionDto action = new MentorshipActionDto("Accepted!");

        mockMvc.perform(patch("/api/mentors/requests/300/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.conversationId").value(45L));
    }
}
