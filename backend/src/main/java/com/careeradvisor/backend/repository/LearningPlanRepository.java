package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.LearningPlan;
import com.careeradvisor.backend.model.LearningPlanStatus;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {
    Optional<LearningPlan> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, LearningPlanStatus status);
    List<LearningPlan> findAllByUserOrderByCreatedAtDesc(User user);
    long countByStatus(LearningPlanStatus status);
    List<LearningPlan> findByStatus(LearningPlanStatus status);
}
