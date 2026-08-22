package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.AdminStatsOverviewDto;
import com.careeradvisor.backend.dto.AdminUserDetailDto;
import com.careeradvisor.backend.dto.AdminUserDto;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.AdminService;
import com.careeradvisor.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getAdminProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User admin = userService.getUserByEmail(userDetails.getUsername());
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", admin.getId());
        profile.put("name", admin.getName());
        profile.put("email", admin.getEmail());
        profile.put("role", admin.getRole().name());

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Admin Governance & Metrics Service");
        status.put("role", "ROLE_ADMIN");
        if (userDetails != null) {
            status.put("authenticatedUser", userDetails.getUsername());
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<AdminStatsOverviewDto> getStatsOverview() {
        AdminStatsOverviewDto stats = adminService.getStatsOverview();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers(@RequestParam(value = "search", required = false) String search) {
        List<AdminUserDto> users = adminService.getAllUsers(search);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailDto> getUserDetail(@PathVariable("id") Long id) {
        AdminUserDetailDto userDetail = adminService.getUserDetail(id);
        return ResponseEntity.ok(userDetail);
    }
}
