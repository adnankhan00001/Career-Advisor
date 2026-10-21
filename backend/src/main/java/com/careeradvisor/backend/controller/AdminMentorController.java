package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.MentorProfileDto;
import com.careeradvisor.backend.dto.MentorshipActionDto;
import com.careeradvisor.backend.dto.MentorshipCapacityConfigDto;
import com.careeradvisor.backend.model.MentorVerificationStatus;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.MentorProfileService;
import com.careeradvisor.backend.service.MentorshipCapacityConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/mentors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMentorController {

    private final MentorProfileService mentorProfileService;
    private final MentorshipCapacityConfigService capacityConfigService;

    public AdminMentorController(MentorProfileService mentorProfileService,
                                MentorshipCapacityConfigService capacityConfigService) {
        this.mentorProfileService = mentorProfileService;
        this.capacityConfigService = capacityConfigService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<MentorProfileDto>> getPendingApplications() {
        List<MentorProfileDto> pending = mentorProfileService.getPendingApplications();
        return ResponseEntity.ok(pending);
    }

    @GetMapping
    public ResponseEntity<List<MentorProfileDto>> getAllMentors(
            @RequestParam(required = false) MentorVerificationStatus status) {
        List<MentorProfileDto> list = mentorProfileService.getAllMentorsForAdmin(status);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{mentorId}")
    public ResponseEntity<MentorProfileDto> getMentorById(@PathVariable Long mentorId) {
        MentorProfileDto profile = mentorProfileService.getMentorByIdForAdmin(mentorId);
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/{mentorId}/approve")
    public ResponseEntity<MentorProfileDto> approveApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long mentorId) {
        Long adminUserId = userDetails != null ? userDetails.getId() : null;
        MentorProfileDto approved = mentorProfileService.approveApplication(adminUserId, mentorId);
        return ResponseEntity.ok(approved);
    }

    @PatchMapping("/{mentorId}/reject")
    public ResponseEntity<MentorProfileDto> rejectApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long mentorId,
            @RequestBody(required = false) MentorshipActionDto dto) {
        Long adminUserId = userDetails != null ? userDetails.getId() : null;
        String reason = dto != null ? dto.getNote() : null;
        MentorProfileDto rejected = mentorProfileService.rejectApplication(adminUserId, mentorId, reason);
        return ResponseEntity.ok(rejected);
    }

    @PatchMapping("/{mentorId}/suspend")
    public ResponseEntity<MentorProfileDto> suspendMentor(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long mentorId,
            @RequestBody(required = false) MentorshipActionDto dto) {
        Long adminUserId = userDetails != null ? userDetails.getId() : null;
        String reason = dto != null ? dto.getNote() : null;
        MentorProfileDto suspended = mentorProfileService.suspendMentor(adminUserId, mentorId, reason);
        return ResponseEntity.ok(suspended);
    }

    @PatchMapping("/{mentorId}/reactivate")
    public ResponseEntity<MentorProfileDto> reactivateMentor(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long mentorId) {
        Long adminUserId = userDetails != null ? userDetails.getId() : null;
        MentorProfileDto reactivated = mentorProfileService.reactivateMentor(adminUserId, mentorId);
        return ResponseEntity.ok(reactivated);
    }

    @GetMapping("/config")
    public ResponseEntity<MentorshipCapacityConfigDto> getCapacityConfig() {
        return ResponseEntity.ok(capacityConfigService.getConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<MentorshipCapacityConfigDto> updateCapacityConfig(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MentorshipCapacityConfigDto dto) {
        Long adminUserId = userDetails != null ? userDetails.getId() : null;
        MentorshipCapacityConfigDto updated = capacityConfigService.updateConfig(adminUserId, dto);
        return ResponseEntity.ok(updated);
    }
}
