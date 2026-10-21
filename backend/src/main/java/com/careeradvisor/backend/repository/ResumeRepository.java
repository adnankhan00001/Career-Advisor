package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.Resume;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserOrderByUploadTimestampDesc(User user);

    Optional<Resume> findFirstByUserOrderByUploadTimestampDesc(User user);

    Optional<Resume> findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);

    long countByUser(User user);

    long countByUploadTimestampAfter(LocalDateTime since);

    @Query("SELECT DISTINCT r.user.id FROM Resume r WHERE r.user.id IN :userIds")
    List<Long> findUserIdsWithResumes(@Param("userIds") List<Long> userIds);

    @Query("SELECT FUNCTION('DATE', r.uploadTimestamp), COUNT(r) FROM Resume r WHERE r.uploadTimestamp >= :startDate GROUP BY FUNCTION('DATE', r.uploadTimestamp) ORDER BY FUNCTION('DATE', r.uploadTimestamp) ASC")
    List<Object[]> countUploadsByDateSince(@Param("startDate") LocalDateTime startDate);
}
