package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.PersonalizedRoadmap;
import com.careeradvisor.backend.model.RoadmapRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapRevisionRepository extends JpaRepository<RoadmapRevision, Long> {

    List<RoadmapRevision> findByRoadmapOrderByRevisionNumberDesc(PersonalizedRoadmap roadmap);

    Optional<RoadmapRevision> findFirstByRoadmapOrderByRevisionNumberDesc(PersonalizedRoadmap roadmap);
}
