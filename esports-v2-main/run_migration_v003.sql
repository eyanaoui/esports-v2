-- Migration V003: Add phone number and password reset functionality
-- Execute this file in MySQL to apply the migration

USE esports_db;

-- Add phone number field to user table for SMS-based password reset
ALTER TABLE user ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20) AFTER email;

-- Create table for password reset tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SELECT 'Migration V003 completed successfully!' AS status;
