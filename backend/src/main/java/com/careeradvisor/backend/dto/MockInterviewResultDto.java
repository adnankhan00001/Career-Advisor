package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.model.ProblemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewResultDto {
    private Long id;
    private ProblemCategory category;
    private Difficulty difficulty;
    private InterviewStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int timeTakenSeconds;
    private int durationSeconds;
    private int score; // 0 - 100%
    private int totalQuestions;
    private int correctCount;
    private List<String> strongAreas;
    private List<String> weakAreas;
    private Map<String, Integer> categoryBreakdown;
    private List<InterviewQuestionReviewDto> questionReviews;
    private String recommendation;
}
