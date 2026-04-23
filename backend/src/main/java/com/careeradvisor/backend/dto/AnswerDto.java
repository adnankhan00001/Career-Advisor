package com.careeradvisor.backend.dto;

public class AnswerDto {
    private int questionId;
    private String selected;

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public String getSelected() { return selected; }
    public void setSelected(String selected) { this.selected = selected; }
}