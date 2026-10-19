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
            @PathVariable Long mentorId,
            @RequestBody(required = false) MentorshipActionDto dto) {
        String reason = dto != null ? dto.getNote() : null;
        MentorProfileDto rejected = mentorProfileService.rejectApplication(mentorId, reason);
        return ResponseEntity.ok(rejected);
    }

    @PatchMapping("/{mentorId}/suspend")
    public ResponseEntity<MentorProfileDto> suspendMentor(
            @PathVariable Long mentorId,
            @RequestBody(required = false) MentorshipActionDto dto) {
        String reason = dto != null ? dto.getNote() : null;
        MentorProfileDto suspended = mentorProfileService.suspendMentor(mentorId, reason);
        return ResponseEntity.ok(suspended);
    }

    @GetMapping("/config")
    public ResponseEntity<MentorshipCapacityConfigDto> getCapacityConfig() {
        return ResponseEntity.ok(capacityConfigService.getConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<MentorshipCapacityConfigDto> updateCapacityConfig(
            @Valid @RequestBody MentorshipCapacityConfigDto dto) {
        MentorshipCapacityConfigDto updated = capacityConfigService.updateConfig(dto);
        return ResponseEntity.ok(updated);
    }
}
