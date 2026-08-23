package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.RealTimeEventDto;
import com.careeradvisor.backend.dto.WebRtcSignalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RealTimeMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeMessagingService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public RealTimeMessagingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastToConversation(Long conversationId, RealTimeEventDto event) {
        if (conversationId == null || event == null) return;
        String destination = "/topic/conversations/" + conversationId;
        try {
            messagingTemplate.convertAndSend(destination, event);
            logger.debug("Dispatched STOMP event {} to {}", event.getType(), destination);
        } catch (Exception e) {
            logger.warn("Failed to dispatch STOMP event to {}: {}", destination, e.getMessage());
        }
    }

    public void broadcastPresence(Long userId, String type) {
        if (userId == null || type == null) return;
        RealTimeEventDto event = RealTimeEventDto.builder()
                .type(type)
                .senderId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        try {
            messagingTemplate.convertAndSend("/topic/presence", event);
            logger.debug("Dispatched presence event {} for user {}", type, userId);
        } catch (Exception e) {
            logger.warn("Failed to dispatch presence event: {}", e.getMessage());
        }
    }

    public void broadcastCallSignal(Long conversationId, WebRtcSignalDto signal) {
        if (conversationId == null || signal == null) return;
        String destination = "/topic/conversations/" + conversationId;
        try {
            messagingTemplate.convertAndSend(destination, signal);
            logger.debug("Dispatched WebRTC call signal {} to {}", signal.getType(), destination);
        } catch (Exception e) {
            logger.warn("Failed to dispatch WebRTC call signal to {}: {}", destination, e.getMessage());
        }
    }
}
