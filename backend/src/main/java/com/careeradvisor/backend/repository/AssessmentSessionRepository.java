package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.AssessmentSession;
import com.careeradvisor.backend.model.AssessmentStatus;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {

    List<AssessmentSession> findByUserOrderByCreatedAtDesc(User user);

    Optional<AssessmentSession> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, AssessmentStatus status);

    List<AssessmentSession> findByStatusAndExpiresAtBefore(AssessmentStatus status, LocalDateTime threshold);

    @Query("SELECT s FROM AssessmentSession s LEFT JOIN FETCH s.selectedTechnologies WHERE s.id = :id")
    Optional<AssessmentSession> findByIdWithTechnologies(@Param("id") Long id);
}
