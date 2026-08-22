package com.careeradvisor.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coding_problems")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProblemCategory category;

    @Column(nullable = false)
    private String topic;

    @Column(name = "external_url")
    private String externalUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "coding_problem_tags", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "acceptance_rate")
    private String acceptanceRate;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "sample_input", columnDefinition = "TEXT")
    private String sampleInput;

    @Column(name = "sample_output", columnDefinition = "TEXT")
    private String sampleOutput;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public CodingProblem(String slug, String title, String description, Difficulty difficulty,
                         ProblemCategory category, String topic, String externalUrl,
                         List<String> tags, String acceptanceRate, Integer orderIndex) {
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.category = category;
        this.topic = topic;
        this.externalUrl = externalUrl;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.acceptanceRate = acceptanceRate;
        this.orderIndex = orderIndex != null ? orderIndex : 0;
        this.createdAt = LocalDateTime.now();
    }
}
