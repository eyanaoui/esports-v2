-- Migration Script: Advanced Features Suite Database Schema
-- Version: V001
-- Description: Creates new tables and modifies existing user table for:
--   1. Google OAuth 2.0 Authentication
--   2. Signature-Based Authentication
--   3. PDF/Excel Export Functionality
--   4. Advanced Statistics Dashboard
--   5. Account Banning System
-- Requirements: 1.1-1.10, 2.1-2.12, 6.1-6.12, 7.1-7.10, 8.1-8.10, 11.1-11.10

-- ============================================================================
-- STEP 1: Modify existing user table
-- ============================================================================

-- Add new columns to user table for OAuth, signature auth, and ban management
ALTER TABLE user 
ADD COLUMN google_id VARCHAR(255) UNIQUE COMMENT 'Google OAuth user identifier',
ADD COLUMN profile_picture_url TEXT COMMENT 'URL to user profile picture from Google',
ADD COLUMN preferred_auth_method ENUM('PASSWORD', 'GOOGLE', 'SIGNATURE') DEFAULT 'PASSWORD' COMMENT 'User preferred authentication method',
ADD COLUMN is_banned BOOLEAN DEFAULT FALSE COMMENT 'Flag indicating if user account is banned',
ADD COLUMN last_login TIMESTAMP NULL COMMENT 'Timestamp of last successful login';

-- Create indexes for performance optimization
CREATE INDEX idx_user_google ON user(google_id);
CREATE INDEX idx_user_banned ON user(is_banned);
CREATE INDEX idx_user_last_login ON user(last_login);

-- ============================================================================
-- STEP 2: Create oauth_tokens table
-- ============================================================================

CREATE TABLE oauth_tokens (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    user_id INT NOT NULL COMMENT 'Foreign key to user table',
    encrypted_access_token TEXT NOT NULL COMMENT 'AES-256 encrypted OAuth access token',
    encrypted_refresh_token TEXT NOT NULL COMMENT 'AES-256 encrypted OAuth refresh token',
    expires_at TIMESTAMP NOT NULL COMMENT 'Access token expiration timestamp',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record last update timestamp',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_token (user_id) COMMENT 'One token set per user'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Stores encrypted OAuth 2.0 tokens for Google authentication';

-- Index for token expiration queries
CREATE INDEX idx_oauth_expires ON oauth_tokens(expires_at);

-- ============================================================================
-- STEP 3: Create user_signatures table
-- ============================================================================

CREATE TABLE user_signatures (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    user_id INT NOT NULL COMMENT 'Foreign key to user table',
    signature_data MEDIUMBLOB NOT NULL COMMENT 'Binary PNG image data of user signature',
    signature_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of signature for comparison',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record last update timestamp',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_signature (user_id) COMMENT 'One signature per user'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Stores user signatures for signature-based authentication';

-- ============================================================================
-- STEP 4: Create user_bans table
-- ============================================================================

CREATE TABLE user_bans (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    user_id INT NOT NULL COMMENT 'Foreign key to banned user',
    admin_id INT NOT NULL COMMENT 'Foreign key to admin who issued ban',
    reason TEXT NOT NULL COMMENT 'Reason for banning the user',
    banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when ban was issued',
    unbanned_at TIMESTAMP NULL COMMENT 'Timestamp when user was unbanned (NULL if still banned)',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Flag indicating if ban is currently active',
    notes TEXT COMMENT 'Additional notes about the ban',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record last update timestamp',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tracks user ban records and history';

-- Indexes for ban queries
CREATE INDEX idx_bans_user ON user_bans(user_id, is_active);
CREATE INDEX idx_bans_admin ON user_bans(admin_id);

-- ============================================================================
-- STEP 5: Create audit_logs table
-- ============================================================================

CREATE TABLE audit_logs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    action_type ENUM('BAN', 'UNBAN', 'EXPORT', 'LOGIN_FAILURE', 'OAUTH_AUTH', 'SIGNATURE_AUTH') NOT NULL COMMENT 'Type of action being logged',
    user_id INT COMMENT 'Foreign key to user involved in action (nullable for system actions)',
    admin_id INT COMMENT 'Foreign key to admin who performed action (nullable for user actions)',
    details TEXT COMMENT 'JSON or text details about the action',
    ip_address VARCHAR(45) COMMENT 'IP address from which action was performed (supports IPv6)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when action occurred',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
    FOREIGN KEY (admin_id) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable audit trail for administrative and security actions';

-- Indexes for audit log queries
CREATE INDEX idx_audit_action ON audit_logs(action_type, created_at);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_admin ON audit_logs(admin_id);

-- ============================================================================
-- STEP 6: Create signature_auth_attempts table
-- ============================================================================

CREATE TABLE signature_auth_attempts (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    user_id INT NOT NULL COMMENT 'Foreign key to user attempting authentication',
    similarity_score DECIMAL(5,2) COMMENT 'Calculated similarity score (0.00-100.00)',
    success BOOLEAN NOT NULL COMMENT 'Whether authentication attempt succeeded',
    attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp of authentication attempt',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tracks signature authentication attempts for rate limiting and security';

-- Index for rate limiting queries
CREATE INDEX idx_attempts_user_time ON signature_auth_attempts(user_id, attempt_time);

-- ============================================================================
-- STEP 7: Create scheduled_exports table
-- ============================================================================

CREATE TABLE scheduled_exports (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    admin_id INT NOT NULL COMMENT 'Foreign key to admin who created the schedule',
    export_type ENUM('USERS', 'GAMES', 'GUIDES', 'TOURNAMENTS') NOT NULL COMMENT 'Type of data to export',
    export_format ENUM('PDF', 'EXCEL') NOT NULL COMMENT 'Export file format',
    frequency ENUM('DAILY', 'WEEKLY', 'MONTHLY') NOT NULL COMMENT 'Export frequency',
    next_run_at TIMESTAMP NOT NULL COMMENT 'Next scheduled execution time',
    last_run_at TIMESTAMP NULL COMMENT 'Last execution time (NULL if never run)',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether schedule is active',
    email_recipients TEXT COMMENT 'Comma-separated list of email addresses for notifications',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record last update timestamp',
    FOREIGN KEY (admin_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Manages scheduled automatic export jobs';

-- Index for scheduled job execution
CREATE INDEX idx_scheduled_next_run ON scheduled_exports(next_run_at, is_active);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Verify user table modifications
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'esports_db' 
  AND TABLE_NAME = 'user' 
  AND COLUMN_NAME IN ('google_id', 'profile_picture_url', 'preferred_auth_method', 'is_banned', 'last_login');

-- Verify new tables exist
SELECT 
    TABLE_NAME, 
    TABLE_ROWS, 
    CREATE_TIME
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'esports_db' 
  AND TABLE_NAME IN ('oauth_tokens', 'user_signatures', 'user_bans', 'audit_logs', 'signature_auth_attempts', 'scheduled_exports');

-- Verify indexes
SELECT 
    TABLE_NAME, 
    INDEX_NAME, 
    COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'esports_db' 
  AND TABLE_NAME IN ('user', 'oauth_tokens', 'user_signatures', 'user_bans', 'audit_logs', 'signature_auth_attempts', 'scheduled_exports')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================
