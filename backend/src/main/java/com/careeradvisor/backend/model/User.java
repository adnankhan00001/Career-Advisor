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

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.careerGoal = "Java Backend Developer";
        this.userLevel = "Intermediate";
        this.role = Role.USER;
    }
}
