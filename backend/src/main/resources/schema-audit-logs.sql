-- =============================================================================
-- Career Advisor: Phase 5 Centralized Administrative Audit Logging Migration
-- Target Table: admin_audit_logs
-- Engine: InnoDB, Charset: utf8mb4
-- =============================================================================

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NULL,
    admin_name VARCHAR(150) NULL,
    admin_email VARCHAR(150) NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    details_json TEXT NULL,
    ip_address VARCHAR(64) NULL,
    correlation_id VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_created_at (created_at),
    INDEX idx_audit_action (action),
    INDEX idx_audit_admin_user (admin_user_id),
    INDEX idx_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
