package com.careeradvisor.backend.ai.service;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import com.careeradvisor.backend.ai.dto.AiActionPlanDto;
import com.careeradvisor.backend.ai.dto.AiActionPlanItemDto;
import com.careeradvisor.backend.ai.prompt.CareerIntelligenceAiPromptBuilder;
import com.careeradvisor.backend.ai.provider.AiPrompt;
import com.careeradvisor.backend.ai.provider.AiProvider;
import com.careeradvisor.backend.ai.provider.AiProviderFactory;
import com.careeradvisor.backend.ai.provider.AiProviderResponse;
import com.careeradvisor.backend.dto.CareerActionDto;
import com.careeradvisor.backend.dto.CareerIntelligenceDto;
import com.careeradvisor.backend.dto.GapPriority;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.service.CareerIntelligenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AiActionPlanService {

    private static final Logger logger = LoggerFactory.getLogger(AiActionPlanService.class);
    private static final long CACHE_TTL_MINUTES = 15;

    private final CareerIntelligenceService careerIntelligenceService;
    private final CareerIntelligenceAiPromptBuilder promptBuilder;
    private final AiProviderFactory providerFactory;
    private final AiConfigProperties aiConfigProperties;
    private final AiUsageLogService usageLogService;
    private final ObjectMapper objectMapper;

    // Lightweight in-memory plan cache (User ID -> Cached Plan)
    private final Map<Long, CachedPlan> planCache = new ConcurrentHashMap<>();

    public AiActionPlanService(CareerIntelligenceService careerIntelligenceService,
                               CareerIntelligenceAiPromptBuilder promptBuilder,
                               AiProviderFactory providerFactory,
                               AiConfigProperties aiConfigProperties,
                               AiUsageLogService usageLogService,
                               ObjectMapper objectMapper) {
        this.careerIntelligenceService = careerIntelligenceService;
        this.promptBuilder = promptBuilder;
        this.providerFactory = providerFactory;
        this.aiConfigProperties = aiConfigProperties;
        this.usageLogService = usageLogService;
        this.objectMapper = objectMapper;
    }

    public AiActionPlanDto getActionPlan(User user, boolean forceRefresh) {
        if (user == null) {
            return buildAnonymousPlan();
        }

        if (!forceRefresh) {
            CachedPlan cached = planCache.get(user.getId());
            if (cached != null && Duration.between(cached.timestamp, Instant.now()).toMinutes() < CACHE_TTL_MINUTES) {
                return cached.plan;
            }
        }

        AiActionPlanDto generated = generateActionPlan(user);
        planCache.put(user.getId(), new CachedPlan(generated, Instant.now()));
        return generated;
    }

    private AiActionPlanDto generateActionPlan(User user) {
        long startTime = System.currentTimeMillis();
        CareerIntelligenceDto intel = careerIntelligenceService.calculateCareerIntelligence(user);

        if (!aiConfigProperties.isEnabled()) {
            logger.info("AI subsystem disabled. Returning deterministic fallback action plan for user {}", user.getId());
            return buildDeterministicFallbackPlan(user, intel, "AI subsystem is currently disabled in system configuration.", System.currentTimeMillis() - startTime);
        }

        AiProvider provider = providerFactory.getActiveProvider();
        if (provider == null || !provider.isAvailable()) {
            logger.warn("Active AI provider unavailable. Returning deterministic fallback for user {}", user.getId());
            return buildDeterministicFallbackPlan(user, intel, "AI provider is currently unavailable.", System.currentTimeMillis() - startTime);
        }

        AiPrompt prompt = promptBuilder.buildActionPlanPrompt(intel);

        try {
            AiProviderResponse response = provider.generateCompletion(prompt);
            long latencyMs = System.currentTimeMillis() - startTime;

            usageLogService.logUsage(user, "ACTION_PLAN", response);

            if (!response.isSuccess() || response.getContent() == null || response.getContent().trim().isEmpty()) {
                logger.warn("AI generation failed for user {}: {}. Falling back.", user.getId(), response.getErrorMessage());
                return buildDeterministicFallbackPlan(user, intel, "AI generation encountered an upstream provider error.", latencyMs);
            }

            AiActionPlanDto parsedPlan = parseAndValidatePlan(user, intel, response.getContent(), provider.getProviderName(), aiConfigProperties.getModel(), latencyMs);
            return parsedPlan;

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            logger.error("Exception during AI Action Plan generation for user {}: {}. Falling back to deterministic plan.", user.getId(), e.getMessage());
            AiProviderResponse errResp = AiProviderResponse.builder()
                    .provider(provider.getProviderName())
                    .model(aiConfigProperties.getModel())
                    .success(false)
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .latencyMs(latencyMs)
                    .build();
            usageLogService.logUsage(user, "ACTION_PLAN", errResp);
            return buildDeterministicFallbackPlan(user, intel, "Parsing or generation exception encountered.", latencyMs);
        }
    }

    private AiActionPlanDto parseAndValidatePlan(User user, CareerIntelligenceDto intel, String content, String providerName, String modelName, long latencyMs) {
        try {
            String jsonContent = sanitizeJson(content);
            JsonNode root = objectMapper.readTree(jsonContent);

            String title = root.hasNonNull("title") ? root.get("title").asText() : "Personalized " + intel.getTargetCareer() + " Action Plan";
            String summary = root.hasNonNull("summary") ? root.get("summary").asText() : intel.getSummary();
            String timeframe = root.hasNonNull("estimatedTimeframe") ? root.get("estimatedTimeframe").asText() : "3-4 Weeks";
            String motivational = root.hasNonNull("motivationalNote") ? root.get("motivationalNote").asText() : "Stay consistent with daily practice milestones!";

            List<AiActionPlanItemDto> actionItems = new ArrayList<>();
            if (root.has("actions") && root.get("actions").isArray()) {
                for (JsonNode itemNode : root.get("actions")) {
                    try {
                        AiActionPlanItemDto item = objectMapper.treeToValue(itemNode, AiActionPlanItemDto.class);
                        if (item != null && item.getTitle() != null && item.getActionUrl() != null) {
                            actionItems.add(item);
                        }
                    } catch (Exception parseItemErr) {
                        logger.debug("Failed parsing individual action item: {}", parseItemErr.getMessage());
                    }
                }
            }

            if (actionItems.isEmpty() || actionItems.size() > 5) {
                logger.warn("AI generated invalid action count ({}) for user {}. Supplementing from deterministic engine.", actionItems.size(), user.getId());
                return buildDeterministicFallbackPlan(user, intel, "AI generated non-standard action list.", latencyMs);
            }

            GapPriority primaryPriority = intel.getSkillGaps() != null && !intel.getSkillGaps().isEmpty()
                    ? intel.getSkillGaps().get(0).getPriority() : GapPriority.MEDIUM;

            return AiActionPlanDto.builder()
                    .userId(user.getId())
                    .targetCareer(intel.getTargetCareer())
                    .title(title)
                    .summary(summary)
                    .currentState(intel.getReadinessState())
                    .currentStateDisplayName(intel.getReadinessStateDisplayName())
                    .overallReadinessScore(intel.getOverallReadinessScore())
                    .primaryPriority(primaryPriority)
                    .actions(actionItems)
                    .estimatedTimeframe(timeframe)
                    .motivationalNote(motivational)
                    .source("AI_GENERATED")
                    .provider(providerName)
                    .model(modelName)
                    .latencyMs(latencyMs)
                    .generatedAt(Instant.now())
                    .build();

        } catch (Exception parseErr) {
            logger.warn("Failed parsing JSON from AI response: {}. Falling back to deterministic plan.", parseErr.getMessage());
            return buildDeterministicFallbackPlan(user, intel, "Malformed AI JSON output.", latencyMs);
        }
    }

    private String sanitizeJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    public AiActionPlanDto buildDeterministicFallbackPlan(User user, CareerIntelligenceDto intel, String fallbackReason, long latencyMs) {
        List<CareerActionDto> deterministicActions = (intel.getPrioritizedActions() != null && !intel.getPrioritizedActions().isEmpty())
                ? intel.getPrioritizedActions()
                : careerIntelligenceService.getPrioritizedActions(user);

        List<AiActionPlanItemDto> items = deterministicActions.stream().map(act -> AiActionPlanItemDto.builder()
                .id(act.getId())
                .title(act.getTitle())
                .description(act.getDescription())
                .category(act.getCategory())
                .priority(act.getPriority())
                .whyItMatters(act.getReasoning())
                .expectedOutcome(String.format("Directly enhances %s competency and readiness score.", act.getCategory()))
                .estimatedEffort("2-4 hours")
                .actionUrl(act.getActionUrl())
                .estimatedImpactPoints(act.getEstimatedImpactPoints())
                .build()
        ).limit(5).collect(Collectors.toList());

        GapPriority primaryPriority = intel.getSkillGaps() != null && !intel.getSkillGaps().isEmpty()
                ? intel.getSkillGaps().get(0).getPriority() : GapPriority.MEDIUM;

        return AiActionPlanDto.builder()
                .userId(user.getId())
                .targetCareer(intel.getTargetCareer())
                .title("Personalized " + intel.getTargetCareer() + " Action Plan")
                .summary(intel.getSummary())
                .currentState(intel.getReadinessState())
                .currentStateDisplayName(intel.getReadinessStateDisplayName())
                .overallReadinessScore(intel.getOverallReadinessScore())
                .primaryPriority(primaryPriority)
                .actions(items)
                .estimatedTimeframe("2-4 Weeks")
                .motivationalNote("Execute your highest-impact action items consistently to unlock advanced career readiness.")
                .source("DETERMINISTIC_FALLBACK")
                .provider("DeterministicRuleEngine")
                .model("Rules-v15A")
                .latencyMs(latencyMs)
                .generatedAt(Instant.now())
                .build();
    }

    private AiActionPlanDto buildAnonymousPlan() {
        return AiActionPlanDto.builder()
                .userId(null)
                .targetCareer("Java Backend Developer")
                .title("Career Action Plan")
                .summary("Sign in to receive your personalized AI Career Action Plan.")
                .actions(new ArrayList<>())
                .source("DETERMINISTIC_FALLBACK")
                .provider("None")
                .model("None")
                .latencyMs(0)
                .generatedAt(Instant.now())
                .build();
    }

    private static class CachedPlan {
        final AiActionPlanDto plan;
        final Instant timestamp;

        CachedPlan(AiActionPlanDto plan, Instant timestamp) {
            this.plan = plan;
            this.timestamp = timestamp;
        }
    }
}
