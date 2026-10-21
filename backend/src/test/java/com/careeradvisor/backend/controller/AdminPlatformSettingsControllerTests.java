package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.MentorshipCapacityConfigDto;
import com.careeradvisor.backend.dto.PlatformSettingsDto;
import com.careeradvisor.backend.dto.UpdatePlatformSettingsRequest;
import com.careeradvisor.backend.service.PlatformSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminPlatformSettingsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlatformSettingsService platformSettingsService;

    @Test
    @DisplayName("GET /api/admin/settings returns 200 and platform settings for admin")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testGetSettingsSuccess() throws Exception {
        PlatformSettingsDto settings = PlatformSettingsDto.builder()
                .mentorshipCapacity(new MentorshipCapacityConfigDto(2, 12, 4))
                .lastUpdatedBy("admin@careeradvisor.com")
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        when(platformSettingsService.getPlatformSettings()).thenReturn(settings);

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentorshipCapacity.minMentees").value(2))
                .andExpect(jsonPath("$.mentorshipCapacity.maxMentees").value(12))
                .andExpect(jsonPath("$.mentorshipCapacity.defaultMentees").value(4))
                .andExpect(jsonPath("$.lastUpdatedBy").value("admin@careeradvisor.com"));

        verify(platformSettingsService).getPlatformSettings();
    }

    @Test
    @DisplayName("PUT /api/admin/settings updates settings and returns 200")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testUpdateSettingsSuccess() throws Exception {
        MentorshipCapacityConfigDto newCapacity = new MentorshipCapacityConfigDto(2, 15, 5);
        UpdatePlatformSettingsRequest request = new UpdatePlatformSettingsRequest(newCapacity);

        PlatformSettingsDto response = PlatformSettingsDto.builder()
                .mentorshipCapacity(newCapacity)
                .lastUpdatedBy("admin@careeradvisor.com")
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        when(platformSettingsService.updatePlatformSettings(any(), any(), any(UpdatePlatformSettingsRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentorshipCapacity.minMentees").value(2))
                .andExpect(jsonPath("$.mentorshipCapacity.maxMentees").value(15))
                .andExpect(jsonPath("$.mentorshipCapacity.defaultMentees").value(5));
    }

    @Test
    @DisplayName("PUT /api/admin/settings returns 400 when payload is invalid")
    @WithMockUser(username = "admin@careeradvisor.com", roles = {"ADMIN"})
    void testUpdateSettingsInvalidPayload() throws Exception {
        // Missing mentorshipCapacity
        UpdatePlatformSettingsRequest invalidRequest = new UpdatePlatformSettingsRequest(null);

        mockMvc.perform(put("/api/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
