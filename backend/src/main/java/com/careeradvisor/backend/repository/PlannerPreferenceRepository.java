package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.PlannerPreference;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlannerPreferenceRepository extends JpaRepository<PlannerPreference, Long> {
    Optional<PlannerPreference> findByUser(User user);
}
