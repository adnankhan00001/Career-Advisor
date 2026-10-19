package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorApplicationDto {

    @NotBlank(message = "Job title is required")
    @Size(max = 120, message = "Job title cannot exceed 120 characters")
    private String jobTitle;

    @NotBlank(message = "Company is required")
    @Size(max = 120, message = "Company cannot exceed 120 characters")
    private String company;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Years of experience must be non-negative")
    private Integer yearsOfExperience;

    @Size(max = 2000, message = "Bio cannot exceed 2000 characters")
    private String bio;

    @NotBlank(message = "Expertise areas are required")
    @Size(max = 500, message = "Expertise areas cannot exceed 500 characters")
    private String expertiseAreas;

    @Min(value = 1, message = "Maximum mentees must be at least 1")
    private Integer maxMentees;

    @NotBlank(message = "LinkedIn URL is required")
    @Size(max = 255, message = "LinkedIn URL cannot exceed 255 characters")
    private String linkedinUrl;

    @NotBlank(message = "GitHub URL is required")
    @Size(max = 255, message = "GitHub URL cannot exceed 255 characters")
    private String githubUrl;

    @NotBlank(message = "Coding practice profile URL is required")
    @Size(max = 255, message = "Coding profile URL cannot exceed 255 characters")
    private String codingProfileUrl;

    @Size(max = 255, message = "Portfolio URL cannot exceed 255 characters")
    private String portfolioUrl;
}
