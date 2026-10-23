package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final UserRepository userRepository;

    public OnboardingController(OnboardingService onboardingService, UserRepository userRepository) {
        this.onboardingService = onboardingService;
        this.userRepository = userRepository;
    }

    @GetMapping("/state")
    public ResponseEntity<OnboardingStateDto> getOnboardingState(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(onboardingService.getOnboardingState(user));
    }

    @PostMapping("/select-career")
    public ResponseEntity<OnboardingStateDto> selectCareer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingSelectCareerRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(onboardingService.selectCareer(user, request.getCareerSlug()));
    }

    @PostMapping("/select-technologies")
    public ResponseEntity<OnboardingStateDto> selectTechnologies(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingSelectTechnologiesRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(onboardingService.selectTechnologies(user, request.getTechnologySlugs()));
    }

    @PostMapping("/study-preferences")
    public ResponseEntity<OnboardingStateDto> saveStudyPreferences(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingStudyPreferencesRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(onboardingService.saveStudyPreferences(user, request));
    }

    @PostMapping("/start-assessment")
    public ResponseEntity<AssessmentSessionDto> startAssessment(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(onboardingService.startOnboardingAssessment(user));
    }

    @PostMapping("/review-report")
    public ResponseEntity<OnboardingStateDto> reviewReport(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(onboardingService.reviewAssessmentReport(user));
    }

    @PostMapping("/complete")
    public ResponseEntity<OnboardingStateDto> completeOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(onboardingService.completeOnboarding(user));
    }

    @PostMapping("/reset")
    public ResponseEntity<OnboardingStateDto> resetOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(onboardingService.resetOnboarding(user));
    }

    private User getUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new SecurityException("Unauthenticated user");
        }
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getId()));
    }
}
