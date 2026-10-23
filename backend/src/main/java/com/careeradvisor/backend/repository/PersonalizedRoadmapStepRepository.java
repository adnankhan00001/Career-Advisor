package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.PersonalizedRoadmapStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonalizedRoadmapStepRepository extends JpaRepository<PersonalizedRoadmapStep, Long> {

    @Query("SELECT s FROM PersonalizedRoadmapStep s " +
           "JOIN FETCH s.milestone m " +
           "JOIN FETCH m.roadmap r " +
           "JOIN FETCH r.user u " +
           "WHERE s.id = :stepId")
    Optional<PersonalizedRoadmapStep> findByIdWithMilestoneAndRoadmap(@Param("stepId") Long stepId);
}
