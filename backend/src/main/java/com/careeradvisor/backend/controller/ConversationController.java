package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.ChatService;
import com.careeradvisor.backend.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatService chatService;
    private final UserRepository userRepository;

    public ConversationController(ConversationService conversationService,
                                  ChatService chatService,
                                  UserRepository userRepository) {
        this.conversationService = conversationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            return null;
        }
        return userRepository.findById(userDetails.getId()).orElse(null);
    }

    // ================================================================
    // AI CONVERSATION ENDPOINTS (Phase 14B - 100% PRESERVED)
    // ================================================================

    /**
     * Creates a new persistent AI conversation for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<ConversationResponseDto> createConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) CreateConversationRequest request) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String title = request != null ? request.getTitle() : null;
        ConversationResponseDto created = conversationService.createAiConversation(user, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Lists all AI conversations belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ConversationResponseDto>> listConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(conversationService.getUserConversations(user));
    }

    /**
     * Retrieves full conversation metadata and messages for the specified conversation.
     * Enforces strict user ownership (HTTP 404 on cross-user access).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponseDto> getConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(conversationService.getConversationForUser(id, user));
    }

    /**
     * Retrieves chronological message history for the specified conversation.
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(conversationService.getMessagesForUser(id, user));
    }

    /**
     * Sends a new message in the conversation and generates a context-aware AI response.
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<ChatResponseDto> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @Valid @RequestBody SendMessageRequest request) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ChatResponseDto response = chatService.processChatMessage(id, user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Archives the specified conversation.
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<ConversationResponseDto> archiveConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(conversationService.archiveConversation(id, user));
    }

    /**
     * Deletes the specified conversation and all its messages.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        conversationService.deleteConversation(id, user);
        return ResponseEntity.ok(Map.of("message", "Conversation deleted successfully"));
    }

    // ================================================================
    // HUMAN CONVERSATION ENDPOINTS (Phase 14C)
    // ================================================================

    /**
     * Starts a new USER_TO_USER human conversation between authenticated user and target user.
     */
    @PostMapping("/user")
    public ResponseEntity<ConversationResponseDto> startUserConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StartUserConversationRequest request) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ConversationResponseDto conv = conversationService.createUserToUserConversation(user, request.getTargetUserId(), request.getInitialMessage());
        return ResponseEntity.status(HttpStatus.CREATED).body(conv);
    }

    /**
     * Starts a new USER_TO_ADMIN human support ticket conversation.
     */
    @PostMapping("/admin")
    public ResponseEntity<ConversationResponseDto> startAdminConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StartAdminConversationRequest request) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ConversationResponseDto conv = conversationService.createUserToAdminConversation(user, request.getSubject(), request.getInitialMessage());
        return ResponseEntity.status(HttpStatus.CREATED).body(conv);
    }

    /**
     * Lists all human conversations (USER_TO_USER and USER_TO_ADMIN) where user is a participant.
     */
    @GetMapping("/human")
    public ResponseEntity<List<ConversationResponseDto>> listHumanConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(conversationService.getUserHumanConversations(user));
    }

    /**
     * Retrieves specific human conversation details.
     */
    @GetMapping("/human/{id}")
    public ResponseEntity<ConversationResponseDto> getHumanConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(conversationService.getConversationForHuman(id, user));
    }

    /**
     * Sends a message in a human conversation (USER_TO_USER or USER_TO_ADMIN).
     */
    @PostMapping("/{id}/human-messages")
    public ResponseEntity<ChatMessageDto> sendHumanMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @Valid @RequestBody HumanMessageRequest request) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ChatMessageDto msg = conversationService.sendHumanMessage(id, user, request.getContent());
        return ResponseEntity.ok(msg);
    }

    /**
     * Marks a conversation as read for the authenticated user.
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        conversationService.markConversationAsRead(id, user);
        return ResponseEntity.ok(Map.of("message", "Conversation marked as read"));
    }

    /**
     * Emits typing indicator to conversation participants.
     */
    @PostMapping("/{id}/typing")
    public ResponseEntity<Map<String, String>> emitTyping(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestBody TypingEventRequest request) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        conversationService.emitTyping(id, user, request != null && request.isTyping());
        return ResponseEntity.ok(Map.of("message", "Typing event emitted"));
    }

    /**
     * Admin Support Inbox: returns all USER_TO_ADMIN support conversations.
     */
    @GetMapping("/admin/inbox")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ConversationResponseDto>> getAdminInbox(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(conversationService.getAdminInbox(user));
    }
}
