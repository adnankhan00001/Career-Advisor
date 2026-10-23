package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.PersonalizedRoadmapService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmaps/personalized")
public class PersonalizedRoadmapController {

    private final PersonalizedRoadmapService personalizedRoadmapService;
    private final UserRepository userRepository;

    public PersonalizedRoadmapController(PersonalizedRoadmapService personalizedRoadmapService,
                                         UserRepository userRepository) {
        this.personalizedRoadmapService = personalizedRoadmapService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<PersonalizedRoadmapDto> getPersonalizedRoadmap(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        PersonalizedRoadmapDto dto = personalizedRoadmapService.getPersonalizedRoadmap(user);
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/generate")
    public ResponseEntity<PersonalizedRoadmapDto> generateRoadmap(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "true") boolean forceRegenerate) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.generateRoadmap(user, forceRegenerate));
    }

    @PostMapping("/steps/{stepId}/toggle")
    public ResponseEntity<PersonalizedRoadmapDto> toggleStep(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long stepId) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.toggleStep(user, stepId));
    }

    @PutMapping("/steps/{stepId}/state")
    public ResponseEntity<PersonalizedRoadmapDto> updateStepState(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long stepId,
            @Valid @RequestBody RoadmapStepStateRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.updateStepState(user, stepId, request.getState()));
    }

    @PutMapping("/steps/{stepId}/priority")
    public ResponseEntity<PersonalizedRoadmapDto> updateStepPriority(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long stepId,
            @Valid @RequestBody RoadmapStepPriorityRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.updateStepPriority(user, stepId, request.getPriority()));
    }

    @PostMapping("/milestones/custom")
    public ResponseEntity<PersonalizedRoadmapDto> addCustomMilestone(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddCustomMilestoneRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.addCustomMilestone(user, request));
    }

    @PutMapping("/milestones/{milestoneId}")
    public ResponseEntity<PersonalizedRoadmapDto> updateMilestone(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long milestoneId,
            @Valid @RequestBody UpdateMilestoneRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.updateMilestone(user, milestoneId, request));
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<PersonalizedRoadmapDto> deleteCustomMilestone(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long milestoneId) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.deleteCustomMilestone(user, milestoneId));
    }

    @PostMapping("/milestones/{milestoneId}/skip")
    public ResponseEntity<PersonalizedRoadmapDto> skipMilestone(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long milestoneId,
            @RequestParam(defaultValue = "true") boolean skip) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.skipMilestone(user, milestoneId, skip));
    }

    @PostMapping("/milestones/reorder")
    public ResponseEntity<PersonalizedRoadmapDto> reorderMilestones(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReorderMilestonesRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.reorderMilestones(user, request.getMilestoneIds()));
    }

    @PostMapping("/milestones/{milestoneId}/evidence")
    public ResponseEntity<ProjectEvidenceDto> submitEvidence(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long milestoneId,
            @Valid @RequestBody SubmitEvidenceRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.submitEvidence(user, milestoneId, request));
    }

    @GetMapping("/milestones/{milestoneId}/evidence")
    public ResponseEntity<List<ProjectEvidenceDto>> getMilestoneEvidence(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long milestoneId) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.getMilestoneEvidence(user, milestoneId));
    }

    @GetMapping("/evidence")
    public ResponseEntity<List<ProjectEvidenceDto>> getRoadmapEvidence(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.getRoadmapEvidence(user));
    }

    @PutMapping("/schedule")
    public ResponseEntity<PersonalizedRoadmapDto> updateSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateScheduleRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.updateSchedule(user, request));
    }

    @GetMapping("/revisions")
    public ResponseEntity<List<RoadmapRevisionDto>> getRevisionHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(personalizedRoadmapService.getRevisionHistory(user));
    }

    private User getUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new SecurityException("Unauthenticated user");
        }
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getId()));
    }
}
