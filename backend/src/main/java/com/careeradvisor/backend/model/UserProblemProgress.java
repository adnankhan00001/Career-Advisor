package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_problem_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_problem", columnNames = {"user_id", "problem_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProblemProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "problem_id", nullable = false)
    private CodingProblem problem;

    @Builder.Default
    @Column(nullable = false)
    private boolean solved = true;

    @Builder.Default
    @Column(name = "solved_at")
    private LocalDateTime solvedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String notes;

    public UserProblemProgress(User user, CodingProblem problem, boolean solved) {
        this.user = user;
        this.problem = problem;
        this.solved = solved;
        this.solvedAt = LocalDateTime.now();
    }
}
