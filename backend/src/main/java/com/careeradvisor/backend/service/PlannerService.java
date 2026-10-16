package com.careeradvisor.backend.service;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import com.careeradvisor.backend.ai.prompt.AdaptivePlannerAiPromptBuilder;
import com.careeradvisor.backend.ai.provider.AiPrompt;
import com.careeradvisor.backend.ai.provider.AiProvider;
import com.careeradvisor.backend.ai.provider.AiProviderFactory;
import com.careeradvisor.backend.ai.provider.AiProviderResponse;
import com.careeradvisor.backend.ai.service.AiActionPlanService;
import com.careeradvisor.backend.ai.service.AiUsageLogService;
import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.LearningPlanRepository;
import com.careeradvisor.backend.repository.LearningTaskRepository;
import com.careeradvisor.backend.repository.PlannerPreferenceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PlannerService {

    private static final Logger logger = LoggerFactory.getLogger(PlannerService.class);

    private final LearningPlanRepository planRepository;
    private final LearningTaskRepository taskRepository;
    private final PlannerPreferenceRepository preferenceRepository;
    private final CareerIntelligenceService careerIntelligenceService;
    private final AiActionPlanService aiActionPlanService;
    private final DeterministicPlannerEngine deterministicEngine;
    private final AdaptivePlannerAiPromptBuilder promptBuilder;
    private final AiProviderFactory providerFactory;
    private final AiConfigProperties aiConfigProperties;
    private final AiUsageLogService usageLogService;
    private final ObjectMapper objectMapper;

    // Rate-limiting / debounce cache (User ID -> Timestamp)
    private final Map<Long, Long> lastGenerationTimestamps = new ConcurrentHashMap<>();

    public PlannerService(LearningPlanRepository planRepository,
                          LearningTaskRepository taskRepository,
                          PlannerPreferenceRepository preferenceRepository,
                          CareerIntelligenceService careerIntelligenceService,
                          AiActionPlanService aiActionPlanService,
                          DeterministicPlannerEngine deterministicEngine,
                          AdaptivePlannerAiPromptBuilder promptBuilder,
                          AiProviderFactory providerFactory,
                          AiConfigProperties aiConfigProperties,
                          AiUsageLogService usageLogService,
                          ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.preferenceRepository = preferenceRepository;
        this.careerIntelligenceService = careerIntelligenceService;
        this.aiActionPlanService = aiActionPlanService;
        this.deterministicEngine = deterministicEngine;
        this.promptBuilder = promptBuilder;
        this.providerFactory = providerFactory;
        this.aiConfigProperties = aiConfigProperties;
        this.usageLogService = usageLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlannerPreference getOrCreatePreferences(User user) {
        return preferenceRepository.findByUser(user).orElseGet(() -> {
            PlannerPreference defaultPref = PlannerPreference.builder()
                    .user(user)
                    .dailyStudyMinutes(60)
                    .preferredStudyWindow(StudyWindow.FLEXIBLE)
                    .preferredDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY")
                    .weeklyGoal("Strengthen " + (user.getCareerGoal() != null ? user.getCareerGoal() : "Java Backend Developer") + " readiness")
                    .build();
            return preferenceRepository.save(defaultPref);
        });
    }

    @Transactional
    public PlannerPreference updatePreferences(User user, PlannerPreferenceDto dto) {
        PlannerPreference pref = getOrCreatePreferences(user);
        if (dto.getDailyStudyMinutes() != null) {
            pref.setDailyStudyMinutes(dto.getDailyStudyMinutes());
        }
        if (dto.getPreferredStudyWindow() != null) {
            pref.setPreferredStudyWindow(dto.getPreferredStudyWindow());
        }
        if (dto.getPreferredDays() != null && !dto.getPreferredDays().isEmpty()) {
            pref.setPreferredDays(String.join(",", dto.getPreferredDays()));
        }
        if (dto.getWeeklyGoal() != null && !dto.getWeeklyGoal().trim().isEmpty()) {
            pref.setWeeklyGoal(dto.getWeeklyGoal().trim());
        }
        return preferenceRepository.save(pref);
    }

    @Transactional
    public WeeklyPlanDto getCurrentWeeklyPlan(User user) {
        Optional<LearningPlan> activeOpt = planRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, LearningPlanStatus.ACTIVE);
        LearningPlan plan;

        if (activeOpt.isPresent()) {
            plan = activeOpt.get();
            // If current plan is from a past week, archive and generate fresh
            if (plan.getWeekEndDate().isBefore(LocalDate.now().minusDays(1))) {
                plan.setStatus(LearningPlanStatus.COMPLETED);
                planRepository.save(plan);
                plan = generateNewPlan(user, false);
            }
        } else {
            plan = generateNewPlan(user, false);
        }

        return toWeeklyPlanDto(plan, user);
    }

    @Transactional
    public WeeklyPlanDto generateWeeklyPlan(User user, boolean forceAi) {
        enforceRateLimit(user.getId());
        LearningPlan plan = generateNewPlan(user, forceAi);
        return toWeeklyPlanDto(plan, user);
    }

    @Transactional
    public WeeklyPlanDto refreshActivePlan(User user) {
        enforceRateLimit(user.getId());
        Optional<LearningPlan> activeOpt = planRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, LearningPlanStatus.ACTIVE);
        
        List<LearningTask> preservedTasks = new ArrayList<>();
        if (activeOpt.isPresent()) {
            LearningPlan existing = activeOpt.get();
            // Preserve completed tasks
            preservedTasks = taskRepository.findByLearningPlanOrderByScheduledDateAscSequenceNumberAsc(existing).stream()
                    .filter(t -> t.getStatus() == LearningTaskStatus.COMPLETED)
                    .collect(Collectors.toList());
            existing.setStatus(LearningPlanStatus.ARCHIVED);
            planRepository.save(existing);
        }

        LearningPlan freshPlan = generateNewPlan(user, true);

        // Re-inject preserved completed tasks into appropriate dates
        if (!preservedTasks.isEmpty()) {
            for (LearningTask comp : preservedTasks) {
                LearningTask migrated = LearningTask.builder()
                        .learningPlan(freshPlan)
                        .title(comp.getTitle())
                        .description(comp.getDescription())
                        .category(comp.getCategory())
                        .priority(comp.getPriority())
                        .scheduledDate(comp.getScheduledDate().isBefore(freshPlan.getWeekStartDate()) ? freshPlan.getWeekStartDate() : comp.getScheduledDate())
                        .estimatedMinutes(comp.getEstimatedMinutes())
                        .whyItMatters(comp.getWhyItMatters())
                        .expectedOutcome(comp.getExpectedOutcome())
                        .actionUrl(comp.getActionUrl())
                        .status(LearningTaskStatus.COMPLETED)
                        .completedAt(comp.getCompletedAt())
                        .sequenceNumber(comp.getSequenceNumber())
                        .build();
                taskRepository.save(migrated);
            }
        }

        return toWeeklyPlanDto(freshPlan, user);
    }

    private LearningPlan generateNewPlan(User user, boolean forceAi) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        CareerIntelligenceDto intel = careerIntelligenceService.calculateCareerIntelligence(user);
        PlannerPreference pref = getOrCreatePreferences(user);

        // Archive any existing active plan
        planRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, LearningPlanStatus.ACTIVE)
                .ifPresent(p -> {
                    p.setStatus(LearningPlanStatus.ARCHIVED);
                    planRepository.save(p);
                });

        LearningPlan plan = LearningPlan.builder()
                .user(user)
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .targetCareer(intel.getTargetCareer())
                .readinessScoreAtGeneration(intel.getOverallReadinessScore())
                .status(LearningPlanStatus.ACTIVE)
                .source(LearningPlanSource.DETERMINISTIC)
                .weeklyGoal(pref.getWeeklyGoal())
                .build();

        // 1. Try AI Generation if enabled
        boolean aiSuccess = false;
        if (aiConfigProperties.isEnabled()) {
            try {
                AiProvider provider = providerFactory.getActiveProvider();
                if (provider != null && provider.isAvailable()) {
                    AiPrompt prompt = promptBuilder.buildWeeklyPlanPrompt(intel, pref, weekStart, weekEnd);
                    AiProviderResponse response = provider.generateCompletion(prompt);
                    usageLogService.logUsage(user, "WEEKLY_PLANNER", response);

                    if (response.isSuccess() && response.getContent() != null && !response.getContent().trim().isEmpty()) {
                        List<LearningTask> aiTasks = parseAiPlanTasks(plan, response.getContent(), weekStart);
                        if (!aiTasks.isEmpty()) {
                            plan.setTasks(aiTasks);
                            plan.setSource(LearningPlanSource.AI);
                            aiSuccess = true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("AI planner generation failed for user {}: {}. Falling back to deterministic planner.", user.getId(), e.getMessage());
            }
        }

        // 2. Deterministic Fallback if AI not used or failed
        if (!aiSuccess) {
            List<LearningTask> detTasks = deterministicEngine.generateWeeklyTasks(plan, intel, pref);
            plan.setTasks(detTasks);
            plan.setSource(LearningPlanSource.DETERMINISTIC);
        }

        LearningPlan savedPlan = planRepository.save(plan);
        if (savedPlan.getTasks() != null) {
            for (LearningTask t : savedPlan.getTasks()) {
                t.setLearningPlan(savedPlan);
                taskRepository.save(t);
            }
        }
        return savedPlan;
    }

    private List<LearningTask> parseAiPlanTasks(LearningPlan plan, String content, LocalDate weekStart) {
        List<LearningTask> tasks = new ArrayList<>();
        try {
            String json = sanitizeJson(content);
            JsonNode root = objectMapper.readTree(json);

            if (root.hasNonNull("weeklyGoal")) {
                plan.setWeeklyGoal(root.get("weeklyGoal").asText());
            }

            if (root.has("days") && root.get("days").isArray()) {
                for (JsonNode dayNode : root.get("days")) {
                    LocalDate date = dayNode.hasNonNull("date") ? LocalDate.parse(dayNode.get("date").asText()) : weekStart;
                    if (dayNode.has("tasks") && dayNode.get("tasks").isArray()) {
                        int seq = 1;
                        for (JsonNode taskNode : dayNode.get("tasks")) {
                            String title = taskNode.hasNonNull("title") ? taskNode.get("title").asText() : "Milestone Focus";
                            String desc = taskNode.hasNonNull("description") ? taskNode.get("description").asText() : "";
                            LearningTaskCategory cat = parseCategory(taskNode.hasNonNull("category") ? taskNode.get("category").asText() : "ROADMAP");
                            GapPriority prio = parsePriority(taskNode.hasNonNull("priority") ? taskNode.get("priority").asText() : "MEDIUM");
                            int mins = taskNode.hasNonNull("estimatedMinutes") ? Math.max(15, Math.min(180, taskNode.get("estimatedMinutes").asInt())) : 45;
                            String why = taskNode.hasNonNull("whyItMatters") ? taskNode.get("whyItMatters").asText() : "";
                            String outcome = taskNode.hasNonNull("expectedOutcome") ? taskNode.get("expectedOutcome").asText() : "";
                            String url = taskNode.hasNonNull("actionUrl") ? sanitizeUrl(taskNode.get("actionUrl").asText()) : "/roadmap";

                            tasks.add(LearningTask.builder()
                                    .learningPlan(plan)
                                    .scheduledDate(date)
                                    .title(title)
                                    .description(desc)
                                    .category(cat)
                                    .priority(prio)
                                    .estimatedMinutes(mins)
                                    .whyItMatters(why)
                                    .expectedOutcome(outcome)
                                    .actionUrl(url)
                                    .status(LearningTaskStatus.PENDING)
                                    .sequenceNumber(seq++)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed parsing AI weekly plan JSON: {}", e.getMessage());
        }
        return tasks;
    }

    @Transactional
    public LearningTaskDto startTask(User user, Long taskId) {
        LearningTask task = taskRepository.findByIdAndLearningPlan_User(taskId, user)
                .orElseThrow(() -> new RuntimeException("Task not found or access denied"));
        task.setStatus(LearningTaskStatus.IN_PROGRESS);
        return toTaskDto(taskRepository.save(task));
    }

    @Transactional
    public LearningTaskDto completeTask(User user, Long taskId) {
        LearningTask task = taskRepository.findByIdAndLearningPlan_User(taskId, user)
                .orElseThrow(() -> new RuntimeException("Task not found or access denied"));
        task.setStatus(LearningTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        return toTaskDto(taskRepository.save(task));
    }

    @Transactional
    public LearningTaskDto skipTask(User user, Long taskId) {
        LearningTask task = taskRepository.findByIdAndLearningPlan_User(taskId, user)
                .orElseThrow(() -> new RuntimeException("Task not found or access denied"));
        task.setStatus(LearningTaskStatus.SKIPPED);
        return toTaskDto(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<LearningTaskDto> getTodayTasks(User user) {
        LocalDate today = LocalDate.now();
        Optional<LearningPlan> activeOpt = planRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, LearningPlanStatus.ACTIVE);
        if (activeOpt.isEmpty()) {
            return Collections.emptyList();
        }
        return taskRepository.findByLearningPlanAndScheduledDateOrderBySequenceNumberAsc(activeOpt.get(), today)
                .stream().map(this::toTaskDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DailyScheduleDto> getWeeklySchedule(User user) {
        WeeklyPlanDto plan = getCurrentWeeklyPlan(user);
        return plan.getDays();
    }

    @Transactional(readOnly = true)
    public PlannerProgressDto getProgress(User user) {
        Optional<LearningPlan> activeOpt = planRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, LearningPlanStatus.ACTIVE);
        if (activeOpt.isEmpty()) {
            return PlannerProgressDto.builder()
                    .totalTasks(0)
                    .completedTasks(0)
                    .pendingTasks(0)
                    .skippedTasks(0)
                    .overdueTasks(0)
                    .completionPercentage(0)
                    .weeklyMinutesPlanned(0)
                    .weeklyMinutesCompleted(0)
                    .currentStreak(calculateStreak(user))
                    .build();
        }

        LearningPlan plan = activeOpt.get();
        List<LearningTask> tasks = taskRepository.findByLearningPlanOrderByScheduledDateAscSequenceNumberAsc(plan);
        LocalDate today = LocalDate.now();

        int total = tasks.size();
        int completed = (int) tasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.COMPLETED).count();
        int skipped = (int) tasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.SKIPPED).count();
        int inProgress = (int) tasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.IN_PROGRESS).count();
        int pending = (int) tasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.PENDING).count();
        int overdue = (int) tasks.stream().filter(t -> t.getScheduledDate().isBefore(today) && t.getStatus() != LearningTaskStatus.COMPLETED && t.getStatus() != LearningTaskStatus.SKIPPED).count();

        int minsPlanned = tasks.stream().mapToInt(LearningTask::getEstimatedMinutes).sum();
        int minsCompleted = tasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.COMPLETED).mapToInt(LearningTask::getEstimatedMinutes).sum();
        int pct = total > 0 ? (int) Math.round(((double) completed / total) * 100) : 0;

        return PlannerProgressDto.builder()
                .activePlanId(plan.getId())
                .weekStartDate(plan.getWeekStartDate())
                .weekEndDate(plan.getWeekEndDate())
                .totalTasks(total)
                .completedTasks(completed)
                .pendingTasks(pending + inProgress)
                .skippedTasks(skipped)
                .overdueTasks(overdue)
                .completionPercentage(pct)
                .weeklyMinutesPlanned(minsPlanned)
                .weeklyMinutesCompleted(minsCompleted)
                .currentStreak(calculateStreak(user))
                .build();
    }

    @Transactional(readOnly = true)
    public PlannerAdminStatsDto getAdminStats() {
        long active = planRepository.countByStatus(LearningPlanStatus.ACTIVE);
        long total = planRepository.count();

        List<LearningTask> allTasks = taskRepository.findAll();
        int totalMinutesPlanned = 0;
        int totalMinutesCompleted = 0;
        int totalTasks = allTasks.size();
        int totalCompletedTasks = 0;
        Map<String, Long> tasksByCategory = new HashMap<>();

        for (LearningTask t : allTasks) {
            totalMinutesPlanned += t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 0;
            if (t.getStatus() == LearningTaskStatus.COMPLETED) {
                totalCompletedTasks++;
                totalMinutesCompleted += t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 0;
            }
            String cat = t.getCategory() != null ? t.getCategory().name() : "ROADMAP";
            tasksByCategory.put(cat, tasksByCategory.getOrDefault(cat, 0L) + 1);
        }

        double avgCompletion = totalTasks > 0 ? ((double) totalCompletedTasks / totalTasks) * 100.0 : 0.0;
        double avgPlannedMins = total > 0 ? ((double) totalMinutesPlanned / total) : 0.0;
        double avgCompletedMins = total > 0 ? ((double) totalMinutesCompleted / total) : 0.0;

        return PlannerAdminStatsDto.builder()
                .activePlanners(active)
                .totalPlanners(total)
                .avgCompletionPercentage(Math.round(avgCompletion * 10.0) / 10.0)
                .avgPlannedMinutes(Math.round(avgPlannedMins * 10.0) / 10.0)
                .avgCompletedMinutes(Math.round(avgCompletedMins * 10.0) / 10.0)
                .tasksByCategory(tasksByCategory)
                .overallAdherencePercentage(Math.round(avgCompletion * 10.0) / 10.0)
                .build();
    }

    private int calculateStreak(User user) {
        long count = taskRepository.countByLearningPlan_UserAndStatus(user, LearningTaskStatus.COMPLETED);
        return (int) Math.min(30, count);
    }

    private WeeklyPlanDto toWeeklyPlanDto(LearningPlan plan, User user) {
        PlannerPreference pref = getOrCreatePreferences(user);
        Set<DayOfWeek> activeDays = parsePreferredDays(pref.getPreferredDays());

        List<LearningTask> allTasks = taskRepository.findByLearningPlanOrderByScheduledDateAscSequenceNumberAsc(plan);
        Map<LocalDate, List<LearningTask>> tasksByDate = allTasks.stream()
                .collect(Collectors.groupingBy(LearningTask::getScheduledDate));

        List<DailyScheduleDto> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = plan.getWeekStartDate().plusDays(i);
            List<LearningTask> dayTasks = tasksByDate.getOrDefault(d, Collections.emptyList());
            int plannedMins = dayTasks.stream().mapToInt(LearningTask::getEstimatedMinutes).sum();
            int compMins = dayTasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.COMPLETED).mapToInt(LearningTask::getEstimatedMinutes).sum();
            int totalT = dayTasks.size();
            int compT = (int) dayTasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.COMPLETED).count();
            int pct = totalT > 0 ? (int) Math.round(((double) compT / totalT) * 100) : 0;

            days.add(DailyScheduleDto.builder()
                    .date(d)
                    .dayOfWeek(d.getDayOfWeek().name())
                    .totalMinutesPlanned(plannedMins)
                    .totalMinutesCompleted(compMins)
                    .completionPercentage(pct)
                    .isPreferredDay(activeDays.contains(d.getDayOfWeek()))
                    .tasks(dayTasks.stream().map(this::toTaskDto).collect(Collectors.toList()))
                    .build());
        }

        int totalTasks = allTasks.size();
        int completedTasks = (int) allTasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.COMPLETED).count();
        int totalMinutesPlanned = allTasks.stream().mapToInt(LearningTask::getEstimatedMinutes).sum();
        int totalMinutesCompleted = allTasks.stream().filter(t -> t.getStatus() == LearningTaskStatus.COMPLETED).mapToInt(LearningTask::getEstimatedMinutes).sum();
        int completionPct = totalTasks > 0 ? (int) Math.round(((double) completedTasks / totalTasks) * 100) : 0;

        return WeeklyPlanDto.builder()
                .id(plan.getId())
                .userId(user.getId())
                .targetCareer(plan.getTargetCareer())
                .readinessScoreAtGeneration(plan.getReadinessScoreAtGeneration())
                .status(plan.getStatus())
                .source(plan.getSource())
                .weeklyGoal(plan.getWeeklyGoal())
                .weekStartDate(plan.getWeekStartDate())
                .weekEndDate(plan.getWeekEndDate())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .completionPercentage(completionPct)
                .totalMinutesPlanned(totalMinutesPlanned)
                .totalMinutesCompleted(totalMinutesCompleted)
                .currentStreak(calculateStreak(user))
                .days(days)
                .generatedAt(plan.getCreatedAt())
                .build();
    }

    private LearningTaskDto toTaskDto(LearningTask task) {
        return LearningTaskDto.builder()
                .id(task.getId())
                .planId(task.getLearningPlan() != null ? task.getLearningPlan().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .category(task.getCategory())
                .priority(task.getPriority())
                .scheduledDate(task.getScheduledDate())
                .estimatedMinutes(task.getEstimatedMinutes())
                .whyItMatters(task.getWhyItMatters())
                .expectedOutcome(task.getExpectedOutcome())
                .actionUrl(task.getActionUrl())
                .status(task.getStatus())
                .sequenceNumber(task.getSequenceNumber())
                .completedAt(task.getCompletedAt())
                .build();
    }

    private void enforceRateLimit(Long userId) {
        long now = System.currentTimeMillis();
        Long last = lastGenerationTimestamps.get(userId);
        if (last != null && (now - last) < 2000) { // 2 second anti-hammer debounce
            throw new RuntimeException("Please wait a moment before generating another plan.");
        }
        lastGenerationTimestamps.put(userId, now);
    }

    private Set<DayOfWeek> parsePreferredDays(String daysStr) {
        Set<DayOfWeek> set = new HashSet<>();
        if (daysStr != null) {
            for (String part : daysStr.split(",")) {
                try {
                    set.add(DayOfWeek.valueOf(part.trim().toUpperCase()));
                } catch (Exception ignored) {}
            }
        }
        return set;
    }

    private LearningTaskCategory parseCategory(String cat) {
        try {
            return LearningTaskCategory.valueOf(cat.toUpperCase());
        } catch (Exception e) {
            return LearningTaskCategory.ROADMAP;
        }
    }

    private GapPriority parsePriority(String p) {
        try {
            return GapPriority.valueOf(p.toUpperCase());
        } catch (Exception e) {
            return GapPriority.MEDIUM;
        }
    }

    private String sanitizeUrl(String url) {
        if (url == null || !url.startsWith("/")) {
            return "/roadmap";
        }
        return url;
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
}
