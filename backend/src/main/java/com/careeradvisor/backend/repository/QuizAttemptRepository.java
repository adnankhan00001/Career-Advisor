package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.QuizAttempt;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserOrderByCreatedAtDesc(User user);
    Optional<QuizAttempt> findFirstByUserOrderByCreatedAtDesc(User user);
}
