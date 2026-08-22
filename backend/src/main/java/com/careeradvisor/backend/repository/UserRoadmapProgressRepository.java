package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserRoadmapProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoadmapProgressRepository extends JpaRepository<UserRoadmapProgress, Long> {
    List<UserRoadmapProgress> findByUser(User user);
    long countByUser(User user);
    List<UserRoadmapProgress> findByUserAndCareerTitle(User user, String careerTitle);
    Optional<UserRoadmapProgress> findByUserAndCareerTitleAndStepTitle(User user, String careerTitle, String stepTitle);
    boolean existsByUserAndStepTitle(User user, String stepTitle);
    void deleteByUser(User user);
}
