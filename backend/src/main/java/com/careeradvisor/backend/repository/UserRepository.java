package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    List<User> findByRole(Role role);
    long countByLatestQuizScoreIsNotNull();
    long countByRole(Role role);
}