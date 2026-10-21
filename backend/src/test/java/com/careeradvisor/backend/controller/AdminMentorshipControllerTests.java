package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.GlobalExceptionHandler;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.MentorshipRequestStatus;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.AdminMentorshipService;
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
class AdminMentorshipControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminMentorshipService adminMentorshipService;

    @InjectMocks
    private AdminMentorshipController adminMentorshipController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(adminMentorshipController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/mentorships/summary returns 200 OK with summary statistics")
    void testGetMentorshipSummary() throws Exception {
        MentorshipOversightSummaryDto summary = MentorshipOversightSummaryDto.builder()
                .totalRequests(15L)
                .pendingRequests(3L)
                .activeMentorships(7L)
                .completedRequests(4L)
                .declinedRequests(1L)
                .terminatedRequests(0L)
                .build();

        when(adminMentorshipService.getMentorshipSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/admin/mentorships/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(15L))
                .andExpect(jsonPath("$.activeMentorships").value(7L));
    }

    @Test
    @DisplayName("GET /api/admin/mentorships returns 200 OK with paginated list")
    void testGetMentorshipRequests() throws Exception {
        AdminMentorshipRequestDto item = AdminMentorshipRequestDto.builder()
                .id(42L)
                .studentName("Alice Smith")
                .mentorName("Bob Jones")
                .status(MentorshipRequestStatus.ACCEPTED)
                .build();

        PageResponse<AdminMentorshipRequestDto> page = PageResponse.<AdminMentorshipRequestDto>builder()
                .content(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(adminMentorshipService.getMentorshipRequests(eq(0), eq(10), eq("createdAt,desc"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/mentorships")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42L))
                .andExpect(jsonPath("$.content[0].studentName").value("Alice Smith"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/mentorships/{id} returns 200 OK with details")
    void testGetMentorshipDetailSuccess() throws Exception {
        AdminMentorshipDetailDto detail = AdminMentorshipDetailDto.builder()
                .id(42L)
                .studentName("Alice Smith")
                .mentorName("Bob Jones")
                .status(MentorshipRequestStatus.ACCEPTED)
                .goalDescription("Technical interview preparation")
                .build();

        when(adminMentorshipService.getMentorshipDetail(42L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/mentorships/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42L))
                .andExpect(jsonPath("$.goalDescription").value("Technical interview preparation"));
    }

    @Test
    @DisplayName("GET /api/admin/mentorships/{id} returns 404 when request not found")
    void testGetMentorshipDetailNotFound() throws Exception {
        when(adminMentorshipService.getMentorshipDetail(999L))
                .thenThrow(new ResourceNotFoundException("Mentorship request not found with id: 999"));

        mockMvc.perform(get("/api/admin/mentorships/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/admin/mentorships/{id}/terminate terminates mentorship with reason")
    void testTerminateMentorshipSuccess() throws Exception {
        TerminateMentorshipRequest req = TerminateMentorshipRequest.builder()
                .reason("Administrative cancellation due to policy breach")
                .build();

        AdminMentorshipDetailDto terminated = AdminMentorshipDetailDto.builder()
                .id(42L)
                .status(MentorshipRequestStatus.TERMINATED)
                .responseNote("[ADMIN TERMINATED]: Administrative cancellation due to policy breach")
                .build();

        when(adminMentorshipService.terminateMentorship(eq(1L), eq(42L), any(TerminateMentorshipRequest.class)))
                .thenReturn(terminated);

        mockMvc.perform(post("/api/admin/mentorships/42/terminate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42L))
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    @Test
    @DisplayName("POST /api/admin/mentorships/{id}/terminate with blank reason returns 400 Bad Request")
    void testTerminateMentorshipBlankReason() throws Exception {
        TerminateMentorshipRequest req = TerminateMentorshipRequest.builder()
                .reason("")
                .build();

        mockMvc.perform(post("/api/admin/mentorships/42/terminate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
