package com.careeradvisor.backend.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProviderResponse {

    private String content;
    private String provider;
    private String model;
    private boolean success;
    private String status; // "SUCCESS", "AI_DISABLED", "PROVIDER_UNAVAILABLE", "TIMEOUT", "RATE_LIMITED", "ERROR"
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private long latencyMs;
    private String errorMessage;
}
