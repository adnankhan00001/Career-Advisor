package com.careeradvisor.backend.service;

import com.careeradvisor.backend.ai.prompt.ProgressCoachAiPromptBuilder;
import com.careeradvisor.backend.ai.provider.*;
import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.WeeklyProgressReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiProgressCoachService {

    private static final Logger logger = LoggerFactory.getLogger(AiProgressCoachService.class);

    private final WeeklyProgressReportRepository reportRepository;
    private final DeterministicProgressEngine deterministicEngine;
    private final ProgressCoachAiPromptBuilder promptBuilder;
    private final AiProviderFactory aiProviderFactory;
    private final ObjectMapper objectMapper;

    // Rate-limiting debounce per user (2 seconds)
    private final Map<Long, Instant> lastRefreshTimestamps = new ConcurrentHashMap<>();

    public AiProgressCoachService(WeeklyProgressReportRepository reportRepository,
                                  DeterministicProgressEngine deterministicEngine,
                                  ProgressCoachAiPromptBuilder promptBuilder,
                                  AiProviderFactory aiProviderFactory,
                                  ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.deterministicEngine = deterministicEngine;
        this.promptBuilder = promptBuilder;
        this.aiProviderFactory = aiProviderFactory;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WeeklyProgressReportDto getWeeklyReport(User user) {
        LocalDate currentMonday = LocalDate.now().with(DayOfWeek.MONDAY);

        // Check if report generated within the last 24h exists for current week
        Optional<WeeklyProgressReport> existingOpt = reportRepository.findFirstByUserOrderByGeneratedAtDesc(user);
        if (existingOpt.isPresent()) {
            WeeklyProgressReport existing = existingOpt.get();
            if (existing.getPeriodStart().equals(currentMonday) &&
                existing.getGeneratedAt() != null &&
                Duration.between(existing.getGeneratedAt(), Instant.now()).toHours() < 24) {
                return mapToDto(existing);
            }
        }

        return generateAndSaveReport(user, false);
    }

    @Transactional
    public WeeklyProgressReportDto refreshWeeklyReport(User user) {
        enforceRateLimit(user.getId());
        return generateAndSaveReport(user, true);
    }

    @Transactional(readOnly = true)
    public List<WeeklyProgressReportDto> getReportHistory(User user) {
        List<WeeklyProgressReport> reports = reportRepository.findByUserOrderByPeriodStartDesc(user);
        List<WeeklyProgressReportDto> dtos = new ArrayList<>();
        for (WeeklyProgressReport r : reports) {
            dtos.add(mapToDto(r));
        }
        return dtos;
    }

    @Transactional
    public WeeklyProgressReportDto generateAndSaveReport(User user, boolean forceRefresh) {
        // 1. Calculate deterministic base metrics & trends
        WeeklyProgressReportDto report = deterministicEngine.generateDeterministicReport(user);

        // 2. Try AI Interpretation
        boolean aiSuccess = false;
        try {
            AiProvider aiProvider = aiProviderFactory.getActiveProvider();
            if (aiProvider != null && aiProvider.isAvailable()) {
                String systemPrompt = promptBuilder.buildSystemPrompt();
                String userPrompt = promptBuilder.buildUserPrompt(user, report);

                AiPrompt aiPrompt = AiPrompt.builder()
                        .systemPrompt(systemPrompt)
                        .userPrompt(userPrompt)
                        .maxTokens(1200)
                        .temperature(0.3)
                        .build();

                AiProviderResponse aiRes = aiProvider.generateCompletion(aiPrompt);
                if (aiRes != null && aiRes.isSuccess() && aiRes.getContent() != null) {
                    aiSuccess = parseAiInsightsIntoReport(aiRes.getContent(), report);
                }
            }
        } catch (Exception e) {
            logger.warn("AI Progress Coach generation failed, falling back to deterministic report: {}", e.getMessage());
        }

        report.setSource(aiSuccess ? "AI" : "DETERMINISTIC");

        // 3. Persist or Update WeeklyProgressReport entity
        LocalDate currentMonday = report.getPeriodStart();
        Optional<WeeklyProgressReport> existingOpt = reportRepository.findByUserAndPeriodStart(user, currentMonday);
        WeeklyProgressReport entity = existingOpt.orElseGet(() -> WeeklyProgressReport.builder()
                .user(user)
                .periodStart(currentMonday)
                .periodEnd(report.getPeriodEnd())
                .build());

        entity.setOverallProgressScore(report.getOverallProgressScore());
        entity.setCareerReadinessScore(report.getCareerReadinessScore());
        entity.setRoadmapProgressPercent(report.getRoadmapProgress());
        entity.setDsaSolvedCount(report.getDsaSolvedCount());
        entity.setDsaTotalCount(report.getDsaTotalCount());
        entity.setInterviewAvgScore(report.getInterviewProgress());
        entity.setInterviewCompletedCount(report.getInterviewCompletedCount());
        entity.setPlannerCompletionPercent(report.getPlannerConsistency());
        entity.setPlannerCompletedTasks(report.getCompletedTasks());
        entity.setPlannerSkippedTasks(report.getSkippedTasks());
        entity.setPlannerStudyMinutes(report.getStudyMinutes());
        entity.setStreakDays(report.getStreakDays());
        entity.setRoadmapTrend(report.getRoadmapTrend());
        entity.setDsaTrend(report.getDsaTrend());
        entity.setInterviewTrend(report.getInterviewTrend());
        entity.setPlannerTrend(report.getPlannerTrend());
        entity.setOverallTrend(report.getOverallTrend());

        try {
            entity.setStrengthsJson(objectMapper.writeValueAsString(report.getStrengths()));
            entity.setWeaknessesJson(objectMapper.writeValueAsString(report.getWeaknesses()));
            entity.setImprovementsJson(objectMapper.writeValueAsString(report.getImprovements()));
            entity.setTopAchievementsJson(objectMapper.writeValueAsString(report.getTopAchievements()));
            entity.setPriorityAreasJson(objectMapper.writeValueAsString(report.getPriorityAreas()));
            entity.setNextWeekFocusJson(objectMapper.writeValueAsString(report.getNextWeekFocus()));
        } catch (Exception e) {
            logger.error("Failed to serialize progress report JSON fields: {}", e.getMessage());
        }

        entity.setCoachSummary(report.getCoachSummary());
        entity.setSource(report.getSource());

        WeeklyProgressReport saved = reportRepository.save(entity);
        report.setId(saved.getId());
        report.setGeneratedAt(saved.getGeneratedAt() != null ? saved.getGeneratedAt() : Instant.now());
        return report;
    }

    private boolean parseAiInsightsIntoReport(String jsonContent, WeeklyProgressReportDto report) {
        try {
            String sanitized = jsonContent.trim();
            if (sanitized.startsWith("```json")) {
                sanitized = sanitized.substring(7);
            }
            if (sanitized.startsWith("```")) {
                sanitized = sanitized.substring(3);
            }
            if (sanitized.endsWith("```")) {
                sanitized = sanitized.substring(0, sanitized.length() - 3);
            }
            sanitized = sanitized.trim();

            JsonNode root = objectMapper.readTree(sanitized);
            if (root.has("coachAssessment") && !root.get("coachAssessment").asText().isEmpty()) {
                report.setCoachSummary(root.get("coachAssessment").asText());
            }

            if (root.has("topAchievements") && root.get("topAchievements").isArray()) {
                List<String> list = new ArrayList<>();
                root.get("topAchievements").forEach(n -> list.add(n.asText()));
                if (!list.isEmpty()) report.setTopAchievements(list);
            }

            if (root.has("priorityAreas") && root.get("priorityAreas").isArray()) {
                List<String> list = new ArrayList<>();
                root.get("priorityAreas").forEach(n -> list.add(n.asText()));
                if (!list.isEmpty()) report.setPriorityAreas(list);
            }

            if (root.has("strengths") && root.get("strengths").isArray()) {
                List<String> list = new ArrayList<>();
                root.get("strengths").forEach(n -> list.add(n.asText()));
                if (!list.isEmpty()) report.setStrengths(list);
            }

            if (root.has("weaknesses") && root.get("weaknesses").isArray()) {
                List<String> list = new ArrayList<>();
                root.get("weaknesses").forEach(n -> list.add(n.asText()));
                if (!list.isEmpty()) report.setWeaknesses(list);
            }

            if (root.has("improvements") && root.get("improvements").isArray()) {
                List<String> list = new ArrayList<>();
                root.get("improvements").forEach(n -> list.add(n.asText()));
                if (!list.isEmpty()) report.setImprovements(list);
            }

            if (root.has("nextWeekFocus") && root.get("nextWeekFocus").isArray()) {
                List<ProgressActionDto> actionList = new ArrayList<>();
                Set<String> validUrls = Set.of("/roadmap", "/practice", "/mock-interview", "/resume", "/quiz", "/skills");
                for (JsonNode aNode : root.get("nextWeekFocus")) {
                    String title = aNode.has("title") ? aNode.get("title").asText() : "Next Focus Action";
                    String cat = aNode.has("category") ? aNode.get("category").asText() : "ROADMAP";
                    String why = aNode.has("whyItMatters") ? aNode.get("whyItMatters").asText() : "";
                    String url = aNode.has("actionUrl") ? aNode.get("actionUrl").asText() : "/roadmap";
                    if (!validUrls.contains(url)) url = "/roadmap";
                    String effort = aNode.has("estimatedEffort") ? aNode.get("estimatedEffort").asText() : "30 mins";

                    actionList.add(ProgressActionDto.builder()
                            .title(title)
                            .category(cat)
                            .whyItMatters(why)
                            .actionUrl(url)
                            .estimatedEffort(effort)
                            .build());
                }
                if (!actionList.isEmpty()) {
                    report.setNextWeekFocus(actionList);
                }
            }

            return true;
        } catch (Exception e) {
            logger.warn("Failed to parse AI Progress Coach JSON: {}", e.getMessage());
            return false;
        }
    }

    private WeeklyProgressReportDto mapToDto(WeeklyProgressReport r) {
        List<String> strengths = deserializeList(r.getStrengthsJson());
        List<String> weaknesses = deserializeList(r.getWeaknessesJson());
        List<String> improvements = deserializeList(r.getImprovementsJson());
        List<String> topAchievements = deserializeList(r.getTopAchievementsJson());
        List<String> priorityAreas = deserializeList(r.getPriorityAreasJson());
        List<ProgressActionDto> nextWeekFocus = deserializeActions(r.getNextWeekFocusJson());

        return WeeklyProgressReportDto.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .periodStart(r.getPeriodStart())
                .periodEnd(r.getPeriodEnd())
                .overallProgressScore(r.getOverallProgressScore())
                .careerReadinessScore(r.getCareerReadinessScore())
                .roadmapProgress(r.getRoadmapProgressPercent())
                .dsaProgress(r.getDsaTotalCount() > 0 ? (r.getDsaSolvedCount() * 100 / r.getDsaTotalCount()) : 0)
                .dsaSolvedCount(r.getDsaSolvedCount())
                .dsaTotalCount(r.getDsaTotalCount())
                .interviewProgress(r.getInterviewAvgScore())
                .interviewCompletedCount(r.getInterviewCompletedCount())
                .plannerConsistency(r.getPlannerCompletionPercent())
                .completedTasks(r.getPlannerCompletedTasks())
                .skippedTasks(r.getPlannerSkippedTasks())
                .studyMinutes(r.getPlannerStudyMinutes())
                .streakDays(r.getStreakDays())
                .roadmapTrend(r.getRoadmapTrend())
                .dsaTrend(r.getDsaTrend())
                .interviewTrend(r.getInterviewTrend())
                .plannerTrend(r.getPlannerTrend())
                .overallTrend(r.getOverallTrend())
                .strengths(strengths)
                .weaknesses(weaknesses)
                .improvements(improvements)
                .topAchievements(topAchievements)
                .priorityAreas(priorityAreas)
                .nextWeekFocus(nextWeekFocus)
                .coachSummary(r.getCoachSummary())
                .source(r.getSource())
                .generatedAt(r.getGeneratedAt())
                .build();
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<ProgressActionDto> deserializeActions(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ProgressActionDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void enforceRateLimit(Long userId) {
        Instant now = Instant.now();
        Instant last = lastRefreshTimestamps.get(userId);
        if (last != null && Duration.between(last, now).toMillis() < 2000) {
            throw new RuntimeException("Please wait a moment before refreshing your weekly progress report.");
        }
        lastRefreshTimestamps.put(userId, now);
    }
}
