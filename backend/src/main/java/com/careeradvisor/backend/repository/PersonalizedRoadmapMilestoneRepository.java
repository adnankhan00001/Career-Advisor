package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.PersonalizedRoadmap;
import com.careeradvisor.backend.model.PersonalizedRoadmapMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalizedRoadmapMilestoneRepository extends JpaRepository<PersonalizedRoadmapMilestone, Long> {

    @Query("SELECT m FROM PersonalizedRoadmapMilestone m " +
           "JOIN FETCH m.roadmap r " +
           "JOIN FETCH r.user u " +
           "WHERE m.id = :milestoneId")
    Optional<PersonalizedRoadmapMilestone> findByIdWithRoadmap(@Param("milestoneId") Long milestoneId);

    List<PersonalizedRoadmapMilestone> findByRoadmapOrderBySequenceOrderAsc(PersonalizedRoadmap roadmap);
}
