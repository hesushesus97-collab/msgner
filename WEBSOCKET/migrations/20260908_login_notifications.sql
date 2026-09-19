-- Apply on a BACKUP first. Chat.php also applies both statements on start-up (errors ignored).

-- Язык интерфейса пользователя (ru/en). Клиент присылает его в auth_connect;
-- на этом языке Vibe cat пишет коды входа и уведомления о входе на все устройства аккаунта.
ALTER TABLE users ADD COLUMN language VARCHAR(8) DEFAULT NULL;

-- Флаг «о входе уже сообщили». Для уже существующих сессий = 1, чтобы после обновления
-- сервера никто не получил уведомление о давнем входе. Новые сессии создаются с 0 и
-- переводятся в 1 первым auth_connect, который и отправляет сообщение.
ALTER TABLE auth_sessions ADD COLUMN login_notified TINYINT(1) NOT NULL DEFAULT 1;
