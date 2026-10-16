package com.careeradvisor.backend.ai.prompt;

import com.careeradvisor.backend.dto.WeeklyProgressReportDto;
import com.careeradvisor.backend.model.User;
import org.springframework.stereotype.Component;

@Component
public class ProgressCoachAiPromptBuilder {

    public String buildSystemPrompt() {
        return "You are an elite, highly analytical AI Career Progress Coach for the OneStop Career Advisor platform.\n" +
               "Your role is to interpret deterministic telemetry from a candidate's actual weekly activity and provide precise, actionable coaching.\n\n" +
               "CRITICAL RULES:\n" +
               "1. NEVER invent, modify, or hallucinate numerical scores, completed counts, streaks, or test results.\n" +
               "2. Interpret the provided numbers and explain WHAT improved, WHAT is blocking progress, and WHERE the candidate should focus.\n" +
               "3. Avoid generic motivation. Give concrete tactical feedback grounded in the student's telemetry.\n" +
               "4. Output MUST BE valid, parseable JSON matching the required schema exactly.\n" +
               "5. All action URLs in nextWeekFocus MUST strictly be from the whitelisted paths: /roadmap, /practice, /mock-interview, /resume, /quiz, /skills.\n";
    }

    public String buildUserPrompt(User user, WeeklyProgressReportDto metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze this candidate's weekly career progress telemetry and generate an authoritative coaching assessment.\n\n");
        sb.append("=== CANDIDATE TELEMETRY ===\n");
        sb.append("Candidate: ").append(user.getName()).append("\n");
        sb.append("Target Career: ").append(user.getCareerGoal() != null ? user.getCareerGoal() : "Software Engineer").append("\n");
        sb.append("Overall Progress Score: ").append(metrics.getOverallProgressScore()).append("%\n");
        sb.append("Career Readiness Score: ").append(metrics.getCareerReadinessScore()).append("%\n");
        sb.append("Roadmap Completion: ").append(metrics.getRoadmapProgress()).append("% (Trend: ").append(metrics.getRoadmapTrend()).append(")\n");
        sb.append("DSA Practice: ").append(metrics.getDsaSolvedCount()).append(" solved of ").append(metrics.getDsaTotalCount()).append(" (Trend: ").append(metrics.getDsaTrend()).append(")\n");
        sb.append("Mock Interviews: ").append(metrics.getInterviewCompletedCount()).append(" completed, Average: ").append(metrics.getInterviewProgress()).append("% (Trend: ").append(metrics.getInterviewTrend()).append(")\n");
        sb.append("Weekly Planner: ").append(metrics.getCompletedTasks()).append(" completed, ").append(metrics.getSkippedTasks()).append(" skipped, ").append(metrics.getStudyMinutes()).append(" mins study time (Trend: ").append(metrics.getPlannerTrend()).append(")\n");
        sb.append("Active Streak: ").append(metrics.getStreakDays()).append(" days\n\n");

        sb.append("=== REQUIRED JSON SCHEMA ===\n");
        sb.append("Return ONLY a JSON object with this exact structure:\n");
        sb.append("{\n");
        sb.append("  \"coachAssessment\": \"2-3 sentence executive coaching assessment explaining progress and bottlenecks\",\n");
        sb.append("  \"topAchievements\": [\"Concrete achievement 1\", \"Concrete achievement 2\"],\n");
        sb.append("  \"priorityAreas\": [\"Needs attention item 1\", \"Needs attention item 2\"],\n");
        sb.append("  \"strengths\": [\"Observed strength 1\", \"Observed strength 2\"],\n");
        sb.append("  \"weaknesses\": [\"Identified weakness 1\", \"Identified weakness 2\"],\n");
        sb.append("  \"improvements\": [\"Improvement note 1\", \"Improvement note 2\"],\n");
        sb.append("  \"nextWeekFocus\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"Action title\",\n");
        sb.append("      \"category\": \"ROADMAP|DSA|MOCK_INTERVIEW|SKILL|RESUME|QUIZ\",\n");
        sb.append("      \"whyItMatters\": \"Reasoning grounded in telemetry\",\n");
        sb.append("      \"actionUrl\": \"/roadmap|/practice|/mock-interview|/resume|/quiz|/skills\",\n");
        sb.append("      \"estimatedEffort\": \"30-45 mins\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }
}
