package com.careeradvisor.backend.ai.provider;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component("openAiCompatibleProvider")
public class OpenAiCompatibleProvider implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);
    private final AiConfigProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleProvider(AiConfigProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(5000, properties.getTimeoutSeconds() * 1000);
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().trim().isEmpty();
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
                    .errorMessage("AI is currently disabled.")
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        if (!isAvailable()) {
            return AiProviderResponse.builder()
                    .success(false)
                    .status("PROVIDER_UNAVAILABLE")
                    .provider(getProviderName())
                    .model(properties.getModel())
                    .errorMessage("AI provider API key is not configured.")
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            String url = properties.getBaseUrl() != null && !properties.getBaseUrl().trim().isEmpty()
                    ? properties.getBaseUrl()
                    : "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey().trim());

            List<Map<String, String>> messages = new ArrayList<>();
            if (prompt.getSystemPrompt() != null && !prompt.getSystemPrompt().trim().isEmpty()) {
                messages.add(Map.of("role", "system", "content", prompt.getSystemPrompt()));
            }
            if (prompt.getUserPrompt() != null && !prompt.getUserPrompt().trim().isEmpty()) {
                messages.add(Map.of("role", "user", "content", prompt.getUserPrompt()));
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", prompt.getModel() != null ? prompt.getModel() : properties.getModel());
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", prompt.getMaxTokens() != null ? prompt.getMaxTokens() : properties.getMaxTokens());
            requestBody.put("temperature", prompt.getTemperature() != null ? prompt.getTemperature() : properties.getTemperature());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            long latencyMs = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").path(0).path("message").path("content").asText("");
                int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
                int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
                int totalTokens = root.path("usage").path("total_tokens").asInt(promptTokens + completionTokens);

                return AiProviderResponse.builder()
                        .content(content)
                        .provider(getProviderName())
                        .model(properties.getModel())
                        .success(true)
                        .status("SUCCESS")
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .totalTokens(totalTokens)
                        .latencyMs(latencyMs)
                        .build();
            } else {
                return AiProviderResponse.builder()
                        .success(false)
                        .status("ERROR")
                        .provider(getProviderName())
                        .model(properties.getModel())
                        .errorMessage("Provider returned HTTP status: " + response.getStatusCode())
                        .latencyMs(latencyMs)
                        .build();
            }

        } catch (ResourceAccessException e) {
            logger.warn("AI provider network timeout/access error: {}", e.getMessage());
            return AiProviderResponse.builder()
                    .success(false)
                    .status("TIMEOUT")
                    .provider(getProviderName())
                    .model(properties.getModel())
                    .errorMessage("AI provider request timed out or connection was refused.")
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            logger.error("AI provider invocation failed: {}", e.getMessage());
            return AiProviderResponse.builder()
                    .success(false)
                    .status("ERROR")
                    .provider(getProviderName())
                    .model(properties.getModel())
                    .errorMessage("External AI provider call failed: " + e.getMessage())
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}
