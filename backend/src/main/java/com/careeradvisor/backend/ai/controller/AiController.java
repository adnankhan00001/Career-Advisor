package com.careeradvisor.backend.ai.controller;

import com.careeradvisor.backend.ai.context.UserAiContextService;
import com.careeradvisor.backend.ai.dto.AiChatRequest;
import com.careeradvisor.backend.ai.dto.AiChatResponse;
import com.careeradvisor.backend.ai.dto.AiHealthDto;
import com.careeradvisor.backend.ai.dto.PersonalAiContextDto;
import com.careeradvisor.backend.ai.service.AiService;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final UserAiContextService userContextService;
    private final UserRepository userRepository;

    public AiController(AiService aiService,
                        UserAiContextService userContextService,
                        UserRepository userRepository) {
        this.aiService = aiService;
        this.userContextService = userContextService;
        this.userRepository = userRepository;
    }

    /**
     * Public / Authenticated health check for AI infrastructure availability.
     */
    @GetMapping("/health")
    public ResponseEntity<AiHealthDto> getAiHealth() {
        return ResponseEntity.ok(aiService.getHealth());
    }

    /**
     * Returns the authoritative Personal AI Context for the authenticated user.
     * Requires JWT. Derives user strictly from Spring Security principal.
     */
    @GetMapping("/context")
    public ResponseEntity<PersonalAiContextDto> getPersonalAiContext(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(userContextService.buildUserContext(user));
    }

    /**
     * Executes a chat generation request grounded in the authenticated user's context.
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AiChatRequest request) {
        if (userDetails == null || userDetails.getId() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(aiService.chat(user, request));
    }
}
