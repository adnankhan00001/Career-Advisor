package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.LearningPlan;
import com.careeradvisor.backend.model.LearningTask;
import com.careeradvisor.backend.model.LearningTaskStatus;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LearningTaskRepository extends JpaRepository<LearningTask, Long> {
    List<LearningTask> findByLearningPlanOrderByScheduledDateAscSequenceNumberAsc(LearningPlan learningPlan);
    List<LearningTask> findByLearningPlanAndScheduledDateOrderBySequenceNumberAsc(LearningPlan learningPlan, LocalDate scheduledDate);
    Optional<LearningTask> findByIdAndLearningPlan_User(Long id, User user);
    long countByLearningPlan_UserAndStatus(User user, LearningTaskStatus status);
}
