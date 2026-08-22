package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.model.ProblemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewHistoryItemDto {
    private Long id;
    private ProblemCategory category;
    private Difficulty difficulty;
    private InterviewStatus status;
    private int score;
    private int totalQuestions;
    private int correctCount;
    private int durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
