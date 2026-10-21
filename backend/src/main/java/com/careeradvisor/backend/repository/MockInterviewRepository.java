package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.model.MockInterview;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MockInterviewRepository extends JpaRepository<MockInterview, Long> {

    List<MockInterview> findByUserOrderByStartedAtDesc(User user);

    List<MockInterview> findByUserAndStatusOrderByStartedAtDesc(User user, InterviewStatus status);

    Optional<MockInterview> findFirstByUserAndStatusOrderByStartedAtDesc(User user, InterviewStatus status);

    Optional<MockInterview> findFirstByUserOrderByStartedAtDesc(User user);

    long countByUser(User user);

    long countByUserAndStatus(User user, InterviewStatus status);

    long countByStatus(InterviewStatus status);

    long countByStartedAtAfter(LocalDateTime since);

    @Query("SELECT mi.user.id, COUNT(mi) FROM MockInterview mi WHERE mi.user.id IN :userIds GROUP BY mi.user.id")
    List<Object[]> countInterviewsByUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT FUNCTION('DATE', m.startedAt), COUNT(m) FROM MockInterview m WHERE m.startedAt >= :startDate GROUP BY FUNCTION('DATE', m.startedAt) ORDER BY FUNCTION('DATE', m.startedAt) ASC")
    List<Object[]> countInterviewsByDateSince(@Param("startDate") LocalDateTime startDate);
}
