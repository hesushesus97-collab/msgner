-- Apply on a BACKUP first. No data from the uploaded dump is imported.
CREATE TABLE IF NOT EXISTS user_security (
 user_id INT NOT NULL PRIMARY KEY,
 password_hash VARCHAR(255) DEFAULT NULL,
 hint VARCHAR(255) DEFAULT NULL,
 failed_attempts INT NOT NULL DEFAULT 0,
 locked_until DATETIME DEFAULT NULL,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS auth_sessions (
 token_hash CHAR(64) CHARACTER SET ascii NOT NULL PRIMARY KEY,
 user_id INT NOT NULL,
 device_id VARCHAR(255) NOT NULL,
 expires_at DATETIME NOT NULL,
 UNIQUE KEY auth_user_device (user_id, device_id),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS auth_challenges (
 token_hash CHAR(64) CHARACTER SET ascii NOT NULL PRIMARY KEY,
 user_id INT NOT NULL,
 device_id VARCHAR(255) NOT NULL,
 expires_at DATETIME NOT NULL,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS notification_settings (
 user_id INT NOT NULL PRIMARY KEY,
 mute_all TINYINT(1) NOT NULL DEFAULT 0,
 auto_mute_new TINYINT(1) NOT NULL DEFAULT 0,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Identify existing contacts so enabling auto-mute never changes old chats.
CREATE TABLE IF NOT EXISTS notification_contacts (
 user_id INT NOT NULL,
 peer_id INT NOT NULL,
 PRIMARY KEY (user_id, peer_id),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO notification_contacts (user_id, peer_id)
 SELECT m.receiver_id, m.sender_id FROM messages m JOIN users u ON u.id=m.receiver_id;
INSERT IGNORE INTO notification_contacts (user_id, peer_id)
 SELECT m.sender_id, m.receiver_id FROM messages m JOIN users u ON u.id=m.sender_id WHERE m.sender_type='user';
-- Old clients never had a secret credential. All old logins must sign in again.
