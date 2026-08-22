package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.CodingProblem;
import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.ProblemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodingProblemRepository extends JpaRepository<CodingProblem, Long> {

    List<CodingProblem> findAllByOrderByOrderIndexAsc();

    Optional<CodingProblem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<CodingProblem> findByCategoryOrderByOrderIndexAsc(ProblemCategory category);

    List<CodingProblem> findByTopicIgnoreCaseOrderByOrderIndexAsc(String topic);

    List<CodingProblem> findByDifficultyOrderByOrderIndexAsc(Difficulty difficulty);

    List<CodingProblem> findByCategoryAndTopicIgnoreCaseOrderByOrderIndexAsc(ProblemCategory category, String topic);

    List<CodingProblem> findByCategoryAndDifficultyOrderByOrderIndexAsc(ProblemCategory category, Difficulty difficulty);

    List<CodingProblem> findByTopicIgnoreCaseAndDifficultyOrderByOrderIndexAsc(String topic, Difficulty difficulty);

    List<CodingProblem> findByCategoryAndTopicIgnoreCaseAndDifficultyOrderByOrderIndexAsc(
            ProblemCategory category, String topic, Difficulty difficulty);
}
