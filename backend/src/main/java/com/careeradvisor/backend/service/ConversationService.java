package com.careeradvisor.backend.service;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import com.careeradvisor.backend.dto.ChatMessageDto;
import com.careeradvisor.backend.dto.ConversationParticipantDto;
import com.careeradvisor.backend.dto.ConversationResponseDto;
import com.careeradvisor.backend.dto.RealTimeEventDto;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.ChatMessageRepository;
import com.careeradvisor.backend.repository.ConversationParticipantRepository;
import com.careeradvisor.backend.repository.ConversationRepository;
import com.careeradvisor.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final RealTimeMessagingService messagingService;
    private final PresenceService presenceService;
    private final AiConfigProperties aiConfigProperties;

    public ConversationService(ConversationRepository conversationRepository,
                               ChatMessageRepository chatMessageRepository,
                               ConversationParticipantRepository participantRepository,
                               UserRepository userRepository,
                               RealTimeMessagingService messagingService,
                               PresenceService presenceService,
                               AiConfigProperties aiConfigProperties) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.messagingService = messagingService;
        this.presenceService = presenceService;
        this.aiConfigProperties = aiConfigProperties;
    }

    // ================================================================
    // AI CONVERSATION METHODS (Phase 14B - 100% PRESERVED)
    // ================================================================

    @Transactional
    public ConversationResponseDto createAiConversation(User user, String requestedTitle) {
        String title = "New AI Conversation";
        if (requestedTitle != null && !requestedTitle.trim().isEmpty()) {
            title = sanitizeTitle(requestedTitle.trim());
        }

        Conversation conversation = Conversation.builder()
                .owner(user)
                .type(ConversationType.USER_TO_AI)
                .title(title)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build();

        Conversation saved = conversationRepository.save(conversation);
        return mapToDto(saved, 0L);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponseDto> getUserConversations(User user) {
        List<Conversation> conversations = conversationRepository.findByOwnerOrderByUpdatedAtDesc(user);
        return conversations.stream()
                .filter(c -> c.getType() == ConversationType.USER_TO_AI)
                .map(c -> {
                    long count = chatMessageRepository.countByConversation(c);
                    return mapToDto(c, count);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationResponseDto getConversationForUser(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findByIdAndOwner(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        List<ChatMessage> messages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        List<ChatMessageDto> messageDtos = messages.stream()
                .map(this::mapMessageToDto)
                .collect(Collectors.toList());

        ConversationResponseDto dto = mapToDto(conversation, (long) messages.size());
        dto.setMessages(messageDtos);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesForUser(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findByIdAndOwner(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        return chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation).stream()
                .map(this::mapMessageToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponseDto archiveConversation(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findByIdAndOwner(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        conversation.setArchived(true);
        conversation.setUpdatedAt(LocalDateTime.now());
        Conversation saved = conversationRepository.save(conversation);
        long count = chatMessageRepository.countByConversation(saved);
        return mapToDto(saved, count);
    }

    @Transactional
    public void deleteConversation(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findByIdAndOwner(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        participantRepository.deleteByConversation(conversation);
        chatMessageRepository.deleteByConversation(conversation);
        conversationRepository.delete(conversation);
    }

    // ================================================================
    // HUMAN CONVERSATION METHODS (Phase 14C)
    // ================================================================

    @Transactional
    public ConversationResponseDto createUserToUserConversation(User creator, Long targetUserId, String initialMessage) {
        if (targetUserId == null) {
            throw new IllegalArgumentException("targetUserId is required");
        }
        if (creator.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot start conversation with yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        // Check if an active conversation already exists between creator and targetUser
        List<ConversationParticipant> creatorParts = participantRepository.findByUser(creator);
        for (ConversationParticipant cp : creatorParts) {
            Conversation c = cp.getConversation();
            if (c.getType() == ConversationType.USER_TO_USER && !c.isArchived()) {
                boolean hasTarget = participantRepository.existsByConversationIdAndUserId(c.getId(), targetUserId);
                if (hasTarget) {
                    if (initialMessage != null && !initialMessage.trim().isEmpty()) {
                        sendHumanMessage(c.getId(), creator, initialMessage.trim());
                    }
                    return getConversationForHuman(c.getId(), creator);
                }
            }
        }

        String title = creator.getName() + " & " + targetUser.getName();
        Conversation conversation = Conversation.builder()
                .owner(creator)
                .type(ConversationType.USER_TO_USER)
                .title(title)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build();

        Conversation saved = conversationRepository.save(conversation);

        ConversationParticipant p1 = ConversationParticipant.builder()
                .conversation(saved)
                .user(creator)
                .role(ParticipantRole.CREATOR)
                .joinedAt(LocalDateTime.now())
                .lastReadAt(LocalDateTime.now())
                .build();

        ConversationParticipant p2 = ConversationParticipant.builder()
                .conversation(saved)
                .user(targetUser)
                .role(ParticipantRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .lastReadAt(LocalDateTime.now().minusMinutes(1))
                .build();

        participantRepository.save(p1);
        participantRepository.save(p2);

        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            sendHumanMessage(saved.getId(), creator, initialMessage.trim());
        }

        return getConversationForHuman(saved.getId(), creator);
    }

    @Transactional
    public ConversationResponseDto createUserToAdminConversation(User user, String subject, String initialMessage) {
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required");
        }

        String title = "Support: " + subject.trim();
        if (title.length() > 120) {
            title = title.substring(0, 117) + "...";
        }

        Conversation conversation = Conversation.builder()
                .owner(user)
                .type(ConversationType.USER_TO_ADMIN)
                .title(title)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .build();

        Conversation saved = conversationRepository.save(conversation);

        ConversationParticipant participant = ConversationParticipant.builder()
                .conversation(saved)
                .user(user)
                .role(ParticipantRole.CREATOR)
                .joinedAt(LocalDateTime.now())
                .lastReadAt(LocalDateTime.now())
                .build();

        participantRepository.save(participant);

        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            sendHumanMessage(saved.getId(), user, initialMessage.trim());
        }

        return getConversationForHuman(saved.getId(), user);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponseDto> getUserHumanConversations(User user) {
        List<ConversationParticipant> myParticipants = participantRepository.findByUser(user);
        
        List<ConversationResponseDto> dtos = new ArrayList<>();
        for (ConversationParticipant cp : myParticipants) {
            Conversation c = cp.getConversation();
            if (c.getType() == ConversationType.USER_TO_USER || c.getType() == ConversationType.USER_TO_ADMIN) {
                long totalCount = chatMessageRepository.countByConversation(c);
                long unreadCount = calculateUnreadCount(c, cp.getLastReadAt(), user.getId());

                ConversationResponseDto dto = mapToDto(c, totalCount);
                dto.setUnreadCount(unreadCount);

                List<ConversationParticipantDto> partDtos = participantRepository.findByConversation(c).stream()
                        .map(this::mapParticipantToDto)
                        .collect(Collectors.toList());
                dto.setParticipants(partDtos);

                dtos.add(dto);
            }
        }

        dtos.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponseDto> getAdminInbox(User adminUser) {
        if (adminUser.getRole() != Role.ADMIN) {
            throw new ResourceNotFoundException("Not found");
        }

        List<Conversation> adminConvs = conversationRepository.findByTypeOrderByUpdatedAtDesc(ConversationType.USER_TO_ADMIN);
        return adminConvs.stream()
                .map(c -> {
                    long totalCount = chatMessageRepository.countByConversation(c);
                    List<ConversationParticipantDto> partDtos = participantRepository.findByConversation(c).stream()
                            .map(this::mapParticipantToDto)
                            .collect(Collectors.toList());

                    ConversationResponseDto dto = mapToDto(c, totalCount);
                    dto.setParticipants(partDtos);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationResponseDto getConversationForHuman(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        validateHumanConversationAccess(conversation, user);

        List<ChatMessage> messages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        List<ChatMessageDto> messageDtos = messages.stream()
                .map(this::mapMessageToDto)
                .collect(Collectors.toList());

        List<ConversationParticipantDto> partDtos = participantRepository.findByConversation(conversation).stream()
                .map(this::mapParticipantToDto)
                .collect(Collectors.toList());

        ConversationResponseDto dto = mapToDto(conversation, (long) messages.size());
        dto.setMessages(messageDtos);
        dto.setParticipants(partDtos);

        Optional<ConversationParticipant> myPart = participantRepository.findByConversationAndUser(conversation, user);
        if (myPart.isPresent()) {
            dto.setUnreadCount(calculateUnreadCount(conversation, myPart.get().getLastReadAt(), user.getId()));
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesForHuman(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        validateHumanConversationAccess(conversation, user);

        return chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation).stream()
                .map(this::mapMessageToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto sendHumanMessage(Long conversationId, User sender, String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }
        String content = rawContent.trim();
        int maxLength = aiConfigProperties.getChatMaxMessageLength() > 0 ? aiConfigProperties.getChatMaxMessageLength() : 4000;
        if (content.length() > maxLength) {
            throw new IllegalArgumentException("Message length exceeds maximum allowed limit (" + maxLength + " characters)");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (conversation.isArchived()) {
            throw new IllegalArgumentException("Cannot send message to an archived conversation");
        }

        if (conversation.getType() == ConversationType.USER_TO_AI) {
            throw new IllegalArgumentException("Use AI chat endpoint for USER_TO_AI conversations");
        }

        validateHumanConversationAccess(conversation, sender);

        MessageSenderType senderType = MessageSenderType.USER;
        if (sender.getRole() == Role.ADMIN && conversation.getType() == ConversationType.USER_TO_ADMIN) {
            senderType = MessageSenderType.ADMIN;
        }

        int sequenceNumber = (int) chatMessageRepository.countByConversation(conversation) + 1;
        LocalDateTime now = LocalDateTime.now();

        ChatMessage chatMessage = ChatMessage.builder()
                .conversation(conversation)
                .senderUser(sender)
                .senderType(senderType)
                .content(content)
                .sequenceNumber(sequenceNumber)
                .status("SENT")
                .createdAt(now)
                .build();

        ChatMessage saved = chatMessageRepository.save(chatMessage);

        conversation.setUpdatedAt(now);
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);

        // Update sender lastReadAt
        Optional<ConversationParticipant> senderPartOpt = participantRepository.findByConversationAndUser(conversation, sender);
        if (senderPartOpt.isPresent()) {
            ConversationParticipant sp = senderPartOpt.get();
            sp.setLastReadAt(now);
            participantRepository.save(sp);
        } else if (sender.getRole() == Role.ADMIN && conversation.getType() == ConversationType.USER_TO_ADMIN) {
            // Register admin as participant if not already present
            ConversationParticipant adminPart = ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(sender)
                    .role(ParticipantRole.ADMIN)
                    .joinedAt(now)
                    .lastReadAt(now)
                    .build();
            participantRepository.save(adminPart);
        }

        // Broadcast RealTimeEvent
        RealTimeEventDto event = RealTimeEventDto.builder()
                .type("MESSAGE_SENT")
                .conversationId(conversation.getId())
                .messageId(saved.getId())
                .senderId(sender.getId())
                .senderName(sender.getName())
                .senderRole(senderType.name())
                .content(saved.getContent())
                .status("SENT")
                .sequenceNumber(sequenceNumber)
                .timestamp(now)
                .build();

        messagingService.broadcastToConversation(conversation.getId(), event);

        return mapMessageToDto(saved);
    }

    @Transactional
    public void markConversationAsRead(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        validateHumanConversationAccess(conversation, user);

        LocalDateTime now = LocalDateTime.now();
        Optional<ConversationParticipant> partOpt = participantRepository.findByConversationAndUser(conversation, user);
        if (partOpt.isPresent()) {
            ConversationParticipant part = partOpt.get();
            part.setLastReadAt(now);
            participantRepository.save(part);
        }

        // Mark messages in thread as READ
        List<ChatMessage> unreadMessages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        for (ChatMessage msg : unreadMessages) {
            if (msg.getSenderUser() != null && !msg.getSenderUser().getId().equals(user.getId())) {
                msg.setStatus("READ");
                chatMessageRepository.save(msg);
            }
        }

        RealTimeEventDto readEvent = RealTimeEventDto.builder()
                .type("MESSAGE_READ")
                .conversationId(conversation.getId())
                .senderId(user.getId())
                .senderName(user.getName())
                .timestamp(now)
                .build();

        messagingService.broadcastToConversation(conversation.getId(), readEvent);
    }

    public void emitTyping(Long conversationId, User user, boolean isTyping) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        validateHumanConversationAccess(conversation, user);

        String eventType = isTyping ? "TYPING_STARTED" : "TYPING_STOPPED";
        RealTimeEventDto event = RealTimeEventDto.builder()
                .type(eventType)
                .conversationId(conversation.getId())
                .senderId(user.getId())
                .senderName(user.getName())
                .timestamp(LocalDateTime.now())
                .build();

        messagingService.broadcastToConversation(conversation.getId(), event);
    }

    // ================================================================
    // HELPER & VALIDATION METHODS
    // ================================================================

    private void validateHumanConversationAccess(Conversation conversation, User user) {
        if (conversation.getType() == ConversationType.USER_TO_ADMIN) {
            if (user.getRole() == Role.ADMIN) {
                return; // Admin permitted to access support conversations
            }
            boolean isParticipant = participantRepository.existsByConversationIdAndUserId(conversation.getId(), user.getId());
            boolean isOwner = conversation.getOwner() != null && conversation.getOwner().getId().equals(user.getId());
            if (!isParticipant && !isOwner) {
                throw new ResourceNotFoundException("Conversation not found with id: " + conversation.getId());
            }
            return;
        }

        if (conversation.getType() == ConversationType.USER_TO_USER) {
            boolean isParticipant = participantRepository.existsByConversationIdAndUserId(conversation.getId(), user.getId());
            if (!isParticipant) {
                throw new ResourceNotFoundException("Conversation not found with id: " + conversation.getId());
            }
            return;
        }

        if (conversation.getType() == ConversationType.USER_TO_AI) {
            boolean isOwner = conversation.getOwner() != null && conversation.getOwner().getId().equals(user.getId());
            if (!isOwner) {
                throw new ResourceNotFoundException("Conversation not found with id: " + conversation.getId());
            }
        }
    }

    private long calculateUnreadCount(Conversation conversation, LocalDateTime lastReadAt, Long currentUserId) {
        if (lastReadAt == null) return 0;
        List<ChatMessage> messages = chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        return messages.stream()
                .filter(m -> m.getSenderUser() != null && !m.getSenderUser().getId().equals(currentUserId))
                .filter(m -> m.getCreatedAt().isAfter(lastReadAt))
                .count();
    }

    public String generateDeterministicTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.trim().isEmpty()) {
            return "New AI Conversation";
        }
        String cleaned = firstMessage.trim().replaceAll("[\\r\\n]+", " ");
        String lower = cleaned.toLowerCase();
        if (lower.startsWith("what should i learn for ")) {
            cleaned = cleaned.substring(24).trim() + " Learning";
        } else if (lower.startsWith("how to prepare for ")) {
            cleaned = cleaned.substring(19).trim() + " Prep";
        } else if (lower.startsWith("can you help me with ")) {
            cleaned = cleaned.substring(21).trim() + " Guidance";
        }

        return sanitizeTitle(cleaned);
    }

    private String sanitizeTitle(String rawTitle) {
        int max = aiConfigProperties.getChatTitleMaxLength() > 0 ? aiConfigProperties.getChatTitleMaxLength() : 80;
        String title = rawTitle.length() > max ? rawTitle.substring(0, max - 3) + "..." : rawTitle;
        if (title.length() > 1) {
            return Character.toUpperCase(title.charAt(0)) + title.substring(1);
        }
        return title;
    }

    public ConversationResponseDto mapToDto(Conversation conversation, Long messageCount) {
        return ConversationResponseDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .conversationType(conversation.getType() != null ? conversation.getType().name() : "USER_TO_AI")
                .archived(conversation.isArchived())
                .messageCount(messageCount != null ? messageCount : 0L)
                .unreadCount(0L)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .build();
    }

    public ConversationParticipantDto mapParticipantToDto(ConversationParticipant participant) {
        User u = participant.getUser();
        return ConversationParticipantDto.builder()
                .id(participant.getId())
                .userId(u != null ? u.getId() : null)
                .userName(u != null ? u.getName() : "Unknown")
                .userEmail(u != null ? u.getEmail() : null)
                .role(participant.getRole())
                .online(u != null && presenceService.isUserOnline(u.getId()))
                .joinedAt(participant.getJoinedAt())
                .lastReadAt(participant.getLastReadAt())
                .build();
    }

    public ChatMessageDto mapMessageToDto(ChatMessage message) {
        String senderName = "User";
        if (message.getSenderType() != null) {
            switch (message.getSenderType()) {
                case AI:
                    senderName = "OneStop AI Advisor";
                    break;
                case ADMIN:
                    senderName = message.getSenderUser() != null ? message.getSenderUser().getName() + " (Admin)" : "Support Admin";
                    break;
                case SYSTEM:
                    senderName = "System";
                    break;
                default:
                    senderName = message.getSenderUser() != null ? message.getSenderUser().getName() : "User";
                    break;
            }
        }

        return ChatMessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversation() != null ? message.getConversation().getId() : null)
                .senderType(message.getSenderType() != null ? message.getSenderType().name() : "USER")
                .senderName(senderName)
                .content(message.getContent())
                .sequenceNumber(message.getSequenceNumber())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
