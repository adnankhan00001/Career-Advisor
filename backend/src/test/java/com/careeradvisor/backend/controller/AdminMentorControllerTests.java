package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.MentorProfileDto;
import com.careeradvisor.backend.dto.MentorshipActionDto;
import com.careeradvisor.backend.dto.MentorshipCapacityConfigDto;
import com.careeradvisor.backend.exception.GlobalExceptionHandler;
import com.careeradvisor.backend.model.MentorVerificationStatus;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.MentorProfileService;
import com.careeradvisor.backend.service.MentorshipCapacityConfigService;
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
class AdminMentorControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MentorProfileService mentorProfileService;

    @Mock
    private MentorshipCapacityConfigService capacityConfigService;

    @InjectMocks
    private AdminMentorController adminMentorController;

    private CustomUserDetails mockAdminUserDetails;

    @BeforeEach
    void setUp() {
        User adminUser = User.builder()
                .id(1L)
                .name("Admin Boss")
                .email("admin@careeradvisor.com")
                .role(Role.ADMIN)
                .build();
        mockAdminUserDetails = CustomUserDetails.build(adminUser);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockAdminUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(adminMentorController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/mentors/pending returns list of applications awaiting review")
    void testGetPendingApplications() throws Exception {
        MentorProfileDto app = MentorProfileDto.builder()
                .id(15L)
                .name("Candidate Dev")
                .jobTitle("Senior Engineer")
                .company("Oracle")
                .verificationStatus(MentorVerificationStatus.APPLIED)
                .build();

        when(mentorProfileService.getPendingApplications()).thenReturn(List.of(app));

        mockMvc.perform(get("/api/admin/mentors/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15L))
                .andExpect(jsonPath("$[0].verificationStatus").value("APPLIED"));
    }

    @Test
    @DisplayName("PATCH /api/admin/mentors/{mentorId}/approve approves application")
    void testApproveMentorApplication() throws Exception {
        MentorProfileDto approved = MentorProfileDto.builder()
                .id(15L)
                .name("Candidate Dev")
                .verificationStatus(MentorVerificationStatus.APPROVED)
                .isActive(true)
                .build();

        when(mentorProfileService.approveApplication(eq(1L), eq(15L))).thenReturn(approved);

        mockMvc.perform(patch("/api/admin/mentors/15/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("PATCH /api/admin/mentors/{mentorId}/reject rejects application with reason")
    void testRejectMentorApplication() throws Exception {
        MentorProfileDto rejected = MentorProfileDto.builder()
                .id(15L)
                .verificationStatus(MentorVerificationStatus.REJECTED)
                .rejectionReason("Insufficient experience")
                .build();

        when(mentorProfileService.rejectApplication(eq(1L), eq(15L), eq("Insufficient experience"))).thenReturn(rejected);

        MentorshipActionDto action = new MentorshipActionDto("Insufficient experience");

        mockMvc.perform(patch("/api/admin/mentors/15/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Insufficient experience"));
    }

    @Test
    @DisplayName("GET /api/admin/mentors/config returns capacity limits")
    void testGetCapacityConfig() throws Exception {
        MentorshipCapacityConfigDto config = MentorshipCapacityConfigDto.builder()
                .minMentees(1)
                .maxMentees(10)
                .defaultMentees(3)
                .build();

        when(capacityConfigService.getConfig()).thenReturn(config);

        mockMvc.perform(get("/api/admin/mentors/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minMentees").value(1))
                .andExpect(jsonPath("$.maxMentees").value(10))
                .andExpect(jsonPath("$.defaultMentees").value(3));
    }

    @Test
    @DisplayName("PUT /api/admin/mentors/config updates capacity limits")
    void testUpdateCapacityConfig() throws Exception {
        MentorshipCapacityConfigDto input = new MentorshipCapacityConfigDto(2, 8, 4);
        when(capacityConfigService.updateConfig(eq(1L), any(MentorshipCapacityConfigDto.class))).thenReturn(input);

        mockMvc.perform(put("/api/admin/mentors/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minMentees").value(2))
                .andExpect(jsonPath("$.maxMentees").value(8))
                .andExpect(jsonPath("$.defaultMentees").value(4));
    }

    @Test
    @DisplayName("PATCH /api/admin/mentors/{mentorId}/reactivate reactivates suspended mentor")
    void testReactivateMentor() throws Exception {
        MentorProfileDto reactivated = MentorProfileDto.builder()
                .id(15L)
                .name("Candidate Dev")
                .verificationStatus(MentorVerificationStatus.APPROVED)
                .isActive(true)
                .build();

        when(mentorProfileService.reactivateMentor(eq(1L), eq(15L))).thenReturn(reactivated);

        mockMvc.perform(patch("/api/admin/mentors/15/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.active").value(true));
    }
}
