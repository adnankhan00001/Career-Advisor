package com.careeradvisor.backend.ai.provider;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiProviderFactory {

    private final AiConfigProperties properties;
    private final Map<String, AiProvider> providers;
    private final MockAiProvider defaultMockProvider;

    public AiProviderFactory(AiConfigProperties properties,
                             Map<String, AiProvider> providers,
                             MockAiProvider defaultMockProvider) {
        this.properties = properties;
        this.providers = providers;
        this.defaultMockProvider = defaultMockProvider;
    }

    /**
     * Resolves the configured AI provider, automatically falling back to the safe mock provider
     * if the configured external provider is unavailable or missing credentials.
     */
    public AiProvider getActiveProvider() {
        if (!properties.isEnabled()) {
            return defaultMockProvider;
        }

        String configuredName = properties.getProvider() != null ? properties.getProvider().trim().toLowerCase() : "mock";

        if ("openai".equals(configuredName) || "gemini".equals(configuredName)) {
            AiProvider openAiProvider = providers.get("openAiCompatibleProvider");
            if (openAiProvider != null && openAiProvider.isAvailable()) {
                return openAiProvider;
            }
        }

        return defaultMockProvider;
    }
}
