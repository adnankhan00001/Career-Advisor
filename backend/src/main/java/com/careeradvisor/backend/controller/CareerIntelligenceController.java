package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.CareerActionDto;
import com.careeradvisor.backend.dto.CareerIntelligenceDto;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.CareerIntelligenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/career-intelligence")
public class CareerIntelligenceController {

    private final CareerIntelligenceService careerIntelligenceService;
    private final UserRepository userRepository;

    public CareerIntelligenceController(CareerIntelligenceService careerIntelligenceService,
                                        UserRepository userRepository) {
        this.careerIntelligenceService = careerIntelligenceService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            return null;
        }
        return userRepository.findById(userDetails.getId()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<CareerIntelligenceDto> getCareerIntelligence(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CareerIntelligenceDto intelligence = careerIntelligenceService.calculateCareerIntelligence(user);
        return ResponseEntity.ok(intelligence);
    }

    @GetMapping("/actions")
    public ResponseEntity<List<CareerActionDto>> getPrioritizedActions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<CareerActionDto> actions = careerIntelligenceService.getPrioritizedActions(user);
        return ResponseEntity.ok(actions);
    }
}
