package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.ProblemCategory;
import com.careeradvisor.backend.model.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionReviewDto {
    private Long id;
    private String question;
    private ProblemCategory category;
    private String topic;
    private Difficulty difficulty;
    private QuestionType questionType;
    private List<String> options;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private String explanation;
    private List<String> expectedConcepts;
}
