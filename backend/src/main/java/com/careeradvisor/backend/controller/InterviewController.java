package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.InterviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class InterviewController {

    private final InterviewService interviewService;
    private final UserRepository userRepository;

    public InterviewController(InterviewService interviewService, UserRepository userRepository) {
        this.interviewService = interviewService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<MockInterviewSessionDto> startInterview(
            @RequestBody(required = false) StartInterviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        StartInterviewRequest req = request != null ? request : new StartInterviewRequest();
        return ResponseEntity.ok(interviewService.startInterview(req, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MockInterviewSessionDto> getActiveSession(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(interviewService.getActiveSession(id, user));
    }

    @PostMapping("/{id}/answers")
    public ResponseEntity<MockInterviewSessionDto> saveAnswer(
            @PathVariable Long id,
            @RequestBody SaveAnswerRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(interviewService.saveAnswer(id, request, user));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<MockInterviewResultDto> submitInterview(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(interviewService.submitInterview(id, user));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<MockInterviewResultDto> getInterviewResult(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(interviewService.getInterviewResult(id, user));
    }

    @GetMapping
    public ResponseEntity<List<MockInterviewHistoryItemDto>> getInterviewHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(interviewService.getInterviewHistory(user));
    }

    @GetMapping("/summary")
    public ResponseEntity<InterviewSummaryDto> getInterviewSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(interviewService.getInterviewSummary(user));
    }

    private User getUserFromPrincipal(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getId() != null) {
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }
}
