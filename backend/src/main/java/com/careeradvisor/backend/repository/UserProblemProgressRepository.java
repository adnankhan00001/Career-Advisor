package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.CodingProblem;
import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.ProblemCategory;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserProblemProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProblemProgressRepository extends JpaRepository<UserProblemProgress, Long> {

    List<UserProblemProgress> findByUser(User user);

    List<UserProblemProgress> findByUserAndSolvedTrue(User user);

    Optional<UserProblemProgress> findByUserAndProblem(User user, CodingProblem problem);

    Optional<UserProblemProgress> findByUserAndProblem_Id(User user, Long problemId);

    boolean existsByUserAndProblemAndSolvedTrue(User user, CodingProblem problem);

    int countByUserAndSolvedTrue(User user);

    long countBySolvedTrue();

    @Query("SELECT COUNT(p) FROM UserProblemProgress p WHERE p.user = :user AND p.problem.difficulty = :difficulty AND p.solved = true")
    int countByUserAndDifficultyAndSolvedTrue(@Param("user") User user, @Param("difficulty") Difficulty difficulty);

    @Query("SELECT COUNT(p) FROM UserProblemProgress p WHERE p.user = :user AND p.problem.category = :category AND p.solved = true")
    int countByUserAndCategoryAndSolvedTrue(@Param("user") User user, @Param("category") ProblemCategory category);

    @Query("SELECT COUNT(p) FROM UserProblemProgress p WHERE p.user = :user AND LOWER(p.problem.topic) = LOWER(:topic) AND p.solved = true")
    int countByUserAndTopicAndSolvedTrue(@Param("user") User user, @Param("topic") String topic);

    void deleteByUser(User user);
}
