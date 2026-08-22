package com.careeradvisor.backend.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPrompt {

    private String systemPrompt;
    private String userPrompt;
    private String model;
    private Double temperature;
    private Integer maxTokens;

    @Builder.Default
    private Map<String, Object> contextData = new HashMap<>();
}
