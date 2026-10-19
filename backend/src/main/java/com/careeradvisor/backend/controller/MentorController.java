package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.MentorProfileService;
import com.careeradvisor.backend.service.MentorshipRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mentors")
public class MentorController {

    private final MentorProfileService mentorProfileService;
    private final MentorshipRequestService mentorshipRequestService;

    public MentorController(MentorProfileService mentorProfileService,
                            MentorshipRequestService mentorshipRequestService) {
        this.mentorProfileService = mentorProfileService;
        this.mentorshipRequestService = mentorshipRequestService;
    }

    private Long getAuthUserId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("User is not authenticated");
        }
        return userDetails.getId();
    }

    @PostMapping("/apply")
    public ResponseEntity<MentorProfileDto> applyAsMentor(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MentorApplicationDto dto) {
        Long userId = getAuthUserId(userDetails);
        MentorProfileDto created = mentorProfileService.submitApplication(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/me")
    public ResponseEntity<MentorProfileDto> getMyMentorProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = getAuthUserId(userDetails);
        MentorProfileDto profile = mentorProfileService.getMyProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<MentorProfileDto> updateMyMentorProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MentorProfileUpdateDto dto) {
        Long userId = getAuthUserId(userDetails);
        MentorProfileDto updated = mentorProfileService.updateMyProfile(userId, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<List<MentorProfileDto>> getApprovedMentors(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String expertise) {
        List<MentorProfileDto> list = mentorProfileService.getApprovedMentors(query, expertise);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{mentorId}")
    public ResponseEntity<MentorProfileDto> getMentorById(@PathVariable Long mentorId) {
        MentorProfileDto profile = mentorProfileService.getMentorById(mentorId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/{mentorId}/requests")
    public ResponseEntity<MentorshipRequestDto> createMentorshipRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long mentorId,
            @Valid @RequestBody CreateMentorshipRequestDto dto) {
        Long studentId = getAuthUserId(userDetails);
        MentorshipRequestDto request = mentorshipRequestService.createRequest(studentId, mentorId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<List<MentorshipRequestDto>> getSentRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long studentId = getAuthUserId(userDetails);
        List<MentorshipRequestDto> list = mentorshipRequestService.getSentRequests(studentId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/requests/received")
    public ResponseEntity<List<MentorshipRequestDto>> getReceivedRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long mentorUserId = getAuthUserId(userDetails);
        List<MentorshipRequestDto> list = mentorshipRequestService.getReceivedRequests(mentorUserId);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/requests/{requestId}/accept")
    public ResponseEntity<MentorshipRequestDto> acceptRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long requestId,
            @RequestBody(required = false) MentorshipActionDto dto) {
        Long mentorUserId = getAuthUserId(userDetails);
        String note = dto != null ? dto.getNote() : null;
        MentorshipRequestDto accepted = mentorshipRequestService.acceptRequest(mentorUserId, requestId, note);
        return ResponseEntity.ok(accepted);
    }

    @PatchMapping("/requests/{requestId}/decline")
    public ResponseEntity<MentorshipRequestDto> declineRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long requestId,
            @RequestBody(required = false) MentorshipActionDto dto) {
        Long mentorUserId = getAuthUserId(userDetails);
        String note = dto != null ? dto.getNote() : null;
        MentorshipRequestDto declined = mentorshipRequestService.declineRequest(mentorUserId, requestId, note);
        return ResponseEntity.ok(declined);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ResponseEntity<MentorshipRequestDto> cancelRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long requestId) {
        Long studentId = getAuthUserId(userDetails);
        MentorshipRequestDto cancelled = mentorshipRequestService.cancelRequest(studentId, requestId);
        return ResponseEntity.ok(cancelled);
    }

    @PatchMapping("/requests/{requestId}/complete")
    public ResponseEntity<MentorshipRequestDto> completeMentorship(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long requestId,
            @RequestBody(required = false) MentorshipActionDto dto) {
        Long currentUserId = getAuthUserId(userDetails);
        String note = dto != null ? dto.getNote() : null;
        MentorshipRequestDto completed = mentorshipRequestService.completeMentorship(currentUserId, requestId, note);
        return ResponseEntity.ok(completed);
    }
}
