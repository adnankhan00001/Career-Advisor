package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.service.ResumeService;
import com.careeradvisor.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;

    public ResumeController(ResumeService resumeService, UserService userService) {
        this.resumeService = resumeService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        return userService.getUserByEmail(userDetails.getUsername());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeAnalysisResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        ResumeAnalysisResponse response = resumeService.uploadAndAnalyze(user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeDto>> getUserResumes(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        List<ResumeDto> resumes = resumeService.getUserResumes(user);
        return ResponseEntity.ok(resumes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeAnalysisResponse> getResume(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        ResumeAnalysisResponse response = resumeService.getResumeAnalysis(user, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<ResumeAnalysisResponse> getResumeAnalysis(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        ResumeAnalysisResponse response = resumeService.getResumeAnalysis(user, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest/analysis")
    public ResponseEntity<ResumeAnalysisResponse> getLatestAnalysis(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return resumeService.getLatestAnalysis(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteResume(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        resumeService.deleteResume(user, id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Resume deleted successfully"));
    }

    @PostMapping("/{id}/sync-skills")
    public ResponseEntity<SyncSkillsResponse> syncSkills(
            @PathVariable("id") Long id,
            @Valid @RequestBody SyncSkillsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        SyncSkillsResponse response = resumeService.syncSkills(user, id, request.getSkills());
        return ResponseEntity.ok(response);
    }
}
