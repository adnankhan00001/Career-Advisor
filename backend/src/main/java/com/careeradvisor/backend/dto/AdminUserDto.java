package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private UserStatus status;
    private String suspensionReason;
    private LocalDateTime suspendedAt;
    private LocalDateTime createdAt;
    private String careerGoal;
    private String userLevel;
    private String latestQuizScore;
    private long skillCount;
    private boolean resumePresent;
    private long mockInterviewCount;
    private long solvedProblemsCount;
}
