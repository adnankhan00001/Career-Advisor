package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.HumanMessageRequest;
import com.careeradvisor.backend.dto.TypingEventRequest;
import com.careeradvisor.backend.dto.WebRtcSignalDto;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.CallService;
import com.careeradvisor.backend.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class RealTimeMessageController {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeMessageController.class);

    private final ConversationService conversationService;
    private final CallService callService;
    private final UserRepository userRepository;

    public RealTimeMessageController(ConversationService conversationService,
                                     CallService callService,
                                     UserRepository userRepository) {
        this.conversationService = conversationService;
        this.callService = callService;
        this.userRepository = userRepository;
    }

    private User getUserFromPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
            if (auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                return userRepository.findById(userDetails.getId()).orElse(null);
            }
        }
        return null;
    }

    @MessageMapping("/chat.send/{conversationId}")
    public void handleSendMessage(@DestinationVariable Long conversationId,
                                  @Payload HumanMessageRequest request,
                                  Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user != null && request != null && request.getContent() != null) {
            conversationService.sendHumanMessage(conversationId, user, request.getContent());
        }
    }

    @MessageMapping("/chat.typing/{conversationId}")
    public void handleTyping(@DestinationVariable Long conversationId,
                             @Payload TypingEventRequest request,
                             Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user != null && request != null) {
            conversationService.emitTyping(conversationId, user, request.isTyping());
        }
    }

    @MessageMapping("/chat.read/{conversationId}")
    public void handleRead(@DestinationVariable Long conversationId,
                           Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user != null) {
            conversationService.markConversationAsRead(conversationId, user);
        }
    }

    @MessageMapping("/call.signal/{callId}")
    public void handleCallSignal(@DestinationVariable Long callId,
                                 @Payload WebRtcSignalDto signal,
                                 Principal principal) {
        User user = getUserFromPrincipal(principal);
        if (user != null && signal != null) {
            callService.processWebRtcSignal(callId, user, signal);
        }
    }
}
