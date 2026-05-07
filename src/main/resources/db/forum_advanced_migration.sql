-- Migration avancée module forum (MySQL)
-- Compatible avec les tables existantes du projet: sujets_forum / messages_forum
-- Exécution idempotente: ne recrée pas les colonnes existantes.

SET @db_name = DATABASE();

-- ===== sujets_forum =====
SET @tbl = 'sujets_forum';

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='trending_score'),
    'SELECT 1',
    'ALTER TABLE sujets_forum ADD COLUMN trending_score DOUBLE DEFAULT 0'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='status'),
    'SELECT 1',
    'ALTER TABLE sujets_forum ADD COLUMN status VARCHAR(30) DEFAULT ''ACTIVE'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='auto_summary'),
    'SELECT 1',
    'ALTER TABLE sujets_forum ADD COLUMN auto_summary TEXT NULL'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='keywords'),
    'SELECT 1',
    'ALTER TABLE sujets_forum ADD COLUMN keywords TEXT NULL'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='last_activity'),
    'SELECT 1',
    'ALTER TABLE sujets_forum ADD COLUMN last_activity DATETIME NULL'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='replies_count'),
    'SELECT 1',
    'ALTER TABLE sujets_forum ADD COLUMN replies_count INT DEFAULT 0'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='created_at'),
    'SELECT 1',
    'ALTER TABLE sujets_forum ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== messages_forum =====
SET @tbl = 'messages_forum';

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='status'),
    'SELECT 1',
    'ALTER TABLE messages_forum ADD COLUMN status VARCHAR(30) DEFAULT ''ACCEPTED'''
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='spam_score'),
    'SELECT 1',
    'ALTER TABLE messages_forum ADD COLUMN spam_score DOUBLE DEFAULT 0'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='moderation_reason'),
    'SELECT 1',
    'ALTER TABLE messages_forum ADD COLUMN moderation_reason VARCHAR(255) NULL'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@tbl AND column_name='created_at'),
    'SELECT 1',
    'ALTER TABLE messages_forum ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
