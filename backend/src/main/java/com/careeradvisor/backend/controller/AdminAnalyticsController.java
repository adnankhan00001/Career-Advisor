package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.CareerPrepAnalyticsDto;
import com.careeradvisor.backend.dto.MentorshipAnalyticsDto;
import com.careeradvisor.backend.dto.UserAnalyticsDto;
import com.careeradvisor.backend.service.AdminAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/users")
    public ResponseEntity<UserAnalyticsDto> getUserAnalytics(
            @RequestParam(value = "days", defaultValue = "30") int days
    ) {
        UserAnalyticsDto analytics = adminAnalyticsService.getUserAnalytics(days);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/career-preparation")
    public ResponseEntity<CareerPrepAnalyticsDto> getCareerPrepAnalytics(
            @RequestParam(value = "days", defaultValue = "30") int days
    ) {
        CareerPrepAnalyticsDto analytics = adminAnalyticsService.getCareerPrepAnalytics(days);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/mentorship")
    public ResponseEntity<MentorshipAnalyticsDto> getMentorshipAnalytics(
            @RequestParam(value = "days", defaultValue = "30") int days
    ) {
        MentorshipAnalyticsDto analytics = adminAnalyticsService.getMentorshipAnalytics(days);
        return ResponseEntity.ok(analytics);
    }
}
