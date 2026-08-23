package com.careeradvisor.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);

    // userId -> set of active WebSocket session IDs
    private final ConcurrentHashMap<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    // sessionId -> userId for fast disconnect lookups
    private final ConcurrentHashMap<String, Long> sessionToUser = new ConcurrentHashMap<>();

    /**
     * Registers a WebSocket session for a user.
     * @return true if the user just transitioned from OFFLINE to ONLINE.
     */
    public synchronized boolean registerSession(Long userId, String sessionId) {
        if (userId == null || sessionId == null) return false;

        sessionToUser.put(sessionId, userId);
        Set<String> sessions = userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        boolean wasEmpty = sessions.isEmpty();
        sessions.add(sessionId);

        if (wasEmpty) {
            logger.info("User {} connected - now ONLINE (session: {})", userId, sessionId);
            return true;
        }
        return false;
    }

    /**
     * Unregisters a WebSocket session.
     * @return true if the user just transitioned from ONLINE to OFFLINE.
     */
    public synchronized boolean unregisterSession(String sessionId) {
        if (sessionId == null) return false;

        Long userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    logger.info("User {} disconnected - now OFFLINE (last session: {})", userId, sessionId);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isUserOnline(Long userId) {
        if (userId == null) return false;
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<Long> getOnlineUserIds() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }

    public Long getUserIdBySession(String sessionId) {
        return sessionToUser.get(sessionId);
    }
}
