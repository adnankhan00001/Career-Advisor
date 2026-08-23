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

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSecurityInterceptor.class);

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;

    public WebSocketSecurityInterceptor(JwtUtils jwtUtils,
                                        CustomUserDetailsService userDetailsService,
                                        ConversationRepository conversationRepository,
                                        ConversationParticipantRepository participantRepository) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
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
