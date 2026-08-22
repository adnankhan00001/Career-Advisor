package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.model.MockInterview;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
