package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.UserSearchDto;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    private final UserRepository userRepository;
    private final PresenceService presenceService;

    public UserSearchController(UserRepository userRepository, PresenceService presenceService) {
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> searchUsers(
            @RequestParam(value = "q", required = false) String query,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String sanitized = query.trim();
        List<User> found = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(sanitized, sanitized);

        Long currentUserId = userDetails != null ? userDetails.getId() : null;

        List<UserSearchDto> result = found.stream()
                .filter(u -> currentUserId == null || !u.getId().equals(currentUserId))
                .limit(20)
                .map(u -> UserSearchDto.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .careerGoal(u.getCareerGoal())
                        .userLevel(u.getUserLevel())
                        .role(u.getRole())
                        .online(presenceService.isUserOnline(u.getId()))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
