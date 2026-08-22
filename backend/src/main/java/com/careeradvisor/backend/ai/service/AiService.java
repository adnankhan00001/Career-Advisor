package com.careeradvisor.backend.ai.service;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import com.careeradvisor.backend.ai.context.AiContextBuilder;
import com.careeradvisor.backend.ai.context.UserAiContextService;
import com.careeradvisor.backend.ai.dto.AiChatRequest;
import com.careeradvisor.backend.ai.dto.AiChatResponse;
import com.careeradvisor.backend.ai.dto.AiHealthDto;
import com.careeradvisor.backend.ai.dto.PersonalAiContextDto;
import com.careeradvisor.backend.ai.provider.AiPrompt;
import com.careeradvisor.backend.ai.provider.AiProvider;
import com.careeradvisor.backend.ai.provider.AiProviderFactory;
import com.careeradvisor.backend.ai.provider.AiProviderResponse;
import com.careeradvisor.backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private final AiConfigProperties properties;
    private final AiProviderFactory providerFactory;
    private final UserAiContextService userContextService;
    private final AiContextBuilder contextBuilder;
    private final AiUsageLogService usageLogService;

    public AiService(AiConfigProperties properties,
                     AiProviderFactory providerFactory,
                     UserAiContextService userContextService,
                     AiContextBuilder contextBuilder,
                     AiUsageLogService usageLogService) {
        this.properties = properties;
        this.providerFactory = providerFactory;
        this.userContextService = userContextService;
        this.contextBuilder = contextBuilder;
        this.usageLogService = usageLogService;
    }

    /**
     * Returns sanitized health & availability status of the AI infrastructure.
     */
    public AiHealthDto getHealth() {
        AiProvider activeProvider = providerFactory.getActiveProvider();
        boolean enabled = properties.isEnabled();
        boolean available = activeProvider != null && activeProvider.isAvailable();
        String providerName = activeProvider != null ? activeProvider.getProviderName() : "none";

        String message = enabled
                ? (available ? "AI service is active and available." : "AI service is enabled but provider is currently offline.")
                : "AI service is disabled in configuration.";

        return AiHealthDto.builder()
                .enabled(enabled)
                .provider(providerName)
                .available(available)
                .model(properties.getModel())
                .message(message)
                .build();
    }

    /**
     * Executes a contextual AI chat completion grounded in the authenticated user's state.
     */
    public AiChatResponse chat(User user, AiChatRequest request) {
        long startTime = System.currentTimeMillis();

        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return AiChatResponse.builder()
                    .response("Please provide a valid question or message.")
                    .status("INVALID_REQUEST")
                    .provider("none")
                    .model(properties.getModel())
                    .tokensUsed(0)
                    .latencyMs(0)
                    .conversationId(request != null ? request.getConversationId() : null)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        AiProvider provider = providerFactory.getActiveProvider();
        if (!properties.isEnabled() || provider == null) {
            return AiChatResponse.builder()
                    .response("AI assistant features are currently disabled.")
                    .status("AI_DISABLED")
                    .provider("none")
                    .model(properties.getModel())
                    .tokensUsed(0)
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .conversationId(request.getConversationId())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Build Personal System Context
        String systemPrompt = "";
        if (request.isIncludePersonalContext() && user != null) {
            PersonalAiContextDto context = userContextService.buildUserContext(user);
            systemPrompt = contextBuilder.buildSystemContextPrompt(context);
        } else {
            systemPrompt = contextBuilder.buildSystemContextPrompt(null);
        }

        AiPrompt prompt = AiPrompt.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(request.getMessage())
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .build();

        AiProviderResponse providerResponse = provider.generateCompletion(prompt);

        // Record telemetry asynchronously / transactionally
        usageLogService.logUsage(user, "CHAT_COMPLETION", providerResponse);

        return AiChatResponse.builder()
                .response(providerResponse.getContent() != null ? providerResponse.getContent() : providerResponse.getErrorMessage())
                .status(providerResponse.getStatus())
                .provider(providerResponse.getProvider())
                .model(providerResponse.getModel())
                .tokensUsed(providerResponse.getTotalTokens())
                .latencyMs(providerResponse.getLatencyMs())
                .conversationId(request.getConversationId())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
