package com.careeradvisor.backend.dto;

public class QuizResponse {
    private String level;

    public QuizResponse(String level) {
        this.level = level;
    }

    public String getLevel() { return level; }
}