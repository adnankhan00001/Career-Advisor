package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.MentorshipRequest;
import com.careeradvisor.backend.model.MentorshipRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MentorshipRequestRepository extends JpaRepository<MentorshipRequest, Long>, JpaSpecificationExecutor<MentorshipRequest> {

    List<MentorshipRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<MentorshipRequest> findByMentorIdOrderByCreatedAtDesc(Long mentorProfileId);

    List<MentorshipRequest> findByMentorIdAndStatusOrderByCreatedAtDesc(Long mentorProfileId, MentorshipRequestStatus status);

    Optional<MentorshipRequest> findByIdAndStudentId(Long id, Long studentId);

    Optional<MentorshipRequest> findByIdAndMentorId(Long id, Long mentorProfileId);

    boolean existsByStudentIdAndMentorIdAndStatusIn(Long studentId, Long mentorProfileId, List<MentorshipRequestStatus> statuses);

    long countByMentorIdAndStatus(Long mentorProfileId, MentorshipRequestStatus status);

    long countByStudentIdAndStatus(Long studentId, MentorshipRequestStatus status);

    long countByStatus(MentorshipRequestStatus status);

    long countByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT r.status, COUNT(r) FROM MentorshipRequest r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT FUNCTION('DATE', r.createdAt), COUNT(r) FROM MentorshipRequest r WHERE r.createdAt >= :startDate GROUP BY FUNCTION('DATE', r.createdAt) ORDER BY FUNCTION('DATE', r.createdAt) ASC")
    List<Object[]> countRequestsByDateSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(DISTINCT r.mentor.id) FROM MentorshipRequest r WHERE r.status = :status")
    long countDistinctMentorsByStatus(@Param("status") MentorshipRequestStatus status);

    @Query("SELECT r.createdAt, r.updatedAt FROM MentorshipRequest r WHERE r.status IN :statuses AND r.updatedAt IS NOT NULL AND r.updatedAt > r.createdAt")
    List<Object[]> findResponseTimestamps(@Param("statuses") List<MentorshipRequestStatus> statuses);
}
