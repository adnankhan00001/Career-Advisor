package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.ConversationResponseDto;
import com.careeradvisor.backend.dto.UpdateSupportStatusRequest;
import com.careeradvisor.backend.exception.GlobalExceptionHandler;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.ChatService;
import com.careeradvisor.backend.service.ConversationService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminSupportInboxControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ConversationService conversationService;

    @Mock
    private ChatService chatService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConversationController conversationController;

    private User adminUser;
    private CustomUserDetails mockAdminUserDetails;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .name("Admin Boss")
                .email("admin@careeradvisor.com")
                .role(Role.ADMIN)
                .build();
        mockAdminUserDetails = CustomUserDetails.build(adminUser);

        org.mockito.Mockito.lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

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

        mockMvc = MockMvcBuilders.standaloneSetup(conversationController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/conversations/admin/inbox returns 200 OK with conversation list")
    void testGetAdminInbox() throws Exception {
        ConversationResponseDto conv = ConversationResponseDto.builder()
                .id(101L)
                .title("Support: Resume Issue")
                .conversationType("USER_TO_ADMIN")
                .archived(false)
                .messageCount(3L)
                .build();

        when(conversationService.getAdminInbox(eq(adminUser), eq("Resume"), eq("OPEN")))
                .thenReturn(List.of(conv));

        mockMvc.perform(get("/api/conversations/admin/inbox")
                        .param("search", "Resume")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101L))
                .andExpect(jsonPath("$[0].title").value("Support: Resume Issue"))
                .andExpect(jsonPath("$[0].archived").value(false));
    }

    @Test
    @DisplayName("PATCH /api/conversations/admin/{id}/status resolves ticket successfully")
    void testUpdateSupportStatusResolve() throws Exception {
        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("RESOLVED")
                .note("Issue addressed")
                .build();

        ConversationResponseDto resolved = ConversationResponseDto.builder()
                .id(101L)
                .title("Support: Resume Issue")
                .conversationType("USER_TO_ADMIN")
                .archived(true)
                .build();

        when(conversationService.updateSupportStatus(eq(101L), eq(adminUser), any(UpdateSupportStatusRequest.class)))
                .thenReturn(resolved);

        mockMvc.perform(patch("/api/conversations/admin/101/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    @DisplayName("PATCH /api/conversations/admin/{id}/status reopens ticket successfully")
    void testUpdateSupportStatusReopen() throws Exception {
        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("OPEN")
                .note("Follow up required")
                .build();

        ConversationResponseDto reopened = ConversationResponseDto.builder()
                .id(101L)
                .title("Support: Resume Issue")
                .conversationType("USER_TO_ADMIN")
                .archived(false)
                .build();

        when(conversationService.updateSupportStatus(eq(101L), eq(adminUser), any(UpdateSupportStatusRequest.class)))
                .thenReturn(reopened);

        mockMvc.perform(patch("/api/conversations/admin/101/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    @DisplayName("PATCH /api/conversations/admin/{id}/status with blank status returns 400 Bad Request")
    void testUpdateSupportStatusBlankStatus() throws Exception {
        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("")
                .build();

        mockMvc.perform(patch("/api/conversations/admin/101/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/conversations/admin/{id}/status on not found returns 404")
    void testUpdateSupportStatusNotFound() throws Exception {
        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("RESOLVED")
                .build();

        when(conversationService.updateSupportStatus(eq(999L), eq(adminUser), any(UpdateSupportStatusRequest.class)))
                .thenThrow(new ResourceNotFoundException("Conversation not found with id: 999"));

        mockMvc.perform(patch("/api/conversations/admin/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
