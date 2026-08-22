package com.careeradvisor.backend.ai.repository;

import com.careeradvisor.backend.ai.model.AiUsageLog;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    List<AiUsageLog> findByUserOrderByCreatedAtDesc(User user);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(l.totalTokens), 0) FROM AiUsageLog l")
    long getTotalTokensConsumed();
}
