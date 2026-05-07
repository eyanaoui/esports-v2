-- Forum social features migration (idempotent, MySQL)
SET @db_name = DATABASE();

-- Detect table names used by this project
SET @message_table = (
    SELECT CASE
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=@db_name AND table_name='messages_forum') THEN 'messages_forum'
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=@db_name AND table_name='messageforum') THEN 'messageforum'
        ELSE NULL
    END
);

SET @topic_table = (
    SELECT CASE
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=@db_name AND table_name='sujets_forum') THEN 'sujets_forum'
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=@db_name AND table_name='sujetforum') THEN 'sujetforum'
        ELSE NULL
    END
);

-- ===== message table columns =====
SET @sql = IF(@message_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@message_table AND column_name='likes'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @message_table, ' ADD COLUMN likes INT DEFAULT 0')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@message_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@message_table AND column_name='dislikes'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @message_table, ' ADD COLUMN dislikes INT DEFAULT 0')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@message_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@message_table AND column_name='is_best'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @message_table, ' ADD COLUMN is_best BOOLEAN DEFAULT FALSE')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@message_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@message_table AND column_name='file_path'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @message_table, ' ADD COLUMN file_path VARCHAR(255) NULL')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@message_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@message_table AND column_name='updated_at'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @message_table, ' ADD COLUMN updated_at DATETIME NULL')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== topic table columns =====
SET @sql = IF(@topic_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@topic_table AND column_name='is_pinned'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @topic_table, ' ADD COLUMN is_pinned BOOLEAN DEFAULT FALSE')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@topic_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@topic_table AND column_name='views_count'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @topic_table, ' ADD COLUMN views_count INT DEFAULT 0')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@topic_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@topic_table AND column_name='updated_at'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @topic_table, ' ADD COLUMN updated_at DATETIME NULL')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Resolve message PK column for FK usage
SET @message_pk = (
    SELECT COLUMN_NAME
    FROM information_schema.columns
    WHERE table_schema=@db_name AND table_name=@message_table
    ORDER BY CASE WHEN COLUMN_KEY='PRI' THEN 0 ELSE 1 END, ORDINAL_POSITION
    LIMIT 1
);

-- ===== history / notifications / activity / scores =====
CREATE TABLE IF NOT EXISTS message_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message_id INT NOT NULL,
    old_content TEXT NOT NULL,
    new_content TEXT NULL,
    date_modif DATETIME DEFAULT CURRENT_TIMESTAMP
);

SET @has_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='message_history' AND COLUMN_NAME='message_id' AND REFERENCED_TABLE_NAME IS NOT NULL
);
SET @sql = IF(@has_fk > 0 OR @message_table IS NULL OR @message_pk IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE message_history ADD CONSTRAINT fk_message_history_message FOREIGN KEY (message_id) REFERENCES ', @message_table, '(', @message_pk, ') ON DELETE CASCADE')
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS forum_notification (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    sujet_id INT NULL,
    message_id INT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forum_activity (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    sujet_id INT NULL,
    message_id INT NULL,
    action_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forum_user_score (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    score INT DEFAULT 0,
    messages_count INT DEFAULT 0,
    best_answers_count INT DEFAULT 0,
    likes_received INT DEFAULT 0,
    UNIQUE KEY uq_forum_user_score_user (user_id)
);
