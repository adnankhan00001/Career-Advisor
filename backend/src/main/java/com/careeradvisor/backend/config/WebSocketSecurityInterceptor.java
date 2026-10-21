package com.careeradvisor.backend.config;

import com.careeradvisor.backend.model.Conversation;
import com.careeradvisor.backend.model.ConversationType;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.repository.ConversationParticipantRepository;
import com.careeradvisor.backend.repository.ConversationRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.security.CustomUserDetailsService;
import com.careeradvisor.backend.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserStatus;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.service.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSecurityInterceptor.class);

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final PresenceService presenceService;

    @Autowired
    public WebSocketSecurityInterceptor(JwtUtils jwtUtils,
                                        CustomUserDetailsService userDetailsService,
                                        ConversationRepository conversationRepository,
                                        ConversationParticipantRepository participantRepository,
                                        UserRepository userRepository,
                                        @Autowired(required = false) PresenceService presenceService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    public WebSocketSecurityInterceptor(JwtUtils jwtUtils,
                                        CustomUserDetailsService userDetailsService,
                                        ConversationRepository conversationRepository,
                                        ConversationParticipantRepository participantRepository,
                                        UserRepository userRepository) {
        this(jwtUtils, userDetailsService, conversationRepository, participantRepository, userRepository, null);
    }

    public WebSocketSecurityInterceptor(JwtUtils jwtUtils,
                                        CustomUserDetailsService userDetailsService,
                                        ConversationRepository conversationRepository,
                                        ConversationParticipantRepository participantRepository) {
        this(jwtUtils, userDetailsService, conversationRepository, participantRepository, null, null);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        } else if (StompCommand.MESSAGE.equals(command)) {
            if (!handleOutboundMessage(accessor)) {
                return null;
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        if (token == null || !jwtUtils.validateToken(token)) {
            logger.warn("WebSocket CONNECT rejected: Invalid or missing JWT token");
            throw new MessageDeliveryException("Unauthorized: Invalid or missing JWT token");
        }

        String email = jwtUtils.extractEmail(token);
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);

        if (!userDetails.isAccountNonLocked()) {
            logger.warn("WebSocket CONNECT rejected: User {} account is suspended", email);
            throw new MessageDeliveryException("Forbidden: Your account has been suspended");
        }
        if (!userDetails.isEnabled()) {
            logger.warn("WebSocket CONNECT rejected: User {} account is deactivated", email);
            throw new MessageDeliveryException("Forbidden: Your account has been deactivated");
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        accessor.setUser(auth);
        logger.info("WebSocket CONNECT authenticated for user: {} (ID: {})", email, userDetails.getId());
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        if (accessor.getUser() == null || !(accessor.getUser() instanceof UsernamePasswordAuthenticationToken)) {
            logger.warn("WebSocket SUBSCRIBE rejected: Unauthenticated connection");
            throw new MessageDeliveryException("Unauthorized: WebSocket session is unauthenticated");
        }

        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getId();

        // Fresh check against database to ensure account was not suspended after initial CONNECT
        if (userRepository != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                logger.warn("WebSocket SUBSCRIBE rejected: User {} is suspended or deactivated", userId);
                throw new MessageDeliveryException("Forbidden: Account is suspended or deactivated");
            }
        } else if (!userDetails.isAccountNonLocked() || !userDetails.isEnabled()) {
            logger.warn("WebSocket SUBSCRIBE rejected: User {} is suspended or deactivated", userDetails.getUsername());
            throw new MessageDeliveryException("Forbidden: Account is suspended or deactivated");
        }

        boolean isAdmin = userDetails.getRole() == Role.ADMIN;

        // Authorization check for conversation topics: /topic/conversations/{id}
        if (destination.startsWith("/topic/conversations/")) {
            String convIdStr = destination.substring("/topic/conversations/".length());
            try {
                Long conversationId = Long.parseLong(convIdStr);
                Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
                if (convOpt.isEmpty()) {
                    throw new MessageDeliveryException("Conversation not found");
                }

                Conversation conv = convOpt.get();

                // If USER_TO_ADMIN: admin or owner/participant permitted
                if (conv.getType() == ConversationType.USER_TO_ADMIN) {
                    if (isAdmin) {
                        return; // Admin can monitor user-to-admin support threads
                    }
                }

                // Verify participant or owner
                boolean isParticipant = participantRepository.existsByConversationIdAndUserId(conversationId, userId);
                boolean isOwner = conv.getOwner() != null && conv.getOwner().getId().equals(userId);

                if (!isParticipant && !isOwner) {
                    logger.warn("User {} forbidden from subscribing to conversation {}", userId, conversationId);
                    throw new MessageDeliveryException("Forbidden: You are not a participant in this conversation");
                }
            } catch (NumberFormatException e) {
                throw new MessageDeliveryException("Invalid conversation destination");
            }
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null || !(accessor.getUser() instanceof UsernamePasswordAuthenticationToken)) {
            logger.warn("WebSocket SEND rejected: Unauthenticated connection");
            throw new MessageDeliveryException("Unauthorized: WebSocket session is unauthenticated");
        }

        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long userId = userDetails.getId();

        // Fresh check against database to ensure account was not suspended after initial CONNECT
        if (userRepository != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                logger.warn("WebSocket SEND rejected: User {} is suspended or deactivated", userId);
                throw new MessageDeliveryException("Forbidden: Account is suspended or deactivated");
            }
        } else if (!userDetails.isAccountNonLocked() || !userDetails.isEnabled()) {
            logger.warn("WebSocket SEND rejected: User {} is suspended or deactivated", userDetails.getUsername());
            throw new MessageDeliveryException("Forbidden: Account is suspended or deactivated");
        }

        String destination = accessor.getDestination();
        if (destination == null) return;

        // Authorization check for sending to conversation endpoints: /app/chat.send/{id}, /app/chat.typing/{id}, /app/chat.read/{id}
        if (destination.startsWith("/app/chat.send/") || destination.startsWith("/app/chat.typing/") || destination.startsWith("/app/chat.read/")) {
            String convIdStr = destination.substring(destination.lastIndexOf('/') + 1);
            try {
                Long conversationId = Long.parseLong(convIdStr);
                Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
                if (convOpt.isEmpty()) {
                    throw new MessageDeliveryException("Conversation not found");
                }
                Conversation conv = convOpt.get();

                boolean isAdmin = userDetails.getRole() == Role.ADMIN;
                if (conv.getType() == ConversationType.USER_TO_ADMIN && isAdmin) {
                    return; // Admin can reply in user-to-admin
                }

                boolean isParticipant = participantRepository.existsByConversationIdAndUserId(conversationId, userId);
                boolean isOwner = conv.getOwner() != null && conv.getOwner().getId().equals(userId);

                if (!isParticipant && !isOwner) {
                    logger.warn("User {} forbidden from sending to conversation {}", userId, conversationId);
                    throw new MessageDeliveryException("Forbidden: You are not authorized for this conversation");
                }
            } catch (NumberFormatException e) {
                throw new MessageDeliveryException("Invalid conversation destination");
            }
        }
    }

    private boolean handleOutboundMessage(StompHeaderAccessor accessor) {
        Long userId = null;
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
            if (auth.getPrincipal() instanceof CustomUserDetails) {
                userId = ((CustomUserDetails) auth.getPrincipal()).getId();
            }
        }

        if (userId == null && presenceService != null && accessor.getSessionId() != null) {
            userId = presenceService.getUserIdBySession(accessor.getSessionId());
        }

        if (userId != null && userRepository != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                logger.warn("Outbound WebSocket message dropped: recipient user {} is suspended or deactivated", userId);
                return false;
            }
        }
        return true;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Try native headers Authorization
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearer = authHeaders.get(0);
            if (bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
            return bearer;
        }

        // 2. Try native header token / auth-token
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        List<String> authTokenHeaders = accessor.getNativeHeader("auth-token");
        if (authTokenHeaders != null && !authTokenHeaders.isEmpty()) {
            return authTokenHeaders.get(0);
        }

        return null;
    }
}
