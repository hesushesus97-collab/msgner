<?php

require_once __DIR__ . '/VibeSecurity.php';

use Ratchet\MessageComponentInterface;
use Ratchet\ConnectionInterface;

class Chat implements MessageComponentInterface {
    protected $clients;
    protected $db;
    protected $userConnections; // userId => ConnectionInterface
    private $lastFreezeCheck = 0;
    private $lastMaxMessageId = null;
    private $lastMaxActionId = null;
    private $callbackRateLimits = []; // userId => [ts1, ts2, ...]
    private $pendingCallbacks = [];   // callbackId => ['user_id' => int, 'bot_id' => int, 'message_id' => int]
    private $lastCallbackId = 0;
    // Анти-флуд для публичных (неавторизованных) команд: "scope|key" => [ts, ts, ...]
    private $floodBuckets = [];
    private $floodBans = [];          // ip => unix time окончания бана
    private $lastFloodPrune = 0;

    public function __construct() {
        $this->clients = new \SplObjectStorage;
        $this->userConnections = [];
        $this->connectDB();
        $this->loadFcmCredentials();
        echo "✅ VibeMessenger WebSocket Server (Ratchet) запущен!\n";
    }

    private $fcmProjectId;
    private $fcmClientEmail;
    private $fcmPrivateKey;
    private $fcmAccessToken;
    private $fcmTokenExpiry = 0;

    private function loadFcmCredentials() {
        $keyFile = __DIR__ . '/firebase-service-account.json';
        if (file_exists($keyFile)) {
            $json = json_decode(file_get_contents($keyFile), true);
            $this->fcmProjectId = $json['project_id'] ?? '';
            $this->fcmClientEmail = $json['client_email'] ?? '';
            $this->fcmPrivateKey = $json['private_key'] ?? '';
            echo "[FCM] Credentials loaded for project: {$this->fcmProjectId}\n";
        } else {
            echo "[FCM] WARNING: firebase-service-account.json not found!\n";
        }
    }

    private function base64UrlEncode($data) {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private function getAccessToken() {
        if ($this->fcmAccessToken && time() < $this->fcmTokenExpiry - 60) {
            return $this->fcmAccessToken;
        }

        $now = time();
        $header = $this->base64UrlEncode(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
        $payload = $this->base64UrlEncode(json_encode([
            'iss' => $this->fcmClientEmail,
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud' => 'https://oauth2.googleapis.com/token',
            'iat' => $now,
            'exp' => $now + 3600
        ]));

        $signatureInput = "$header.$payload";
        $signature = '';
        openssl_sign($signatureInput, $signature, $this->fcmPrivateKey, OPENSSL_ALGO_SHA256);
        $jwt = $signatureInput . '.' . $this->base64UrlEncode($signature);

        $ch = curl_init('https://oauth2.googleapis.com/token');
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => http_build_query([
                'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
                'assertion' => $jwt
            ]),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 10
        ]);
        $response = curl_exec($ch);
        curl_close($ch);

        $result = json_decode($response, true);
        if (isset($result['access_token'])) {
            $this->fcmAccessToken = $result['access_token'];
            $this->fcmTokenExpiry = $now + ($result['expires_in'] ?? 3600);
            echo "[FCM] Access token obtained\n";
            return $this->fcmAccessToken;
        }

        echo "[FCM] Failed to get access token: $response\n";
        return null;
    }

    private function isNotificationMuted($userId,$peerId) {
        $q=$this->getDB()->prepare('SELECT u.id FROM users u WHERE u.id=? AND u.is_banned=0 AND u.is_freezed=0 AND NOT EXISTS (SELECT 1 FROM notification_settings n WHERE n.user_id=u.id AND n.mute_all=1) AND NOT EXISTS (SELECT 1 FROM muted_users m WHERE m.user_id=u.id AND m.muted_id=?)');
        $q->execute([$userId,$peerId]); return !$q->fetchColumn();
    }
    private function sendFcmNotification($userId, $senderName, $content, $senderId, $action = 'new') {
        (new VibeSecurity($this->getDB()))->registerContact($userId,$senderId);
        if ($this->isNotificationMuted($userId,$senderId) || !$this->fcmProjectId) return;

        try {
            // Check if muted
            $muteStmt = $this->getDB()->prepare("SELECT 1 FROM muted_users WHERE user_id = ? AND muted_id = ?");
            $muteStmt->execute([$userId, $senderId]);
            if ($muteStmt->fetch()) return;
            $stmt = $this->getDB()->prepare("SELECT token FROM device_tokens WHERE user_id = ?");
            $stmt->execute([$userId]);
            $tokens = $stmt->fetchAll(PDO::FETCH_COLUMN);

            if (empty($tokens)) return;

            $accessToken = $this->getAccessToken();
            if (!$accessToken) return;

            $url = "https://fcm.googleapis.com/v1/projects/{$this->fcmProjectId}/messages:send";

            foreach ($tokens as $deviceToken) {
                $message = [
                    'message' => [
                        'token' => $deviceToken,
                        'data' => [
                            'action' => $action,
                            'sender_name' => $senderName,
                            'content' => $content,
                            'sender_id' => (string)$senderId,
                            'receiver_id' => (string)$userId
                        ]
                    ]
                ];

                $ch = curl_init($url);
                curl_setopt_array($ch, [
                    CURLOPT_POST => true,
                    CURLOPT_HTTPHEADER => [
                        'Authorization: Bearer ' . $accessToken,
                        'Content-Type: application/json'
                    ],
                    CURLOPT_POSTFIELDS => json_encode($message),
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_TIMEOUT => 10
                ]);
                $result = curl_exec($ch);
                $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                curl_close($ch);

                if ($httpCode === 200) {
                    echo "[FCM] Push sent to user $userId (token: ..." . substr($deviceToken, -8) . ")\n";
                } else {
                    echo "[FCM] Push failed ($httpCode): $result\n";
                    // Remove invalid tokens
                    if ($httpCode === 404 || $httpCode === 400) {
                        $del = $this->getDB()->prepare("DELETE FROM device_tokens WHERE token = ?");
                        $del->execute([$deviceToken]);
                        echo "[FCM] Removed invalid token\n";
                    }
                }
            }
        } catch (\Exception $e) {
            echo "[FCM Error] " . $e->getMessage() . "\n";
        }
    }

    private function sendEmailToRabbitMQ($email, $subject, $messageBody) {
        try {
            $host = defined('RABBITMQ_HOST') ? RABBITMQ_HOST : 'rabbitmq-flasskdev.alwaysdata.net';
            $port = defined('RABBITMQ_PORT') ? RABBITMQ_PORT : 5672;
            $user = defined('RABBITMQ_USER') ? RABBITMQ_USER : 'flasskdev';
            $pass = defined('RABBITMQ_PASS') ? RABBITMQ_PASS : (getenv('RABBITMQ_PASS') ?: '31052019RoG+');
            $vhost = defined('RABBITMQ_VHOST') ? RABBITMQ_VHOST : 'flasskdev_mobile';

            $connection = new \PhpAmqpLib\Connection\AMQPStreamConnection($host, $port, $user, $pass, $vhost);
            $channel = $connection->channel();
            
            $queueName = 'email_queue';
            $channel->queue_declare($queueName, false, true, false, false);
            
            $headers = "From: noreply@flasskdev.alwaysdata.net\r\n" .
                       "Reply-To: noreply@flasskdev.alwaysdata.net\r\n" .
                       "Content-Type: text/plain; charset=UTF-8\r\n" .
                       "X-Mailer: PHP/" . phpversion();

            $data = json_encode([
                'email' => $email,
                'subject' => $subject,
                'message' => $messageBody,
                'headers' => $headers
            ]);
            
            $msg = new \PhpAmqpLib\Message\AMQPMessage($data, ['delivery_mode' => 2]); // Persistent
            $channel->basic_publish($msg, '', $queueName);
            
            $channel->close();
            $connection->close();
            return true;
        } catch (\Exception $e) {
            echo "[RabbitMQ Error] Не удалось отправить письмо в очередь: " . $e->getMessage() . "\n";
            return false;
        }
    }

    private function connectDB() {
        try {
            $this->db = new PDO(
                "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4",
                DB_USER,
                DB_PASS,
                [
                    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                    PDO::ATTR_EMULATE_PREPARES => false
                ]
            );
            
            // Migration: Add avatar_url column if not exists
            try {
                $this->db->exec("ALTER TABLE users ADD COLUMN avatar_url VARCHAR(255) DEFAULT NULL");
            } catch (\PDOException $e) {
                // Ignore, column likely exists
            }

            // Migration: Add attempts column to pending_users
            try {
                $this->db->exec("ALTER TABLE pending_users ADD COLUMN attempts INT DEFAULT 0");
            } catch (\PDOException $e) {
                // Ignore, column likely exists
            }

            // Migration: Add about column if not exists
            try {
                $this->db->exec("ALTER TABLE users ADD COLUMN about TEXT DEFAULT NULL");
            } catch (\PDOException $e) {
                // Ignore, column likely exists
            }

            // Migration: язык интерфейса пользователя — на нём Vibe cat пишет системные сообщения
            try { $this->db->exec("ALTER TABLE users ADD COLUMN language VARCHAR(8) DEFAULT NULL"); } catch (\PDOException $e) {}

            // Migration: флаг «о входе уже сообщили». Для существующих сессий = 1, чтобы после
            // обновления сервера никто не получил уведомление о давнем входе.
            try { $this->db->exec("ALTER TABLE auth_sessions ADD COLUMN login_notified TINYINT(1) NOT NULL DEFAULT 1"); } catch (\PDOException $e) {}

            // Migration: 20260908_two_factor_recovery.sql — резервная почта и сброс второго фактора
            try { $this->db->exec("ALTER TABLE user_security ADD COLUMN recovery_email VARCHAR(255) DEFAULT NULL"); } catch (\PDOException $e) {}
            try { $this->db->exec("ALTER TABLE user_security ADD COLUMN recovery_pending_email VARCHAR(255) DEFAULT NULL"); } catch (\PDOException $e) {}
            try { $this->db->exec("ALTER TABLE user_security ADD COLUMN recovery_code_hash CHAR(64) DEFAULT NULL"); } catch (\PDOException $e) {}
            try { $this->db->exec("ALTER TABLE user_security ADD COLUMN recovery_code_expires DATETIME DEFAULT NULL"); } catch (\PDOException $e) {}
            try { $this->db->exec("ALTER TABLE user_security ADD COLUMN recovery_attempts INT NOT NULL DEFAULT 0"); } catch (\PDOException $e) {}
            try { $this->db->exec("ALTER TABLE user_security ADD COLUMN reset_code_hash CHAR(64) DEFAULT NULL"); } catch (\PDOException $e) {}
            try { $this->db->exec("ALTER TABLE user_security ADD COLUMN reset_code_expires DATETIME DEFAULT NULL"); } catch (\PDOException $e) {}
            try { $this->db->exec("ALTER TABLE user_security ADD COLUMN reset_attempts INT NOT NULL DEFAULT 0"); } catch (\PDOException $e) {}
            
            echo "[DB] Подключено.\n";
            
            // Migrate table to add forwarded_from_id if needed
            try {
                $this->db->exec("ALTER TABLE messages ADD COLUMN forwarded_from_id INT NULL");
                echo "[DB] Добавлена колонка forwarded_from_id.\n";
            } catch (\PDOException $e) {
                // Column likely already exists
            }
            
            // Migrate table to add attachments if needed
            try {
                $this->db->exec("ALTER TABLE messages ADD COLUMN attachments JSON NULL");
                echo "[DB] Добавлена колонка attachments.\n";
            } catch (\PDOException $e) {
                // Column likely already exists
            }

            // Migrate table to add reply_markup if needed
            try {
                $this->db->exec("ALTER TABLE messages ADD COLUMN reply_markup JSON NULL");
                echo "[DB] Добавлена колонка reply_markup.\n";
            } catch (\PDOException $e) {
                // Column likely already exists
            }

            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS flasskdev_mobilestickerpacks (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(128) NOT NULL,
                    owner INT NOT NULL,
                    stickers LONGTEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_sticker_packs_owner (owner),
                    INDEX idx_sticker_packs_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
                $this->db->exec("CREATE TABLE IF NOT EXISTS flasskdev_mobileuserstickerpacks (
                    user_id INT NOT NULL,
                    pack_id INT NOT NULL,
                    position INT NOT NULL DEFAULT 0,
                    installed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, pack_id),
                    INDEX idx_user_sticker_packs_position (user_id, position),
                    INDEX idx_user_sticker_packs_pack (pack_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
            } catch (\PDOException $e) {
                echo "[DB Error sticker packs] " . $e->getMessage() . "\n";
            }

            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS bot_updates (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    bot_id INT NOT NULL,
                    update_type VARCHAR(32) NOT NULL,
                    sender_id INT NOT NULL,
                    payload JSON NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_bot_offset (bot_id, id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
            } catch (\PDOException $e) {}

            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS bot_callbacks (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    bot_id INT NOT NULL,
                    user_id INT NOT NULL,
                    message_id INT NOT NULL,
                    data TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
            } catch (\PDOException $e) {}

            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS bot_callback_answers (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    callback_id INT NOT NULL,
                    user_id INT NOT NULL,
                    text TEXT NULL,
                    show_alert TINYINT(1) DEFAULT 0,
                    is_delivered TINYINT(1) DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
            } catch (\PDOException $e) {}
            
            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS pinned_messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    message_id INT NOT NULL,
                    pinned_by_id INT NOT NULL,
                    pinned_for_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
                echo "[DB] Таблица pinned_messages проверена/создана.\n";
            } catch (\PDOException $e) {
                echo "[DB Error pinned_messages] " . $e->getMessage() . "\n";
            }
            
            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS sessions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    device_id VARCHAR(255) NOT NULL,
                    device_name VARCHAR(255),
                    os_version VARCHAR(255),
                    location VARCHAR(255),
                    last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY (user_id, device_id),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
                echo "[DB] Таблица sessions проверена/создана.\n";
            } catch (\PDOException $e) {
                echo "[DB Error sessions] " . $e->getMessage() . "\n";
            }
            
            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS privacy_settings (
                    user_id INT PRIMARY KEY,
                    activity VARCHAR(20) DEFAULT 'EVERYONE',
                    activity_users TEXT,
                    avatar VARCHAR(20) DEFAULT 'EVERYONE',
                    avatar_users TEXT,
                    forwarded VARCHAR(20) DEFAULT 'EVERYONE',
                    forwarded_users TEXT,
                    messages VARCHAR(20) DEFAULT 'EVERYONE',
                    messages_users TEXT,
                    status VARCHAR(20) DEFAULT 'EVERYONE',
                    status_users TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
                echo "[DB] Таблица privacy_settings проверена/создана.\n";
            } catch (\PDOException $e) {
                echo "[DB Error privacy_settings] " . $e->getMessage() . "\n";
            }
            
            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS chat_actions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    sender_id INT NOT NULL,
                    receiver_id INT NOT NULL,
                    sender_type VARCHAR(20) NOT NULL,
                    action VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
                echo "[DB] Таблица chat_actions проверена/создана.\n";
            } catch (\PDOException $e) {
                echo "[DB Error chat_actions] " . $e->getMessage() . "\n";
            }

            try {
                $this->db->exec("CREATE TABLE IF NOT EXISTS blocked_users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    blocked_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY (user_id, blocked_id),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");
                echo "[DB] Таблица blocked_users проверена/создана.\n";
            } catch (\PDOException $e) {
                echo "[DB Error blocked_users] " . $e->getMessage() . "\n";
            }
            
        } catch (PDOException $e) {
            echo "[DB Error] " . $e->getMessage() . "\n";
        }
    }

    private function getDB() {
        try {
            $this->db->query("SELECT 1");
        } catch (Exception $e) {
            echo "[DB] Переподключение...\n";
            $this->connectDB();
        }
        return $this->db;
    }

    private function decodeStickers($raw) {
        $list = json_decode($raw ?: '[]', true);
        if (!is_array($list)) return [];

        $stickers = [];
        foreach ($list as $sticker) {
            if (is_string($sticker) && $sticker !== '') {
                $stickers[] = [
                    'id' => (string)count($stickers),
                    'path' => $sticker
                ];
            } elseif (is_array($sticker) && isset($sticker['path']) && is_string($sticker['path']) && $sticker['path'] !== '') {
                $stickers[] = [
                    'id' => (string)($sticker['id'] ?? count($stickers)),
                    'path' => $sticker['path']
                ];
            }
        }
        return $stickers;
    }

    private function stickerPackRow($row, $isInstalled, $currentUserId) {
        $stickers = $this->decodeStickers($row['stickers'] ?? '[]');
        return [
            'id' => (int)$row['id'],
            'name' => (string)$row['name'],
            'owner' => (int)$row['owner'],
            'is_owner' => ((int)$row['owner'] === (int)$currentUserId),
            'is_installed' => (bool)$isInstalled,
            'sticker_count' => count($stickers),
            'stickers' => $stickers
        ];
    }

    private function sendStickerPackError($from, $error, $message) {
        $from->send(json_encode([
            'type' => 'sticker_pack_error',
            'error' => $error,
            'message' => $message
        ], JSON_UNESCAPED_UNICODE));
    }

    private function sendToUser($userId, $data) {
        if (($data['type']??'')==='chat_message' && (int)($data['receiver_id']??0)===(int)$userId) {
            (new VibeSecurity($this->getDB()))->registerContact($userId,(int)$data['sender_id']);
            $data['suppress_notification']=$this->isNotificationMuted($userId,(int)$data['sender_id']);
        }
        if (isset($this->userConnections[$userId])) {
            $conn=$this->userConnections[$userId];
            if (!(new VibeSecurity($this->getDB()))->validate($conn->vibeSessionToken??'', $userId)) { $this->rejectSession($conn); return false; }
            $this->userConnections[$userId]->send(json_encode($data));
            return true;
        }
        return false;
    }

    private function isUserBlocked($blockerId, $targetId) {
        if ($blockerId <= 0 || $targetId <= 0) return false;
        try {
            $stmt = $this->getDB()->prepare("SELECT 1 FROM blocked_users WHERE user_id = ? AND blocked_id = ?");
            $stmt->execute([$blockerId, $targetId]);
            return (bool)$stmt->fetchColumn();
        } catch (\PDOException $e) {
            return false;
        }
    }

    private function checkPrivacyAccess($ownerId, $requesterId, $settingType) {
        if ($ownerId === $requesterId) return true;
        try {
            // Check allowed columns to prevent SQL injection
            $allowedTypes = ['activity', 'avatar', 'forwarded', 'messages', 'status'];
            if (!in_array($settingType, $allowedTypes)) return true;
            
            $stmt = $this->getDB()->prepare("SELECT {$settingType}, {$settingType}_users FROM privacy_settings WHERE user_id = ?");
            $stmt->execute([$ownerId]);
            $row = $stmt->fetch();
            if (!$row) return true;
            
            $val = $row[$settingType];
            if ($val === 'NOBODY') return false;
            if ($val === 'EVERYONE') return true;
            if ($val === 'SELECTED') {
                $usersStr = $row["{$settingType}_users"];
                if ($usersStr) {
                    $users = json_decode($usersStr, true);
                    if (is_array($users)) {
                        return in_array((int)$requesterId, $users) || in_array((string)$requesterId, $users);
                    }
                }
                return false;
            }
        } catch (\PDOException $e) {
            echo "[Privacy DB Error] " . $e->getMessage() . "\n";
        }
        return true;
    }

    private function getUserInfoForBot($userId) {
        try {
            $stmt = $this->getDB()->prepare("SELECT name, username, avatar_url, is_developer, is_verified FROM users WHERE id = ?");
            $stmt->execute([$userId]);
            $u = $stmt->fetch();
            if ($u) {
                return [
                    'name' => $u['name'],
                    'username' => $u['username'],
                    'avatar_url' => $u['avatar_url'],
                    'is_developer' => (bool)$u['is_developer'],
                    'is_verified' => (bool)$u['is_verified'],
                    'is_bot' => false
                ];
            }
        } catch (\PDOException $e) {}
        return null;
    }

    private function getUserName($userId, $preferBot = false) {
        try {
            if ($preferBot) {
                $stmtBot = $this->getDB()->prepare("SELECT name, username FROM bots WHERE id = ?");
                $stmtBot->execute([$userId]);
                $rowBot = $stmtBot->fetch();
                if ($rowBot) {
                    return $rowBot['name'] ?: $rowBot['username'];
                }
            }
            $stmt = $this->getDB()->prepare("SELECT name, username, is_banned, is_freezed FROM users WHERE id = ?");
            $stmt->execute([$userId]);
            $row = $stmt->fetch();
            if ($row) {
                return $row['name'] ?: $row['username'];
            }
            if (!$preferBot) {
                $stmtBot = $this->getDB()->prepare("SELECT name, username FROM bots WHERE id = ?");
                $stmtBot->execute([$userId]);
                $rowBot = $stmtBot->fetch();
                if ($rowBot) {
                    return $rowBot['name'] ?: $rowBot['username'];
                }
            }
        } catch (\PDOException $e) {
            // fallback
        }
        return "User #$userId";
    }

    private function broadcastUserStatus($userId) {
        try {
            $stmt = $this->getDB()->prepare("SELECT name, username, avatar_url, is_online, UNIX_TIMESTAMP(last_seen) * 1000 AS last_seen, is_developer, is_verified, UNIX_TIMESTAMP(register_date) * 1000 AS register_date, about, is_banned, is_freezed FROM users WHERE id = ?");
            $stmt->execute([$userId]);
            $row = $stmt->fetch();
            if ($row) {
                foreach ($this->userConnections as $id => $conn) {
                    if ($id !== $userId) {
                        $isBlockedByUser = $this->isUserBlocked($userId, $id);
                        $isBlockedByMe = $this->isUserBlocked($id, $userId);

                        $avatarUrl = $row['avatar_url'];
                        if (!$this->checkPrivacyAccess($userId, $id, 'avatar') || $isBlockedByUser) {
                            $avatarUrl = null;
                        }
                        
                        $about = $row['about'];
                        if (!$this->checkPrivacyAccess($userId, $id, 'status') || $isBlockedByUser) {
                            $about = null;
                        }
                        
                        $isOnline = (int)$row['is_online'] === 1;
                        $realLastSeen = $row['last_seen'] ? (int)$row['last_seen'] : null;
                        $lastSeenStatus = null;
                        
                        if ($isBlockedByUser) {
                            $isOnline = false;
                            $realLastSeen = null;
                            $lastSeenStatus = 'long_ago';
                        } elseif (!$this->checkPrivacyAccess($userId, $id, 'activity')) {
                            $isOnline = false;
                            if ($realLastSeen !== null) {
                                $diff = time() - ($realLastSeen / 1000);
                                if ($diff < 3 * 86400) $lastSeenStatus = 'recently';
                                elseif ($diff < 7 * 86400) $lastSeenStatus = 'this_week';
                                elseif ($diff < 30 * 86400) $lastSeenStatus = 'this_month';
                                else $lastSeenStatus = 'long_ago';
                            }
                            $realLastSeen = null;
                        }
                        
                        $canMessage = $this->checkPrivacyAccess($userId, $id, 'messages');

                        $payload = json_encode([
                            'type' => 'user_info_result',
                            'user_id' => $userId,
                            'name' => $row['name'],
                            'username' => $row['username'],
                            'avatar_url' => $avatarUrl,
                            'is_online' => $isOnline,
                            'last_seen' => $realLastSeen,
                            'last_seen_status' => $lastSeenStatus,
                            'is_developer' => (int)$row['is_developer'] === 1,
                            'is_verified' => (int)$row['is_verified'] === 1,
                            'register_date' => $row['register_date'] ? (int)$row['register_date'] : null,
                            'is_bot' => false,
                            'about' => $about,
                            'can_message' => $canMessage,
                            'is_banned' => (int)$row['is_banned'] === 1,
                            'is_freezed' => (int)$row['is_freezed'] === 1,
                            'is_blocked_by_me' => $isBlockedByMe,
                            'is_blocked_by_user' => $isBlockedByUser
                        ]);
                        $conn->send($payload);
                    }
                }
            }
        } catch (\PDOException $e) {}
    }

    private function buildChatList($userId) {
        try {
        $uid = (int)$userId;
        $stmt = $this->getDB()->prepare("
    SELECT 
        m.id,
        CASE WHEN m.sender_id = $uid THEN m.receiver_id ELSE m.sender_id END AS interlocutor_id,
        CASE 
            WHEN (m.sender_id = $uid AND m.deleted_by_sender = 1) OR (m.receiver_id = $uid AND m.deleted_by_receiver = 1) THEN ''
            ELSE m.content 
        END AS last_message,
        m.timestamp AS last_timestamp,
        (SELECT COUNT(*) FROM messages m2 WHERE m2.sender_id = CASE WHEN m.sender_id = $uid THEN m.receiver_id ELSE m.sender_id END AND m2.receiver_id = $uid AND m2.is_read = 0 AND m2.deleted_by_receiver = 0) AS unread_count,
        (CASE WHEN m.sender_id = $uid THEN 1 ELSE 0 END) AS is_last_message_mine,
        m.is_read AS is_last_message_read,
        COALESCE(u.is_online, 1) AS is_online,
        COALESCE(u.is_developer, 0) AS is_developer,
        CASE WHEN b.id IS NOT NULL THEN b.is_verified ELSE COALESCE(u.is_verified, 0) END AS is_verified,
        CASE WHEN b.id IS NOT NULL THEN b.username ELSE u.username END AS username,
        CASE WHEN b.id IS NOT NULL THEN NULL ELSE u.avatar_url END AS avatar_url,
        UNIX_TIMESTAMP(u.register_date) * 1000 AS register_date,
        UNIX_TIMESTAMP(u.last_seen) * 1000 AS last_seen,
        COALESCE(u.is_banned, 0) AS is_banned,
        COALESCE(u.is_freezed, 0) AS is_freezed,
        CASE WHEN b.id IS NOT NULL THEN 1 ELSE 0 END AS is_bot,
        b.about,
        CASE WHEN mu.muted_id IS NOT NULL THEN 1 ELSE 0 END AS is_muted,
        CASE 
            WHEN (m.sender_id = $uid AND m.deleted_by_sender = 1) OR (m.receiver_id = $uid AND m.deleted_by_receiver = 1) THEN NULL
            ELSE m.attachments 
        END AS last_attachments
        FROM messages m
        LEFT JOIN users u ON u.id = (CASE WHEN m.sender_id = $uid THEN m.receiver_id ELSE m.sender_id END)
        LEFT JOIN bots b ON b.id = (CASE WHEN m.sender_id = $uid THEN m.receiver_id ELSE m.sender_id END)
        LEFT JOIN muted_users mu ON mu.user_id = $uid AND mu.muted_id = (CASE WHEN m.sender_id = $uid THEN m.receiver_id ELSE m.sender_id END)
        WHERE m.id IN (
            SELECT COALESCE(
                MAX(CASE WHEN (m3.sender_id = $uid AND m3.deleted_by_sender = 0) OR (m3.receiver_id = $uid AND m3.deleted_by_receiver = 0) THEN m3.id END),
                MAX(m3.id)
            )
            FROM messages m3
            WHERE m3.sender_id = $uid OR m3.receiver_id = $uid
            GROUP BY CASE WHEN m3.sender_id = $uid THEN m3.receiver_id ELSE m3.sender_id END
        )
        ORDER BY m.timestamp DESC
        ");
        $stmt->execute();
            $chats = [];
            while ($row = $stmt->fetch()) {
                $interlocutorId = (int)$row['interlocutor_id'];

                // BUGFIX: $isBanned, $isFreezed и $isBot НЕ НАЗНАЧАЛИСЬ вообще.
                // SELECT их отдавал (COALESCE(u.is_banned, 0) AS is_banned и т.д.),
                // но в $row они и оставались. PHP подставлял undefined -> null,
                // поэтому load_chats всегда возвращал is_banned/is_freezed = null,
                // клиент парсил их как false и затирал флаги в users_cache.
                $isBanned = (int)$row['is_banned'] === 1;
                $isFreezed = (int)$row['is_freezed'] === 1;
                $isBot = (int)$row['is_bot'] === 1;

                $isBlockedByUser = $this->isUserBlocked($interlocutorId, $userId);
                $isBlockedByMe = $this->isUserBlocked($userId, $interlocutorId);
                
                if ($isBanned || $isFreezed || $isBlockedByUser) {
                    $name = $this->getUserName($interlocutorId, $isBot);
                    $avatarUrl = null;
                } else {
                    $name = $this->getUserName($interlocutorId, $isBot);
                    $avatarUrl = $row['avatar_url'];
                    if (!$this->checkPrivacyAccess($interlocutorId, $userId, 'avatar')) {
                        $avatarUrl = null;
                    }
                }
                
                $about = $row['about'];
                if (!$this->checkPrivacyAccess($interlocutorId, $userId, 'status') || $isBlockedByUser) {
                    $about = null;
                }
                
                $isOnline = (int)$row['is_online'] === 1;
                $realLastSeen = $row['last_seen'] ? (int)$row['last_seen'] : null;
                $lastSeenStatus = null;
                
                if ($isBlockedByUser) {
                    $isOnline = false;
                    $realLastSeen = null;
                    $lastSeenStatus = 'long_ago';
                } elseif (!$this->checkPrivacyAccess($interlocutorId, $userId, 'activity')) {
                    $isOnline = false;
                    if ($realLastSeen !== null) {
                        $diff = time() - ($realLastSeen / 1000);
                        if ($diff < 3 * 86400) $lastSeenStatus = 'recently';
                        elseif ($diff < 7 * 86400) $lastSeenStatus = 'this_week';
                        elseif ($diff < 30 * 86400) $lastSeenStatus = 'this_month';
                        else $lastSeenStatus = 'long_ago';
                    }
                    $realLastSeen = null;
                }
                
                $canMessage = $this->checkPrivacyAccess($interlocutorId, $userId, 'messages');

                $chats[] = [
                    'interlocutor_id' => $interlocutorId,
                    'name' => $name,
                    'last_message' => $row['last_message'],
                    'last_timestamp' => $row['last_timestamp'],
                    'unread_count' => (int)$row['unread_count'],
                    'is_last_message_mine' => (int)$row['is_last_message_mine'],
                    'is_last_message_read' => (int)$row['is_last_message_read'],
                    'is_online' => $isOnline,
                    'last_seen' => $realLastSeen,
                    'last_seen_status' => $lastSeenStatus,
                    'is_developer' => (int)$row['is_developer'],
                    'is_verified' => (int)$row['is_verified'],
                    'username' => $row['username'],
                    'avatar_url' => $avatarUrl,
                    'register_date' => $row['register_date'] ? (int)$row['register_date'] : null,
                    'is_bot' => $isBot,
                    'about' => $about,
                    'is_muted' => (int)$row['is_muted'] === 1,
                    'can_message' => $canMessage,
                    'is_banned' => $isBanned,
                    'is_freezed' => $isFreezed,
                    'is_blocked_by_me' => $isBlockedByMe,
                    'is_blocked_by_user' => $isBlockedByUser,
                    'last_attachments' => isset($row['last_attachments']) && $row['last_attachments'] ? json_decode($row['last_attachments'], true) : null
                ];
            }
            return $chats;
        } catch (\PDOException $e) {
            echo "[DB Error buildChatList] " . $e->getMessage() . "\n";
            return [];
        }
    }

    private function checkGlobalFreezes() {
        $now = time();
        if ($now - $this->lastFreezeCheck > 60) {
            $this->lastFreezeCheck = $now;
            try {
                $stmt = $this->getDB()->prepare("UPDATE users SET is_freezed = 0, freeze_time = NULL WHERE is_freezed = 1 AND freeze_time IS NOT NULL AND freeze_time <= NOW()");
                $stmt->execute();
            } catch (\PDOException $e) {
                echo "[DB Error checkGlobalFreezes] " . $e->getMessage() . "\n";
            }
        }
    }

    public function onOpen(ConnectionInterface $conn) {
        $this->clients->attach($conn);
        $realIp = null;
        try {
            if (isset($conn->httpRequest)) {
                if ($conn->httpRequest->hasHeader('X-Forwarded-For')) {
                    $realIp = $conn->httpRequest->getHeaderLine('X-Forwarded-For');
                    $realIp = explode(',', $realIp)[0];
                } elseif ($conn->httpRequest->hasHeader('X-Real-IP')) {
                    $realIp = $conn->httpRequest->getHeaderLine('X-Real-IP');
                }
            }
        } catch (\Exception $e) {}
        
        $conn->realIp = $realIp ? trim($realIp) : trim($conn->remoteAddress, '[]');
        echo "[+] Новое соединение ({$conn->resourceId}) IP: {$conn->realIp}\n";
    }

    private function rejectSession($conn) {
        $conn->send(json_encode(['type'=>'force_logout','reason'=>'session_invalid']));
        $conn->close();
    }
    /* ------------------------------------------------------------------ */
    /*  Анти-флуд авторизации                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Скользящее окно в памяти. Возвращает 0, если запрос разрешён, иначе — сколько
     * секунд ждать. Разрешённый запрос сразу записывается в окно.
     */
    private function floodCheck($scope, $key, $limit, $windowSec) {
        $now = microtime(true);
        if ($now - $this->lastFloodPrune > 60) {
            $this->lastFloodPrune = $now;
            foreach ($this->floodBuckets as $k => $ts) {
                $ts = array_values(array_filter($ts, function ($t) use ($now) { return $now - $t < 900; }));
                if (empty($ts)) unset($this->floodBuckets[$k]); else $this->floodBuckets[$k] = $ts;
            }
            foreach ($this->floodBans as $ip => $until) {
                if ($until <= time()) unset($this->floodBans[$ip]);
            }
        }
        $bucket = $scope . '|' . mb_strtolower(trim((string)$key));
        $ts = array_values(array_filter(
            $this->floodBuckets[$bucket] ?? [],
            function ($t) use ($now, $windowSec) { return $now - $t < $windowSec; }
        ));
        if (count($ts) >= $limit) {
            $this->floodBuckets[$bucket] = $ts;
            return max(1, (int)ceil($windowSec - ($now - $ts[0])));
        }
        $ts[] = $now;
        $this->floodBuckets[$bucket] = $ts;
        return 0;
    }

    /**
     * Лимиты для команд входа/регистрации. По IP — от перебора и спама письмами,
     * по e-mail — чтобы чужой ящик нельзя было забросать кодами с разных адресов.
     * Упорный флуд (10 отказов за 10 минут) — бан IP на 15 минут на все публичные команды.
     */
    private function authFloodGuard($type, $ip, array $data) {
        if (isset($this->floodBans[$ip])) {
            if ($this->floodBans[$ip] > time()) return $this->floodBans[$ip] - time();
            unset($this->floodBans[$ip]);
        }
        $email = mb_strtolower(trim((string)($data['email'] ?? '')));
        $retry = 0;
        switch ($type) {
            case 'check_availability':
                $retry = $this->floodCheck('avail_ip', $ip, 30, 60);
                break;
            case 'register':
            case 'login':
                $retry = $this->floodCheck('code_ip', $ip, 6, 600);
                if (!$retry && $email !== '') $retry = $this->floodCheck('code_email', $email, 4, 600);
                break;
            case 'verify_code':
                $retry = $this->floodCheck('verify_ip', $ip, 12, 60);
                if (!$retry && $email !== '') $retry = $this->floodCheck('verify_email', $email, 8, 300);
                break;
            case 'verify_two_factor':
                $retry = $this->floodCheck('2fa_ip', $ip, 10, 60);
                break;
            case 'request_two_factor_reset':
            case 'confirm_two_factor_reset':
                $retry = $this->floodCheck('2fa_reset_ip', $ip, 8, 300);
                break;
            case 'auth_connect':
                $retry = $this->floodCheck('auth_ip', $ip, 20, 60);
                break;
        }
        if ($retry > 0 && $this->floodCheck('penalty', $ip, 10, 600) > 0) {
            $this->floodBans[$ip] = time() + 900;
            echo "[FLOOD] IP $ip banned for 15 min ($type)\n";
            return 900;
        }
        return $retry;
    }

    private function floodResultType($type) {
        switch ($type) {
            case 'auth_connect': return 'auth_connect_result';
            case 'register': return 'register_result';
            case 'login': return 'login_result';
            case 'request_two_factor_reset':
            case 'confirm_two_factor_reset': return 'two_factor_reset_result';
            default: return 'verify_code_result'; // verify_code, verify_two_factor
        }
    }

    private function floodReject($from, $type, $retry) {
        if ($type === 'check_availability') return; // без ответа: клиент просто переспросит на следующем вводе
        $human = $retry >= 60 ? ((int)ceil($retry / 60)) . ' мин' : $retry . ' сек';
        $from->send(json_encode([
            'type' => $this->floodResultType($type),
            'success' => false,
            'error' => 'rate_limited',
            'retry_after' => $retry,
            'message' => 'Слишком много запросов. Повторите через ' . $human . '.'
        ], JSON_UNESCAPED_UNICODE));
    }

    /* ------------------------------------------------------------------ */
    /*  Системные сообщения Vibe cat: код входа и уведомление о входе    */
    /* ------------------------------------------------------------------ */

    /** id бота Vibe cat (тот же, что шлёт приветствие новым пользователям). */
    const SYSTEM_BOT_ID = 1;

    private function normalizeLang($raw) {
        $l = strtolower(substr(trim((string)$raw), 0, 2));
        return in_array($l, ['ru', 'en'], true) ? $l : 'ru';
    }

    /**
     * Язык, на котором писать пользователю. Источник — users.language: клиент присылает
     * язык интерфейса в auth_connect, и мы запоминаем его за аккаунтом (сообщения уходят
     * на ВСЕ устройства, поэтому язык нужен именно аккаунта, а не текущего соединения).
     * Пока аккаунт ни разу не подключался новой версией — берём язык из текущего
     * запроса, иначе ru.
     */
    private function userLanguage($userId, $fallback = null) {
        try {
            $q = $this->getDB()->prepare("SELECT language FROM users WHERE id = ?");
            $q->execute([$userId]);
            $stored = $q->fetchColumn();
            if ($stored) return $this->normalizeLang($stored);
        } catch (\Throwable $e) {}
        return $this->normalizeLang($fallback);
    }

    private function rememberUserLanguage($userId, $raw) {
        $lang = strtolower(substr(trim((string)$raw), 0, 2));
        if (!in_array($lang, ['ru', 'en'], true)) return;
        try {
            $this->getDB()->prepare("UPDATE users SET language = ? WHERE id = ? AND (language IS NULL OR language <> ?)")->execute([$lang, $userId, $lang]);
        } catch (\Throwable $e) {}
    }

    /** Убирает символы разметки клиента (**, __, ||, `, [ ]( ) …) из данных, попадающих в текст. */
    private function cleanForMarkup($value) {
        $v = preg_replace('/[*_~`|\[\]{}<>]/u', '', (string)$value);
        $v = str_replace('--', '-', $v);
        $v = trim(preg_replace('/\s+/u', ' ', $v));
        return $v === '' ? '—' : $v;
    }

    /**
     * Сообщение от Vibe cat пользователю. Строка попадает в messages как обычное
     * сообщение бота: онлайн-доставку и FCM-пуш офлайн-устройствам делает checkNewMessages().
     */
    private function sendSystemBotMessage($userId, $text) {
        try {
            $stmt = $this->getDB()->prepare("INSERT INTO messages (sender_id, receiver_id, sender_type, content) VALUES (?, ?, 'bot', ?)");
            $stmt->execute([self::SYSTEM_BOT_ID, (int)$userId, $text]);
            return true;
        } catch (\Throwable $e) {
            echo "[SYSBOT] Failed to queue message for user $userId: " . $e->getMessage() . "\n";
            return false;
        }
    }

    /** Код в `моноширинном` блоке: клиент копирует его в буфер по тапу. */
    private function loginCodeText($lang, $code) {
        if ($lang === 'en') {
            return "**Someone is trying to sign in to your account**\n\n"
                 . "Sign-in code: ||{$code}||
\n"
                 . "Never share this code with anyone. It is used only to sign in to your Vibe account.";
        }
        return "**Кто-то пытается войти в ваш аккаунт**\n\n"
             . "Код авторизации: ||{$code}||
\n"
             . "Не передавайте код никому! Он используется только для входа в аккаунт Vibe.";
    }

    /** Ссылка vibe://settings/devices открывается клиентом внутри приложения (Настройки → Устройства). */
    private function loginAlertText($lang, $device, $location) {
        $link = 'vibe://settings/devices';
        if ($lang === 'en') {
            return "**New sign-in to your account**\n\n"
                 . "Device: ||{$device}||\n"
                 . "Location: ||{$location}||\n\n"
                 . "If this wasn't you, end that session in [Devices]({$link}).";
        }
        return "**В ваш аккаунт совершён вход**\n\n"
             . "Устройство: ||{$device}||\n"
             . "Локация: ||{$location}||\n\n"
             . "Если это не вы, завершите сессию в разделе [Устройства]({$link}).";
    }

    /**
     * Уведомление о входе отправляется один раз на сессию: при первом auth_connect после
     * ввода кода (и второго фактора, если он включён). Флаг login_notified переводится
     * атомарным UPDATE, поэтому переподключения и гонки соединений повторов не дают.
     */
    private function notifyNewLogin($userId, $token, $deviceName, $osVersion, $location, $lang) {
        try {
            $upd = $this->getDB()->prepare("UPDATE auth_sessions SET login_notified = 1 WHERE token_hash = ? AND login_notified = 0");
            $upd->execute([hash('sha256', (string)$token)]);
            if ($upd->rowCount() === 0) return; // уже сообщали, либо сессия создана до обновления
        } catch (\Throwable $e) {
            return; // колонки нет — миграция 20260908_login_notifications не применена
        }
        $device = $this->cleanForMarkup($deviceName);
        if ($osVersion && $osVersion !== 'Unknown OS') $device .= ' · ' . $this->cleanForMarkup($osVersion);
        $this->sendSystemBotMessage($userId, $this->loginAlertText($lang, $device, $this->cleanForMarkup($location)));
        echo "[SYSBOT] Login alert queued for user $userId ($device)\n";
    }

    private $lastSessionSweep = 0;
    private function sweepSessions() {
        if (time() === $this->lastSessionSweep) return;
        $this->lastSessionSweep = time();
        $security = new VibeSecurity($this->getDB());
        foreach ($this->clients as $conn) {
            if (isset($conn->vibeUserId) && !$security->validate($conn->vibeSessionToken ?? '', $conn->vibeUserId)) {
                $this->rejectSession($conn);
            }
        }
    }
    public function onMessage(ConnectionInterface $from, $msg) {
        $this->checkGlobalFreezes();
        try {
            $data = json_decode($msg, true);
            if (!is_array($data) || !is_string($data['type'] ?? null)) return;
            // Коды резервной почты и сброса защиты уходят через ту же очередь, что и коды входа.
            $security = new VibeSecurity(
                $this->getDB(),
                function ($to, $subject, $body) { return $this->sendEmailToRabbitMQ($to, $subject, $body); }
            );
            $type = $data['type'];
            $public = ['auth_connect','check_availability','register','login','verify_code'];

            // Анти-флуд на все команды, доступные без сессии.
            $resetPublic = ['request_two_factor_reset', 'confirm_two_factor_reset'];
            if ($type === 'verify_two_factor' || in_array($type, $resetPublic, true) || in_array($type, $public, true)) {
                $ip = isset($from->realIp) ? $from->realIp : trim($from->remoteAddress, '[]');
                $retry = $this->authFloodGuard($type, $ip, $data);
                if ($retry > 0) { $this->floodReject($from, $type, $retry); return; }
            }

            if ($type === 'verify_two_factor') {
                $from->send(json_encode($security->challenge($data))); return;
            }
            // Сброс второго фактора с экрана входа: пользователя опознаёт challenge_token
            // (код с основной почты / из Vibe cat уже введён), код сброса уходит на резервную почту.
            if (!isset($from->vibeUserId) && $type === 'request_two_factor_reset') {
                $from->send(json_encode($security->resetRequest($data), JSON_UNESCAPED_UNICODE)); return;
            }
            if (!isset($from->vibeUserId) && $type === 'confirm_two_factor_reset') {
                $from->send(json_encode($security->resetConfirm($data), JSON_UNESCAPED_UNICODE)); return;
            }
            if (!in_array($type, $public, true)) {
                if (!isset($from->vibeUserId)) {
                    // Команда пришла раньше auth_connect (гонка на клиенте или старая сборка).
                    // Это не повод разлогинивать устройство и стирать его свежий токен:
                    // просим авторизоваться и отбрасываем кадр.
                    $from->send(json_encode(['type' => 'auth_required']));
                    return;
                }
                // Сессия была и перестала быть валидной (отозвана, истекла, бан) — вот теперь выход.
                $session = $security->validate($from->vibeSessionToken ?? '', $from->vibeUserId);
                if (!$session) { $this->rejectSession($from); return; }
                $uid = (int)$session['user_id'];
                // User identity comes from the authenticated connection, never the frame.
                $data['user_id'] = $uid;
                if (in_array($type,['send_message','typing','stop_typing'],true)) $data['sender_id']=$uid;
                if ($type==='logout') {
                    $this->getDB()->prepare('DELETE FROM auth_sessions WHERE token_hash=?')->execute([hash('sha256',$from->vibeSessionToken)]);
                    $this->getDB()->prepare('DELETE FROM device_tokens WHERE user_id=? AND token=?')->execute([$uid,$from->vibeFcmToken??'']);
                    $this->rejectSession($from); return;
                }
                if ($type==='session_ping') { $from->send(json_encode(['type'=>'session_pong'])); return; }
                if ($type==='get_two_factor' || $type==='set_two_factor') {
                    $from->send(json_encode($security->settings($uid,$data,$from->vibeSessionToken))); return;
                }
                if ($type==='set_recovery_email') {
                    $from->send(json_encode($security->recoveryEmail($uid,$data), JSON_UNESCAPED_UNICODE)); return;
                }
                if ($type==='request_two_factor_reset') {
                    $from->send(json_encode($security->resetRequest($data,$uid), JSON_UNESCAPED_UNICODE)); return;
                }
                if ($type==='confirm_two_factor_reset') {
                    $from->send(json_encode($security->resetConfirm($data,$uid,$from->vibeSessionToken), JSON_UNESCAPED_UNICODE)); return;
                }
                if ($type==='get_notification_settings' || $type==='set_notification_settings') {
                    $from->send(json_encode($security->notifications($uid,$data))); return;
                }
                // Bot responses must use the separately authenticated bot HTTP API.
                if (in_array($type,['answer_callback_query','bot_callback_answer'],true)) return;
            }

        // ==========================================
        // 0. АУТЕНТИФИКАЦИЯ СОЕДИНЕНИЯ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'auth_connect') {
            $userId = (int)($data['user_id'] ?? 0);
            $fcmToken = $data['fcm_token'] ?? null;
            $deviceId = $data['device_id'] ?? null;
            $deviceName = $data['device_name'] ?? 'Unknown Device';
            $osVersion = $data['os_version'] ?? 'Unknown OS';
            if (isset($from->vibeUserId) && (int)$from->vibeUserId !== $userId) { $this->rejectSession($from); return; }
            $token = $data['session_token'] ?? '';
            if (!$security->validate($token,$userId,$deviceId)) { $this->rejectSession($from); return; }
            $from->vibeSessionToken=$token;
            $from->vibeFcmToken=$fcmToken;
            // Язык интерфейса запоминаем за аккаунтом: на нём Vibe cat пишет коды входа
            // и уведомления о входе на все устройства пользователя.
            if (!empty($data['lang'])) $this->rememberUserLanguage($userId, $data['lang']);


            if ($userId > 0) {
                // Check if user is banned or freezed
                $stmtCheck = $this->getDB()->prepare("SELECT is_banned, is_freezed FROM users WHERE id = ?");
                $stmtCheck->execute([$userId]);
                $userStatus = $stmtCheck->fetch();
                if (!$userStatus) { $this->rejectSession($from); return; }
                if ($userStatus && ((int)$userStatus['is_banned'] === 1 || (int)$userStatus['is_freezed'] === 1)) {
                    $reason = ((int)$userStatus['is_banned'] === 1) ? 'banned' : 'freezed';
                    $from->send(json_encode([
                        'type' => 'force_logout',
                        'reason' => $reason
                    ]));
                    $from->close();
                    return;
                }
                
                // Hardware ban check
                if ($deviceId) {
                    try {
                        $hwStmt = $this->getDB()->prepare("SELECT device_id FROM banned_devices WHERE device_id = ?");
                        $hwStmt->execute([$deviceId]);
                        if ($hwStmt->fetch()) {
                            $from->send(json_encode([
                                'type' => 'force_logout',
                                'reason' => 'banned'
                            ]));
                            $from->close();
                            return;
                        }
                    } catch (\Throwable $e) {
                        echo "[AUTH] HW Ban check failed (table missing?): " . $e->getMessage() . "\n";
                    }
                }

                $this->userConnections[$userId] = $from;
                $from->vibeUserId = $userId;
                if ($deviceId) {
                    $from->vibeDeviceId = $deviceId;
                }
                echo "[AUTH] Пользователь $userId подключён (conn {$from->resourceId})\n";
                
                try {
                    $stmt = $this->getDB()->prepare("UPDATE users SET is_online = 1, last_device_id = ? WHERE id = ?");
                    $stmt->execute([$deviceId, $userId]);
                } catch (\Throwable $e) {
                    echo "[AUTH] Update users failed (missing last_device_id column?): " . $e->getMessage() . "\n";
                    // Fallback to update just is_online
                    try {
                        $stmt = $this->getDB()->prepare("UPDATE users SET is_online = 1 WHERE id = ?");
                        $stmt->execute([$userId]);
                    } catch (\Throwable $e2) {
                        // ignore
                    }
                }
                
                // Сохраняем сессию
                if ($deviceId) {
                    try {
                        $ip = isset($from->realIp) ? $from->realIp : trim($from->remoteAddress, '[]');
                        // Handle IPv4 with port just in case (e.g. 192.168.1.1:5000)
                        if (strpos($ip, '.') !== false && strpos($ip, ':') !== false) {
                            $ip = explode(':', $ip)[0];
                        }
                        
                        if ($ip === '127.0.0.1' || $ip === '::1' || strpos($ip, '192.168.') === 0 || strpos($ip, '10.') === 0) {
                            // If local network (like emulator to local server), query without IP to get server's public IP location
                            $ipQuery = '';
                        } else {
                            $ipQuery = $ip;
                        }

                        $lang = $this->userLanguage($userId, $data['lang'] ?? null);
                        $geoUnknown = $lang === 'en' ? 'Unknown location' : 'Неизвестное местоположение';
                        $url = "http://ipwho.is/{$ipQuery}?lang={$lang}";
                        $ipData = null;
                        if (function_exists('curl_version')) {
                            $ch = curl_init();
                            curl_setopt($ch, CURLOPT_URL, $url);
                            curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
                            curl_setopt($ch, CURLOPT_TIMEOUT, 3);
                            $response = curl_exec($ch);
                            curl_close($ch);
                            if ($response !== false) {
                                $ipData = @json_decode($response, true);
                            }
                        }
                        
                        if (!$ipData) {
                            $ctx = stream_context_create(['http' => ['timeout' => 3]]);
                            $response = @file_get_contents($url, false, $ctx);
                            if ($response !== false) {
                                $ipData = @json_decode($response, true);
                            }
                        }

                        if ($ipData && isset($ipData['success']) && $ipData['success'] === true) {
                            $location = $ipData['city'] . ', ' . $ipData['country'];
                        } else {
                            echo "[GEO] Failed to determine location. IP: $ipQuery\n";
                            $location = $geoUnknown;
                        }

                        $sessStmt = $this->getDB()->prepare("
                            INSERT INTO sessions (user_id, device_id, device_name, os_version, location)
                            VALUES (?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE 
                                device_name = VALUES(device_name),
                                os_version = VALUES(os_version),
                                location = IF(VALUES(location) NOT IN ('Неизвестное местоположение', 'Unknown location'), VALUES(location), location),
                                last_active = CURRENT_TIMESTAMP
                        ");
                        $sessStmt->execute([$userId, $deviceId, $deviceName, $osVersion, $location]);

                        // Свежая сессия (первый auth_connect после ввода кода и 2FA):
                        // сообщаем о входе через Vibe cat на все устройства аккаунта.
                        $this->notifyNewLogin($userId, $token, $deviceName, $osVersion, $location, $lang);
                    } catch (\Exception $e) {
                        echo "[Session DB Error] " . $e->getMessage() . "\n";
                    }
                }

                // Сохраняем FCM-токен устройства
                if ($fcmToken) {
                    try {
                        // Удаляем этот токен у других пользователей (смена аккаунта)
                        $this->getDB()->prepare("DELETE FROM device_tokens WHERE token = ?")->execute([$fcmToken]);
                        // Добавляем токен текущему пользователю
                        $stmt2 = $this->getDB()->prepare("INSERT INTO device_tokens (user_id, token) VALUES (?, ?)");
                        $stmt2->execute([$userId, $fcmToken]);
                        echo "[FCM] Token saved for user $userId\n";
                    } catch (\PDOException $e) {
                        echo "[FCM DB Error] " . $e->getMessage() . "\n";
                    }
                }
                
                $this->broadcastUserStatus($userId);
                
                $from->send(json_encode([
                    'type' => 'auth_connect_result',
                    'success' => true,
                    'user_id' => $userId
                ]));
            }
            return;
        }

        // ==========================================
        // 0.1 СЕССИИ (УСТРОЙСТВА)
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'get_sessions') {
            $userId = (int)($data['user_id'] ?? 0);
            if ($userId > 0) {
                try {
                    $stmt = $this->getDB()->prepare("SELECT device_id, device_name, os_version, location, UNIX_TIMESTAMP(last_active) * 1000 AS last_active FROM sessions WHERE user_id = ? ORDER BY last_active DESC");
                    $stmt->execute([$userId]);
                    $sessions = $stmt->fetchAll();
                    $from->send(json_encode([
                        'type' => 'sessions_result',
                        'sessions' => $sessions
                    ]));
                } catch (\PDOException $e) {
                    echo "[DB Error get_sessions] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        if (isset($data['type']) && $data['type'] == 'terminate_session') {
            $userId = (int)($data['user_id'] ?? 0);
            $targetDeviceId = $data['device_id'] ?? '';
            if ($userId > 0 && !empty($targetDeviceId)) {
                try {
                    $this->getDB()->prepare('DELETE FROM auth_sessions WHERE user_id=? AND device_id=?')->execute([$userId,$targetDeviceId]);
                    $stmt = $this->getDB()->prepare("DELETE FROM sessions WHERE user_id = ? AND device_id = ?");
                    $stmt->execute([$userId, $targetDeviceId]);

                    // Ищем активное соединение этого устройства и кикаем
                    foreach ($this->clients as $client) {
                        if (isset($client->vibeUserId) && $client->vibeUserId === $userId && isset($client->vibeDeviceId) && $client->vibeDeviceId === $targetDeviceId) {
                            $client->send(json_encode(['type' => 'force_logout']));
                            $client->close();
                        }
                    }

                    $from->send(json_encode([
                        'type' => 'session_terminated',
                        'device_id' => $targetDeviceId
                    ]));
                } catch (\PDOException $e) {
                    echo "[DB Error terminate_session] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        // ==========================================
        // 0.2 НАСТРОЙКИ КОНФИДЕНЦИАЛЬНОСТИ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'get_privacy_settings') {
            $userId = (int)($data['user_id'] ?? 0);
            if ($userId > 0) {
                try {
                    $stmt = $this->getDB()->prepare("SELECT * FROM privacy_settings WHERE user_id = ?");
                    $stmt->execute([$userId]);
                    $row = $stmt->fetch();
                    if (!$row) {
                        // Default
                        $row = [
                            'activity' => 'EVERYONE', 'activity_users' => '[]',
                            'avatar' => 'EVERYONE', 'avatar_users' => '[]',
                            'forwarded' => 'EVERYONE', 'forwarded_users' => '[]',
                            'messages' => 'EVERYONE', 'messages_users' => '[]',
                            'status' => 'EVERYONE', 'status_users' => '[]'
                        ];
                    }
                    $from->send(json_encode([
                        'type' => 'privacy_settings_result',
                        'settings' => $row
                    ]));
                } catch (\PDOException $e) {
                    echo "[Privacy DB Error] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        if (isset($data['type']) && $data['type'] == 'update_privacy_settings') {
            $userId = (int)($data['user_id'] ?? 0);
            $settings = $data['settings'] ?? [];
            if ($userId > 0 && !empty($settings)) {
                try {
                    $activity = $settings['activity'] ?? 'EVERYONE';
                    $activityUsers = isset($settings['activity_users']) && is_array($settings['activity_users']) ? json_encode($settings['activity_users']) : '[]';
                    $avatar = $settings['avatar'] ?? 'EVERYONE';
                    $avatarUsers = isset($settings['avatar_users']) && is_array($settings['avatar_users']) ? json_encode($settings['avatar_users']) : '[]';
                    $forwarded = $settings['forwarded'] ?? 'EVERYONE';
                    $forwardedUsers = isset($settings['forwarded_users']) && is_array($settings['forwarded_users']) ? json_encode($settings['forwarded_users']) : '[]';
                    $messages = $settings['messages'] ?? 'EVERYONE';
                    $messagesUsers = isset($settings['messages_users']) && is_array($settings['messages_users']) ? json_encode($settings['messages_users']) : '[]';
                    $status = $settings['status'] ?? 'EVERYONE';
                    $statusUsers = isset($settings['status_users']) && is_array($settings['status_users']) ? json_encode($settings['status_users']) : '[]';

                    $stmt = $this->getDB()->prepare("
                        INSERT INTO privacy_settings (user_id, activity, activity_users, avatar, avatar_users, forwarded, forwarded_users, messages, messages_users, status, status_users)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE 
                            activity = VALUES(activity), activity_users = VALUES(activity_users),
                            avatar = VALUES(avatar), avatar_users = VALUES(avatar_users),
                            forwarded = VALUES(forwarded), forwarded_users = VALUES(forwarded_users),
                            messages = VALUES(messages), messages_users = VALUES(messages_users),
                            status = VALUES(status), status_users = VALUES(status_users)
                    ");
                    $stmt->execute([
                        $userId,
                        $activity, $activityUsers,
                        $avatar, $avatarUsers,
                        $forwarded, $forwardedUsers,
                        $messages, $messagesUsers,
                        $status, $statusUsers
                    ]);
                    
                    $from->send(json_encode([
                        'type' => 'update_privacy_settings_result',
                        'success' => true
                    ]));
                } catch (\PDOException $e) {
                    echo "[Privacy DB Error] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        // ==========================================
        // 1. ПРОВЕРКА ДОСТУПНОСТИ ПОЧТЫ/НИКА
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'check_availability') {
            $email = $data['email'] ?? null;
            $username = $data['username'] ?? null;
            
            $emailTaken = false;
            $usernameTaken = false;
            
            try {
                if ($email) {
                    $stmt = $this->getDB()->prepare("SELECT id FROM users WHERE email = ?");
                    $stmt->execute([$email]);
                    if ($stmt->fetch()) $emailTaken = true;
                }
                if ($username) {
                    $stmt = $this->getDB()->prepare("SELECT id FROM users WHERE username = ?");
                    $stmt->execute([$username]);
                    if ($stmt->fetch()) $usernameTaken = true;
                }
            } catch (\PDOException $e) {
                // Игнорируем ошибку для проверки доступности
            }
            
            $from->send(json_encode([
                'type' => 'check_availability_result',
                'email_taken' => $emailTaken,
                'username_taken' => $usernameTaken
            ]));
            return;
        }

        // ==========================================
        // 2. РЕГИСТРАЦИЯ (ВРЕМЕННОЕ СОХРАНЕНИЕ)
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'register') {
            $email = trim($data['email'] ?? '');
            $username = trim($data['username'] ?? '');
            
            $code = sprintf("%06d", mt_rand(0, 999999));
            
            try {
                $checkStmt = $this->getDB()->prepare("SELECT id FROM users WHERE email = ? OR username = ?");
                $checkStmt->execute([$email, $username]);
                if ($checkStmt->fetch()) {
                    $from->send(json_encode([
                        'type' => 'register_result',
                        'success' => false,
                        'message' => 'Email или Username уже зарегистрированы'
                    ]));
                    return;
                }

                $delStmt = $this->getDB()->prepare("DELETE FROM pending_users WHERE email = ?");
                $delStmt->execute([$email]);

                $stmt = $this->getDB()->prepare("
                    INSERT INTO pending_users (email, username, code)
                    VALUES (?, ?, ?)
                ");
                $stmt->execute([$email, $username, $code]);
                
                $subject = "Код подтверждения VibeMessenger";
                $message = "Ваш код подтверждения: " . $code;
                $this->sendEmailToRabbitMQ($email, $subject, $message);
                
                $from->send(json_encode([
                    'type' => 'register_result',
                    'success' => true
                ]));
            } catch (\PDOException $e) {
                $from->send(json_encode([
                    'type' => 'register_result',
                    'success' => false,
                    'message' => 'Ошибка при регистрации: ' . $e->getMessage()
                ]));
            }
            return;
        }
        
        // ==========================================
        // 2.5. ВХОД СУЩЕСТВУЮЩЕГО ПОЛЬЗОВАТЕЛЯ (LOGIN)
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'login') {
            $email = trim($data['email'] ?? '');
            
            try {
                $stmt = $this->getDB()->prepare("SELECT id, is_banned, is_freezed FROM users WHERE email = ?");
                $stmt->execute([$email]);
                $userRow = $stmt->fetch();
                if (!$userRow) {
                    $from->send(json_encode([
                        'type' => 'login_result',
                        'success' => false,
                        'message' => 'Аккаунт не найден. Пожалуйста, зарегистрируйтесь.'
                    ]));
                    return;
                }
                if ((int)$userRow['is_banned'] === 1) {
                    $from->send(json_encode([
                        'type' => 'login_result',
                        'success' => false,
                        'message' => 'Аккаунт заблокирован'
                    ]));
                    return;
                }
                if ((int)$userRow['is_freezed'] === 1) {
                    $from->send(json_encode([
                        'type' => 'login_result',
                        'success' => false,
                        'message' => 'Аккаунт заморожен'
                    ]));
                    return;
                }

                $code = random_int(100000, 999999);
                
                $delStmt = $this->getDB()->prepare("DELETE FROM pending_users WHERE email = ?");
                $delStmt->execute([$email]);

                $stmt = $this->getDB()->prepare("
                    INSERT INTO pending_users (email, username, code)
                    VALUES (?, ?, ?)
                ");
                $stmt->execute([$email, 'login_flow', $code]);
                
                // Как в Telegram: код входа уходит сообщением от Vibe cat на уже вошедшие
                // устройства аккаунта. Почта используется ТОЛЬКО если других активных сессий
                // нет (иначе код было бы негде прочитать) или сообщение бота не удалось записать.
                // Принудительной отправки на почту (force_email) больше нет.
                $userId = (int)$userRow['id'];
                $deviceId = is_string($data['device_id'] ?? null) ? $data['device_id'] : '';
                $viaApp = false;
                $sessQ = $this->getDB()->prepare("SELECT COUNT(*) FROM auth_sessions WHERE user_id = ? AND expires_at > NOW() AND device_id <> ?");
                $sessQ->execute([$userId, $deviceId]);
                $otherSessions = (int)$sessQ->fetchColumn();
                if ($otherSessions > 0) {
                    $lang = $this->userLanguage($userId, $data['lang'] ?? null);
                    $viaApp = $this->sendSystemBotMessage($userId, $this->loginCodeText($lang, $code));
                }
                if (!$viaApp) {
                    $subject = "Код для входа в VibeMessenger";
                    $message = "Ваш код для входа: " . $code . "\nНе передавайте ваш код третьим лицам.";
                    $this->sendEmailToRabbitMQ($email, $subject, $message);
                }
                echo "[LOGIN] user $userId: code via " . ($viaApp ? 'Vibe cat' : 'e-mail') . " (other active sessions: $otherSessions)\n";

                $from->send(json_encode([
                    'type' => 'login_result',
                    'success' => true,
                    'delivery' => $viaApp ? 'app' : 'email'
                ]));
            } catch (\PDOException $e) {
                $from->send(json_encode([
                    'type' => 'login_result',
                    'success' => false,
                    'message' => 'Ошибка при входе: ' . $e->getMessage()
                ]));
            }
            return;
        }

        // ==========================================
        // 3. ВЕРИФИКАЦИЯ КОДА И СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'verify_code') {
            if (!is_string($data['device_id']??null) || strlen($data['device_id'])<8 || strlen($data['device_id'])>255) {
                $from->send(json_encode(['type'=>'verify_code_result','success'=>false,'message'=>'Обновите приложение перед входом.'])); return;
            }
            $email = trim($data['email'] ?? '');
            $code = trim($data['code'] ?? '');
            
            try {
                $stmt = $this->getDB()->prepare("
                    SELECT email, username, code, created_at, attempts
                    FROM pending_users 
                    WHERE email = ? 
                    ORDER BY created_at DESC 
                    LIMIT 1
                ");
                $stmt->execute([$email]);
                $row = $stmt->fetch();
                
                if ($row) {
                    $createdAt = strtotime($row['created_at']);
                    if (time() - $createdAt > 300) {
                        $del = $this->getDB()->prepare("DELETE FROM pending_users WHERE email = ?");
                        $del->execute([$email]);
                        $from->send(json_encode([
                            'type' => 'verify_code_result',
                            'success' => false,
                            'message' => 'Срок действия кода истек. Пожалуйста, запросите новый.'
                        ]));
                        return;
                    }

                    if ($row['code'] === $code) {
                        $checkUser = $this->getDB()->prepare("SELECT id FROM users WHERE email = ?");
                        $checkUser->execute([$row['email']]);
                        $existingUser = $checkUser->fetch();
                        $isNewUser = !$existingUser; 

                        if ($existingUser) {
                            $userId = (int)$existingUser['id'];
                        } else {
                            $insertStmt = $this->getDB()->prepare("INSERT INTO users (email, username, name, freeze_time, spamblock_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
                            $insertStmt->execute([$row['email'], $row['username'], $row['username']]);
                            $userId = (int)$this->getDB()->lastInsertId();
                            
                            // Страховка: если lastInsertId вернул 0 (баг MariaDB/PDO)
                            if ($userId === 0) {
                                $fallback = $this->getDB()->prepare("SELECT id FROM users WHERE email = ?");
                                $fallback->execute([$row['email']]);
                                $fbRow = $fallback->fetch();
                                $userId = $fbRow ? (int)$fbRow['id'] : 0;
                            }
                        }

                        if ($userId > 0 && $isNewUser) {
                            $botId = 1;
                            $welcomeText = "Добро пожаловать в семейство Vibe!\nЗадавай мне любые вопросы, я подскажу что да как!\nМяу.";
                            $botMsgStmt = $this->getDB()->prepare("INSERT INTO messages (sender_id, receiver_id, sender_type, content, reply_to_id) VALUES (?, ?, 'bot', ?, NULL)");
                            $botMsgStmt->execute([$botId, $userId, $welcomeText]);
                        }
                        
                        $del = $this->getDB()->prepare("DELETE FROM pending_users WHERE email = ?");
                        $del->execute([$email]);

                        $result=$security->afterEmail($userId,$data['device_id']??'', $isNewUser);
                        $from->send(json_encode($result));
                    } else {
                        $attempts = (int)($row['attempts'] ?? 0) + 1;
                        if ($attempts >= 5) {
                            $del = $this->getDB()->prepare("DELETE FROM pending_users WHERE email = ?");
                            $del->execute([$email]);
                            $from->send(json_encode([
                                'type' => 'verify_code_result',
                                'success' => false,
                                'message' => 'Превышено количество попыток. Запросите новый код.'
                            ]));
                        } else {
                            $upd = $this->getDB()->prepare("UPDATE pending_users SET attempts = ? WHERE email = ?");
                            $upd->execute([$attempts, $email]);
                            $from->send(json_encode([
                                'type' => 'verify_code_result',
                                'success' => false,
                                'message' => 'Неверный код. Осталось попыток: ' . (5 - $attempts)
                            ]));
                        }
                    }
                } else {
                    $from->send(json_encode([
                        'type' => 'verify_code_result',
                        'success' => false,
                        'message' => 'Код не найден или срок его действия истек.'
                    ]));
                }
            } catch (\PDOException $e) {
                $from->send(json_encode([
                    'type' => 'verify_code_result',
                    'success' => false,
                    'message' => 'Сервер временно недоступен. Попробуйте позже.'
                ]));
            }
            return;
        }

        // ==========================================
        // 4. УСТАНОВКА ИМЕНИ (name)
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'set_nickname') {
            $userId = $data['user_id'] ?? null;
            $nickname = trim($data['nickname'] ?? '');
            $email = trim($data['email'] ?? '');
            
            try {
                if ($userId) {
                    $stmt = $this->getDB()->prepare("UPDATE users SET name = ? WHERE id = ?");
                    $stmt->execute([$nickname, $userId]);
                } elseif ($email) {
                    $stmt = $this->getDB()->prepare("UPDATE users SET name = ? WHERE email = ?");
                    $stmt->execute([$nickname, $email]);
                }
                
                $from->send(json_encode([
                    'type' => 'set_nickname_result',
                    'success' => true
                ]));
            } catch (\PDOException $e) {
                $from->send(json_encode([
                    'type' => 'set_nickname_result',
                    'success' => false,
                    'message' => 'Ошибка: ' . $e->getMessage()
                ]));
            }
            return;
        }

        // ==========================================
        // 4.1. ОБНОВЛЕНИЕ ПРОФИЛЯ (update_profile)
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'update_profile') {
            $userId = $data['user_id'] ?? null;
            if (!$userId) return;

            $updates = [];
            $params = [];

            if (isset($data['username'])) {
                $updates[] = "username = ?";
                $params[] = trim($data['username']);
            }
            if (isset($data['nickname'])) {
                $updates[] = "name = ?";
                $params[] = trim($data['nickname']);
            }
            if (isset($data['content'])) {
                $updates[] = "about = ?";
                $params[] = trim($data['content']);
            }

            if (!empty($updates)) {
                $params[] = $userId;
                $sql = "UPDATE users SET " . implode(', ', $updates) . " WHERE id = ?";
                try {
                    $stmt = $this->getDB()->prepare($sql);
                    $stmt->execute($params);

                    // Broadcast the update so other clients see it immediately
                    $this->broadcastUserStatus($userId);

                    $from->send(json_encode([
                        'type' => 'update_profile_result',
                        'success' => true
                    ]));
                } catch (\PDOException $e) {
                    $from->send(json_encode([
                        'type' => 'update_profile_result',
                        'success' => false,
                        'message' => 'Ошибка: ' . $e->getMessage()
                    ]));
                }
            }
            return;
        }

        // ==========================================
        // 4.2. ИНДИКАТОР ПЕЧАТИ (TYPING)
        // ==========================================
        if (isset($data['type']) && ($data['type'] == 'typing' || $data['type'] == 'stop_typing')) {
            $senderId = (int)($data['sender_id'] ?? 0);
            $receiverId = (int)($data['receiver_id'] ?? 0);
            if ($senderId > 0 && $receiverId > 0) {
                if ($this->isUserBlocked($receiverId, $senderId)) {
                    return;
                }
                $type = $data['type'] == 'typing' ? 'typing_indicator' : 'typing_indicator_stop';
                $senderName = $this->getUserName($senderId);
                $this->sendToUser($receiverId, [
                    'type' => $type,
                    'sender_id' => $senderId,
                    'sender_name' => $senderName
                ]);
            }
            return;
        }

        // ==========================================
        // 5. ОТПРАВКА СООБЩЕНИЯ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'send_message') {
            $senderId = (int)($data['sender_id'] ?? 0);
            $receiverId = (int)($data['receiver_id'] ?? 0);
            $content = mb_substr(trim($data['content'] ?? ''), 0, 2048);
            $replyToId = isset($data['reply_to_id']) && (int)$data['reply_to_id'] > 0 ? (int)$data['reply_to_id'] : null;
            $forwardedFromId = isset($data['forwarded_from_id']) && (int)$data['forwarded_from_id'] > 0 ? (int)$data['forwarded_from_id'] : null;
            $attachments = isset($data['attachments']) && is_array($data['attachments']) ? $data['attachments'] : null;

            if ($senderId <= 0 || $receiverId <= 0 || ($content === '' && empty($attachments))) return;

            if (!$this->checkPrivacyAccess($receiverId, $senderId, 'messages')) {
                $from->send(json_encode([
                    'type' => 'send_message_error',
                    'error' => 'Пользователь ограничил круг общения'
                ]));
                return;
            }

            try {
                $senderType = 'user';
                $this->getDB()->prepare('INSERT IGNORE INTO notification_contacts (user_id,peer_id) VALUES (?,?)')->execute([$senderId,$receiverId]);

                // Проверка spamblock
                $spamCheckStmt = $this->getDB()->prepare("SELECT is_spamblock, spamblock_time FROM users WHERE id = ?");
                $spamCheckStmt->execute([$senderId]);
                $spamRow = $spamCheckStmt->fetch();
                if ($spamRow && (int)$spamRow['is_spamblock'] === 1 && strtotime($spamRow['spamblock_time']) > time()) {
                    // Проверяем, есть ли уже переписка
                    $historyCheckStmt = $this->getDB()->prepare("
                        SELECT id FROM messages 
                        WHERE (sender_id = ? AND receiver_id = ?) 
                           OR (sender_id = ? AND receiver_id = ?) 
                        LIMIT 1
                    ");
                    $historyCheckStmt->execute([$senderId, $receiverId, $receiverId, $senderId]);
                    if (!$historyCheckStmt->fetch()) {
                        $from->send(json_encode([
                            'type' => 'send_message_error',
                            'error' => 'spamblock_active',
                            'message' => 'Вы не можете писать первым, так как находитесь под спамблоком.'
                        ]));
                        return;
                    }
                }

                $isBlockedByReceiver = $this->isUserBlocked($receiverId, $senderId);
                $deletedByReceiver = $isBlockedByReceiver ? 1 : 0;

                $stmt = $this->getDB()->prepare("
                    INSERT INTO messages (sender_id, receiver_id, sender_type, content, reply_to_id, forwarded_from_id, attachments, deleted_by_receiver)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ");
                $stmt->execute([$senderId, $receiverId, $senderType, $content, $replyToId, $forwardedFromId, $attachments ? json_encode($attachments) : null, $deletedByReceiver]);
                $messageId = (int)$this->getDB()->lastInsertId();

                // Страховка, если PDO вернул 0
                if ($messageId === 0) {
                    $maxStmt = $this->getDB()->query("SELECT MAX(id) FROM messages");
                    $messageId = (int)$maxStmt->fetchColumn();
                }

                $tsStmt = $this->getDB()->prepare("SELECT timestamp FROM messages WHERE id = ?");
                $tsStmt->execute([$messageId]);
                $tsRow = $tsStmt->fetch();
                $timestamp = $tsRow['timestamp'] ?? date('Y-m-d H:i:s');

                $replyToContent = null;
                $replyToSenderName = null;
                if ($replyToId) {
                    $replyStmt = $this->getDB()->prepare("
                        SELECT m.content, COALESCE(u.name, b.name) AS sender_name 
                        FROM messages m 
                        LEFT JOIN users u ON u.id = m.sender_id AND m.sender_type = 'user'
                        LEFT JOIN bots b ON b.id = m.sender_id AND m.sender_type = 'bot'
                        WHERE m.id = ?
                    ");
                    $replyStmt->execute([$replyToId]);
                    $replyRow = $replyStmt->fetch();
                    if ($replyRow) {
                        $replyToContent = $replyRow['content'];
                        $replyToSenderName = $replyRow['sender_name'];
                    }
                }

                $forwardedFromName = null;
                if ($forwardedFromId) {
                    $fwdStmt2 = $this->getDB()->prepare("
                        SELECT name FROM users WHERE id = ?
                        UNION
                        SELECT name FROM bots WHERE id = ?
                    ");
                    $fwdStmt2->execute([$forwardedFromId, $forwardedFromId]);
                    $fwdRow = $fwdStmt2->fetch();
                    if ($fwdRow) {
                        $forwardedFromName = $fwdRow['name'];
                    }
                }

                $senderName = $this->getUserName($senderId);

                $messagePayload = [
                    'type' => 'chat_message',
                    'message_id' => $messageId,
                    'sender_id' => $senderId,
                    'receiver_id' => $receiverId,
                    'sender_type' => $senderType,
                    'content' => $content,
                    'reply_to_id' => $replyToId,
                    'reply_to_content' => $replyToContent,
                    'reply_to_sender_name' => $replyToSenderName,
                    'forwarded_from_id' => $forwardedFromId,
                    'forwarded_from_name' => $forwardedFromName,
                    'timestamp' => $timestamp,
                    'sender_name' => $senderName,
                    'attachments' => $attachments
                ];

                $messagePayloadSender = $messagePayload;
                $messagePayloadReceiver = $messagePayload;
                
                if ($forwardedFromId) {
                    if (!$this->checkPrivacyAccess($forwardedFromId, $senderId, 'forwarded')) {
                        $messagePayloadSender['forwarded_from_id'] = -1;
                    }
                    if (!$this->checkPrivacyAccess($forwardedFromId, $receiverId, 'forwarded')) {
                        $messagePayloadReceiver['forwarded_from_id'] = -1;
                    }
                }

                // If receiver is a bot, insert update into bot_updates
                try {
                    $botCheckReceiver = $this->getDB()->prepare("SELECT id FROM bots WHERE id = ?");
                    $botCheckReceiver->execute([$receiverId]);
                    if ($botCheckReceiver->fetch()) {
                        $botMsgPayload = [
                            'id' => $messageId,
                            'type' => 'message',
                            'sender_id' => $senderId,
                            'sender_info' => $this->getUserInfoForBot($senderId),
                            'content' => $content,
                            'timestamp' => $timestamp,
                            'reply_to_id' => $replyToId,
                            'is_bot' => 0,
                            'is_media' => !empty($attachments) ? 1 : 0,
                            'attachments' => $attachments,
                            'reply_markup' => null
                        ];
                        $stmtUpd = $this->getDB()->prepare("INSERT INTO bot_updates (bot_id, update_type, sender_id, payload) VALUES (?, 'message', ?, ?)");
                        $stmtUpd->execute([$receiverId, $senderId, json_encode($botMsgPayload, JSON_UNESCAPED_UNICODE)]);
                    }
                } catch (\PDOException $e) {}

                // Всегда отправляем подтверждение и обновление чатов автору сообщения
                $this->sendToUser($senderId, $messagePayloadSender);
                $senderChats = $this->buildChatList($senderId);
                $this->sendToUser($senderId, [
                    'type' => 'chat_list_update',
                    'chats' => $senderChats
                ]);

                // Проверяем, заблокировал ли получатель отправителя
                $isBlockedByReceiver = $this->isUserBlocked($receiverId, $senderId);

                if (!$isBlockedByReceiver) {
                    $this->sendToUser($receiverId, $messagePayloadReceiver);

                    $receiverChats = $this->buildChatList($receiverId);
                    $this->sendToUser($receiverId, [
                        'type' => 'chat_list_update',
                        'chats' => $receiverChats
                    ]);

                    // Отправляем FCM push если получатель оффлайн
                    if (!isset($this->userConnections[$receiverId])) {
                        $isMutedCheck = $this->getDB()->prepare("SELECT 1 FROM muted_users WHERE user_id = ? AND muted_id = ?");
                        $isMutedCheck->execute([$receiverId, $senderId]);
                        if (!$isMutedCheck->fetchColumn()) {
                            $this->sendFcmNotification($receiverId, $senderName, $content, $senderId);
                        }
                    }
                }

                echo "[MSG] $senderId -> $receiverId (blocked: " . ($isBlockedByReceiver ? 'yes' : 'no') . "): $content\n";

            } catch (\PDOException $e) {
                $from->send(json_encode([
                    'type' => 'send_message_error',
                    'message' => 'Ошибка: ' . $e->getMessage()
                ]));
            }
            return;
        }

        // ==========================================
        // 5.1. РЕДАКТИРОВАНИЕ СООБЩЕНИЯ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'edit_message') {
            $messageId = (int)($data['message_id'] ?? 0);
            $content = trim($data['content'] ?? '');
            if ($messageId <= 0 || $content === '') return;

            try {
                $stmt = $this->getDB()->prepare("UPDATE messages SET content = ?, is_edited = 1 WHERE id = ? AND sender_id = ? AND sender_type = 'user'");
                $stmt->execute([$content, $messageId, $from->vibeUserId]);
                if ($stmt->rowCount() === 0) return;

                $stmt2 = $this->getDB()->prepare("SELECT sender_id, receiver_id FROM messages WHERE id = ?");
                $stmt2->execute([$messageId]);
                $row = $stmt2->fetch();
                if ($row) {
                    $payload = [
                        'type' => 'message_edited',
                        'message_id' => $messageId,
                        'content' => $content
                    ];
                    $this->sendToUser((int)$row['sender_id'], $payload);
                    $this->sendToUser((int)$row['receiver_id'], $payload);
                    
                    // Обновляем списки чатов, так как last_message мог измениться
                    $senderChats = $this->buildChatList((int)$row['sender_id']);
                    $this->sendToUser((int)$row['sender_id'], ['type' => 'chat_list_update', 'chats' => $senderChats]);
                    
                    $receiverChats = $this->buildChatList((int)$row['receiver_id']);
                    $this->sendToUser((int)$row['receiver_id'], ['type' => 'chat_list_update', 'chats' => $receiverChats]);
                    
                    // Отправляем FCM push для получателя, если он оффлайн, с action = 'edit'
                    if (!isset($this->userConnections[(int)$row['receiver_id']])) {
                        $isMutedCheck = $this->getDB()->prepare("SELECT 1 FROM muted_users WHERE user_id = ? AND muted_id = ?");
                        $isMutedCheck->execute([(int)$row['receiver_id'], (int)$row['sender_id']]);
                        if (!$isMutedCheck->fetchColumn()) {
                            $senderName = $this->getUserName((int)$row['sender_id']);
                            $this->sendFcmNotification((int)$row['receiver_id'], $senderName, $content, (int)$row['sender_id'], 'edit');
                        }
                    }
                }
            } catch (\PDOException $e) {
                echo "[DB Error editMessage] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 5.2. УДАЛЕНИЕ СООБЩЕНИЙ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'delete_messages') {
            $userId = $data['user_id'] ?? null;
            $messageIds = $data['message_ids'] ?? [];
            $forEveryone = $data['for_everyone'] ?? false;

            if (!$userId || empty($messageIds) || !is_array($messageIds)) return;

            try {
                $placeholders = implode(',', array_fill(0, count($messageIds), '?'));
                $params = array_merge([$userId, $userId], $messageIds);
                
                // Проверяем, что все сообщения принадлежат данному чату (отправлены или получены юзером)
                $verifyStmt = $this->getDB()->prepare("SELECT id, sender_id, receiver_id FROM messages WHERE (sender_id = ? OR receiver_id = ?) AND id IN ($placeholders)");
                $verifyStmt->execute($params);
                $messages = $verifyStmt->fetchAll();
                
                if (empty($messages)) return;
                
                $affectedPartnerIds = [];
                $actuallyDeletedIds = [];
                
                if ($forEveryone) {
                    // Удаление для всех (только свои сообщения)
                    $ownMessages = array_filter($messages, function($m) use ($userId) { return (int)$m['sender_id'] === (int)$userId; });
                    $ownIds = array_column($ownMessages, 'id');
                    
                    if (!empty($ownIds)) {
                        $delPlaceholders = implode(',', array_fill(0, count($ownIds), '?'));
                        $delParams = array_merge([$userId], $ownIds);
                        $delStmt = $this->getDB()->prepare("DELETE FROM messages WHERE sender_id = ? AND id IN ($delPlaceholders)");
                        $delStmt->execute($delParams);
                        
                        foreach ($ownMessages as $m) {
                            $affectedPartnerIds[(int)$m['receiver_id']] = true;
                            $actuallyDeletedIds[] = (int)$m['id'];
                        }
                    }
                } else {
                    // Удаление только для себя
                    foreach ($messages as $m) {
                        $mId = (int)$m['id'];
                        $isSender = ((int)$m['sender_id'] === (int)$userId);
                        $isReceiver = ((int)$m['receiver_id'] === (int)$userId);
                        
                        if ($isSender) {
                            $update = $this->getDB()->prepare("UPDATE messages SET deleted_by_sender = 1 WHERE id = ?");
                            $update->execute([$mId]);
                        }
                        if ($isReceiver) {
                            $update = $this->getDB()->prepare("UPDATE messages SET deleted_by_receiver = 1 WHERE id = ?");
                            $update->execute([$mId]);
                        }
                        $actuallyDeletedIds[] = $mId;
                    }
                    
                    // Если удалено обоими, стираем из базы окончательно
                    $cleanup = $this->getDB()->prepare("DELETE FROM messages WHERE deleted_by_sender = 1 AND deleted_by_receiver = 1");
                    $cleanup->execute();
                }

                if (!empty($actuallyDeletedIds)) {
                    // Рассылаем событие самому юзеру
                    $payload = [
                        'type' => 'messages_deleted',
                        'message_ids' => $actuallyDeletedIds
                    ];
                    $this->sendToUser((int)$userId, $payload);
                    $this->sendToUser((int)$userId, ['type' => 'chat_list_update', 'chats' => $this->buildChatList((int)$userId)]);
                    
                    // Рассылаем собеседникам, если "Удалить для всех"
                    if ($forEveryone) {
                        foreach (array_keys($affectedPartnerIds) as $partnerId) {
                            $this->sendToUser($partnerId, $payload);
                            $this->sendToUser($partnerId, ['type' => 'chat_list_update', 'chats' => $this->buildChatList($partnerId)]);
                        }
                    }
                }

            } catch (\PDOException $e) {
                echo "[DB Error deleteMessages] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 5.3. ЗАКРЕПЛЕНИЕ СООБЩЕНИЙ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'pin_message') {
            $userId = (int)($data['user_id'] ?? 0);
            $messageId = (int)($data['message_id'] ?? 0);
            $forBoth = $data['for_both'] ?? false;
            if ($userId <= 0 || $messageId <= 0) return;

            try {
                $stmt = $this->getDB()->prepare("SELECT sender_id, receiver_id FROM messages WHERE id = ?");
                $stmt->execute([$messageId]);
                $msgRow = $stmt->fetch();
                if (!$msgRow) return;

                $partnerId = ((int)$msgRow['sender_id'] === $userId) ? (int)$msgRow['receiver_id'] : (int)$msgRow['sender_id'];
                
                $ins = $this->getDB()->prepare("INSERT IGNORE INTO pinned_messages (message_id, pinned_by_id, pinned_for_id) VALUES (?, ?, ?)");
                $ins->execute([$messageId, $userId, $userId]);
                $rowsAffected1 = $ins->rowCount();
                
                if ($forBoth) {
                    $ins->execute([$messageId, $userId, $partnerId]);
                    $rowsAffected2 = $ins->rowCount();
                    
                    if ($rowsAffected1 > 0 || $rowsAffected2 > 0) {
                        $userName = $this->getUserName($userId);
                        $contentStmt = $this->getDB()->prepare("SELECT content FROM messages WHERE id = ?");
                        $contentStmt->execute([$messageId]);
                        $contentRow = $contentStmt->fetch();
                        $shortContent = mb_substr($contentRow['content'] ?? '', 0, 30);
                        if (mb_strlen($contentRow['content'] ?? '') > 30) $shortContent .= '...';
                        
                        $sysContent = '$$SYSTEM$$PINNED_MESSAGE|' . $userName . '|' . $shortContent;
                        $sysStmt = $this->getDB()->prepare("INSERT INTO messages (sender_id, receiver_id, sender_type, content) VALUES (?, ?, 'bot', ?)");
                        $sysStmt->execute([$userId, $partnerId, $sysContent]);
                        $maxStmt = $this->getDB()->query("SELECT MAX(id) FROM messages");
                        $sysMsgId = (int)$maxStmt->fetchColumn();
                        
                        $sysPayload = [
                            'type' => 'chat_message',
                            'message_id' => $sysMsgId,
                            'sender_id' => $userId,
                            'receiver_id' => $partnerId,
                            'sender_type' => 'bot',
                            'content' => $sysContent,
                            'timestamp' => date('Y-m-d H:i:s'),
                            'sender_name' => 'System'
                        ];
                        $this->sendToUser($userId, $sysPayload);
                        if ($partnerId !== $userId) {
                            $this->sendToUser($partnerId, $sysPayload);
                        }
                    }
                }
                
                // Notify clients
                $this->sendToUser($userId, ['type' => 'message_pinned', 'message_id' => $messageId, 'with_user_id' => $partnerId]);
                if ($forBoth && isset($this->userConnections[$partnerId])) {
                    $this->sendToUser($partnerId, ['type' => 'message_pinned', 'message_id' => $messageId, 'with_user_id' => $userId]);
                }

            } catch (\PDOException $e) {
                echo "[DB Error pinMessage] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 5.4. ОТКРЕПЛЕНИЕ СООБЩЕНИЙ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'unpin_message') {
            $userId = (int)($data['user_id'] ?? 0);
            $messageId = (int)($data['message_id'] ?? 0);
            $forBoth = $data['for_both'] ?? false;
            if ($userId <= 0 || $messageId <= 0) return;

            try {
                $stmt = $this->getDB()->prepare("SELECT sender_id, receiver_id FROM messages WHERE id = ?");
                $stmt->execute([$messageId]);
                $msgRow = $stmt->fetch();
                $partnerId = 0;
                if ($msgRow) {
                    $partnerId = ((int)$msgRow['sender_id'] === $userId) ? (int)$msgRow['receiver_id'] : (int)$msgRow['sender_id'];
                }

                if ($forBoth) {
                    $del = $this->getDB()->prepare("DELETE FROM pinned_messages WHERE message_id = ?");
                    $del->execute([$messageId]);
                } else {
                    $del = $this->getDB()->prepare("DELETE FROM pinned_messages WHERE message_id = ? AND pinned_for_id = ?");
                    $del->execute([$messageId, $userId]);
                }
                
                $this->sendToUser($userId, ['type' => 'message_unpinned', 'message_id' => $messageId, 'with_user_id' => $partnerId]);
                if ($forBoth && $partnerId > 0 && isset($this->userConnections[$partnerId])) {
                    $this->sendToUser($partnerId, ['type' => 'message_unpinned', 'message_id' => $messageId, 'with_user_id' => $userId]);
                }
            } catch (\PDOException $e) {
                echo "[DB Error unpinMessage] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 5.5. ЗАГРУЗКА АВАТАРА
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'upload_avatar') {
            $userId = (int)($data['user_id'] ?? 0);
            $base64Data = $data['base64_data'] ?? '';
            
            if ($userId > 0 && !empty($base64Data)) {
                $decoded = base64_decode($base64Data);
                if ($decoded) {
                    $hash = hash('sha256', $decoded);
                    // Пытаемся найти публичную папку www, чтобы файлы были доступны по ссылке
                    $avatarsDir = '/home/flasskdev/www/avatars';

                    
                    if (!is_dir($avatarsDir)) {
                        mkdir($avatarsDir, 0755, true);
                    }
                    $filename = $hash . '.webp';
                    $filepath = $avatarsDir . '/' . $filename;
                    
                    if (!file_exists($filepath)) {
                        file_put_contents($filepath, $decoded);
                    }
                    
                    $avatarUrl = 'https://flasskdev.alwaysdata.net/avatars/' . $filename;
                    
                    try {
                        $stmt = $this->getDB()->prepare("UPDATE users SET avatar_url = ? WHERE id = ?");
                        $stmt->execute([$avatarUrl, $userId]);
                        
                        // Send success back to the user
                        $this->sendToUser($userId, [
                            'type' => 'avatar_uploaded',
                            'user_id' => $userId,
                            'avatar_url' => $avatarUrl
                        ]);
                        
                        // Broadcast updated user info to everyone (for instant UI update)
                        $this->broadcastUserStatus($userId);
                    } catch (\PDOException $e) {
                        echo "[DB Error Upload Avatar] " . $e->getMessage() . "\n";
                    }
                }
            }
            return;
        }

        // ==========================================
        // 5.6. РЕАКЦИИ НА СООБЩЕНИЯ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'send_reaction') {
            $messageId = (int)($data['message_id'] ?? 0);
            $userId = (int)($data['user_id'] ?? 0);
            $emoji = trim($data['emoji'] ?? '');

            if ($messageId <= 0 || $userId <= 0) return;

            try {
                $stmt = $this->getDB()->prepare("SELECT sender_id, receiver_id, reactions FROM messages WHERE id = ?");
                $stmt->execute([$messageId]);
                $msgRow = $stmt->fetch();
                if (!$msgRow) return;

                $senderId = (int)$msgRow['sender_id'];
                $receiverId = (int)$msgRow['receiver_id'];
                
                if ($userId !== $senderId && $userId !== $receiverId) return;

                $currentReactions = !empty($msgRow['reactions']) ? json_decode($msgRow['reactions'], true) : [];
                if (!is_array($currentReactions)) $currentReactions = [];

                $userPreviousEmoji = null;
                foreach ($currentReactions as $r) {
                    $em = $r['emoji'] ?? '';
                    $uIds = isset($r['userIds']) && is_array($r['userIds']) ? $r['userIds'] : [];
                    if (in_array($userId, $uIds)) {
                        $userPreviousEmoji = $em;
                    }
                }

                $nowMillis = (int)(microtime(true) * 1000);
                $updatedReactions = [];
                foreach ($currentReactions as $r) {
                    $em = $r['emoji'] ?? '';
                    $uIds = isset($r['userIds']) && is_array($r['userIds']) ? $r['userIds'] : [];
                    $usersArr = isset($r['users']) && is_array($r['users']) ? $r['users'] : [];
                    
                    $uIds = array_values(array_filter($uIds, function($id) use ($userId) { return (int)$id !== $userId; }));
                    $usersArr = array_values(array_filter($usersArr, function($u) use ($userId) {
                        $uId = is_array($u) ? ($u['userId'] ?? $u['user_id'] ?? 0) : $u;
                        return (int)$uId !== $userId;
                    }));

                    if ($em === $emoji && $userPreviousEmoji !== $emoji && !empty($emoji)) {
                        $uIds[] = $userId;
                        $usersArr[] = ['userId' => $userId, 'timestamp' => $nowMillis];
                    }

                    if (!empty($uIds)) {
                        $updatedReactions[] = [
                            'emoji' => $em,
                            'count' => count($uIds),
                            'userIds' => $uIds,
                            'users' => $usersArr
                        ];
                    }
                }

                if (!empty($emoji) && $userPreviousEmoji !== $emoji) {
                    $exists = false;
                    foreach ($updatedReactions as $ur) {
                        if ($ur['emoji'] === $emoji) {
                            $exists = true;
                            break;
                        }
                    }
                    if (!$exists) {
                        $updatedReactions[] = [
                            'emoji' => $emoji,
                            'count' => 1,
                            'userIds' => [$userId],
                            'users' => [['userId' => $userId, 'timestamp' => $nowMillis]]
                        ];
                    }
                }

                $reactionsJson = !empty($updatedReactions) ? json_encode($updatedReactions, JSON_UNESCAPED_UNICODE) : null;
                $updStmt = $this->getDB()->prepare("UPDATE messages SET reactions = ? WHERE id = ?");
                $updStmt->execute([$reactionsJson, $messageId]);

                $payload = [
                    'type' => 'message_reaction',
                    'message_id' => $messageId,
                    'user_id' => $userId,
                    'emoji' => $emoji,
                    'reactions' => $updatedReactions
                ];

                $this->sendToUser($senderId, $payload);
                if ($receiverId !== $senderId) {
                    $otherUserId = ($userId === $senderId) ? $receiverId : $senderId;
                    if (!$this->isUserBlocked($otherUserId, $userId)) {
                        $this->sendToUser($receiverId, $payload);
                    }
                }

            } catch (\PDOException $e) {
                echo "[DB Error sendReaction] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 5.7. ПОЛУЧЕНИЕ СПИСКА ПОЛЬЗОВАТЕЛЕЙ РЕАКЦИИ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'get_reaction_users') {
            $messageId = (int)($data['message_id'] ?? 0);
            $filterEmoji = trim($data['emoji'] ?? '');
            $offset = max(0, (int)($data['offset'] ?? 0));
            $limit = max(1, min(50, (int)($data['limit'] ?? 20)));

            if ($messageId <= 0) return;

            try {
                $stmt = $this->getDB()->prepare("SELECT reactions FROM messages WHERE id = ?");
                $stmt->execute([$messageId]);
                $msgRow = $stmt->fetch();
                if (!$msgRow) return;

                $currentReactions = !empty($msgRow['reactions']) ? json_decode($msgRow['reactions'], true) : [];
                if (!is_array($currentReactions)) $currentReactions = [];

                $allEntries = [];
                foreach ($currentReactions as $r) {
                    $em = $r['emoji'] ?? '';
                    if (!empty($filterEmoji) && $em !== $filterEmoji) continue;

                    $uIds = isset($r['userIds']) && is_array($r['userIds']) ? $r['userIds'] : [];
                    $usersArr = isset($r['users']) && is_array($r['users']) ? $r['users'] : [];
                    
                    $timestampMap = [];
                    foreach ($usersArr as $u) {
                        if (is_array($u)) {
                            $uId = (int)($u['userId'] ?? $u['user_id'] ?? 0);
                            $ts = (int)($u['timestamp'] ?? 0);
                            if ($uId > 0) $timestampMap[$uId] = $ts;
                        }
                    }

                    foreach ($uIds as $uId) {
                        $uIdInt = (int)$uId;
                        if ($uIdInt > 0) {
                            $allEntries[] = [
                                'userId' => $uIdInt,
                                'emoji' => $em,
                                'timestamp' => $timestampMap[$uIdInt] ?? 0
                            ];
                        }
                    }
                }

                usort($allEntries, function($a, $b) {
                    return $b['timestamp'] <=> $a['timestamp'];
                });

                $total = count($allEntries);
                $pagedEntries = array_slice($allEntries, $offset, $limit);
                $hasMore = ($offset + $limit) < $total;

                $resultUsers = [];
                if (!empty($pagedEntries)) {
                    $userIdsToQuery = array_unique(array_column($pagedEntries, 'userId'));
                    $placeholders = implode(',', array_fill(0, count($userIdsToQuery), '?'));
                    
                    $uStmt = $this->getDB()->prepare("SELECT id, name, username, avatar_url, is_verified, is_developer, is_bot FROM users WHERE id IN ($placeholders)");
                    $uStmt->execute(array_values($userIdsToQuery));
                    $usersData = $uStmt->fetchAll();
                    $userMap = [];
                    foreach ($usersData as $ud) {
                        $userMap[(int)$ud['id']] = $ud;
                    }

                    foreach ($pagedEntries as $pe) {
                        $uInfo = $userMap[$pe['userId']] ?? null;
                        $resultUsers[] = [
                            'userId' => $pe['userId'],
                            'name' => $uInfo['name'] ?? null,
                            'username' => $uInfo['username'] ?? null,
                            'avatarUrl' => $uInfo['avatar_url'] ?? null,
                            'isVerified' => (bool)($uInfo['is_verified'] ?? false),
                            'isDeveloper' => (bool)($uInfo['is_developer'] ?? false),
                            'isBot' => (bool)($uInfo['is_bot'] ?? false),
                            'emoji' => $pe['emoji'],
                            'timestamp' => $pe['timestamp']
                        ];
                    }
                }

                $from->send(json_encode([
                    'type' => 'reaction_users_result',
                    'message_id' => $messageId,
                    'emoji' => $filterEmoji ?: null,
                    'offset' => $offset,
                    'has_more' => $hasMore,
                    'users' => $resultUsers
                ], JSON_UNESCAPED_UNICODE));

            } catch (\PDOException $e) {
                echo "[DB Error getReactionUsers] " . $e->getMessage() . "\n";
            }
            return;
        }


        // ==========================================
        // 5.6. ОТКРЕПЛЕНИЕ ВСЕХ СООБЩЕНИЙ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'unpin_all_messages') {
            $userId = (int)($data['user_id'] ?? 0);
            $withUserId = (int)($data['with_user_id'] ?? 0);
            $forBoth = $data['for_both'] ?? false;
            if ($userId <= 0 || $withUserId <= 0) return;

            try {
                if ($forBoth) {
                    $del = $this->getDB()->prepare("
                        DELETE pm FROM pinned_messages pm
                        JOIN messages m ON pm.message_id = m.id
                        WHERE ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))
                    ");
                    $del->execute([$userId, $withUserId, $withUserId, $userId]);
                    if (isset($this->userConnections[$withUserId])) {
                        $this->sendToUser($withUserId, ['type' => 'all_messages_unpinned', 'with_user_id' => $userId]);
                    }
                } else {
                    $del = $this->getDB()->prepare("
                        DELETE pm FROM pinned_messages pm
                        JOIN messages m ON pm.message_id = m.id
                        WHERE pm.pinned_for_id = ? AND ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))
                    ");
                    $del->execute([$userId, $userId, $withUserId, $withUserId, $userId]);
                }
                $this->sendToUser($userId, ['type' => 'all_messages_unpinned', 'with_user_id' => $withUserId]);
            } catch (\PDOException $e) {
                echo "[DB Error unpinAllMessages] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 6. ЗАГРУЗКА ИСТОРИИ СООБЩЕНИЙ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'load_messages') {
            $userId = (int)($data['user_id'] ?? 0);
            $withUserId = (int)($data['with_user_id'] ?? 0);
            $offset = (int)($data['offset'] ?? 0);
            $limit = 50;

            if ($userId <= 0 || $withUserId <= 0) return;

            try {
                $stmt = $this->getDB()->prepare("
                    SELECT m.id, m.sender_id, m.receiver_id, m.sender_type, m.content, m.timestamp, m.is_read, m.reply_to_id, m.is_edited, m.attachments, m.reactions, m.reply_markup,
                           r.content AS reply_to_content, COALESCE(ru.name, rb.name) AS reply_to_sender_name,
                           m.forwarded_from_id, COALESCE(fu.name, fb.name) AS forwarded_from_name
                    FROM messages m
                    LEFT JOIN messages r ON m.reply_to_id = r.id
                    LEFT JOIN users ru ON ru.id = r.sender_id AND r.sender_type = 'user'
                    LEFT JOIN bots rb ON rb.id = r.sender_id AND r.sender_type = 'bot'
                    LEFT JOIN users fu ON fu.id = m.forwarded_from_id
                    LEFT JOIN bots fb ON fb.id = m.forwarded_from_id
                    WHERE ((m.sender_id = ? AND m.receiver_id = ?) 
                       OR (m.sender_id = ? AND m.receiver_id = ?))
                       AND ((m.sender_id = $userId AND m.deleted_by_sender = 0) OR (m.receiver_id = $userId AND m.deleted_by_receiver = 0))
                    ORDER BY m.timestamp DESC, m.id DESC
                    LIMIT ? OFFSET ?
                ");
                $stmt->execute([$userId, $withUserId, $withUserId, $userId, $limit, $offset]);
                $messages = $stmt->fetchAll();

                $messages = array_reverse($messages);

                foreach ($messages as &$m) {
                    $m['id'] = (int)$m['id'];
                    $m['sender_id'] = (int)$m['sender_id'];
                    $m['receiver_id'] = (int)$m['receiver_id'];
                    $m['sender_type'] = $m['sender_type'] ?? 'user';
                    $m['is_read'] = (bool)$m['is_read'];
                    $m['reply_to_id'] = $m['reply_to_id'] ? (int)$m['reply_to_id'] : null;
                    $m['reply_to_content'] = $m['reply_to_content'] ?? null;
                    $m['reply_to_sender_name'] = $m['reply_to_sender_name'] ?? null;
                    $m['forwarded_from_id'] = $m['forwarded_from_id'] ? (int)$m['forwarded_from_id'] : null;
                    if ($m['forwarded_from_id'] && !$this->checkPrivacyAccess($m['forwarded_from_id'], $userId, 'forwarded')) {
                        $m['forwarded_from_id'] = -1;
                    }
                    $m['forwarded_from_name'] = $m['forwarded_from_name'] ?? null;
                    $m['is_edited'] = (bool)($m['is_edited'] ?? 0);
                    $m['attachments'] = isset($m['attachments']) && $m['attachments'] ? json_decode($m['attachments'], true) : null;
                    $m['reactions'] = isset($m['reactions']) && $m['reactions'] ? json_decode($m['reactions'], true) : null;
                    $m['reply_markup'] = isset($m['reply_markup']) && $m['reply_markup'] ? json_decode($m['reply_markup'], true) : null;
                }

                $from->send(json_encode([
                    'type' => 'load_messages_result',
                    'with_user_id' => $withUserId,
                    'messages' => $messages,
                    'offset' => $offset
                ]));
                
                if ($offset === 0) {
                    $pinStmt = $this->getDB()->prepare("
                        SELECT p.message_id
                        FROM pinned_messages p
                        JOIN messages m ON p.message_id = m.id
                        WHERE p.pinned_for_id = ? AND ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))
                    ");
                    $pinStmt->execute([$userId, $userId, $withUserId, $withUserId, $userId]);
                    $pinnedIds = $pinStmt->fetchAll(\PDO::FETCH_COLUMN);
                    
                    $from->send(json_encode([
                        'type' => 'pinned_messages_loaded',
                        'with_user_id' => $withUserId,
                        'pinned_message_ids' => array_map('intval', $pinnedIds)
                    ]));
                }
            } catch (\PDOException $e) {
                echo "[DB Error loadMessages] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 6.1. ЗАГРУЗКА ИСТОРИИ ВОКРУГ СООБЩЕНИЯ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'load_messages_around') {
            $userId = (int)($data['user_id'] ?? 0);
            $withUserId = (int)($data['with_user_id'] ?? 0);
            $aroundMessageId = (int)($data['around_message_id'] ?? 0);
            $limitHalf = 20;

            if ($userId <= 0 || $withUserId <= 0 || $aroundMessageId <= 0) return;

            try {
                // Get timestamp of the target message
                $tsStmt = $this->getDB()->prepare("SELECT timestamp FROM messages WHERE id = ?");
                $tsStmt->execute([$aroundMessageId]);
                $tsRow = $tsStmt->fetch();
                if (!$tsRow) return;
                $targetTs = $tsRow['timestamp'];

                // Load older messages including the target one
                $stmtOlder = $this->getDB()->prepare("
                    SELECT m.id, m.sender_id, m.receiver_id, m.sender_type, m.content, m.timestamp, m.is_read, m.reply_to_id, m.is_edited, m.attachments, m.reactions, m.reply_markup,
                           r.content AS reply_to_content, COALESCE(ru.name, rb.name) AS reply_to_sender_name,
                           m.forwarded_from_id, COALESCE(fu.name, fb.name) AS forwarded_from_name
                    FROM messages m
                    LEFT JOIN messages r ON m.reply_to_id = r.id
                    LEFT JOIN users ru ON ru.id = r.sender_id AND r.sender_type = 'user'
                    LEFT JOIN bots rb ON rb.id = r.sender_id AND r.sender_type = 'bot'
                    LEFT JOIN users fu ON fu.id = m.forwarded_from_id
                    LEFT JOIN bots fb ON fb.id = m.forwarded_from_id
                    WHERE ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))
                      AND ((m.sender_id = $userId AND m.deleted_by_sender = 0) OR (m.receiver_id = $userId AND m.deleted_by_receiver = 0))
                      AND (m.timestamp < ? OR (m.timestamp = ? AND m.id <= ?))
                    ORDER BY m.timestamp DESC, m.id DESC
                    LIMIT ?
                ");
                $stmtOlder->execute([$userId, $withUserId, $withUserId, $userId, $targetTs, $targetTs, $aroundMessageId, $limitHalf + 1]);
                $olderMessages = $stmtOlder->fetchAll();

                // Load newer messages
                $stmtNewer = $this->getDB()->prepare("
                    SELECT m.id, m.sender_id, m.receiver_id, m.sender_type, m.content, m.timestamp, m.is_read, m.reply_to_id, m.is_edited, m.attachments, m.reactions, m.reply_markup,
                           r.content AS reply_to_content, COALESCE(ru.name, rb.name) AS reply_to_sender_name,
                           m.forwarded_from_id, COALESCE(fu.name, fb.name) AS forwarded_from_name
                    FROM messages m
                    LEFT JOIN messages r ON m.reply_to_id = r.id
                    LEFT JOIN users ru ON ru.id = r.sender_id AND r.sender_type = 'user'
                    LEFT JOIN bots rb ON rb.id = r.sender_id AND r.sender_type = 'bot'
                    LEFT JOIN users fu ON fu.id = m.forwarded_from_id
                    LEFT JOIN bots fb ON fb.id = m.forwarded_from_id
                    WHERE ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))
                      AND ((m.sender_id = $userId AND m.deleted_by_sender = 0) OR (m.receiver_id = $userId AND m.deleted_by_receiver = 0))
                      AND (m.timestamp > ? OR (m.timestamp = ? AND m.id > ?))
                    ORDER BY m.timestamp ASC, m.id ASC
                    LIMIT ?
                ");
                $stmtNewer->execute([$userId, $withUserId, $withUserId, $userId, $targetTs, $targetTs, $aroundMessageId, $limitHalf]);
                $newerMessages = $stmtNewer->fetchAll();

                $messages = array_merge(array_reverse($olderMessages), $newerMessages);

                foreach ($messages as &$m) {
                    $m['id'] = (int)$m['id'];
                    $m['sender_id'] = (int)$m['sender_id'];
                    $m['receiver_id'] = (int)$m['receiver_id'];
                    $m['sender_type'] = $m['sender_type'] ?? 'user';
                    $m['is_read'] = (bool)$m['is_read'];
                    $m['reply_to_id'] = $m['reply_to_id'] ? (int)$m['reply_to_id'] : null;
                    $m['reply_to_content'] = $m['reply_to_content'] ?? null;
                    $m['reply_to_sender_name'] = $m['reply_to_sender_name'] ?? null;
                    $m['forwarded_from_id'] = $m['forwarded_from_id'] ? (int)$m['forwarded_from_id'] : null;
                    if ($m['forwarded_from_id'] && !$this->checkPrivacyAccess($m['forwarded_from_id'], $userId, 'forwarded')) {
                        $m['forwarded_from_id'] = -1;
                    }
                    $m['forwarded_from_name'] = $m['forwarded_from_name'] ?? null;
                    $m['is_edited'] = (bool)($m['is_edited'] ?? 0);
                    $m['attachments'] = isset($m['attachments']) && $m['attachments'] ? json_decode($m['attachments'], true) : null;
                    $m['reactions'] = isset($m['reactions']) && $m['reactions'] ? json_decode($m['reactions'], true) : null;
                    $m['reply_markup'] = isset($m['reply_markup']) && $m['reply_markup'] ? json_decode($m['reply_markup'], true) : null;
                }

                $from->send(json_encode([
                    'type' => 'load_messages_around_result',
                    'with_user_id' => $withUserId,
                    'messages' => $messages
                ]));
            } catch (\PDOException $e) {
                echo "[MSG Load Around Error] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 7. ЗАГРУЗКА СПИСКА ЧАТОВ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'load_chats') {
            $userId = (int)($data['user_id'] ?? 0);
            if ($userId <= 0) return;

            $chats = $this->buildChatList($userId);
            $from->send(json_encode([
                'type' => 'load_chats_result',
                'chats' => $chats
            ]));
            return;
        }

        // ==========================================
        // 8. ОТМЕТИТЬ СООБЩЕНИЯ ПРОЧИТАННЫМИ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'mark_read') {
            $userId = (int)($data['user_id'] ?? 0);
            $fromUserId = (int)($data['from_user_id'] ?? 0);
            $upToMessageId = isset($data['up_to_message_id']) ? (int)$data['up_to_message_id'] : null;

            if ($userId <= 0 || $fromUserId <= 0) return;

            try {
                if ($upToMessageId !== null && $upToMessageId > 0) {
                    $stmt = $this->getDB()->prepare("
                        UPDATE messages SET is_read = 1 
                        WHERE sender_id = ? AND receiver_id = ? AND is_read = 0 AND id <= ?
                    ");
                    $stmt->execute([$fromUserId, $userId, $upToMessageId]);
                } else {
                    $stmt = $this->getDB()->prepare("
                        UPDATE messages SET is_read = 1 
                        WHERE sender_id = ? AND receiver_id = ? AND is_read = 0
                    ");
                    $stmt->execute([$fromUserId, $userId]);
                }

                $from->send(json_encode([
                    'type' => 'mark_read_result',
                    'success' => true,
                    'from_user_id' => $fromUserId,
                    'up_to_message_id' => $upToMessageId
                ]));

                $chats = $this->buildChatList($userId);
                $this->sendToUser($userId, [
                    'type' => 'chat_list_update',
                    'chats' => $chats
                ]);
            } catch (\PDOException $e) {
                echo "[DB Error markRead] " . $e->getMessage() . "\n";
            }

            // НОВОЕ: Сообщаем ОТПРАВИТЕЛЮ, что его сообщения прочитали в реальном времени
            $this->sendToUser($fromUserId, [
                'type' => 'messages_read_by_partner',
                'partner_id' => $userId,
                'up_to_message_id' => $upToMessageId
            ]);
            
            // Обновляем список чатов отправителя, чтобы синие галочки появились и там
            $senderChats = $this->buildChatList($fromUserId);
            $this->sendToUser($fromUserId, [
                'type' => 'chat_list_update',
                'chats' => $senderChats
            ]);
            return;
        }

        // ==========================================
        // 9. ИНДИКАТОР "ПЕЧАТАЕТ..."
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'typing') {
            $senderId = (int)($data['sender_id'] ?? 0);
            $receiverId = (int)($data['receiver_id'] ?? 0);

            if ($senderId <= 0 || $receiverId <= 0) return;

            $senderName = $this->getUserName($senderId);
            $this->sendToUser($receiverId, [
                'type' => 'typing_indicator',
                'sender_id' => $senderId,
                'sender_name' => $senderName
            ]);
            return;
        }

        if (isset($data['type']) && $data['type'] == 'stop_typing') {
            $senderId = (int)($data['sender_id'] ?? 0);
            $receiverId = (int)($data['receiver_id'] ?? 0);

            if ($senderId <= 0 || $receiverId <= 0) return;

            $this->sendToUser($receiverId, [
                'type' => 'typing_indicator_stop',
                'sender_id' => $senderId
            ]);
            return;
        }

        // ==========================================
        // 10. СТИКЕРПАКИ
        // ==========================================
        if (isset($data['type']) && $data['type'] === 'get_sticker_packs') {
            $userId = isset($from->vibeUserId) ? (int)$from->vibeUserId : 0;
            if ($userId <= 0) {
                $this->sendStickerPackError($from, 'unauthorized', 'Сначала подключитесь к серверу');
                return;
            }

            try {
                $stmt = $this->getDB()->prepare("
                    SELECT p.*, COALESCE(up.position, 0) AS position
                    FROM flasskdev_mobilestickerpacks p
                    LEFT JOIN flasskdev_mobileuserstickerpacks up
                        ON up.pack_id = p.id AND up.user_id = ?
                    WHERE p.owner = ? OR up.user_id IS NOT NULL
                    ORDER BY CASE WHEN p.owner = ? THEN 0 ELSE 1 END, position ASC, p.id ASC
                ");
                $stmt->execute([$userId, $userId, $userId]);
                $rows = $stmt->fetchAll();

                $packs = [];
                foreach ($rows as $row) {
                    $packs[] = $this->stickerPackRow($row, true, $userId);
                }
                $from->send(json_encode([
                    'type' => 'sticker_packs_result',
                    'packs' => $packs
                ], JSON_UNESCAPED_UNICODE));
            } catch (\PDOException $e) {
                echo "[DB Error getStickerPacks] " . $e->getMessage() . "\n";
                $this->sendStickerPackError($from, 'database_error', 'Не удалось загрузить стикерпаки');
            }
            return;
        }

        if (isset($data['type']) && $data['type'] === 'search_sticker_packs') {
            $userId = isset($from->vibeUserId) ? (int)$from->vibeUserId : 0;
            $query = trim((string)($data['query'] ?? ''));
            if ($userId <= 0) {
                $this->sendStickerPackError($from, 'unauthorized', 'Сначала подключитесь к серверу');
                return;
            }
            $query = function_exists('mb_substr') ? mb_substr($query, 0, 64) : substr($query, 0, 64);

            try {
                if ($query === '') {
                    $stmt = $this->getDB()->prepare("
                        SELECT p.*, (up.user_id IS NOT NULL) AS installed,
                            (SELECT COUNT(*) FROM flasskdev_mobileuserstickerpacks x WHERE x.pack_id = p.id) AS installs
                        FROM flasskdev_mobilestickerpacks p
                        LEFT JOIN flasskdev_mobileuserstickerpacks up
                            ON up.pack_id = p.id AND up.user_id = ?
                        ORDER BY installs DESC, p.id DESC
                        LIMIT 30
                    ");
                    $stmt->execute([$userId]);
                } else {
                    $stmt = $this->getDB()->prepare("
                        SELECT p.*, (up.user_id IS NOT NULL) AS installed
                        FROM flasskdev_mobilestickerpacks p
                        LEFT JOIN flasskdev_mobileuserstickerpacks up
                            ON up.pack_id = p.id AND up.user_id = ?
                        WHERE p.name LIKE ?
                        ORDER BY (p.name = ?) DESC, CHAR_LENGTH(p.name) ASC, p.id DESC
                        LIMIT 30
                    ");
                    $stmt->execute([$userId, '%' . $query . '%', $query]);
                }

                $packs = [];
                foreach ($stmt->fetchAll() as $row) {
                    $installed = ((int)($row['installed'] ?? 0) === 1) || ((int)$row['owner'] === $userId);
                    $packs[] = $this->stickerPackRow($row, $installed, $userId);
                }
                $from->send(json_encode([
                    'type' => 'sticker_packs_search_result',
                    'query' => $query,
                    'packs' => $packs
                ], JSON_UNESCAPED_UNICODE));
            } catch (\PDOException $e) {
                echo "[DB Error searchStickerPacks] " . $e->getMessage() . "\n";
                $this->sendStickerPackError($from, 'database_error', 'Не удалось найти стикерпаки');
            }
            return;
        }

        if (isset($data['type']) && $data['type'] === 'add_sticker_pack') {
            $userId = isset($from->vibeUserId) ? (int)$from->vibeUserId : 0;
            $packId = (int)($data['pack_id'] ?? 0);
            if ($userId <= 0) {
                $this->sendStickerPackError($from, 'unauthorized', 'Сначала подключитесь к серверу');
                return;
            }
            if ($packId <= 0) {
                $this->sendStickerPackError($from, 'invalid_pack', 'Некорректный идентификатор стикерпака');
                return;
            }

            try {
                $db = $this->getDB();
                $check = $db->prepare("SELECT owner FROM flasskdev_mobilestickerpacks WHERE id = ? LIMIT 1");
                $check->execute([$packId]);
                $ownerId = $check->fetchColumn();
                if ($ownerId === false) {
                    $this->sendStickerPackError($from, 'not_found', 'Стикерпак не найден');
                    return;
                }

                // Owner packs are already available without an installation row.
                if ((int)$ownerId !== $userId) {
                    $positionStmt = $db->prepare("SELECT COALESCE(MAX(position), 0) + 1 FROM flasskdev_mobileuserstickerpacks WHERE user_id = ?");
                    $positionStmt->execute([$userId]);
                    $position = (int)$positionStmt->fetchColumn();

                    $install = $db->prepare("
                        INSERT INTO flasskdev_mobileuserstickerpacks (user_id, pack_id, position)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE position = position
                    ");
                    $install->execute([$userId, $packId, $position]);
                }

                $from->send(json_encode([
                    'type' => 'sticker_pack_added',
                    'pack_id' => $packId
                ], JSON_UNESCAPED_UNICODE));
            } catch (\PDOException $e) {
                echo "[DB Error addStickerPack] " . $e->getMessage() . "\n";
                $this->sendStickerPackError($from, 'database_error', 'Не удалось добавить стикерпак');
            }
            return;
        }

        if (isset($data['type']) && $data['type'] === 'remove_sticker_pack') {
            $userId = isset($from->vibeUserId) ? (int)$from->vibeUserId : 0;
            $packId = (int)($data['pack_id'] ?? 0);
            if ($userId <= 0) {
                $this->sendStickerPackError($from, 'unauthorized', 'Сначала подключитесь к серверу');
                return;
            }
            if ($packId <= 0) {
                $this->sendStickerPackError($from, 'invalid_pack', 'Некорректный идентификатор стикерпака');
                return;
            }

            try {
                $db = $this->getDB();
                $ownerStmt = $db->prepare("SELECT owner FROM flasskdev_mobilestickerpacks WHERE id = ? LIMIT 1");
                $ownerStmt->execute([$packId]);
                $ownerId = $ownerStmt->fetchColumn();
                if ($ownerId === false) {
                    $this->sendStickerPackError($from, 'not_found', 'Стикерпак не найден');
                    return;
                }

                $db->beginTransaction();
                if ((int)$ownerId === $userId) {
                    $deleteInstalls = $db->prepare("DELETE FROM flasskdev_mobileuserstickerpacks WHERE pack_id = ?");
                    $deleteInstalls->execute([$packId]);
                    $deletePack = $db->prepare("DELETE FROM flasskdev_mobilestickerpacks WHERE id = ? AND owner = ?");
                    $deletePack->execute([$packId, $userId]);
                } else {
                    $deleteInstall = $db->prepare("DELETE FROM flasskdev_mobileuserstickerpacks WHERE user_id = ? AND pack_id = ?");
                    $deleteInstall->execute([$userId, $packId]);
                }
                $db->commit();

                $from->send(json_encode([
                    'type' => 'sticker_pack_removed',
                    'pack_id' => $packId
                ], JSON_UNESCAPED_UNICODE));
            } catch (\PDOException $e) {
                if (isset($db) && $db->inTransaction()) $db->rollBack();
                echo "[DB Error removeStickerPack] " . $e->getMessage() . "\n";
                $this->sendStickerPackError($from, 'database_error', 'Не удалось удалить стикерпак');
            }
            return;
        }

        // ==========================================
        // 11. ПОИСК ПОЛЬЗОВАТЕЛЕЙ
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'search_users') {
            $query = trim($data['query'] ?? '');
            $currentUserId = (int)($data['user_id'] ?? 0);

            if ($query === '' || $currentUserId <= 0) return;

            try {
                $stmt = $this->getDB()->prepare("
                    SELECT id, name, username, avatar_url, is_verified, is_developer, 0 AS is_bot, is_banned, is_freezed
                    FROM users 
                    WHERE (username LIKE ? OR name LIKE ?) AND id != ?
                    UNION
                    SELECT id, name, username, NULL AS avatar_url, is_verified, 0 AS is_developer, 1 AS is_bot, 0 AS is_banned, 0 AS is_freezed
                    FROM bots 
                    WHERE (username LIKE ? OR name LIKE ?) AND id != ?
                    LIMIT 20
                ");
                $searchTerm = "%$query%";
                $stmt->execute([$searchTerm, $searchTerm, $currentUserId, $searchTerm, $searchTerm, $currentUserId]);
                $users = $stmt->fetchAll();

                foreach ($users as &$u) {
                    $u['id'] = (int)$u['id'];
                    $u['is_verified'] = (bool)$u['is_verified'];
                    $u['is_developer'] = (bool)$u['is_developer'];
                    $u['is_bot'] = (bool)$u['is_bot'];
                    
                    if (!$u['is_bot']) {
                        if (!$this->checkPrivacyAccess($u['id'], $currentUserId, 'avatar')) {
                            $u['avatar_url'] = null;
                        }
                    }
                }

                $from->send(json_encode([
                    'type' => 'search_users_result',
                    'users' => $users
                ]));
            } catch (\PDOException $e) {
                echo "[DB Error searchUsers] " . $e->getMessage() . "\n";
            }
            return;
        }

        // ==========================================
        // 11. ПОЛУЧЕНИЕ ИНФОРМАЦИИ О ПОЛЬЗОВАТЕЛЕ (ОНЛАЙН)
        // ==========================================
        if (isset($data['type']) && $data['type'] == 'get_user_info') {
            $targetId = (int)($data['target_id'] ?? 0);
            $requesterId = isset($from->vibeUserId) ? (int)$from->vibeUserId : 0;
            
            if ($targetId > 0 && $requesterId > 0) {
                try {
                    $stmt = $this->getDB()->prepare("SELECT name, username, avatar_url, is_online, UNIX_TIMESTAMP(last_seen) * 1000 AS last_seen, is_developer, is_verified, UNIX_TIMESTAMP(register_date) * 1000 AS register_date, about, is_banned, is_freezed FROM users WHERE id = ?");
                    $stmt->execute([$targetId]);
                    $row = $stmt->fetch();
                    if ($row) {
                        $isBanned = (int)$row['is_banned'] === 1;
                        $isFreezed = (int)$row['is_freezed'] === 1;
                        
                        $isBlockedByUser = $this->isUserBlocked($targetId, $requesterId);
                        $isBlockedByMe = $this->isUserBlocked($requesterId, $targetId);
                        
                        if ($isBanned || $isFreezed || $isBlockedByUser) {
                            $row['avatar_url'] = null;
                            $row['is_online'] = 0;
                            $row['last_seen'] = null;
                        }
                        
                        $avatarUrl = $row['avatar_url'];
                        if (!$this->checkPrivacyAccess($targetId, $requesterId, 'avatar')) {
                            $avatarUrl = null;
                        }
                        
                        $about = $row['about'];
                        if (!$this->checkPrivacyAccess($targetId, $requesterId, 'status') || $isBlockedByUser) {
                            $about = null;
                        }
                        
                        $isOnline = (int)$row['is_online'] === 1;
                        $realLastSeen = $row['last_seen'] ? (int)$row['last_seen'] : null;
                        $lastSeenStatus = null;
                        
                        if ($isBlockedByUser) {
                            $isOnline = false;
                            $realLastSeen = null;
                            $lastSeenStatus = 'long_ago';
                        } elseif (!$this->checkPrivacyAccess($targetId, $requesterId, 'activity')) {
                            $isOnline = false;
                            if ($realLastSeen !== null) {
                                $diff = time() - ($realLastSeen / 1000);
                                if ($diff < 3 * 86400) $lastSeenStatus = 'recently';
                                elseif ($diff < 7 * 86400) $lastSeenStatus = 'this_week';
                                elseif ($diff < 30 * 86400) $lastSeenStatus = 'this_month';
                                else $lastSeenStatus = 'long_ago';
                            }
                            $realLastSeen = null; // hide exact timestamp
                        }
                        
                        if ($isBanned || $isFreezed) {
                            $lastSeenStatus = 'long_ago';
                        }
                        $canMessage = $this->checkPrivacyAccess($targetId, $requesterId, 'messages');
                        
                        $from->send(json_encode([
                            'type' => 'user_info_result',
                            'user_id' => $targetId,
                            'name' => $row['name'],
                            'username' => $row['username'],
                            'avatar_url' => $avatarUrl,
                            'is_online' => $isOnline,
                            'last_seen' => $realLastSeen,
                            'last_seen_status' => $lastSeenStatus,
                            'is_developer' => (int)$row['is_developer'] === 1,
                            'is_verified' => (int)$row['is_verified'] === 1,
                            'register_date' => $row['register_date'] ? (int)$row['register_date'] : null,
                            'is_bot' => false,
                            'about' => $about,
                            'can_message' => $canMessage,
                            'is_banned' => $isBanned,
                            'is_freezed' => $isFreezed,
                            'is_blocked_by_me' => $isBlockedByMe,
                            'is_blocked_by_user' => $isBlockedByUser
                        ]));
                    } else {
                        $stmtBot = $this->getDB()->prepare("SELECT name, username, is_verified, about FROM bots WHERE id = ?");
                        $stmtBot->execute([$targetId]);
                        $rowBot = $stmtBot->fetch();
                        if ($rowBot) {
                            $from->send(json_encode([
                                'type' => 'user_info_result',
                                'user_id' => $targetId,
                                'name' => $rowBot['name'],
                                'username' => $rowBot['username'],
                                'is_online' => true,
                                'last_seen' => null,
                                'is_developer' => false,
                                'is_verified' => (int)$rowBot['is_verified'] === 1,
                                'register_date' => null,
                                'is_bot' => true,
                                'about' => $rowBot['about']
                            ]));
                        }
                    }
                } catch (\PDOException $e) {}
            }
        }

        // ==========================================
        // 12. МУТ/РАЗМУТ ПОЛЬЗОВАТЕЛЕЙ
        // ==========================================
        if (isset($data['type']) && ($data['type'] == 'mute_user' || $data['type'] == 'unmute_user')) {
            $userId = (int)($data['user_id'] ?? 0);
            $mutedId = (int)($data['muted_id'] ?? 0);
            
            if ($userId > 0 && $mutedId > 0) {
                try {
                    if ($data['type'] == 'mute_user') {
                        $stmt = $this->getDB()->prepare("INSERT IGNORE INTO muted_users (user_id, muted_id) VALUES (?, ?)");
                        $stmt->execute([$userId, $mutedId]);
                    } else {
                        $stmt = $this->getDB()->prepare("DELETE FROM muted_users WHERE user_id = ? AND muted_id = ?");
                        $stmt->execute([$userId, $mutedId]);
                    }
                    
                    // Обновляем список чатов
                    $chats = $this->buildChatList($userId);
                    $this->sendToUser($userId, [
                        'type' => 'chat_list_update',
                        'chats' => $chats
                    ]);
                } catch (\PDOException $e) {
                    echo "[DB Error muteUser] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        // ==========================================
        // 12.1. БЛОКИРОВКА / РАЗБЛОКИРОВКА ПОЛЬЗОВАТЕЛЕЙ
        // ==========================================
        if (isset($data['type']) && $data['type'] === 'block_user') {
            $userId = (int)($data['user_id'] ?? 0);
            $blockedId = (int)($data['blocked_id'] ?? 0);
            if ($userId > 0 && $blockedId > 0 && $userId !== $blockedId) {
                try {
                    $stmt = $this->getDB()->prepare("INSERT IGNORE INTO blocked_users (user_id, blocked_id) VALUES (?, ?)");
                    $stmt->execute([$userId, $blockedId]);
                    $from->send(json_encode([
                        'type' => 'block_user_success',
                        'blocked_id' => $blockedId
                    ]));
                    
                    // Обновляем статусы и чаты для обоих
                    $this->broadcastUserStatus($userId);
                    $this->broadcastUserStatus($blockedId);

                    $this->sendToUser($userId, [
                        'type' => 'chat_list_update',
                        'chats' => $this->buildChatList($userId)
                    ]);
                    $this->sendToUser($blockedId, [
                        'type' => 'chat_list_update',
                        'chats' => $this->buildChatList($blockedId)
                    ]);
                } catch (\PDOException $e) {
                    echo "[DB Error block_user] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        if (isset($data['type']) && $data['type'] === 'unblock_user') {
            $userId = (int)($data['user_id'] ?? 0);
            $blockedId = (int)($data['blocked_id'] ?? 0);
            if ($userId > 0 && $blockedId > 0) {
                try {
                    $stmt = $this->getDB()->prepare("DELETE FROM blocked_users WHERE user_id = ? AND blocked_id = ?");
                    $stmt->execute([$userId, $blockedId]);
                    $from->send(json_encode([
                        'type' => 'unblock_user_success',
                        'blocked_id' => $blockedId
                    ]));
                    
                    // Обновляем статусы и чаты для обоих
                    $this->broadcastUserStatus($userId);
                    $this->broadcastUserStatus($blockedId);

                    $this->sendToUser($userId, [
                        'type' => 'chat_list_update',
                        'chats' => $this->buildChatList($userId)
                    ]);
                    $this->sendToUser($blockedId, [
                        'type' => 'chat_list_update',
                        'chats' => $this->buildChatList($blockedId)
                    ]);
                } catch (\PDOException $e) {
                    echo "[DB Error unblock_user] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        if (isset($data['type']) && $data['type'] === 'get_blocked_users') {
            $userId = (int)($data['user_id'] ?? 0);
            $page = max(1, (int)($data['page'] ?? 1));
            $limit = max(1, min(100, (int)($data['limit'] ?? 30)));
            $offset = ($page - 1) * $limit;
            $query = trim($data['query'] ?? '');
            
            if ($userId > 0) {
                try {
                    if (!empty($query)) {
                        $countStmt = $this->getDB()->prepare("SELECT COUNT(*) FROM blocked_users b JOIN users u ON b.blocked_id = u.id WHERE b.user_id = ? AND (u.name LIKE ? OR u.username LIKE ?)");
                        $searchTerm = "%$query%";
                        $countStmt->execute([$userId, $searchTerm, $searchTerm]);
                        $totalCount = (int)$countStmt->fetchColumn();

                        $stmt = $this->getDB()->prepare("SELECT u.id, u.name, u.username, u.avatar_url, u.is_verified, u.is_developer, u.is_banned, u.is_freezed FROM blocked_users b JOIN users u ON b.blocked_id = u.id WHERE b.user_id = ? AND (u.name LIKE ? OR u.username LIKE ?) ORDER BY b.id DESC LIMIT ? OFFSET ?");
                        $stmt->bindValue(1, $userId, PDO::PARAM_INT);
                        $stmt->bindValue(2, $searchTerm, PDO::PARAM_STR);
                        $stmt->bindValue(3, $searchTerm, PDO::PARAM_STR);
                        $stmt->bindValue(4, $limit, PDO::PARAM_INT);
                        $stmt->bindValue(5, $offset, PDO::PARAM_INT);
                        $stmt->execute();
                    } else {
                        $countStmt = $this->getDB()->prepare("SELECT COUNT(*) FROM blocked_users WHERE user_id = ?");
                        $countStmt->execute([$userId]);
                        $totalCount = (int)$countStmt->fetchColumn();

                        $stmt = $this->getDB()->prepare("SELECT u.id, u.name, u.username, u.avatar_url, u.is_verified, u.is_developer, u.is_banned, u.is_freezed FROM blocked_users b JOIN users u ON b.blocked_id = u.id WHERE b.user_id = ? ORDER BY b.id DESC LIMIT ? OFFSET ?");
                        $stmt->bindValue(1, $userId, PDO::PARAM_INT);
                        $stmt->bindValue(2, $limit, PDO::PARAM_INT);
                        $stmt->bindValue(3, $offset, PDO::PARAM_INT);
                        $stmt->execute();
                    }

                    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
                    $users = array_map(function($r) {
                        return [
                            'id' => (int)$r['id'],
                            'name' => $r['name'],
                            'username' => $r['username'],
                            'avatar_url' => $r['avatar_url'],
                            'is_verified' => (int)$r['is_verified'] === 1,
                            'is_developer' => (int)$r['is_developer'] === 1,
                            'is_bot' => false
                        ];
                    }, $rows);

                    $hasMore = ($offset + count($users)) < $totalCount;

                    $from->send(json_encode([
                        'type' => 'blocked_users_result',
                        'users' => $users,
                        'total_count' => $totalCount,
                        'page' => $page,
                        'has_more' => $hasMore
                    ]));
                } catch (\PDOException $e) {
                    echo "[DB Error get_blocked_users] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        if (isset($data['type']) && $data['type'] === 'get_blocked_count') {
            $userId = (int)($data['user_id'] ?? 0);
            if ($userId > 0) {
                try {
                    $countStmt = $this->getDB()->prepare("SELECT COUNT(*) FROM blocked_users WHERE user_id = ?");
                    $countStmt->execute([$userId]);
                    $totalCount = (int)$countStmt->fetchColumn();
                    $from->send(json_encode([
                        'type' => 'blocked_count_result',
                        'count' => $totalCount
                    ]));
                } catch (\PDOException $e) {}
            }
            return;
        }

        // ==========================================
        // 13. ЖАЛОБЫ И МОДЕРАЦИЯ
        // ==========================================
        
        // Отправка жалобы
        if (isset($data['type']) && $data['type'] == 'report_message') {
            $fromUser = isset($from->vibeUserId) ? (int)$from->vibeUserId : 0;
            $messageId = (int)($data['message_id'] ?? 0);
            $theme = $data['theme'] ?? '';
            $comment = substr($data['comment'] ?? '', 0, 512);

            if ($fromUser > 0 && $messageId > 0 && !empty($theme)) {
                try {
                    // Проверка на то, была ли уже жалоба на это сообщение
                    $checkMsg = $this->getDB()->prepare("SELECT id FROM reports WHERE from_user = ? AND message_id = ?");
                    $checkMsg->execute([$fromUser, $messageId]);
                    if ($checkMsg->fetch()) {
                        $from->send(json_encode(['type' => 'report_error', 'message' => 'Вы уже отправляли жалобу на это сообщение.']));
                        return;
                    }

                    // Rate Limit: не более 10 жалоб в час
                    $checkRate = $this->getDB()->prepare("SELECT COUNT(*) FROM reports WHERE from_user = ? AND time >= NOW() - INTERVAL 1 HOUR");
                    $checkRate->execute([$fromUser]);
                    $recentReports = (int)$checkRate->fetchColumn();
                    if ($recentReports >= 10) {
                        $from->send(json_encode(['type' => 'report_error', 'message' => 'Вы отправляете слишком много жалоб. Пожалуйста, попробуйте позже.']));
                        return;
                    }

                    // Ищем автора сообщения
                    $findMsg = $this->getDB()->prepare("SELECT sender_id FROM messages WHERE id = ?");
                    $findMsg->execute([$messageId]);
                    $toUserRow = $findMsg->fetch();
                    if ($toUserRow) {
                        $toUser = (int)$toUserRow['sender_id'];
                        $insertStmt = $this->getDB()->prepare("INSERT INTO reports (theme, from_user, to_user, message_id, comment) VALUES (?, ?, ?, ?, ?)");
                        $insertStmt->execute([$theme, $fromUser, $toUser, $messageId, $comment]);
                        $from->send(json_encode(['type' => 'report_success', 'message_id' => $messageId]));
                    }
                } catch (\PDOException $e) {
                    echo "[DB Error reportMessage] " . $e->getMessage() . "\n";
                }
            }
            return;
        }

        // ==========================================
        // 14. НАЖАТИЕ НА ИНЛАЙН КНОПКУ БОТА (CALLBACK) - IN-MEMORY
        // ==========================================
        if (isset($data['type']) && ($data['type'] === 'bot_callback' || $data['type'] === 'send_callback_query')) {
            $userId = isset($from->vibeUserId) ? (int)$from->vibeUserId : (int)($data['user_id'] ?? 0);
            $botId = (int)($data['bot_id'] ?? 0);
            $messageId = (int)($data['message_id'] ?? 0);
            $callbackData = trim($data['data'] ?? ($data['callback_data'] ?? ''));

            if ($userId <= 0 || $botId <= 0 || $callbackData === '') {
                $from->send(json_encode([
                    'type' => 'bot_callback_error',
                    'error' => 'invalid_params',
                    'message' => 'Неверные параметры запроса.'
                ]));
                return;
            }

            // ----------------------------------------------------
            // Защита от флуда: максимум 3 нажатия в секунду
            // ----------------------------------------------------
            $now = microtime(true);
            if (!isset($this->callbackRateLimits[$userId])) {
                $this->callbackRateLimits[$userId] = [];
            }
            // Оставляем только клики за последнюю 1.0 секунду
            $this->callbackRateLimits[$userId] = array_values(array_filter(
                $this->callbackRateLimits[$userId],
                function($ts) use ($now) { return ($now - $ts) < 1.0; }
            ));

            if (count($this->callbackRateLimits[$userId]) >= 3) {
                $from->send(json_encode([
                    'type' => 'bot_callback_error',
                    'error' => 'rate_limit',
                    'message' => 'Слишком много нажатий! Максимум 3 в секунду.'
                ]));
                return;
            }

            // Добавляем текущую отметку времени
            $this->callbackRateLimits[$userId][] = $now;

            $callbackId = 0;
            try {
                $stmt = $this->getDB()->prepare("INSERT INTO bot_callbacks (bot_id, user_id, message_id, data) VALUES (?, ?, ?, ?)");
                $stmt->execute([$botId, $userId, $messageId, $callbackData]);
                $callbackId = (int)$this->getDB()->lastInsertId();
            } catch (\PDOException $e) {
                $this->lastCallbackId++;
                $callbackId = $this->lastCallbackId;
            }

            // In-memory fallback
            if (count($this->pendingCallbacks) > 5000) {
                $this->pendingCallbacks = array_slice($this->pendingCallbacks, -2500, null, true);
            }
            $this->pendingCallbacks[$callbackId] = [
                'user_id' => $userId,
                'bot_id' => $botId,
                'message_id' => $messageId,
                'time' => $now
            ];

            // Отправляем событие боту напрямую через WebSocket (in-memory если бот подключен)
            $senderInfo = $this->getUserInfoForBot($userId);
            $updatePayload = [
                'id' => $callbackId,
                'type' => 'callback_query',
                'callback_query_id' => $callbackId,
                'sender_id' => $userId,
                'user_id' => $userId,
                'sender_info' => $senderInfo,
                'message_id' => $messageId,
                'data' => $callbackData,
                'timestamp' => date('Y-m-d H:i:s')
            ];

            // Сохраняем в единую таблицу обновлений bot_updates
            try {
                $stmtUpd = $this->getDB()->prepare("INSERT INTO bot_updates (bot_id, update_type, sender_id, payload) VALUES (?, 'callback_query', ?, ?)");
                $stmtUpd->execute([$botId, $userId, json_encode($updatePayload, JSON_UNESCAPED_UNICODE)]);
                $botUpdateId = (int)$this->getDB()->lastInsertId();
                if ($botUpdateId > 0) {
                    // `id` is the queue cursor for get_updates; callback_query_id must remain
                    // the original callback ID so answer_callback_query reaches the correct user.
                    $updatePayload['id'] = $botUpdateId;
                    $updatePayload['bot_update_id'] = $botUpdateId;
                }
            } catch (\PDOException $e) {}

            if (isset($this->userConnections[$botId])) {
                $this->userConnections[$botId]->send(json_encode($updatePayload, JSON_UNESCAPED_UNICODE));
            }

            // Отправляем подтверждение пользователю
            $from->send(json_encode([
                'type' => 'bot_callback_result',
                'success' => true,
                'callback_query_id' => $callbackId,
                'message_id' => $messageId,
                'data' => $callbackData
            ]));

            echo "[CALLBACK IN-MEMORY] User $userId clicked button on bot $botId (cbId: $callbackId, msg: $messageId, data: $callbackData)\n";
            return;
        }

        // ==========================================
        // 15. ОТВЕТ БОТА НА CALLBACK (ANSWER CALLBACK QUERY) - IN-MEMORY
        // ==========================================
        if (isset($data['type']) && ($data['type'] === 'answer_callback_query' || $data['type'] === 'bot_callback_answer')) {
            $callbackId = (int)($data['callback_query_id'] ?? ($data['callback_id'] ?? 0));
            $targetUserId = (int)($data['user_id'] ?? 0);
            $text = $data['text'] ?? null;
            $showAlert = !empty($data['show_alert']);

            if ($targetUserId <= 0 && $callbackId > 0 && isset($this->pendingCallbacks[$callbackId])) {
                $targetUserId = (int)$this->pendingCallbacks[$callbackId]['user_id'];
            }

            if ($targetUserId > 0 && isset($this->userConnections[$targetUserId])) {
                $this->userConnections[$targetUserId]->send(json_encode([
                    'type' => 'bot_callback_answer',
                    'callback_id' => $callbackId,
                    'text' => $text,
                    'show_alert' => $showAlert
                ], JSON_UNESCAPED_UNICODE));
                echo "[CALLBACK ANSWER IN-MEMORY] Delivered answer to user $targetUserId (text: $text, alert: " . ($showAlert ? "1" : "0") . ")\n";
            }

            if ($callbackId > 0 && isset($this->pendingCallbacks[$callbackId])) {
                unset($this->pendingCallbacks[$callbackId]);
            }

            $from->send(json_encode(['type' => 'answer_callback_query_result', 'success' => true]));
            return;
        }
        } catch (\Throwable $e) {
            echo "[onMessage Error] " . $e->getMessage() . " in " . $e->getFile() . " on line " . $e->getLine() . "\n";
        }
    }
    

    public function onClose(ConnectionInterface $conn) {
        $this->clients->detach($conn);
        
        if (isset($conn->vibeUserId)) {
            $userId = $conn->vibeUserId;
            $stmt = $this->getDB()->prepare("UPDATE users SET is_online = 0, last_seen = NOW() WHERE id = ?");
            $stmt->execute([$userId]);
            if (isset($this->userConnections[$userId]) && $this->userConnections[$userId] === $conn) {
                unset($this->userConnections[$userId]);
                echo "[-] Пользователь $userId отключён (conn {$conn->resourceId})\n";
                $this->broadcastUserStatus($userId);
            }
        } else {
            echo "[-] Соединение закрыто ({$conn->resourceId})\n";
        }
    }

    public function onError(ConnectionInterface $conn, \Exception $e) {
        echo "Ошибка: {$e->getMessage()}\n";
        $conn->close();
    }

    public function checkNewMessages() {
        if ($this->getDB() === null) return;
        try { $this->sweepSessions(); }
        catch (\Throwable $e) { error_log('[AUTH] Session sweep unavailable'); return; }
        
        $this->checkNewChatActions();
        $this->checkNewBotCallbackAnswers();
        $this->checkNewBotOutgoingEvents();

        if ($this->lastMaxMessageId === null) {
            try {
                $stmt = $this->getDB()->query("SELECT MAX(id) FROM messages");
                $this->lastMaxMessageId = (int)$stmt->fetchColumn();
            } catch (\PDOException $e) {
                // ignore
            }
            return;
        }

        try {
            $stmt = $this->getDB()->prepare("SELECT * FROM messages WHERE id > ? AND sender_type = 'bot' ORDER BY id ASC");
            $stmt->execute([$this->lastMaxMessageId]);
            $updates = $stmt->fetchAll(\PDO::FETCH_ASSOC);

            foreach ($updates as $row) {
                $msgId = (int)$row['id'];
                if ($msgId > $this->lastMaxMessageId) {
                    $this->lastMaxMessageId = $msgId;
                }
                
                $receiverId = (int)$row['receiver_id'];
                $senderId = (int)$row['sender_id'];
                
                // Get bot info for the sender
                $stmtBot = $this->getDB()->prepare("SELECT name, username FROM bots WHERE id = ?");
                $stmtBot->execute([$senderId]);
                $bot = $stmtBot->fetch();
                $senderName = $bot ? ($bot['name'] ?: $bot['username']) : 'Bot';
                
                $messagePayload = [
                    'type' => 'chat_message',
                    'message_id' => $msgId,
                    'sender_id' => $senderId,
                    'receiver_id' => $receiverId,
                    'sender_type' => 'bot',
                    'sender_name' => $senderName,
                    'content' => $row['content'],
                    'timestamp' => $row['timestamp'],
                    'reply_to_id' => $row['reply_to_id'],
                    'is_edited' => (int)$row['is_edited'] === 1,
                    'attachments' => $row['attachments'] ? json_decode($row['attachments'], true) : null,
                    'reply_markup' => $row['reply_markup'] ? json_decode($row['reply_markup'], true) : null,
                    'reply_to_content' => null,
                    'reply_to_sender_name' => null,
                    'forwarded_from_id' => null,
                    'forwarded_from_name' => null
                ];
                
                if (isset($this->userConnections[$receiverId])) {
                    $this->sendToUser($receiverId,$messagePayload);
                }
                
                $chatListUpdates = $this->buildChatList($receiverId, $senderId);
                if (isset($this->userConnections[$receiverId])) {
                    $this->userConnections[$receiverId]->send(json_encode([
                        'type' => 'chat_list_update',
                        'chats' => $chatListUpdates
                    ], JSON_UNESCAPED_UNICODE));
                } else {
                    // Send FCM push notification if user is offline
                    $isMutedCheck = $this->getDB()->prepare("SELECT 1 FROM muted_users WHERE user_id = ? AND muted_id = ?");
                    $isMutedCheck->execute([$receiverId, $senderId]);
                    if (!$isMutedCheck->fetchColumn()) {
                        $this->sendFcmNotification($receiverId, $senderName, $row['content'], $senderId);
                    }
                }
            }
        } catch (\PDOException $e) {
            echo "[DB Error checkNewMessages] " . $e->getMessage() . "\n";
        }
    }

    private function checkNewBotCallbackAnswers() {
        try {
            $stmt = $this->getDB()->query("SELECT * FROM bot_callback_answers WHERE is_delivered = 0 ORDER BY id ASC LIMIT 50");
            if (!$stmt) return;
            $answers = $stmt->fetchAll(\PDO::FETCH_ASSOC);

            foreach ($answers as $ans) {
                $targetUserId = (int)$ans['user_id'];
                $payload = [
                    'type' => 'bot_callback_answer',
                    'callback_id' => (int)$ans['callback_id'],
                    'text' => $ans['text'],
                    'show_alert' => (bool)$ans['show_alert']
                ];

                if (isset($this->userConnections[$targetUserId])) {
                    $this->sendToUser($targetUserId, $payload);
                }

                $upd = $this->getDB()->prepare("UPDATE bot_callback_answers SET is_delivered = 1 WHERE id = ?");
                $upd->execute([(int)$ans['id']]);
            }
        } catch (\Throwable $e) {
            // Table might be initializing
        }
    }

    private function checkNewBotOutgoingEvents() {
        // Deliver bot-initiated actions (edit / delete / reaction / pin / unpin)
        // queued by the Bot API endpoint (index.php) to connected clients.
        try {
            $stmt = $this->getDB()->query("SELECT * FROM bot_outgoing_events WHERE is_delivered = 0 ORDER BY id ASC LIMIT 100");
            if (!$stmt) return;
            $events = $stmt->fetchAll(\PDO::FETCH_ASSOC);

            $upd = $this->getDB()->prepare("UPDATE bot_outgoing_events SET is_delivered = 1 WHERE id = ?");

            foreach ($events as $ev) {
                $targetUserId = (int)$ev['target_user_id'];
                $payload = json_decode($ev['payload'], true);

                if (is_array($payload) && !empty($payload) && isset($this->userConnections[$targetUserId])) {
                    $this->sendToUser($targetUserId, $payload);

                    // Keep the chat list in sync when the last message may have changed.
                    if (in_array($ev['event_type'], ['message_edited', 'messages_deleted'], true)) {
                        $this->sendToUser($targetUserId, [
                            'type'  => 'chat_list_update',
                            'chats' => $this->buildChatList($targetUserId)
                        ]);
                    }
                }

                $upd->execute([(int)$ev['id']]);
            }
        } catch (\Throwable $e) {
            // Table might be initializing; retry on next tick.
        }
    }

    private function checkNewChatActions() {
        if ($this->lastMaxActionId === null) {
            try {
                $stmt = $this->getDB()->query("SELECT MAX(id) FROM chat_actions");
                $this->lastMaxActionId = (int)$stmt->fetchColumn();
            } catch (\PDOException $e) {}
            return;
        }

        try {
            $stmt = $this->getDB()->prepare("SELECT * FROM chat_actions WHERE id > ? ORDER BY id ASC");
            $stmt->execute([$this->lastMaxActionId]);
            $updates = $stmt->fetchAll(\PDO::FETCH_ASSOC);

            foreach ($updates as $row) {
                $actionId = (int)$row['id'];
                if ($actionId > $this->lastMaxActionId) {
                    $this->lastMaxActionId = $actionId;
                }
                
                $receiverId = (int)$row['receiver_id'];
                $senderId = (int)$row['sender_id'];
                $action = $row['action'];
                $senderType = $row['sender_type'];
                
                $type = 'typing_indicator';
                if ($action === 'stop_typing') {
                    $type = 'typing_indicator_stop';
                }
                
                $senderName = $this->getUserName($senderId, $senderType === 'bot');

                if (isset($this->userConnections[$receiverId])) {
                    $this->sendToUser($receiverId, [
                        'type' => $type,
                        'sender_id' => $senderId,
                        'sender_name' => $senderName
                    ]);
                }
            }
        } catch (\PDOException $e) {
            echo "[DB Error checkNewChatActions] " . $e->getMessage() . "\n";
        }
    }
}

$port = getenv('PORT') ? getenv('PORT') : 8100;
$address = getenv('IP') ? getenv('IP') : '0.0.0.0';

if (strpos($address, ':') !== false && strpos($address, '[') === false) {
    $address = '[' . $address . ']';
}