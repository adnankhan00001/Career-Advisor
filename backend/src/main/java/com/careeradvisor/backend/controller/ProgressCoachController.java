package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.WeeklyProgressReportDto;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.AiProgressCoachService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/progress")
public class ProgressCoachController {

    private final AiProgressCoachService progressCoachService;
    private final UserRepository userRepository;

    public ProgressCoachController(AiProgressCoachService progressCoachService,
                                   UserRepository userRepository) {
        this.progressCoachService = progressCoachService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            return null;
        }
        return userRepository.findById(userDetails.getId()).orElse(null);
    }

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyProgressReportDto> getWeeklyReport(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        WeeklyProgressReportDto report = progressCoachService.getWeeklyReport(user);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/refresh")
    public ResponseEntity<WeeklyProgressReportDto> refreshWeeklyReport(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        WeeklyProgressReportDto report = progressCoachService.refreshWeeklyReport(user);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/history")
    public ResponseEntity<List<WeeklyProgressReportDto>> getReportHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<WeeklyProgressReportDto> history = progressCoachService.getReportHistory(user);
        return ResponseEntity.ok(history);
    }
}
