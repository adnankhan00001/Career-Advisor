package com.careeradvisor.backend.dto;

import java.util.List;

public class QuizRequest {
    private List<AnswerDto> answers;

    public List<AnswerDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDto> answers) { this.answers = answers; }
}