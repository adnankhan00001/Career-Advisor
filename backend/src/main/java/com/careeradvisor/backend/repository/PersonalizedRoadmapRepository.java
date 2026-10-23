package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.PersonalizedRoadmap;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalizedRoadmapRepository extends JpaRepository<PersonalizedRoadmap, Long> {

    Optional<PersonalizedRoadmap> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, String status);

    Optional<PersonalizedRoadmap> findFirstByUserOrderByCreatedAtDesc(User user);

    Optional<PersonalizedRoadmap> findFirstByUserAndCareerSlugOrderByCreatedAtDesc(User user, String careerSlug);

    List<PersonalizedRoadmap> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndStatus(User user, String status);

    @Query("SELECT DISTINCT r FROM PersonalizedRoadmap r " +
           "LEFT JOIN FETCH r.milestones m " +
           "WHERE r.user = :user AND r.status = 'ACTIVE' " +
           "ORDER BY r.createdAt DESC")
    List<PersonalizedRoadmap> findActiveRoadmapsForUser(@Param("user") User user);
}
