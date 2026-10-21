package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.UserStatus;
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
public class AdminUserDetailDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private UserStatus status;
    private String suspensionReason;
    private LocalDateTime suspendedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String careerGoal;
    private String userLevel;
    private String latestQuizScore;
    private List<String> skills;
    private boolean resumePresent;
    private String resumeFileName;
    private String resumeUploadTimestamp;
    private long mockInterviewCount;
    private Double averageInterviewScore;
    private long solvedProblemsCount;
    private long completedRoadmapStepsCount;
}
