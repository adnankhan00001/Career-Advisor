package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.MentorshipRequestStatus;
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
public class AdminMentorshipDetailDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentCareerGoal;
    private String studentUserLevel;
    private List<String> studentSkills;

    private Long mentorProfileId;
    private Long mentorUserId;
    private String mentorName;
    private String mentorEmail;
    private String mentorCompany;
    private String mentorJobTitle;
    private Integer mentorYearsOfExperience;
    private String mentorBio;
    private String mentorExpertiseAreas;
    private Integer mentorMaxMentees;
    private long mentorActiveMenteesCount;

    private String goalDescription;
    private MentorshipRequestStatus status;
    private Long conversationId;
    private String conversationTitle;
    private long messageCount;
    private LocalDateTime lastActivityAt;
    private String responseNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
