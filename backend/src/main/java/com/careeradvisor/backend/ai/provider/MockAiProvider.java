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
        String contextualResponse;
        if (prompt.getUserPrompt() != null && (prompt.getUserPrompt().contains("weekly career progress telemetry") || prompt.getUserPrompt().contains("CANDIDATE TELEMETRY"))) {
            contextualResponse = generateMockProgressCoachJson(prompt.getUserPrompt());
        } else if (prompt.getUserPrompt() != null && prompt.getUserPrompt().contains("Adaptive Career Learning Plan JSON")) {
            contextualResponse = generateMockWeeklyPlanJson(prompt.getUserPrompt());
        } else if (prompt.getUserPrompt() != null && (prompt.getUserPrompt().contains("AI Career Action Plan JSON") || prompt.getUserPrompt().contains("Required JSON Schema"))) {
            contextualResponse = generateMockActionPlanJson(prompt.getUserPrompt());
        } else {
            contextualResponse = generateContextualMockResponse(userPrompt, prompt);
        }
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

    
        private String generateMockWeeklyPlanJson(String prompt) {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate start = now.with(java.time.DayOfWeek.MONDAY);
        return "{\n" +
                "  \"weeklyGoal\": \"Strengthen Core Backend Architecture & Interview Readiness\",\n" +
                "  \"days\": [\n" +
                "    {\n" +
                "      \"date\": \"" + start.toString() + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\n" +
                "          \"title\": \"Complete Java OOP & Polymorphism Milestone\",\n" +
                "          \"description\": \"Study method overriding, abstract classes, and interface contracts.\",\n" +
                "          \"category\": \"ROADMAP\",\n" +
                "          \"priority\": \"CRITICAL\",\n" +
                "          \"estimatedMinutes\": 45,\n" +
                "          \"whyItMatters\": \"Foundation for enterprise Java application architecture.\",\n" +
                "          \"expectedOutcome\": \"Confidence in designing extensible class hierarchies.\",\n" +
                "          \"actionUrl\": \"/roadmap\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"title\": \"Solve 2 Array & Two-Pointer DSA Challenges\",\n" +
                "          \"description\": \"Practice in the interactive coding workspace.\",\n" +
                "          \"category\": \"DSA\",\n" +
                "          \"priority\": \"HIGH\",\n" +
                "          \"estimatedMinutes\": 45,\n" +
                "          \"whyItMatters\": \"Crucial for technical screen rounds.\",\n" +
                "          \"expectedOutcome\": \"Optimal O(N) array manipulation.\",\n" +
                "          \"actionUrl\": \"/practice\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"date\": \"" + start.plusDays(1).toString() + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\n" +
                "          \"title\": \"Master Relational Databases & SQL JOINs\",\n" +
                "          \"description\": \"Review indexing, transactions, and foreign key constraints.\",\n" +
                "          \"category\": \"ROADMAP\",\n" +
                "          \"priority\": \"HIGH\",\n" +
                "          \"estimatedMinutes\": 45,\n" +
                "          \"whyItMatters\": \"Core persistence requirement for backend systems.\",\n" +
                "          \"expectedOutcome\": \"Ability to write optimized multi-table queries.\",\n" +
                "          \"actionUrl\": \"/roadmap\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"date\": \"" + start.plusDays(2).toString() + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\n" +
                "          \"title\": \"Complete 5-Question Technical Mock Interview\",\n" +
                "          \"description\": \"Simulate timed interview conditions under pressure.\",\n" +
                "          \"category\": \"MOCK_INTERVIEW\",\n" +
                "          \"priority\": \"MEDIUM\",\n" +
                "          \"estimatedMinutes\": 30,\n" +
                "          \"whyItMatters\": \"Tests communication and conceptual articulation.\",\n" +
                "          \"expectedOutcome\": \"Identifies technical weak areas before employer interviews.\",\n" +
                "          \"actionUrl\": \"/mock-interview\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"date\": \"" + start.plusDays(3).toString() + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\n" +
                "          \"title\": \"Spring Boot RESTful Services & Dependency Injection\",\n" +
                "          \"description\": \"Deep dive into controller architectures and service wiring.\",\n" +
                "          \"category\": \"ROADMAP\",\n" +
                "          \"priority\": \"HIGH\",\n" +
                "          \"estimatedMinutes\": 45,\n" +
                "          \"whyItMatters\": \"Enterprise microservice construction standard.\",\n" +
                "          \"expectedOutcome\": \"Clean decoupled architecture implementation.\",\n" +
                "          \"actionUrl\": \"/roadmap\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"date\": \"" + start.plusDays(4).toString() + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\n" +
                "          \"title\": \"Binary Search & Stack Problem Set\",\n" +
                "          \"description\": \"Solve 2 medium algorithmic problems in the workspace.\",\n" +
                "          \"category\": \"DSA\",\n" +
                "          \"priority\": \"HIGH\",\n" +
                "          \"estimatedMinutes\": 45,\n" +
                "          \"whyItMatters\": \"Essential pattern for logarithmic search questions.\",\n" +
                "          \"expectedOutcome\": \"O(log N) search implementation mastery.\",\n" +
                "          \"actionUrl\": \"/practice\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"date\": \"" + start.plusDays(5).toString() + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\n" +
                "          \"title\": \"Resume Skills & Portfolio Verification\",\n" +
                "          \"description\": \"Review parsed skills and sync missing competencies to profile.\",\n" +
                "          \"category\": \"RESUME\",\n" +
                "          \"priority\": \"MEDIUM\",\n" +
                "          \"estimatedMinutes\": 25,\n" +
                "          \"whyItMatters\": \"Aligns candidate profile with employer requirements.\",\n" +
                "          \"expectedOutcome\": \"Synchronized verified skills profile.\",\n" +
                "          \"actionUrl\": \"/resume\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"date\": \"" + start.plusDays(6).toString() + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\n" +
                "          \"title\": \"Weekly Comprehensive Review & Knowledge Consolidation\",\n" +
                "          \"description\": \"Review notes and reflect on weekly milestone completions.\",\n" +
                "          \"category\": \"REVIEW\",\n" +
                "          \"priority\": \"LOW\",\n" +
                "          \"estimatedMinutes\": 30,\n" +
                "          \"whyItMatters\": \"Long-term memory retention and conceptual reinforcement.\",\n" +
                "          \"expectedOutcome\": \"Readiness for upcoming week milestones.\",\n" +
                "          \"actionUrl\": \"/roadmap\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private String generateMockActionPlanJson(String prompt) {
        return "{\n" +
                "  \"title\": \"Personalized Java Backend Development Roadmap\",\n" +
                "  \"summary\": \"Focus on mastering core backend fundamentals, database indexing, and mock interview communication to accelerate your readiness.\",\n" +
                "  \"estimatedTimeframe\": \"3-4 Weeks\",\n" +
                "  \"motivationalNote\": \"Consistent daily milestone completion is the fastest pathway to technical interview confidence.\",\n" +
                "  \"actions\": [\n" +
                "    {\n" +
                "      \"id\": \"ACT_AI_1\",\n" +
                "      \"title\": \"Master Relational Databases & SQL Queries\",\n" +
                "      \"description\": \"Complete SQL milestone checkpoints and solve relational database schema design problems.\",\n" +
                "      \"category\": \"ROADMAP\",\n" +
                "      \"priority\": \"CRITICAL\",\n" +
                "      \"whyItMatters\": \"Closes your highest priority skill gap in backend persistence architecture.\",\n" +
                "      \"expectedOutcome\": \"Proficiency in complex SQL joins, indexing, and transactional boundaries.\",\n" +
                "      \"estimatedEffort\": \"3-5 hours\",\n" +
                "      \"actionUrl\": \"/roadmap\",\n" +
                "      \"estimatedImpactPoints\": 15\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"ACT_AI_2\",\n" +
                "      \"title\": \"Solve 5 Intermediate DSA Practice Challenges\",\n" +
                "      \"description\": \"Practice tree traversals, two-pointer arrays, and hashing in the coding workspace.\",\n" +
                "      \"category\": \"DSA\",\n" +
                "      \"priority\": \"HIGH\",\n" +
                "      \"whyItMatters\": \"Builds speed and syntax fluency for live coding screens.\",\n" +
                "      \"expectedOutcome\": \"Demonstrated ability to write clean O(N) algorithmic solutions.\",\n" +
                "      \"estimatedEffort\": \"4 hours\",\n" +
                "      \"actionUrl\": \"/practice\",\n" +
                "      \"estimatedImpactPoints\": 12\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"ACT_AI_3\",\n" +
                "      \"title\": \"Simulate Full Mock Technical Interview\",\n" +
                "      \"description\": \"Test your conceptual explanations and system design articulation under timed conditions.\",\n" +
                "      \"category\": \"INTERVIEW\",\n" +
                "      \"priority\": \"HIGH\",\n" +
                "      \"whyItMatters\": \"Identifies communication gaps before actual employer interviews.\",\n" +
                "      \"expectedOutcome\": \"Authoritative score evaluation and structured feedback breakdown.\",\n" +
                "      \"estimatedEffort\": \"45 minutes\",\n" +
                "      \"actionUrl\": \"/mock-interview\",\n" +
                "      \"estimatedImpactPoints\": 15\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    
    private String generateMockProgressCoachJson(String prompt) {
        return "{\n" +
                "  \"coachAssessment\": \"Your weekly study habit formation is strong, with consistent roadmap progress. However, your mock interview confidence needs attention before recruiter screens. Focus heavily on behavioral and technical articulation next week.\",\n" +
                "  \"topAchievements\": [\n" +
                "    \"Maintained continuous daily study momentum in your weekly planner\",\n" +
                "    \"Successfully advanced core curriculum milestones in your target career roadmap\",\n" +
                "    \"Solved algorithmic practice problems in the interactive coding workspace\"\n" +
                "  ],\n" +
                "  \"priorityAreas\": [\n" +
                "    \"Schedule timed 5-question mock interview to build screen confidence\",\n" +
                "    \"Address top critical skill gap identified by Career Intelligence\",\n" +
                "    \"Review any skipped planner tasks to calibrate daily study budget\"\n" +
                "  ],\n" +
                "  \"strengths\": [\n" +
                "    \"Disciplined daily learning habit formation\",\n" +
                "    \"Proactive milestone completion in technical track\"\n" +
                "  ],\n" +
                "  \"weaknesses\": [\n" +
                "    \"Mock interview practice volume is lower than recommended\",\n" +
                "    \"Key industry framework competencies need practical project validation\"\n" +
                "  ],\n" +
                "  \"improvements\": [\n" +
                "    \"Study streak is actively climbing\",\n" +
                "    \"Overall readiness score has demonstrated measurable upward progress\"\n" +
                "  ],\n" +
                "  \"nextWeekFocus\": [\n" +
                "    {\n" +
                "      \"title\": \"Complete Next Roadmap Checkpoint\",\n" +
                "      \"category\": \"ROADMAP\",\n" +
                "      \"whyItMatters\": \"Directly satisfies foundational curriculum requirements.\",\n" +
                "      \"actionUrl\": \"/roadmap\",\n" +
                "      \"estimatedEffort\": \"45 mins\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Simulate Timed Mock Technical Interview\",\n" +
                "      \"category\": \"MOCK_INTERVIEW\",\n" +
                "      \"whyItMatters\": \"Builds verbal fluency and score benchmarking under pressure.\",\n" +
                "      \"actionUrl\": \"/mock-interview\",\n" +
                "      \"estimatedEffort\": \"30 mins\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"Solve 2 Curated Practice Challenges\",\n" +
                "      \"category\": \"DSA\",\n" +
                "      \"whyItMatters\": \"Strengthens problem solving patterns for live technical rounds.\",\n" +
                "      \"actionUrl\": \"/practice\",\n" +
                "      \"estimatedEffort\": \"45 mins\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private String generateContextualMockResponse(String userPrompt, AiPrompt prompt) {
        String lower = userPrompt.toLowerCase();
        if (lower.contains("progress") || lower.contains("how am i doing") || lower.contains("how was my week") || lower.contains("improving") || lower.contains("doing this week")) {
            return "Based on your weekly progress intelligence, your overall progress score and study habits are trending positively. Your roadmap completion is advancing steadily; to accelerate your career readiness, prioritize completing a timed mock interview and resolving your top skill gaps next week.";
        } else if (lower.contains("roadmap") || lower.contains("next step") || lower.contains("learn")) {
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
