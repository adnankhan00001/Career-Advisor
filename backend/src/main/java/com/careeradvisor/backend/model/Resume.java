package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resumes", indexes = {
    @Index(name = "idx_resume_user_id", columnList = "user_id"),
    @Index(name = "idx_resume_upload_time", columnList = "upload_timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Builder.Default
    @Column(name = "upload_timestamp", nullable = false)
    private LocalDateTime uploadTimestamp = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "parsing_status", nullable = false)
    private ResumeStatus parsingStatus;

    @Lob
    @Column(name = "parsed_text", columnDefinition = "LONGTEXT")
    private String parsedText;

    @Lob
    @Column(name = "extracted_summary", columnDefinition = "TEXT")
    private String extractedSummary;

    @Column(name = "extracted_email")
    private String extractedEmail;

    @Column(name = "extracted_phone")
    private String extractedPhone;

    @Lob
    @Column(name = "extracted_education", columnDefinition = "TEXT")
    private String extractedEducation;

    @Lob
    @Column(name = "extracted_experience", columnDefinition = "TEXT")
    private String extractedExperience;

    @Lob
    @Column(name = "extracted_projects", columnDefinition = "TEXT")
    private String extractedProjects;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "resume_skills", joinColumns = @JoinColumn(name = "resume_id"))
    @Column(name = "skill_name")
    @Builder.Default
    private List<String> extractedSkills = new ArrayList<>();

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
