package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.PlannerPreference;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.PlannerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/planner")
public class PlannerController {

    private final PlannerService plannerService;
    private final UserRepository userRepository;

    public PlannerController(PlannerService plannerService, UserRepository userRepository) {
        this.plannerService = plannerService;
        this.userRepository = userRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<WeeklyPlanDto> getCurrentPlan(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.getCurrentWeeklyPlan(user));
    }

    @PostMapping("/generate")
    public ResponseEntity<WeeklyPlanDto> generatePlan(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.generateWeeklyPlan(user, true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<WeeklyPlanDto> refreshPlan(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.refreshActivePlan(user));
    }

    @GetMapping("/today")
    public ResponseEntity<List<LearningTaskDto>> getTodayTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.getTodayTasks(user));
    }

    @GetMapping("/week")
    public ResponseEntity<List<DailyScheduleDto>> getWeeklySchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.getWeeklySchedule(user));
    }

    @GetMapping("/progress")
    public ResponseEntity<PlannerProgressDto> getProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.getProgress(user));
    }

    @GetMapping("/preferences")
    public ResponseEntity<PlannerPreferenceDto> getPreferences(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        PlannerPreference pref = plannerService.getOrCreatePreferences(user);
        return ResponseEntity.ok(PlannerPreferenceDto.builder()
                .dailyStudyMinutes(pref.getDailyStudyMinutes())
                .preferredStudyWindow(pref.getPreferredStudyWindow())
                .preferredDays(Arrays.asList(pref.getPreferredDays().split(",")))
                .weeklyGoal(pref.getWeeklyGoal())
                .build());
    }

    @PutMapping("/preferences")
    public ResponseEntity<PlannerPreferenceDto> updatePreferences(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PlannerPreferenceDto dto) {
        User user = getAuthenticatedUser(userDetails);
        PlannerPreference updated = plannerService.updatePreferences(user, dto);
        return ResponseEntity.ok(PlannerPreferenceDto.builder()
                .dailyStudyMinutes(updated.getDailyStudyMinutes())
                .preferredStudyWindow(updated.getPreferredStudyWindow())
                .preferredDays(Arrays.asList(updated.getPreferredDays().split(",")))
                .weeklyGoal(updated.getWeeklyGoal())
                .build());
    }

    @PostMapping("/tasks/{id}/start")
    public ResponseEntity<LearningTaskDto> startTask(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.startTask(user, id));
    }

    @PostMapping("/tasks/{id}/complete")
    public ResponseEntity<LearningTaskDto> completeTask(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.completeTask(user, id));
    }

    @PostMapping("/tasks/{id}/skip")
    public ResponseEntity<LearningTaskDto> skipTask(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(plannerService.skipTask(user, id));
    }

    private User getAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
