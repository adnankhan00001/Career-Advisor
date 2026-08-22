package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFocusDto {
    private String subject;
    private String category;
    private String reason;
    private int scoreOrAccuracy;
    private List<String> keyTopicsToReview;
    private String suggestedPracticeProblemSlug;
}
