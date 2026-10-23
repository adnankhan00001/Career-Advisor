package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.PersonalizedRoadmap;
import com.careeradvisor.backend.model.ProposalStatus;
import com.careeradvisor.backend.model.RoadmapUpdateProposal;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapUpdateProposalRepository extends JpaRepository<RoadmapUpdateProposal, Long> {

    List<RoadmapUpdateProposal> findByUserOrderByCreatedAtDesc(User user);

    List<RoadmapUpdateProposal> findByRoadmapAndStatus(PersonalizedRoadmap roadmap, ProposalStatus status);

    Optional<RoadmapUpdateProposal> findFirstByUserAndRoadmapAndStatusOrderByCreatedAtDesc(
            User user, PersonalizedRoadmap roadmap, ProposalStatus status);

    Optional<RoadmapUpdateProposal> findFirstBySourceSessionId(Long sessionId);

    @Query("SELECT p FROM RoadmapUpdateProposal p LEFT JOIN FETCH p.changeItems WHERE p.id = :id")
    Optional<RoadmapUpdateProposal> findByIdWithChangeItems(@Param("id") Long id);
}
