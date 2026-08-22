package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_roadmap_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "career_title", "step_title"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoadmapProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "career_title", nullable = false)
    private String careerTitle;

    @Column(name = "step_title", nullable = false)
    private String stepTitle;

    @Builder.Default
    @Column(name = "completed_at")
    private LocalDateTime completedAt = LocalDateTime.now();

    public UserRoadmapProgress(User user, String careerTitle, String stepTitle) {
        this.user = user;
        this.careerTitle = careerTitle;
        this.stepTitle = stepTitle;
        this.completedAt = LocalDateTime.now();
    }
}
