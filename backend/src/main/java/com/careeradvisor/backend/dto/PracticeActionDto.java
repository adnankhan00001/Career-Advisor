package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeActionDto {
    private String recommendedTopic;
    private String problemSlug;
    private String problemTitle;
    private Difficulty difficulty;
    private int topicSolved;
    private int topicTotal;
    private int overallSolved;
    private int overallTotal;
    private String reason;
}
