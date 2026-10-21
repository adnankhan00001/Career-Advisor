package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.PlatformSettingsDto;
import com.careeradvisor.backend.dto.UpdatePlatformSettingsRequest;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.PlatformSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlatformSettingsController {

    private final PlatformSettingsService platformSettingsService;

    public AdminPlatformSettingsController(PlatformSettingsService platformSettingsService) {
        this.platformSettingsService = platformSettingsService;
    }

    @GetMapping
    public ResponseEntity<PlatformSettingsDto> getSettings() {
        PlatformSettingsDto settings = platformSettingsService.getPlatformSettings();
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<PlatformSettingsDto> updateSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdatePlatformSettingsRequest request) {
        Long adminUserId = userDetails != null ? userDetails.getId() : null;
        String adminEmail = userDetails != null ? userDetails.getUsername() : null;
        PlatformSettingsDto updated = platformSettingsService.updatePlatformSettings(adminUserId, adminEmail, request);
        return ResponseEntity.ok(updated);
    }
}
