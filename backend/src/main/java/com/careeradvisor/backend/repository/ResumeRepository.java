package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.Resume;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserOrderByUploadTimestampDesc(User user);

    Optional<Resume> findFirstByUserOrderByUploadTimestampDesc(User user);

    Optional<Resume> findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);

    long countByUser(User user);
}
