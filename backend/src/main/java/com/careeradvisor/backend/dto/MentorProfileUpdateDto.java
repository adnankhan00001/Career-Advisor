package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorProfileUpdateDto {

    @Size(max = 120, message = "Job title cannot exceed 120 characters")
    private String jobTitle;

    @Size(max = 120, message = "Company cannot exceed 120 characters")
    private String company;

    @Min(value = 0, message = "Years of experience must be non-negative")
    private Integer yearsOfExperience;

    @Size(max = 2000, message = "Bio cannot exceed 2000 characters")
    private String bio;

    @Size(max = 500, message = "Expertise areas cannot exceed 500 characters")
    private String expertiseAreas;

    @Min(value = 1, message = "Maximum mentees must be at least 1")
    private Integer maxMentees;

    @Size(max = 255, message = "LinkedIn URL cannot exceed 255 characters")
    private String linkedinUrl;

    @Size(max = 255, message = "GitHub URL cannot exceed 255 characters")
    private String githubUrl;

    @Size(max = 255, message = "Portfolio URL cannot exceed 255 characters")
    private String portfolioUrl;

    private Boolean isActive;
}
