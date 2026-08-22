package com.careeradvisor.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String response;
    private String status; // "SUCCESS", "AI_DISABLED", "PROVIDER_UNAVAILABLE", "ERROR"
    private String provider;
    private String model;
    private int tokensUsed;
    private long latencyMs;
    private String conversationId;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
