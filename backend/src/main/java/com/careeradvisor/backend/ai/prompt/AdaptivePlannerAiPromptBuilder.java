package com.careeradvisor.backend.ai.prompt;

import com.careeradvisor.backend.ai.provider.AiPrompt;
import com.careeradvisor.backend.dto.CareerGapDto;
import com.careeradvisor.backend.dto.CareerIntelligenceDto;
import com.careeradvisor.backend.model.PlannerPreference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AdaptivePlannerAiPromptBuilder {

    private static final String SYSTEM_PROMPT =
            "You are the OneStop Career Advisor AI Adaptive Learning Planner.\n" +
            "Your role is to generate a realistic, balanced, and highly motivating 7-day learning schedule tailored to the candidate's target career, verified gaps, and daily study budget.\n\n" +
            "SAFETY & PLANNING CONSTRAINTS:\n" +
            "1. Ground all schedule recommendations in the provided candidate intelligence.\n" +
            "2. Strictly respect the candidate's daily study minutes budget and preferred study days.\n" +
            "3. Distribute tasks intelligently (e.g. heavy coding on study days, light consolidation on off-days).\n" +
            "4. Every task actionUrl MUST route to an existing platform path: /roadmap, /practice, /resume, /mock-interview, /quiz, or /skills.\n" +
            "5. Your response MUST be strictly valid JSON conforming to the schema with NO markdown code fences or conversational preamble.\n";

    public AiPrompt buildWeeklyPlanPrompt(CareerIntelligenceDto intel,
                                          PlannerPreference pref,
                                          LocalDate weekStart,
                                          LocalDate weekEnd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a 7-day Adaptive Career Learning Plan JSON for:\n");
        sb.append(String.format("Candidate: %s\n", intel.getUserName()));
        sb.append(String.format("Target Career: %s (Readiness: %d%% - %s)\n",
                intel.getTargetCareer(), intel.getOverallReadinessScore(), intel.getReadinessStateDisplayName()));
        sb.append(String.format("Daily Study Budget: %d minutes/day\n", pref.getDailyStudyMinutes()));
        sb.append(String.format("Preferred Study Days: %s\n", pref.getPreferredDays()));
        sb.append(String.format("Preferred Study Window: %s\n", pref.getPreferredStudyWindow()));
        sb.append(String.format("Weekly Focus Goal: %s\n", pref.getWeeklyGoal()));
        sb.append(String.format("Week Span: %s to %s\n\n", weekStart, weekEnd));

        if (intel.getSkillGaps() != null && !intel.getSkillGaps().isEmpty()) {
            sb.append("Top Priority Skill Gaps:\n");
            for (CareerGapDto gap : intel.getSkillGaps()) {
                sb.append(String.format("- %s [%s]: %s\n", gap.getSkill(), gap.getPriority(), String.join("; ", gap.getEvidence())));
            }
            sb.append("\n");
        }

        sb.append("Required JSON Schema:\n");
        sb.append("{\n");
        sb.append("  \"weeklyGoal\": \"string\",\n");
        sb.append("  \"days\": [\n");
        sb.append("    {\n");
        sb.append("      \"date\": \"YYYY-MM-DD\",\n");
        sb.append("      \"tasks\": [\n");
        sb.append("        {\n");
        sb.append("          \"title\": \"string\",\n");
        sb.append("          \"description\": \"string\",\n");
        sb.append("          \"category\": \"ROADMAP | DSA | SKILL | QUIZ | MOCK_INTERVIEW | RESUME | PROJECT | AI_COACHING | REVIEW\",\n");
        sb.append("          \"priority\": \"CRITICAL | HIGH | MEDIUM | LOW\",\n");
        sb.append("          \"estimatedMinutes\": 45,\n");
        sb.append("          \"whyItMatters\": \"string\",\n");
        sb.append("          \"expectedOutcome\": \"string\",\n");
        sb.append("          \"actionUrl\": \"/roadmap | /practice | /resume | /mock-interview | /quiz | /skills\"\n");
        sb.append("        }\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");

        return AiPrompt.builder()
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(sb.toString())
                .temperature(0.3)
                .maxTokens(2000)
                .build();
    }
}
