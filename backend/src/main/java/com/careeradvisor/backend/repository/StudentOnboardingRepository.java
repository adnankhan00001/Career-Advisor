package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.StudentOnboarding;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentOnboardingRepository extends JpaRepository<StudentOnboarding, Long> {

    Optional<StudentOnboarding> findByUser(User user);

    Optional<StudentOnboarding> findByUserId(Long userId);

    @Query("SELECT o FROM StudentOnboarding o " +
           "LEFT JOIN FETCH o.selectedCareer " +
           "LEFT JOIN FETCH o.selectedTechnologies " +
           "WHERE o.user.id = :userId")
    Optional<StudentOnboarding> findByUserIdWithCareerAndTech(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
