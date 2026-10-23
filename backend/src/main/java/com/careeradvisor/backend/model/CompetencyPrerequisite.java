package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "competency_prerequisites", uniqueConstraints = {
    @UniqueConstraint(name = "uk_competency_prerequisite", columnNames = {"competency_id", "prerequisite_id"})
}, indexes = {
    @Index(name = "idx_prereq_competency", columnList = "competency_id"),
    @Index(name = "idx_prereq_dependency", columnList = "prerequisite_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyPrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competency_id", nullable = false)
    @JsonIgnore
    private CompetencyNode competency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_id", nullable = false)
    @JsonIgnore
    private CompetencyNode prerequisite;

    @Column(length = 255)
    private String reason;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (competency != null && prerequisite != null && competency.equals(prerequisite)) {
            throw new IllegalStateException("A competency cannot depend on itself");
        }
    }
}
