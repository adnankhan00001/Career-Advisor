package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUser(User user);
    List<UserSkill> findByUserOrderByAddedAtDesc(User user);
    long countByUser(User user);
    Optional<UserSkill> findByUserAndSkillNameIgnoreCase(User user, String skillName);
    boolean existsByUserAndSkillNameIgnoreCase(User user, String skillName);
    void deleteByUserAndSkillNameIgnoreCase(User user, String skillName);
}
