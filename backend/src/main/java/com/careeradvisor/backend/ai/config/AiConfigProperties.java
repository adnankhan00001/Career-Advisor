package com.careeradvisor.backend.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiConfigProperties {

    /**
     * Whether AI features and external provider calls are enabled.
     */
    private boolean enabled = true;

    /**
     * The active AI provider name (e.g. "mock", "openai", "gemini", "anthropic").
     */
    private String provider = "mock";

    /**
     * Secret API Key for the external AI provider (empty in dev/mock).
     */
    private String apiKey = "";

    /**
     * Model identifier to use (e.g. "gemini-1.5-flash", "gpt-4o-mini").
     */
    private String model = "gemini-1.5-flash";

    /**
     * Base URL for the external provider API (optional override).
     */
    private String baseUrl = "";

    /**
     * Maximum tokens for AI generation completions.
     */
    private int maxTokens = 1000;

    /**
     * Sampling temperature for model output generation (0.0 to 1.0).
     */
    private double temperature = 0.7;

    /**
     * HTTP client call timeout in seconds.
     */
    private int timeoutSeconds = 30;
}
