package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.AssessmentDifficulty;
import com.careeradvisor.backend.model.AssessmentQuestion;
import com.careeradvisor.backend.model.Technology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {

    Optional<AssessmentQuestion> findByQuestionKey(String questionKey);

    List<AssessmentQuestion> findByTechnologyAndActiveTrue(Technology technology);

    List<AssessmentQuestion> findByTechnologyAndDifficultyAndActiveTrue(Technology technology, AssessmentDifficulty difficulty);

    @Query("SELECT q FROM AssessmentQuestion q WHERE q.technology.slug = :slug AND q.active = true")
    List<AssessmentQuestion> findByTechnologySlug(@Param("slug") String slug);

    @Query("SELECT q FROM AssessmentQuestion q WHERE q.technology.slug = :slug AND q.difficulty = :diff AND q.active = true")
    List<AssessmentQuestion> findByTechnologySlugAndDifficulty(@Param("slug") String slug, @Param("diff") AssessmentDifficulty diff);

    long countByActiveTrue();
}
