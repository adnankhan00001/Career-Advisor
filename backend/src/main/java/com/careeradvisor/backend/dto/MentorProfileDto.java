package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.MentorVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorProfileDto {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String jobTitle;
    private String company;
    private Integer yearsOfExperience;
    private String bio;
    private String expertiseAreas;
    private Integer maxMentees;
    private Long activeMenteesCount;
    private Long availableSlots;
    private MentorVerificationStatus verificationStatus;
    private boolean isActive;
    private String rejectionReason;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
