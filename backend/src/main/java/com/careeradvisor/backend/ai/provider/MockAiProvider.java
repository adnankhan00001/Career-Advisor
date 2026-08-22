package com.careeradvisor.backend.ai.provider;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("mockAiProvider")
public class MockAiProvider implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockAiProvider.class);
    private final AiConfigProperties properties;

    public MockAiProvider(AiConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled();
    }

    @Override
    public AiProviderResponse generateCompletion(AiPrompt prompt) {
        long startTime = System.currentTimeMillis();

        if (!properties.isEnabled()) {
            return AiProviderResponse.builder()
                    .success(false)
                    .status("AI_DISABLED")
                    .provider(getProviderName())
                    .model(properties.getModel())
                    .errorMessage("AI features are currently disabled in application configuration.")
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        String userPrompt = prompt.getUserPrompt() != null ? prompt.getUserPrompt() : "";
        String contextualResponse = generateContextualMockResponse(userPrompt, prompt);
        long latencyMs = System.currentTimeMillis() - startTime;

        int promptTokens = Math.max(10, userPrompt.length() / 4);
        int completionTokens = Math.max(20, contextualResponse.length() / 4);

        return AiProviderResponse.builder()
                .content(contextualResponse)
                .provider(getProviderName())
                .model(properties.getModel())
                .success(true)
                .status("SUCCESS")
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .latencyMs(latencyMs)
                .build();
    }

    private String generateContextualMockResponse(String userPrompt, AiPrompt prompt) {
        String lower = userPrompt.toLowerCase();
        if (lower.contains("roadmap") || lower.contains("next step") || lower.contains("learn")) {
            return "Based on your Career Advisor profile, here is your personalized guidance: focus on completing your next roadmap milestone. Strengthening your practical project portfolio alongside roadmap checkpoints will maximize your readiness.";
        } else if (lower.contains("skill") || lower.contains("resume") || lower.contains("gap")) {
            return "Based on your verified skills and resume analysis, you have strong core capabilities. Review the highlighted skill gaps in your profile to align closely with your target career track.";
        } else if (lower.contains("interview") || lower.contains("mock") || lower.contains("dsa")) {
            return "To prepare for upcoming technical rounds, practice curated DSA challenges in our Practice Hub and complete timed Mock Interview sessions to test your conceptual clarity under real constraints.";
        } else {
            return "Hello! I am your OneStop Career Advisor AI. I am grounded in your personalized career goals, verified skill portfolio, roadmap progress, and assessment telemetry. How can I assist your career preparation today?";
        }
    }
}
