package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_answers", indexes = {
    @Index(name = "idx_aa_session", columnList = "session_id"),
    @Index(name = "idx_aa_session_status", columnList = "session_id, status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private AssessmentSession session;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_question_id", nullable = false, unique = true)
    @JsonIgnore
    private AssessmentSessionQuestion sessionQuestion;

    @Column(name = "selected_option_index")
    private Integer selectedOptionIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private AnswerStatus status = AnswerStatus.UNANSWERED;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "awarded_marks", nullable = false)
    @Builder.Default
    private int awardedMarks = 0;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

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
