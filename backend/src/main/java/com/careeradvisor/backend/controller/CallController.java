package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.CallRequestDto;
import com.careeradvisor.backend.dto.CallSessionDto;
import com.careeradvisor.backend.dto.WebRtcSignalDto;
import com.careeradvisor.backend.model.EndReason;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.CallService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService callService;
    private final UserRepository userRepository;

    public CallController(CallService callService, UserRepository userRepository) {
        this.callService = callService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            return null;
        }
        return userRepository.findById(userDetails.getId()).orElse(null);
    }

    @PostMapping
    public ResponseEntity<CallSessionDto> createCall(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CallRequestDto request) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        CallSessionDto session = callService.initiateCall(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CallSessionDto> getCall(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(callService.getCallSession(id, user));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<CallSessionDto> acceptCall(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(callService.acceptCall(id, user));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<CallSessionDto> rejectCall(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        EndReason reason = EndReason.REJECTED;
        if (body != null && body.containsKey("reason")) {
            try {
                reason = EndReason.valueOf(body.get("reason"));
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(callService.rejectCall(id, user, reason));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CallSessionDto> cancelCall(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(callService.cancelCall(id, user));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<CallSessionDto> endCall(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        EndReason reason = EndReason.USER_ENDED;
        if (body != null && body.containsKey("reason")) {
            try {
                reason = EndReason.valueOf(body.get("reason"));
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(callService.endCall(id, user, reason));
    }

    @PostMapping("/{id}/signal")
    public ResponseEntity<Map<String, String>> sendSignal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestBody WebRtcSignalDto signal) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        callService.processWebRtcSignal(id, user, signal);
        return ResponseEntity.ok(Map.of("status", "SIGNAL_DISPATCHED"));
    }

    @GetMapping("/history")
    public ResponseEntity<List<CallSessionDto>> getCallHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(callService.getUserCallHistory(user));
    }

    @GetMapping("/active")
    public ResponseEntity<List<CallSessionDto>> getActiveCalls(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(callService.getActiveCallsForUser(user));
    }
}
