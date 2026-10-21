package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.dto.CategoryCountProjection;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    List<User> findByRole(Role role);
    long countByLatestQuizScoreIsNotNull();
    long countByRole(Role role);
    long countByRoleAndStatus(Role role, UserStatus status);
    long countByStatus(UserStatus status);
    long countByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT u.careerGoal AS category, COUNT(u) AS count FROM User u " +
           "WHERE u.careerGoal IS NOT NULL AND TRIM(u.careerGoal) != '' " +
           "GROUP BY u.careerGoal")
    List<CategoryCountProjection> countGroupedByCareerGoal();

    @Query("SELECT u.userLevel AS category, COUNT(u) AS count FROM User u " +
           "WHERE u.userLevel IS NOT NULL AND TRIM(u.userLevel) != '' " +
           "GROUP BY u.userLevel")
    List<CategoryCountProjection> countGroupedByUserLevel();

    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countGroupedByRole();

    @Query("SELECT u.status, COUNT(u) FROM User u GROUP BY u.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT FUNCTION('DATE', u.createdAt), COUNT(u) FROM User u WHERE u.createdAt >= :startDate GROUP BY FUNCTION('DATE', u.createdAt) ORDER BY FUNCTION('DATE', u.createdAt) ASC")
    List<Object[]> countRegistrationsByDateSince(@Param("startDate") LocalDateTime startDate);
}