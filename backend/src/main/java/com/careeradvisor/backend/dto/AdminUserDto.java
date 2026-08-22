package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String careerGoal;
    private String userLevel;
    private String latestQuizScore;
    private long skillCount;
    private boolean resumePresent;
    private long mockInterviewCount;
    private long solvedProblemsCount;
}
