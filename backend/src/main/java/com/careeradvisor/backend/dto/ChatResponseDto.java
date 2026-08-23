package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {
    private Long conversationId;
    private String conversationTitle;
    private ChatMessageDto userMessage;
    private ChatMessageDto aiMessage;
    private String status; // "SUCCESS", "AI_DISABLED", "PROVIDER_UNAVAILABLE", "TIMEOUT", "ERROR"
    private String provider;
    private String model;
    private int tokensUsed;
    private long latencyMs;
}
