package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.SkillRequest;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.UserSkillService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final UserSkillService skillService;
    private final UserRepository userRepository;

    public SkillController(UserSkillService skillService, UserRepository userRepository) {
        this.skillService = skillService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<String>> getUserSkills(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(skillService.getUserSkills(user));
    }

    @PostMapping
    public ResponseEntity<List<String>> addSkill(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SkillRequest request) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(skillService.addSkill(user, request.getSkill()));
    }

    @DeleteMapping("/{skillName}")
    public ResponseEntity<List<String>> removeSkill(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String skillName) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(skillService.removeSkill(user, skillName));
    }
}
