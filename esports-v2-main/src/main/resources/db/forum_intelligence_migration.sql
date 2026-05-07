-- Forum intelligence migration (compatible MySQL/MariaDB)
SET @db_name = DATABASE();

SET @topic_table = (
    SELECT CASE
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=@db_name AND table_name='sujets_forum') THEN 'sujets_forum'
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=@db_name AND table_name='sujetforum') THEN 'sujetforum'
        ELSE NULL
    END
);

SET @message_table = (
    SELECT CASE
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=@db_name AND table_name='messages_forum') THEN 'messages_forum'
        WHEN EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=@db_name AND table_name='messageforum') THEN 'messageforum'
        ELSE NULL
    END
);

SET @sql = IF(@topic_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@topic_table AND column_name='status'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @topic_table, ' ADD COLUMN status VARCHAR(30) DEFAULT ''ACTIVE''')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@topic_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@topic_table AND column_name='last_activity'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @topic_table, ' ADD COLUMN last_activity DATETIME NULL')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@topic_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@topic_table AND column_name='archived_at'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @topic_table, ' ADD COLUMN archived_at DATETIME NULL')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@topic_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@topic_table AND column_name='archive_reason'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @topic_table, ' ADD COLUMN archive_reason VARCHAR(255) NULL')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@message_table IS NULL, 'SELECT 1',
    IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=@db_name AND table_name=@message_table AND column_name='report_count'),
        'SELECT 1',
        CONCAT('ALTER TABLE ', @message_table, ' ADD COLUMN report_count INT DEFAULT 0')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS forum_user_reputation (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    score INT DEFAULT 0,
    level VARCHAR(30) DEFAULT 'BRONZE',
    messages_count INT DEFAULT 0,
    likes_received INT DEFAULT 0,
    best_answers_count INT DEFAULT 0,
    rejected_messages_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    UNIQUE KEY uq_forum_user_reputation_user (user_id)
);

CREATE TABLE IF NOT EXISTS forum_user_badge (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    badge_name VARCHAR(50) NOT NULL,
    badge_description VARCHAR(255),
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forum_favorite_topic (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    sujet_id INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_favorite_user_topic (user_id, sujet_id)
);

SET @topic_pk = (
    SELECT COLUMN_NAME
    FROM information_schema.columns
    WHERE table_schema=@db_name AND table_name=@topic_table
    ORDER BY CASE WHEN COLUMN_KEY='PRI' THEN 0 ELSE 1 END, ORDINAL_POSITION
    LIMIT 1
);
SET @has_fav_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='forum_favorite_topic' AND COLUMN_NAME='sujet_id' AND REFERENCED_TABLE_NAME IS NOT NULL
);
SET @sql = IF(@has_fav_fk > 0 OR @topic_table IS NULL OR @topic_pk IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE forum_favorite_topic ADD CONSTRAINT fk_forum_favorite_topic_sujet FOREIGN KEY (sujet_id) REFERENCES ', @topic_table, '(', @topic_pk, ') ON DELETE CASCADE')
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS forum_report (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    sujet_id INT NULL,
    message_id INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description TEXT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL
);

SET @message_pk = (
    SELECT COLUMN_NAME
    FROM information_schema.columns
    WHERE table_schema=@db_name AND table_name=@message_table
    ORDER BY CASE WHEN COLUMN_KEY='PRI' THEN 0 ELSE 1 END, ORDINAL_POSITION
    LIMIT 1
);
SET @has_report_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='forum_report' AND COLUMN_NAME='message_id' AND REFERENCED_TABLE_NAME IS NOT NULL
);
SET @sql = IF(@has_report_fk > 0 OR @message_table IS NULL OR @message_pk IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE forum_report ADD CONSTRAINT fk_forum_report_message FOREIGN KEY (message_id) REFERENCES ', @message_table, '(', @message_pk, ') ON DELETE CASCADE')
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
