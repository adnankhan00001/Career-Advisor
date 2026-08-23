package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.CallRequestDto;
import com.careeradvisor.backend.dto.CallSessionDto;
import com.careeradvisor.backend.dto.WebRtcSignalDto;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.CallSessionRepository;
import com.careeradvisor.backend.repository.ConversationParticipantRepository;
import com.careeradvisor.backend.repository.ConversationRepository;
import com.careeradvisor.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CallService {

    private static final Logger logger = LoggerFactory.getLogger(CallService.class);
    private static final int RINGING_TIMEOUT_SECONDS = 45;

    private final CallSessionRepository callSessionRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final RealTimeMessagingService messagingService;
    private final PresenceService presenceService;

    public CallService(CallSessionRepository callSessionRepository,
                       ConversationRepository conversationRepository,
                       ConversationParticipantRepository participantRepository,
                       UserRepository userRepository,
                       RealTimeMessagingService messagingService,
                       PresenceService presenceService) {
        this.callSessionRepository = callSessionRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.messagingService = messagingService;
        this.presenceService = presenceService;
    }

    @Transactional
    public CallSessionDto initiateCall(User caller, CallRequestDto request) {
        if (request == null || request.getConversationId() == null || request.getCallType() == null) {
            throw new IllegalArgumentException("conversationId and callType are required");
        }

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + request.getConversationId()));

        if (conversation.getType() == ConversationType.USER_TO_AI) {
            throw new IllegalArgumentException("Calls are not supported for AI conversations");
        }

        if (conversation.isArchived()) {
            throw new IllegalArgumentException("Cannot call in an archived conversation");
        }

        User receiver = resolveReceiver(conversation, caller);
        if (receiver == null || receiver.getId().equals(caller.getId())) {
            throw new IllegalArgumentException("Cannot initiate call with yourself");
        }

        // Prevent duplicate active calls
        List<CallStatus> activeStatuses = List.of(CallStatus.RINGING, CallStatus.ACCEPTED);
        List<CallSession> callerActive = callSessionRepository.findActiveCallsForUser(caller.getId(), activeStatuses);
        List<CallSession> receiverActive = callSessionRepository.findActiveCallsForUser(receiver.getId(), activeStatuses);
        List<CallSession> convActive = callSessionRepository.findActiveCallsInConversation(conversation.getId(), activeStatuses);

        if (!callerActive.isEmpty() || !receiverActive.isEmpty() || !convActive.isEmpty()) {
            throw new IllegalStateException("CALL_ALREADY_ACTIVE");
        }

        LocalDateTime now = LocalDateTime.now();
        CallSession session = CallSession.builder()
                .conversation(conversation)
                .caller(caller)
                .receiver(receiver)
                .callType(request.getCallType())
                .status(CallStatus.RINGING)
                .startedAt(now)
                .durationSeconds(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CallSession saved = callSessionRepository.save(session);

        // Broadcast INCOMING_CALL signal over STOMP
        WebRtcSignalDto signal = WebRtcSignalDto.builder()
                .type("INCOMING_CALL")
                .callId(saved.getId())
                .conversationId(conversation.getId())
                .senderId(caller.getId())
                .senderName(caller.getName())
                .targetUserId(receiver.getId())
                .callType(saved.getCallType())
                .timestamp(now)
                .build();

        messagingService.broadcastCallSignal(conversation.getId(), signal);
        logger.info("Call initiated [ID: {}, Type: {}] by user {} to user {}", saved.getId(), saved.getCallType(), caller.getId(), receiver.getId());

        return mapToDto(saved);
    }

    @Transactional
    public CallSessionDto getCallSession(Long callId, User user) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with id: " + callId));

        validateCallParticipant(session, user);
        checkAndExpireRinging(session);

        return mapToDto(session);
    }

    @Transactional
    public CallSessionDto acceptCall(Long callId, User user) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with id: " + callId));

        validateCallParticipant(session, user);

        if (!session.getReceiver().getId().equals(user.getId()) && !(user.getRole() == Role.ADMIN && session.getConversation().getType() == ConversationType.USER_TO_ADMIN)) {
            throw new ResourceNotFoundException("Call not found with id: " + callId);
        }

        if (session.getStatus() != CallStatus.RINGING) {
            throw new IllegalStateException("Cannot accept call in status " + session.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(CallStatus.ACCEPTED);
        session.setAnsweredAt(now);
        session.setUpdatedAt(now);
        CallSession saved = callSessionRepository.save(session);

        WebRtcSignalDto signal = WebRtcSignalDto.builder()
                .type("CALL_ACCEPTED")
                .callId(saved.getId())
                .conversationId(saved.getConversation().getId())
                .senderId(user.getId())
                .senderName(user.getName())
                .targetUserId(saved.getCaller().getId())
                .callType(saved.getCallType())
                .timestamp(now)
                .build();

        messagingService.broadcastCallSignal(saved.getConversation().getId(), signal);
        logger.info("Call accepted [ID: {}] by user {}", saved.getId(), user.getId());

        return mapToDto(saved);
    }

    @Transactional
    public CallSessionDto rejectCall(Long callId, User user, EndReason reason) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with id: " + callId));

        validateCallParticipant(session, user);

        if (session.getStatus() != CallStatus.RINGING) {
            throw new IllegalStateException("Cannot reject call in status " + session.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(CallStatus.REJECTED);
        session.setEndedAt(now);
        session.setEndReason(reason != null ? reason : EndReason.REJECTED);
        session.setUpdatedAt(now);
        CallSession saved = callSessionRepository.save(session);

        WebRtcSignalDto signal = WebRtcSignalDto.builder()
                .type("CALL_REJECTED")
                .callId(saved.getId())
                .conversationId(saved.getConversation().getId())
                .senderId(user.getId())
                .senderName(user.getName())
                .targetUserId(saved.getCaller().getId())
                .callType(saved.getCallType())
                .endReason(saved.getEndReason())
                .timestamp(now)
                .build();

        messagingService.broadcastCallSignal(saved.getConversation().getId(), signal);
        logger.info("Call rejected [ID: {}] by user {}", saved.getId(), user.getId());

        return mapToDto(saved);
    }

    @Transactional
    public CallSessionDto cancelCall(Long callId, User user) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with id: " + callId));

        validateCallParticipant(session, user);

        if (!session.getCaller().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Call not found with id: " + callId);
        }

        if (session.getStatus() != CallStatus.RINGING) {
            throw new IllegalStateException("Cannot cancel call in status " + session.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(CallStatus.CANCELLED);
        session.setEndedAt(now);
        session.setEndReason(EndReason.USER_ENDED);
        session.setUpdatedAt(now);
        CallSession saved = callSessionRepository.save(session);

        WebRtcSignalDto signal = WebRtcSignalDto.builder()
                .type("CALL_CANCELLED")
                .callId(saved.getId())
                .conversationId(saved.getConversation().getId())
                .senderId(user.getId())
                .senderName(user.getName())
                .targetUserId(saved.getReceiver().getId())
                .callType(saved.getCallType())
                .endReason(saved.getEndReason())
                .timestamp(now)
                .build();

        messagingService.broadcastCallSignal(saved.getConversation().getId(), signal);
        logger.info("Call cancelled [ID: {}] by caller {}", saved.getId(), user.getId());

        return mapToDto(saved);
    }

    @Transactional
    public CallSessionDto endCall(Long callId, User user, EndReason reason) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with id: " + callId));

        validateCallParticipant(session, user);

        LocalDateTime now = LocalDateTime.now();
        if (session.getStatus() == CallStatus.RINGING) {
            if (session.getCaller().getId().equals(user.getId())) {
                return cancelCall(callId, user);
            } else {
                return rejectCall(callId, user, reason);
            }
        }

        if (session.getStatus() != CallStatus.ACCEPTED) {
            return mapToDto(session);
        }

        session.setStatus(CallStatus.ENDED);
        session.setEndedAt(now);
        session.setEndReason(reason != null ? reason : EndReason.USER_ENDED);

        if (session.getAnsweredAt() != null) {
            int duration = (int) Math.max(0, Duration.between(session.getAnsweredAt(), now).getSeconds());
            session.setDurationSeconds(duration);
        }

        session.setUpdatedAt(now);
        CallSession saved = callSessionRepository.save(session);

        Long otherUserId = session.getCaller().getId().equals(user.getId()) ? session.getReceiver().getId() : session.getCaller().getId();

        WebRtcSignalDto signal = WebRtcSignalDto.builder()
                .type("CALL_ENDED")
                .callId(saved.getId())
                .conversationId(saved.getConversation().getId())
                .senderId(user.getId())
                .senderName(user.getName())
                .targetUserId(otherUserId)
                .callType(saved.getCallType())
                .endReason(saved.getEndReason())
                .durationSeconds(saved.getDurationSeconds())
                .timestamp(now)
                .build();

        messagingService.broadcastCallSignal(saved.getConversation().getId(), signal);
        logger.info("Call ended [ID: {}] by user {} (Duration: {}s)", saved.getId(), user.getId(), saved.getDurationSeconds());

        return mapToDto(saved);
    }

    @Transactional
    public void processWebRtcSignal(Long callId, User sender, WebRtcSignalDto signal) {
        if (signal == null || signal.getType() == null) {
            throw new IllegalArgumentException("Signal type is required");
        }

        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with id: " + callId));

        validateCallParticipant(session, sender);

        if (session.getStatus() != CallStatus.RINGING && session.getStatus() != CallStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot signal on inactive call");
        }

        // Bound payload size
        if (signal.getSdp() != null && signal.getSdp().length() > 50000) {
            throw new IllegalArgumentException("SDP payload exceeds maximum allowed size");
        }

        Long targetUserId = session.getCaller().getId().equals(sender.getId()) ? session.getReceiver().getId() : session.getCaller().getId();

        signal.setCallId(session.getId());
        signal.setConversationId(session.getConversation().getId());
        signal.setSenderId(sender.getId());
        signal.setSenderName(sender.getName());
        signal.setTargetUserId(targetUserId);
        signal.setCallType(session.getCallType());
        signal.setTimestamp(LocalDateTime.now());

        messagingService.broadcastCallSignal(session.getConversation().getId(), signal);
    }

    @Transactional(readOnly = true)
    public List<CallSessionDto> getUserCallHistory(User user) {
        return callSessionRepository.findUserCallHistory(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CallSessionDto> getActiveCallsForUser(User user) {
        List<CallStatus> active = List.of(CallStatus.RINGING, CallStatus.ACCEPTED);
        List<CallSession> sessions = callSessionRepository.findActiveCallsForUser(user.getId(), active);

        sessions.forEach(this::checkAndExpireRinging);

        return sessions.stream()
                .filter(s -> s.getStatus() == CallStatus.RINGING || s.getStatus() == CallStatus.ACCEPTED)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private User resolveReceiver(Conversation conversation, User caller) {
        if (conversation.getType() == ConversationType.USER_TO_USER) {
            List<ConversationParticipant> participants = participantRepository.findByConversation(conversation);
            for (ConversationParticipant p : participants) {
                if (!p.getUser().getId().equals(caller.getId())) {
                    return p.getUser();
                }
            }
        } else if (conversation.getType() == ConversationType.USER_TO_ADMIN) {
            if (caller.getRole() == Role.ADMIN) {
                // Admin is calling the candidate
                return conversation.getOwner();
            } else {
                // Candidate is calling admin support
                List<ConversationParticipant> participants = participantRepository.findByConversation(conversation);
                for (ConversationParticipant p : participants) {
                    if (p.getRole() == ParticipantRole.ADMIN) {
                        return p.getUser();
                    }
                }
                // Fallback to any active admin
                Optional<User> adminOpt = userRepository.findByRole(Role.ADMIN).stream().findFirst();
                if (adminOpt.isPresent()) return adminOpt.get();
            }
        }
        return null;
    }

    private void validateCallParticipant(CallSession session, User user) {
        if (session.getConversation().getType() == ConversationType.USER_TO_ADMIN && user.getRole() == Role.ADMIN) {
            return;
        }

        boolean isCaller = session.getCaller().getId().equals(user.getId());
        boolean isReceiver = session.getReceiver().getId().equals(user.getId());

        if (!isCaller && !isReceiver) {
            throw new ResourceNotFoundException("Call not found with id: " + session.getId());
        }
    }

    private void checkAndExpireRinging(CallSession session) {
        if (session.getStatus() == CallStatus.RINGING && session.getStartedAt() != null) {
            long seconds = Duration.between(session.getStartedAt(), LocalDateTime.now()).getSeconds();
            if (seconds > RINGING_TIMEOUT_SECONDS) {
                session.setStatus(CallStatus.MISSED);
                session.setEndedAt(LocalDateTime.now());
                session.setEndReason(EndReason.TIMEOUT);
                session.setUpdatedAt(LocalDateTime.now());
                callSessionRepository.save(session);

                WebRtcSignalDto signal = WebRtcSignalDto.builder()
                        .type("CALL_MISSED")
                        .callId(session.getId())
                        .conversationId(session.getConversation().getId())
                        .senderId(session.getCaller().getId())
                        .senderName(session.getCaller().getName())
                        .targetUserId(session.getReceiver().getId())
                        .callType(session.getCallType())
                        .endReason(EndReason.TIMEOUT)
                        .timestamp(LocalDateTime.now())
                        .build();

                messagingService.broadcastCallSignal(session.getConversation().getId(), signal);
            }
        }
    }

    public CallSessionDto mapToDto(CallSession session) {
        return CallSessionDto.builder()
                .id(session.getId())
                .conversationId(session.getConversation() != null ? session.getConversation().getId() : null)
                .callerId(session.getCaller() != null ? session.getCaller().getId() : null)
                .callerName(session.getCaller() != null ? session.getCaller().getName() : "Unknown")
                .receiverId(session.getReceiver() != null ? session.getReceiver().getId() : null)
                .receiverName(session.getReceiver() != null ? session.getReceiver().getName() : "Unknown")
                .callType(session.getCallType())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .answeredAt(session.getAnsweredAt())
                .endedAt(session.getEndedAt())
                .durationSeconds(session.getDurationSeconds())
                .endReason(session.getEndReason())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
