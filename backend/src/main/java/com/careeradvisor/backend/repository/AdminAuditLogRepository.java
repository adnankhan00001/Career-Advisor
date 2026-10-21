package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditLog;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long>, JpaSpecificationExecutor<AdminAuditLog> {

    long countByCreatedAtAfter(LocalDateTime since);

    long countByTargetType(AdminAuditTargetType targetType);

    long countByTargetTypeIn(Collection<AdminAuditTargetType> targetTypes);

    @Query("SELECT a.action, COUNT(a) FROM AdminAuditLog a GROUP BY a.action")
    List<Object[]> countGroupedByAction();

    List<AdminAuditLog> findTop10ByOrderByCreatedAtDesc();
}
