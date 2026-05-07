-- Migration Script: Signature Authentication Audit Log
-- Version: V002
-- Description: Creates audit_log table specifically for signature authentication events
-- Requirements: 5.1, 6.5, 10.1, 10.2, 10.3, 10.4, 10.5

-- ============================================================================
-- Create audit_log table for signature authentication
-- ============================================================================

CREATE TABLE audit_log (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    user_id INT NOT NULL COMMENT 'Foreign key to user associated with event',
    event_type VARCHAR(50) NOT NULL COMMENT 'Type of event: REGISTRATION, UPDATE, AUTH_SUCCESS, AUTH_FAILURE, RATE_LIMITED, LOCKED_OUT',
    account_identifier VARCHAR(255) NOT NULL COMMENT 'Email or username used for authentication',
    similarity_score DOUBLE COMMENT 'Similarity score for authentication events (NULL for registration/update)',
    device_info TEXT COMMENT 'Device/browser information',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When event occurred',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audit trail for signature authentication events';

-- Indexes for efficient querying
CREATE INDEX idx_audit_user_timestamp ON audit_log(user_id, timestamp);
CREATE INDEX idx_audit_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Verify audit_log table exists
SELECT 
    TABLE_NAME, 
    TABLE_ROWS, 
    CREATE_TIME
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'esports_db' 
  AND TABLE_NAME = 'audit_log';

-- Verify indexes
SELECT 
    TABLE_NAME, 
    INDEX_NAME, 
    COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'esports_db' 
  AND TABLE_NAME = 'audit_log'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================
