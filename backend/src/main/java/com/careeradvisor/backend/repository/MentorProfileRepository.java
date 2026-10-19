package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.MentorProfile;
import com.careeradvisor.backend.model.MentorVerificationStatus;
import com.careeradvisor.backend.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long> {

    Optional<MentorProfile> findByUserId(Long userId);

    Optional<MentorProfile> findByUser(User user);

    boolean existsByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MentorProfile m WHERE m.id = :id")
    Optional<MentorProfile> findByIdWithLock(@Param("id") Long id);

    List<MentorProfile> findByVerificationStatusAndIsActiveTrue(MentorVerificationStatus verificationStatus);

    List<MentorProfile> findByVerificationStatus(MentorVerificationStatus verificationStatus);

    long countByVerificationStatus(MentorVerificationStatus verificationStatus);

    @Query("SELECT m FROM MentorProfile m WHERE m.verificationStatus = :status AND m.isActive = true " +
           "AND (LOWER(m.jobTitle) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(m.company) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(m.expertiseAreas) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(m.user.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<MentorProfile> searchApprovedMentors(@Param("status") MentorVerificationStatus status, @Param("query") String query);
}
