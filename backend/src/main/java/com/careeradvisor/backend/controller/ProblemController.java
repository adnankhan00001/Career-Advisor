package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.CodeSubmissionRequest;
import com.careeradvisor.backend.dto.CodeSubmissionResultDto;
import com.careeradvisor.backend.dto.CodingProblemDto;
import com.careeradvisor.backend.dto.ProblemProgressSummaryDto;
import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.ProblemCategory;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.ProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final UserRepository userRepository;

    public ProblemController(ProblemService problemService, UserRepository userRepository) {
        this.problemService = problemService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<CodingProblemDto>> getProblems(
            @RequestParam(required = false) ProblemCategory category,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Difficulty difficulty,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        return ResponseEntity.ok(problemService.getProblems(category, topic, difficulty, user));
    }

    @GetMapping("/progress/summary")
    public ResponseEntity<ProblemProgressSummaryDto> getProgressSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(problemService.getProgressSummary(user));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(Arrays.stream(ProblemCategory.values())
                .map(Enum::name)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<CodingProblemDto> getProblem(
            @PathVariable String idOrSlug,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);

        try {
            Long id = Long.parseLong(idOrSlug);
            return ResponseEntity.ok(problemService.getProblemById(id, user));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(problemService.getProblemBySlug(idOrSlug, user));
        }
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<CodingProblemDto> toggleProblemSolved(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(problemService.toggleProblemSolved(id, user));
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<CodeSubmissionResultDto> runCode(
            @PathVariable Long id,
            @RequestBody CodeSubmissionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        return ResponseEntity.ok(problemService.runCode(id, request, user));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<CodeSubmissionResultDto> submitCode(
            @PathVariable Long id,
            @RequestBody CodeSubmissionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getUserFromPrincipal(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(problemService.submitCode(id, request, user));
    }

    private User getUserFromPrincipal(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getId() != null) {
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }
}
