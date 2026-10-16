package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.PlannerAdminStatsDto;
import com.careeradvisor.backend.service.PlannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/planner")
public class AdminPlannerController {

    private final PlannerService plannerService;

    public AdminPlannerController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlannerAdminStatsDto> getPlannerStats() {
        return ResponseEntity.ok(plannerService.getAdminStats());
    }
}
