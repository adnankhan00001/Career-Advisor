package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_questions", indexes = {
    @Index(name = "idx_aq_tech_diff", columnList = "technology_id, difficulty"),
    @Index(name = "idx_aq_topic", columnList = "topic")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_key", nullable = false, unique = true, length = 120)
    private String questionKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technology_id", nullable = false)
    @JsonIgnore
    private Technology technology;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competency_id")
    @JsonIgnore
    private CompetencyNode competency;

    @Column(nullable = false, length = 120)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssessmentDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 32)
    @Builder.Default
    private AssessmentQuestionType questionType = AssessmentQuestionType.MULTIPLE_CHOICE;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "options_json", columnDefinition = "TEXT", nullable = false)
    private String optionsJson;

    @Column(name = "correct_option_index", nullable = false)
    private int correctOptionIndex;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "scoring_weight", nullable = false)
    @Builder.Default
    private int scoringWeight = 1;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
