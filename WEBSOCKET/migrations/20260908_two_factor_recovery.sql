-- Apply on a BACKUP first. Chat.php also applies these statements on start-up (errors ignored).

-- Резервная почта для сброса пароля двухфакторной защиты. Хранится отдельно от users.email:
-- код входа и код сброса не должны приходить в один и тот же ящик.
ALTER TABLE user_security ADD COLUMN recovery_email VARCHAR(255) DEFAULT NULL;
-- Адрес, ожидающий подтверждения, sha256 кода подтверждения, срок и число попыток.
ALTER TABLE user_security ADD COLUMN recovery_pending_email VARCHAR(255) DEFAULT NULL;
ALTER TABLE user_security ADD COLUMN recovery_code_hash CHAR(64) DEFAULT NULL;
ALTER TABLE user_security ADD COLUMN recovery_code_expires DATETIME DEFAULT NULL;
ALTER TABLE user_security ADD COLUMN recovery_attempts INT NOT NULL DEFAULT 0;
-- Код сброса пароля защиты (уходит на recovery_email).
ALTER TABLE user_security ADD COLUMN reset_code_hash CHAR(64) DEFAULT NULL;
ALTER TABLE user_security ADD COLUMN reset_code_expires DATETIME DEFAULT NULL;
ALTER TABLE user_security ADD COLUMN reset_attempts INT NOT NULL DEFAULT 0;
