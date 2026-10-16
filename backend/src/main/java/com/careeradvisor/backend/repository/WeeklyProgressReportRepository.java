package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.WeeklyProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyProgressReportRepository extends JpaRepository<WeeklyProgressReport, Long> {
    Optional<WeeklyProgressReport> findFirstByUserOrderByGeneratedAtDesc(User user);
    List<WeeklyProgressReport> findByUserOrderByPeriodStartDesc(User user);
    Optional<WeeklyProgressReport> findByUserAndPeriodStart(User user, LocalDate periodStart);
}
