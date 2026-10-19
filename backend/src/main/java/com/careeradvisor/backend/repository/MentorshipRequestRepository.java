package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.MentorshipRequest;
import com.careeradvisor.backend.model.MentorshipRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorshipRequestRepository extends JpaRepository<MentorshipRequest, Long> {

    List<MentorshipRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<MentorshipRequest> findByMentorIdOrderByCreatedAtDesc(Long mentorProfileId);

    List<MentorshipRequest> findByMentorIdAndStatusOrderByCreatedAtDesc(Long mentorProfileId, MentorshipRequestStatus status);

    Optional<MentorshipRequest> findByIdAndStudentId(Long id, Long studentId);

    Optional<MentorshipRequest> findByIdAndMentorId(Long id, Long mentorProfileId);

    boolean existsByStudentIdAndMentorIdAndStatusIn(Long studentId, Long mentorProfileId, List<MentorshipRequestStatus> statuses);

    long countByMentorIdAndStatus(Long mentorProfileId, MentorshipRequestStatus status);

    long countByStudentIdAndStatus(Long studentId, MentorshipRequestStatus status);
}
