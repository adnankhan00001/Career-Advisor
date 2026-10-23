package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.CompetencyNode;
import com.careeradvisor.backend.model.Technology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetencyNodeRepository extends JpaRepository<CompetencyNode, Long> {

    Optional<CompetencyNode> findByCompetencyKey(String competencyKey);

    List<CompetencyNode> findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(Technology technology);

    @Query("SELECT c FROM CompetencyNode c WHERE c.technology.slug = :technologySlug AND c.active = true ORDER BY c.sequenceOrder ASC")
    List<CompetencyNode> findByTechnologySlug(@Param("technologySlug") String technologySlug);

    boolean existsByCompetencyKey(String competencyKey);
}
