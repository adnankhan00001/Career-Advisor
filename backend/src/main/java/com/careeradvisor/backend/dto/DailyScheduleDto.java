package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyScheduleDto {
    private LocalDate date;
    private String dayOfWeek;
    private int totalMinutesPlanned;
    private int totalMinutesCompleted;
    private int completionPercentage;
    private boolean isPreferredDay;

    @Builder.Default
    private List<LearningTaskDto> tasks = new ArrayList<>();
}
