package com.careeradvisor.backend.dto;

public enum ReadinessState {
    BEGINNER("Beginner", "Foundational learning phase. Focus on core programming concepts and roadmap milestones."),
    FOUNDATIONAL("Foundational", "Core basics established. Start hands-on problem solving and diagnostic assessment."),
    DEVELOPING("Developing", "Actively building competency across DSA, roadmaps, and verified technical skills."),
    ADVANCED("Advanced", "Strong technical proficiency. Polish resume and practice mock technical interviews."),
    JOB_READY("Job Ready", "Comprehensive readiness achieved across all dimensions. Ready for target career applications.");

    private final String displayName;
    private final String description;

    ReadinessState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static ReadinessState fromScore(int score) {
        if (score >= 85) return JOB_READY;
        if (score >= 65) return ADVANCED;
        if (score >= 45) return DEVELOPING;
        if (score >= 25) return FOUNDATIONAL;
        return BEGINNER;
    }
}
