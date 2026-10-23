package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.PersonalizedRoadmap;
import com.careeradvisor.backend.model.PersonalizedRoadmapMilestone;
import com.careeradvisor.backend.model.RoadmapProjectEvidence;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapProjectEvidenceRepository extends JpaRepository<RoadmapProjectEvidence, Long> {

    List<RoadmapProjectEvidence> findByMilestoneOrderByCreatedAtDesc(PersonalizedRoadmapMilestone milestone);

    List<RoadmapProjectEvidence> findByRoadmapOrderByCreatedAtDesc(PersonalizedRoadmap roadmap);

    List<RoadmapProjectEvidence> findByUserOrderByCreatedAtDesc(User user);

    Optional<RoadmapProjectEvidence> findByIdAndUser(Long id, User user);
}
