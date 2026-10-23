package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.TechnologyAssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnologyAssessmentResultRepository extends JpaRepository<TechnologyAssessmentResult, Long> {

    @Query("SELECT r FROM TechnologyAssessmentResult r JOIN FETCH r.technology WHERE r.session.id = :sessionId")
    List<TechnologyAssessmentResult> findBySessionIdWithTechnology(@Param("sessionId") Long sessionId);

    Optional<TechnologyAssessmentResult> findBySessionIdAndTechnologyId(Long sessionId, Long technologyId);
}
