package com.careeradvisor.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "career_tracks", indexes = {
    @Index(name = "idx_career_tracks_slug", columnList = "slug", unique = true),
    @Index(name = "idx_career_tracks_category", columnList = "category")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String title;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(nullable = false, length = 32)
    private String level;

    @Column(length = 64)
    private String duration;

    @Column(name = "salary_range", length = 64)
    private String salaryRange;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "career_track_technologies",
        joinColumns = @JoinColumn(name = "career_track_id"),
        inverseJoinColumns = @JoinColumn(name = "technology_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_career_technology", columnNames = {"career_track_id", "technology_id"})
    )
    @Builder.Default
    private Set<Technology> technologies = new HashSet<>();

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
