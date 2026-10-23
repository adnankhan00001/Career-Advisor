package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.RoadmapProposalChangeItem;
import com.careeradvisor.backend.model.RoadmapUpdateProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadmapProposalChangeItemRepository extends JpaRepository<RoadmapProposalChangeItem, Long> {

    List<RoadmapProposalChangeItem> findByProposalOrderBySequenceOrderAsc(RoadmapUpdateProposal proposal);

    List<RoadmapProposalChangeItem> findByStepId(Long stepId);
}
