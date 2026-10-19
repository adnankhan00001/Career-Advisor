package com.careeradvisor.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MentorshipModelTests {

    @Test
    @DisplayName("MentorProfile builder sets defaults properly")
    void testMentorProfileDefaults() {
        User user = User.builder()
                .id(1L)
                .name("Alex Mentor")
                .email("alex@mentor.com")
                .role(Role.USER)
                .build();

        MentorProfile profile = MentorProfile.builder()
                .id(10L)
                .user(user)
                .jobTitle("Staff Software Engineer")
                .company("Google")
                .yearsOfExperience(8)
                .bio("Experienced distributed systems mentor")
                .expertiseAreas("Java, Spring Boot, Microservices")
                .linkedinUrl("https://linkedin.com/in/alex-mentor")
                .githubUrl("https://github.com/alex-mentor")
                .build();

        assertEquals(10L, profile.getId());
        assertEquals("Alex Mentor", profile.getUser().getName());
        assertEquals("Staff Software Engineer", profile.getJobTitle());
        assertEquals("Google", profile.getCompany());
        assertEquals(8, profile.getYearsOfExperience());
        assertEquals("https://linkedin.com/in/alex-mentor", profile.getLinkedinUrl());
        assertEquals("https://github.com/alex-mentor", profile.getGithubUrl());
        assertEquals(3, profile.getMaxMentees(), "Default maxMentees should be 3");
        assertEquals(MentorVerificationStatus.APPLIED, profile.getVerificationStatus());
        assertTrue(profile.isActive());
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
    }

    @Test
    @DisplayName("MentorshipRequest builder sets defaults properly")
    void testMentorshipRequestDefaults() {
        User student = User.builder()
                .id(2L)
                .name("Sam Student")
                .email("sam@student.com")
                .role(Role.USER)
                .build();

        MentorProfile mentor = MentorProfile.builder()
                .id(10L)
                .jobTitle("Principal Architect")
                .company("Amazon")
                .yearsOfExperience(12)
                .build();

        MentorshipRequest request = MentorshipRequest.builder()
                .id(100L)
                .student(student)
                .mentor(mentor)
                .goalDescription("I want to transition from frontend to distributed backend engineering.")
                .build();

        assertEquals(100L, request.getId());
        assertEquals("Sam Student", request.getStudent().getName());
        assertEquals(10L, request.getMentor().getId());
        assertEquals(MentorshipRequestStatus.PENDING, request.getStatus(), "Default status should be PENDING");
        assertNull(request.getConversation(), "Conversation should be null until accepted");
        assertNotNull(request.getCreatedAt());
        assertNotNull(request.getUpdatedAt());
    }

    @Test
    @DisplayName("Enum values coverage check")
    void testEnumCompleteness() {
        assertEquals(4, MentorVerificationStatus.values().length);
        assertNotNull(MentorVerificationStatus.valueOf("APPLIED"));
        assertNotNull(MentorVerificationStatus.valueOf("APPROVED"));
        assertNotNull(MentorVerificationStatus.valueOf("REJECTED"));
        assertNotNull(MentorVerificationStatus.valueOf("SUSPENDED"));

        assertEquals(5, MentorshipRequestStatus.values().length);
        assertNotNull(MentorshipRequestStatus.valueOf("PENDING"));
        assertNotNull(MentorshipRequestStatus.valueOf("ACCEPTED"));
        assertNotNull(MentorshipRequestStatus.valueOf("DECLINED"));
        assertNotNull(MentorshipRequestStatus.valueOf("CANCELLED"));
        assertNotNull(MentorshipRequestStatus.valueOf("COMPLETED"));
    }
}
