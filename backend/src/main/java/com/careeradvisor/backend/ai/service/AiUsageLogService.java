package com.careeradvisor.backend.ai.service;

import com.careeradvisor.backend.ai.model.AiUsageLog;
import com.careeradvisor.backend.ai.provider.AiProviderResponse;
import com.careeradvisor.backend.ai.repository.AiUsageLogRepository;
import com.careeradvisor.backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiUsageLogService {

    private static final Logger logger = LoggerFactory.getLogger(AiUsageLogService.class);
    private final AiUsageLogRepository repository;

    public AiUsageLogService(AiUsageLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logUsage(User user, String requestType, AiProviderResponse response) {
        try {
            AiUsageLog log = AiUsageLog.builder()
                    .user(user)
                    .provider(response.getProvider() != null ? response.getProvider() : "unknown")
                    .model(response.getModel() != null ? response.getModel() : "unknown")
                    .requestType(requestType)
                    .promptTokens(response.getPromptTokens())
                    .completionTokens(response.getCompletionTokens())
                    .totalTokens(response.getTotalTokens())
                    .status(response.getStatus() != null ? response.getStatus() : (response.isSuccess() ? "SUCCESS" : "FAILED"))
                    .latencyMs(response.getLatencyMs())
                    .failureCategory(response.getErrorMessage())
                    .build();

            repository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record AI usage log: {}", e.getMessage());
        }
    }
}
