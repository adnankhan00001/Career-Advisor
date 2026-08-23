package com.careeradvisor.backend.service;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import com.careeradvisor.backend.ai.context.AiContextBuilder;
import com.careeradvisor.backend.ai.context.UserAiContextService;
import com.careeradvisor.backend.ai.dto.PersonalAiContextDto;
import com.careeradvisor.backend.ai.provider.AiPrompt;
import com.careeradvisor.backend.ai.provider.AiProvider;
import com.careeradvisor.backend.ai.provider.AiProviderFactory;
import com.careeradvisor.backend.ai.provider.AiProviderResponse;
import com.careeradvisor.backend.ai.service.AiUsageLogService;
import com.careeradvisor.backend.dto.ChatMessageDto;
import com.careeradvisor.backend.dto.ChatResponseDto;
import com.careeradvisor.backend.dto.SendMessageRequest;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.ChatMessageRepository;
import com.careeradvisor.backend.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationService conversationService;
    private final AiConfigProperties properties;
    private final AiProviderFactory providerFactory;
    private final UserAiContextService userContextService;
    private final AiContextBuilder contextBuilder;
    private final AiUsageLogService usageLogService;

    public ChatService(ConversationRepository conversationRepository,
                       ChatMessageRepository chatMessageRepository,
                       ConversationService conversationService,
                       AiConfigProperties properties,
                       AiProviderFactory providerFactory,
                       UserAiContextService userContextService,
                       AiContextBuilder contextBuilder,
                       AiUsageLogService usageLogService) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.conversationService = conversationService;
        this.properties = properties;
        this.providerFactory = providerFactory;
        this.userContextService = userContextService;
        this.contextBuilder = contextBuilder;
        this.usageLogService = usageLogService;
    }

    @Transactional
    public ChatResponseDto processChatMessage(Long conversationId, User user, SendMessageRequest request) {
        long startTime = System.currentTimeMillis();

        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank.");
        }

        String userContent = request.getContent().trim();
        int maxMsgLen = properties.getChatMaxMessageLength() > 0 ? properties.getChatMaxMessageLength() : 4000;
        if (userContent.length() > maxMsgLen) {
            throw new IllegalArgumentException("Message exceeds maximum allowed length of " + maxMsgLen + " characters.");
        }

        Conversation conversation = conversationRepository.findByIdAndOwner(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (conversation.isArchived()) {
            throw new IllegalArgumentException("Cannot send messages to an archived conversation.");
        }

        long messageCountBefore = chatMessageRepository.countByConversation(conversation);
        int nextSequence = (int) (messageCountBefore + 1);

        // 1. Persist User Message
        ChatMessage userMessage = ChatMessage.builder()
                .conversation(conversation)
                .senderUser(user)
                .senderType(MessageSenderType.USER)
                .content(userContent)
                .sequenceNumber(nextSequence)
                .status("SENT")
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);

        // 2. Update title if this is the first message or title is still default
        if (messageCountBefore == 0 || "New AI Conversation".equalsIgnoreCase(conversation.getTitle())) {
            String newTitle = conversationService.generateDeterministicTitle(userContent);
            conversation.setTitle(newTitle);
        }

        // 3. Assemble Personal Context & Bounded History Prompt
        PersonalAiContextDto userContext = userContextService.buildUserContext(user);
        String baseSystemPrompt = contextBuilder.buildSystemContextPrompt(userContext);

        int historyLimit = properties.getChatHistoryLimit() > 0 ? properties.getChatHistoryLimit() : 20;
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessagesByConversation(
                conversation, PageRequest.of(0, historyLimit));
        // Reverse so chronologically ordered
        Collections.reverse(recentMessages);

        StringBuilder historyBuilder = new StringBuilder();
        if (!recentMessages.isEmpty()) {
            historyBuilder.append("\n=== RECENT CONVERSATION HISTORY ===\n");
            for (ChatMessage msg : recentMessages) {
                // Skip the current user message just saved
                if (msg.getId() != null && msg.getId().equals(savedUserMessage.getId())) {
                    continue;
                }
                String roleTag = msg.getSenderType() == MessageSenderType.USER ? "User" : "AI Career Advisor";
                historyBuilder.append(String.format("%s: %s\n", roleTag, msg.getContent()));
            }
            historyBuilder.append("===================================\n");
        }

        String fullSystemPrompt = baseSystemPrompt + historyBuilder.toString();

        AiPrompt prompt = AiPrompt.builder()
                .systemPrompt(fullSystemPrompt)
                .userPrompt(userContent)
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .build();

        // 4. Generate AI Completion
        AiProvider provider = providerFactory.getActiveProvider();
        AiProviderResponse providerResponse = null;

        if (properties.isEnabled() && properties.isChatEnabled() && provider != null) {
            providerResponse = provider.generateCompletion(prompt);
        } else {
            providerResponse = AiProviderResponse.builder()
                    .success(false)
                    .status("AI_DISABLED")
                    .provider("none")
                    .model(properties.getModel())
                    .errorMessage("AI Career Advisor is currently offline or disabled.")
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        String aiContent = "";
        String msgStatus = "SENT";

        if (providerResponse.isSuccess() && providerResponse.getContent() != null && !providerResponse.getContent().trim().isEmpty()) {
            aiContent = providerResponse.getContent().trim();
            msgStatus = "DELIVERED";
        } else {
            logger.warn("AI generation failed or returned empty: status={}, error={}",
                    providerResponse.getStatus(), providerResponse.getErrorMessage());
            aiContent = "I am temporarily unavailable to process your query. Please review your dashboard recommendations or try asking again shortly.";
            msgStatus = "FALLBACK";
        }

        // 5. Persist AI Response Message
        ChatMessage aiMessage = ChatMessage.builder()
                .conversation(conversation)
                .senderUser(null)
                .senderType(MessageSenderType.AI)
                .content(aiContent)
                .sequenceNumber(nextSequence + 1)
                .status(msgStatus)
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);

        // 6. Update Conversation timestamps
        LocalDateTime now = LocalDateTime.now();
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        Conversation savedConversation = conversationRepository.save(conversation);

        // 7. Record Telemetry (no private user message stored in usage log)
        usageLogService.logUsage(user, "PERSISTENT_CHAT", providerResponse);

        ChatMessageDto userMessageDto = conversationService.mapMessageToDto(savedUserMessage);
        ChatMessageDto aiMessageDto = conversationService.mapMessageToDto(savedAiMessage);

        return ChatResponseDto.builder()
                .conversationId(savedConversation.getId())
                .conversationTitle(savedConversation.getTitle())
                .userMessage(userMessageDto)
                .aiMessage(aiMessageDto)
                .status(providerResponse.getStatus())
                .provider(providerResponse.getProvider())
                .model(providerResponse.getModel())
                .tokensUsed(providerResponse.getTotalTokens())
                .latencyMs(providerResponse.getLatencyMs())
                .build();
    }
}
