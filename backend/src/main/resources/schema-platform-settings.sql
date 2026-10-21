-- ==============================================================================
-- Career Advisor Platform Settings Schema Reference
-- Table: platform_settings
-- Purpose: Persistent, transactional, audited storage for system configuration
-- ==============================================================================

CREATE TABLE IF NOT EXISTS platform_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(255) NULL,
    updated_by VARCHAR(150) NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_platform_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial default settings for Mentorship Capacity
INSERT INTO platform_settings (setting_key, setting_value, description, updated_by, updated_at)
VALUES
    ('mentorship.capacity.min_mentees', '1', 'Minimum mentee capacity per mentor', 'system', NOW()),
    ('mentorship.capacity.max_mentees', '10', 'Maximum mentee capacity per mentor', 'system', NOW()),
    ('mentorship.capacity.default_mentees', '3', 'Default mentee capacity assigned to approved mentors', 'system', NOW())
ON DUPLICATE KEY UPDATE setting_key = setting_key;
