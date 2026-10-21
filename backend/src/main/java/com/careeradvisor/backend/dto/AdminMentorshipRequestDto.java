package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.MentorshipRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMentorshipRequestDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentCareerGoal;
    private Long mentorProfileId;
    private Long mentorUserId;
    private String mentorName;
    private String mentorEmail;
    private String mentorCompany;
    private String mentorJobTitle;
    private String goalDescription;
    private MentorshipRequestStatus status;
    private Long conversationId;
    private LocalDateTime lastActivityAt;
    private String responseNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
