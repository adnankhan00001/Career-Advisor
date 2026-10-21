package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.MentorshipRequestStatus;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.AdminMentorshipService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/mentorships")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMentorshipController {

    private final AdminMentorshipService adminMentorshipService;

    public AdminMentorshipController(AdminMentorshipService adminMentorshipService) {
        this.adminMentorshipService = adminMentorshipService;
    }

    @GetMapping("/summary")
    public ResponseEntity<MentorshipOversightSummaryDto> getMentorshipSummary() {
        MentorshipOversightSummaryDto summary = adminMentorshipService.getMentorshipSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping
    public ResponseEntity<PageResponse<AdminMentorshipRequestDto>> getMentorshipRequests(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) MentorshipRequestStatus status,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        PageResponse<AdminMentorshipRequestDto> requests = adminMentorshipService.getMentorshipRequests(
                page, size, sort, search, status, fromDate, toDate);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminMentorshipDetailDto> getMentorshipDetail(@PathVariable Long id) {
        AdminMentorshipDetailDto detail = adminMentorshipService.getMentorshipDetail(id);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<AdminMentorshipDetailDto> terminateMentorship(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody TerminateMentorshipRequest request
    ) {
        Long adminUserId = userDetails != null ? userDetails.getId() : null;
        AdminMentorshipDetailDto result = adminMentorshipService.terminateMentorship(adminUserId, id, request);
        return ResponseEntity.ok(result);
    }
}
