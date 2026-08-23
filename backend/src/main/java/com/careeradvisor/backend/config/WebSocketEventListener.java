package com.careeradvisor.backend.config;

import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.PresenceService;
import com.careeradvisor.backend.service.RealTimeMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceService presenceService;
    private final RealTimeMessagingService messagingService;

    public WebSocketEventListener(PresenceService presenceService, RealTimeMessagingService messagingService) {
        this.presenceService = presenceService;
        this.messagingService = messagingService;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (event.getUser() instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) event.getUser();
            if (auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                Long userId = userDetails.getId();
                boolean transitionedToOnline = presenceService.registerSession(userId, sessionId);
                if (transitionedToOnline) {
                    messagingService.broadcastPresence(userId, "USER_ONLINE");
                }
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long userId = presenceService.getUserIdBySession(sessionId);

        if (userId != null) {
            boolean transitionedToOffline = presenceService.unregisterSession(sessionId);
            if (transitionedToOffline) {
                messagingService.broadcastPresence(userId, "USER_OFFLINE");
            }
        }
    }
}
