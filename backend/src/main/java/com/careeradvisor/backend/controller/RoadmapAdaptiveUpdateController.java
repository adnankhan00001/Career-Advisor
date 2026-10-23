package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.RoadmapAdaptiveUpdateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/roadmaps/personalized/adaptive-updates", "/api/roadmap/adaptive-updates"})
public class RoadmapAdaptiveUpdateController {

    private final RoadmapAdaptiveUpdateService updateService;
    private final UserRepository userRepository;

    public RoadmapAdaptiveUpdateController(RoadmapAdaptiveUpdateService updateService,
                                           UserRepository userRepository) {
        this.updateService = updateService;
        this.userRepository = userRepository;
    }

    @PostMapping("/preview")
    public ResponseEntity<RoadmapUpdateProposalDto> generateRoadmapPreview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GenerateRoadmapPreviewRequest request) {
        User user = getUser(userDetails);
        RoadmapUpdateProposalDto dto = updateService.generateProposalPreview(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{proposalId}")
    public ResponseEntity<RoadmapUpdateProposalDto> getProposal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long proposalId) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(updateService.getProposal(user, proposalId));
    }

    @PostMapping("/{proposalId}/apply")
    public ResponseEntity<PersonalizedRoadmapDto> applyProposal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long proposalId) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(updateService.applyProposal(user, proposalId));
    }

    @PostMapping("/{proposalId}/reject")
    public ResponseEntity<RoadmapUpdateProposalDto> rejectProposal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long proposalId,
            @RequestBody(required = false) RejectProposalRequest request) {
        User user = getUser(userDetails);
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(updateService.rejectProposal(user, proposalId, reason));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ProposalSummaryDto>> getProposalHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(updateService.getProposalHistory(user));
    }

    private User getUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new SecurityException("Unauthenticated user");
        }
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getId()));
    }
}
