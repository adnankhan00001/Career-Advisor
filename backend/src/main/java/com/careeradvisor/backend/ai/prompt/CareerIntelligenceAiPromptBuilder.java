package com.careeradvisor.backend.ai.prompt;

import com.careeradvisor.backend.ai.provider.AiPrompt;
import com.careeradvisor.backend.dto.CareerGapDto;
import com.careeradvisor.backend.dto.CareerIntelligenceDto;
import org.springframework.stereotype.Component;

@Component
public class CareerIntelligenceAiPromptBuilder {

    private static final String SYSTEM_PROMPT =
            "You are the OneStop Career Advisor AI Career Coach.\n" +
            "Your role is to translate authoritative career intelligence into an actionable, encouraging, and structured career development plan.\n\n" +
            "SAFETY & GROUNDING CONSTRAINTS:\n" +
            "1. Base your plan exclusively on the provided candidate intelligence.\n" +
            "2. Never invent candidate accomplishments, skills, scores, or certifications.\n" +
            "3. If any area is marked unattempted or missing, state that clearly.\n" +
            "4. Never change or recalculate the supplied readiness score or dimension scores.\n" +
            "5. Generate strictly 3 to 5 highest-impact action items that directly target the candidate's verified skill gaps.\n" +
            "6. Your response MUST be valid, well-formed JSON matching the specified schema with NO markdown code fences or conversational preamble.\n";

    public AiPrompt buildActionPlanPrompt(CareerIntelligenceDto intel) {
        StringBuilder userSb = new StringBuilder();
        userSb.append("Generate a personalized AI Career Action Plan JSON for the following candidate intelligence snapshot:\n\n");
        userSb.append(String.format("Candidate Name: %s\n", intel.getUserName()));
        userSb.append(String.format("Target Career Track: %s\n", intel.getTargetCareer()));
        userSb.append(String.format("Current Readiness: %d%% (%s)\n", intel.getOverallReadinessScore(), intel.getReadinessStateDisplayName()));
        userSb.append(String.format("Strongest Dimensions: %s\n", String.join(", ", intel.getStrongestDimensions())));
        userSb.append(String.format("Weakest Dimensions / Growth Areas: %s\n", String.join(", ", intel.getWeakestDimensions())));

        if (intel.getSkillGaps() != null && !intel.getSkillGaps().isEmpty()) {
            userSb.append("Identified Skill Gaps:\n");
            for (CareerGapDto gap : intel.getSkillGaps()) {
                userSb.append(String.format("- %s [%s]: %s\n", gap.getSkill(), gap.getPriority(), String.join("; ", gap.getEvidence())));
            }
        }

        userSb.append("\nRequired JSON Schema:\n");
        userSb.append("{\n");
        userSb.append("  \"title\": \"string\",\n");
        userSb.append("  \"summary\": \"string\",\n");
        userSb.append("  \"estimatedTimeframe\": \"string\",\n");
        userSb.append("  \"motivationalNote\": \"string\",\n");
        userSb.append("  \"actions\": [\n");
        userSb.append("    {\n");
        userSb.append("      \"id\": \"string\",\n");
        userSb.append("      \"title\": \"string\",\n");
        userSb.append("      \"description\": \"string\",\n");
        userSb.append("      \"category\": \"ROADMAP | DSA | RESUME | INTERVIEW | ASSESSMENT | SKILL\",\n");
        userSb.append("      \"priority\": \"CRITICAL | HIGH | MEDIUM | LOW\",\n");
        userSb.append("      \"whyItMatters\": \"string\",\n");
        userSb.append("      \"expectedOutcome\": \"string\",\n");
        userSb.append("      \"estimatedEffort\": \"string\",\n");
        userSb.append("      \"actionUrl\": \"/roadmap | /practice | /resume | /mock-interview | /quiz | /skills\",\n");
        userSb.append("      \"estimatedImpactPoints\": 15\n");
        userSb.append("    }\n");
        userSb.append("  ]\n");
        userSb.append("}\n");

        return AiPrompt.builder()
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(userSb.toString())
                .temperature(0.3)
                .maxTokens(1200)
                .build();
    }
}
