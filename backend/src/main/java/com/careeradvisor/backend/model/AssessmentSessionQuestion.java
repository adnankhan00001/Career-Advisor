package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_session_questions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_question", columnNames = {"session_id", "question_id"}),
    @UniqueConstraint(name = "uk_session_order", columnNames = {"session_id", "presentation_order"})
}, indexes = {
    @Index(name = "idx_asq_session", columnList = "session_id"),
    @Index(name = "idx_asq_session_tech", columnList = "session_id, technology_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private AssessmentSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private AssessmentQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technology_id", nullable = false)
    @JsonIgnore
    private Technology technology;

    @Column(nullable = false, length = 120)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssessmentDifficulty difficulty;

    @Column(name = "presentation_order", nullable = false)
    private int presentationOrder;

    @Column(name = "scoring_weight", nullable = false)
    @Builder.Default
    private int scoringWeight = 1;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
