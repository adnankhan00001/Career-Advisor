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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSessionDto {
    private Long id;
    private ProblemCategory category;
    private Difficulty difficulty;
    private InterviewStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime deadline;
    private long remainingSeconds;
    private int durationSeconds;
    private int totalQuestions;
    private int answeredCount;
    private List<InterviewQuestionDto> questions;
}
