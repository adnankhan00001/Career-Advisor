package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.CareerGoalRequest;
import com.careeradvisor.backend.dto.ProgressSummaryResponse;
import com.careeradvisor.backend.dto.ToggleStepRequest;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.UserProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final UserProgressService progressService;
    private final UserRepository userRepository;

    public ProgressController(UserProgressService progressService, UserRepository userRepository) {
        this.progressService = progressService;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<ProgressSummaryResponse> getProgressSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(progressService.getProgressSummary(user));
    }

    @GetMapping("/roadmap")
    public ResponseEntity<List<String>> getCompletedSteps(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(progressService.getCompletedSteps(user));
    }

    @PostMapping("/roadmap/toggle")
    public ResponseEntity<List<String>> toggleStep(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ToggleStepRequest request) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(progressService.toggleStep(user, request.getStepTitle(), request.getCareerTitle()));
    }

    @PostMapping("/career-goal")
    public ResponseEntity<Map<String, String>> updateCareerGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CareerGoalRequest request) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String updated = progressService.updateCareerGoal(user, request.getCareerGoal());
        return ResponseEntity.ok(Map.of("careerGoal", updated, "message", "Career goal updated successfully"));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        progressService.resetProgress(user);
        return ResponseEntity.ok(Map.of("message", "Roadmap progress reset successfully"));
    }
}
