package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Builder.Default
    @Column(name = "career_goal")
    private String careerGoal = "Java Backend Developer";

    @Builder.Default
    @Column(name = "user_level")
    private String userLevel = "Intermediate";

    @Column(name = "latest_quiz_score")
    private String latestQuizScore;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 32, columnDefinition = "varchar(32) default 'ACTIVE'")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    @Column(name = "suspended_at")
    private java.time.LocalDateTime suspendedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime default CURRENT_TIMESTAMP")
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = java.time.LocalDateTime.now();
        }
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.careerGoal = "Java Backend Developer";
        this.userLevel = "Intermediate";
        this.role = Role.USER;
        this.status = UserStatus.ACTIVE;
        this.createdAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }
}
