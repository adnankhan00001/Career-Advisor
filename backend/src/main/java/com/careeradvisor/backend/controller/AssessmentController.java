package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.AssessmentSession;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.AssessmentSessionRepository;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.AssessmentComparisonService;
import com.careeradvisor.backend.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AssessmentComparisonService comparisonService;
    private final AssessmentSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public AssessmentController(AssessmentService assessmentService,
                                AssessmentComparisonService comparisonService,
                                AssessmentSessionRepository sessionRepository,
                                UserRepository userRepository) {
        this.assessmentService = assessmentService;
        this.comparisonService = comparisonService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/start")
    public ResponseEntity<AssessmentSessionDto> startAssessment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StartAssessmentRequest request) {
        User user = getUser(userDetails);
        AssessmentSessionDto sessionDto = assessmentService.startAssessment(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionDto);
    }

    @GetMapping("/active")
    public ResponseEntity<AssessmentSessionDto> getActiveAssessment(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return assessmentService.getActiveSession(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<AssessmentSessionDto> getSessionState(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(assessmentService.getSessionState(sessionId, user));
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<AssessmentQuestionDto> submitAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(assessmentService.submitAnswer(sessionId, request, user));
    }

    @PostMapping("/{sessionId}/skip")
    public ResponseEntity<AssessmentQuestionDto> skipQuestion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId,
            @Valid @RequestBody SkipQuestionRequest request) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(assessmentService.skipQuestion(sessionId, request.getSessionQuestionId(), user));
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<AssessmentReportDto> submitAssessment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(assessmentService.submitAssessment(sessionId, user));
    }

    @GetMapping("/{sessionId}/report")
    public ResponseEntity<AssessmentReportDto> getAssessmentReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(assessmentService.getAssessmentReport(sessionId, user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AssessmentSummaryDto>> getAssessmentHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(assessmentService.getAssessmentHistory(user));
    }

    @PostMapping("/reassessments")
    public ResponseEntity<AssessmentSessionDto> startReassessment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StartReassessmentRequest request) {
        User user = getUser(userDetails);
        AssessmentSessionDto sessionDto = assessmentService.startReassessment(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionDto);
    }

    @GetMapping("/reassessments/{id}")
    public ResponseEntity<AssessmentSessionDto> getReassessmentAttempt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(assessmentService.getSessionState(id, user));
    }

    @PostMapping("/reassessments/{id}/submit")
    public ResponseEntity<AssessmentReportDto> submitReassessment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(assessmentService.submitAssessment(id, user));
    }

    @GetMapping("/reassessments/{id}/comparison")
    public ResponseEntity<AssessmentComparisonDto> getReassessmentComparison(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(required = false) Long baselineId) {
        User user = getUser(userDetails);
        AssessmentSession reassessment = sessionRepository.findByIdWithTechnologies(id)
                .or(() -> sessionRepository.findById(id))
                .orElseThrow(() -> new IllegalArgumentException("Assessment session not found: " + id));

        AssessmentSession baseline = null;
        if (baselineId != null) {
            baseline = sessionRepository.findByIdWithTechnologies(baselineId)
                    .or(() -> sessionRepository.findById(baselineId))
                    .orElseThrow(() -> new IllegalArgumentException("Baseline session not found: " + baselineId));
        }

        return ResponseEntity.ok(comparisonService.compareSessions(reassessment, baseline, user));
    }

    private User getUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            throw new SecurityException("Unauthenticated user");
        }
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getId()));
    }
}
