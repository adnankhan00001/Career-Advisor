package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.CompetencyNode;
import com.careeradvisor.backend.model.CompetencyPrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetencyPrerequisiteRepository extends JpaRepository<CompetencyPrerequisite, Long> {

    List<CompetencyPrerequisite> findByCompetency(CompetencyNode competency);

    List<CompetencyPrerequisite> findByPrerequisite(CompetencyNode prerequisite);

    boolean existsByCompetencyAndPrerequisite(CompetencyNode competency, CompetencyNode prerequisite);

    Optional<CompetencyPrerequisite> findByCompetencyAndPrerequisite(CompetencyNode competency, CompetencyNode prerequisite);

    @Query("SELECT cp.prerequisite FROM CompetencyPrerequisite cp WHERE cp.competency.id = :competencyId")
    List<CompetencyNode> findPrerequisiteNodes(@Param("competencyId") Long competencyId);

    @Query("SELECT cp.competency FROM CompetencyPrerequisite cp WHERE cp.prerequisite.id = :prerequisiteId")
    List<CompetencyNode> findDependentNodes(@Param("prerequisiteId") Long prerequisiteId);
}
