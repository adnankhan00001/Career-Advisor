package com.careeradvisor.backend.ai.provider;

public interface AiProvider {

    /**
     * Unique identifier name for this AI provider (e.g. "mock", "gemini", "openai").
     */
    String getProviderName();

    /**
     * Checks if this provider is configured and available for live generation.
     */
    boolean isAvailable();

    /**
     * Generates a text completion based on the given prompt.
     */
    AiProviderResponse generateCompletion(AiPrompt prompt);
}
