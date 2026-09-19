<?php
header('Content-Type: application/json; charset=utf-8');

// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

function sendResponse($ok, $result = null, $description = null, $extra = []) {
    $response = ['ok' => $ok];
    if ($result !== null) {
        $response['result'] = $result;
    }
    if ($description !== null) {
        $response['description'] = $description;
    }
    if (!empty($extra)) {
        $response = array_merge($response, $extra);
    }
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit;
}

// ------------------------------------------------------------------
// Database Configuration
// ------------------------------------------------------------------
define('DB_HOST', 'mysql-flasskdev.alwaysdata.net');
define('DB_USER', 'flasskdev');
define('DB_PASS', '31052019RoG+');
define('DB_NAME', 'flasskdev_mobile');

$dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4';

try {
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ]);

    // ------------------------------------------------------------------
    // Auto-migrations (idempotent)
    // ------------------------------------------------------------------
    try { $pdo->exec("ALTER TABLE messages ADD COLUMN reply_markup JSON NULL"); } catch (\PDOException $e) {}

    $pdo->exec("CREATE TABLE IF NOT EXISTS bot_updates (
        id INT AUTO_INCREMENT PRIMARY KEY,
        bot_id INT NOT NULL,
        update_type VARCHAR(32) NOT NULL,
        reference_id INT NOT NULL,
        payload LONGTEXT NOT NULL,
        message_id INT DEFAULT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_bot_offset (bot_id, id),
        INDEX idx_bot_ref (bot_id, update_type, reference_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");

    $pdo->exec("CREATE TABLE IF NOT EXISTS bot_callback_answers (
        id INT AUTO_INCREMENT PRIMARY KEY,
        callback_id INT NOT NULL,
        user_id INT NOT NULL,
        text TEXT NULL,
        show_alert TINYINT(1) DEFAULT 0,
        is_delivered TINYINT(1) DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");

    // Queue of bot-initiated actions (edit/delete/reaction/pin/unpin) that the
    // WebSocket server (Chat.php) delivers to connected clients in real time.
    $pdo->exec("CREATE TABLE IF NOT EXISTS bot_outgoing_events (
        id INT AUTO_INCREMENT PRIMARY KEY,
        bot_id INT NOT NULL,
        event_type VARCHAR(32) NOT NULL,
        target_user_id INT NOT NULL,
        payload LONGTEXT NOT NULL,
        is_delivered TINYINT(1) DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_undelivered (is_delivered, id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");

    // DB-based single-instance polling lock (replaces the old bot_lock_*.txt files).
    $pdo->exec("CREATE TABLE IF NOT EXISTS bot_polling_locks (
        bot_id INT PRIMARY KEY,
        client_id VARCHAR(64) NOT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;");

    // One-time cleanup of legacy file-based locks left over by older versions.
    foreach (glob(__DIR__ . '/bot_lock_*.txt') ?: [] as $legacyLock) {
        @unlink($legacyLock);
    }

} catch (PDOException $e) {
    sendResponse(false, null, "Database connection failed: " . $e->getMessage());
}
// ------------------------------------------------------------------

$requestUri = $_SERVER['REQUEST_URI'];
$parsedUrl = parse_url($requestUri);
$path = trim($parsedUrl['path'], '/');

$pathParts = explode('/', $path);
if (count($pathParts) < 2) {
    sendResponse(false, null, "Invalid request path");
}

$method = array_pop($pathParts);
$token = array_pop($pathParts);

if (empty($token) || strlen($token) < 10) {
    sendResponse(false, null, "Invalid token");
}

// Authenticate Bot
$stmt = $pdo->prepare("SELECT id, name, username, is_verified, about FROM bots WHERE bot_token = :token LIMIT 1");
$stmt->execute([':token' => $token]);
$bot = $stmt->fetch();

if (!$bot) {
    sendResponse(false, null, "Unauthorized: Invalid bot token");
}
$botId = (int)$bot['id'];

// Read input
$input = $_REQUEST;
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $rawInput = file_get_contents('php://input');
    if (!empty($rawInput)) {
        $jsonInput = json_decode($rawInput, true);
        if (is_array($jsonInput)) {
            $input = array_merge($input, $jsonInput);
        }
    }
}

// ------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------

// Sanitize & validate reply_markup
function sanitizeReplyMarkup($rawMarkup) {
    if (empty($rawMarkup)) return null;
    if (is_string($rawMarkup)) {
        $rawMarkup = json_decode($rawMarkup, true);
    }
    if (!is_array($rawMarkup)) return null;

    $inlineKeyboard = [];
    if (isset($rawMarkup['inline_keyboard']) && is_array($rawMarkup['inline_keyboard'])) {
        $inlineKeyboard = $rawMarkup['inline_keyboard'];
    } elseif (isset($rawMarkup[0]) && is_array($rawMarkup[0])) {
        $inlineKeyboard = $rawMarkup;
    } else {
        return null;
    }

    // Limit to max 10 rows, max 5 buttons per row
    $sanitized = [];
    $rowCount = 0;
    foreach ($inlineKeyboard as $row) {
        if ($rowCount >= 10) break;
        if (!is_array($row)) continue;

        $sanitizedRow = [];
        $btnCount = 0;
        foreach ($row as $btn) {
            if ($btnCount >= 5) break;
            if (!is_array($btn) || empty($btn['text'])) continue;

            $b = ['text' => (string)$btn['text']];
            if (isset($btn['callback_data'])) {
                $b['callback_data'] = (string)$btn['callback_data'];
            }
            if (isset($btn['url'])) {
                $b['url'] = (string)$btn['url'];
            }
            if (isset($btn['bg_color'])) {
                $b['bg_color'] = (string)$btn['bg_color'];
            }
            if (isset($btn['text_color'])) {
                $b['text_color'] = (string)$btn['text_color'];
            }
            $sanitizedRow[] = $b;
            $btnCount++;
        }
        if (!empty($sanitizedRow)) {
            $sanitized[] = $sanitizedRow;
            $rowCount++;
        }
    }

    return !empty($sanitized) ? ['inline_keyboard' => $sanitized] : null;
}

// Get user info for bot updates
function getUserInfo($pdo, $userId) {
    $stmt = $pdo->prepare("SELECT id, name, username, avatar_url, about, is_online, last_seen, is_developer, is_verified FROM users WHERE id = ?");
    $stmt->execute([$userId]);
    $u = $stmt->fetch();
    if ($u) {
        return [
            'id'           => (int)$u['id'],
            'name'         => $u['name'],
            'username'     => $u['username'],
            'avatar_url'   => $u['avatar_url'],
            'about'        => $u['about'],
            'is_online'    => (bool)$u['is_online'],
            'last_seen'    => $u['last_seen'],
            'is_developer' => (bool)$u['is_developer'],
            'is_verified'  => (bool)$u['is_verified'],
            'is_bot'       => false
        ];
    }
    return null;
}

// Enqueue a real-time event to be delivered by the WebSocket server.
function enqueueBotEvent($pdo, $botId, $eventType, $targetUserId, array $payload) {
    if ($targetUserId <= 0) return;
    try {
        $stmt = $pdo->prepare("INSERT INTO bot_outgoing_events (bot_id, event_type, target_user_id, payload) VALUES (?, ?, ?, ?)");
        $stmt->execute([$botId, $eventType, $targetUserId, json_encode($payload, JSON_UNESCAPED_UNICODE)]);
    } catch (\PDOException $e) {
        // Best-effort delivery; ignore queue failures.
    }
}

// Fetch a message and ensure the bot is a participant of its chat.
function fetchBotMessage($pdo, $botId, $messageId) {
    $stmt = $pdo->prepare("SELECT * FROM messages WHERE id = ? LIMIT 1");
    $stmt->execute([$messageId]);
    $row = $stmt->fetch();
    if (!$row) return null;
    if ((int)$row['sender_id'] !== $botId && (int)$row['receiver_id'] !== $botId) return null;
    return $row;
}

// Route the API methods
switch ($method) {
    case 'send_chat_action':
        $userId = (int)($input['user_id'] ?? 0);
        $action = $input['action'] ?? 'typing';

        if ($userId <= 0) {
            sendResponse(false, null, "Missing user_id");
        }

        $stmt = $pdo->prepare("INSERT INTO chat_actions (sender_id, receiver_id, sender_type, action) VALUES (:bot_id, :user_id, 'bot', :action)");
        try {
            $stmt->execute([
                ':bot_id' => $botId,
                ':user_id' => $userId,
                ':action' => $action
            ]);
            sendResponse(true, true);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to save chat action: " . $e->getMessage());
        }
        break;

    case 'send_message':
        $userId = (int)($input['user_id'] ?? 0);
        $text = $input['text'] ?? null;
        $rawReplyMarkup = $input['reply_markup'] ?? null;
        $replyToId = isset($input['reply_to_id']) ? (int)$input['reply_to_id'] : null;

        if ($userId <= 0 || $text === null || $text === '') {
            sendResponse(false, null, "Missing user_id or text");
        }

        $replyMarkup = sanitizeReplyMarkup($rawReplyMarkup);
        $replyMarkupJson = $replyMarkup ? json_encode($replyMarkup, JSON_UNESCAPED_UNICODE) : null;

        $stmt = $pdo->prepare("INSERT INTO messages (sender_id, receiver_id, sender_type, content, reply_to_id, reply_markup) VALUES (:bot_id, :user_id, 'bot', :text, :reply_to_id, :reply_markup)");
        try {
            $stmt->execute([
                ':bot_id' => $botId,
                ':user_id' => $userId,
                ':text' => $text,
                ':reply_to_id' => $replyToId,
                ':reply_markup' => $replyMarkupJson
            ]);
            $messageId = (int)$pdo->lastInsertId();

            $stmt = $pdo->prepare("SELECT * FROM messages WHERE id = :id");
            $stmt->execute([':id' => $messageId]);
            $messageData = $stmt->fetch();
            if ($messageData) {
                $messageData['message_id'] = (int)$messageData['id'];
                if (!empty($messageData['reply_markup'])) {
                    $messageData['reply_markup'] = json_decode($messageData['reply_markup'], true);
                }
            }

            sendResponse(true, $messageData);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to save message: " . $e->getMessage());
        }
        break;

    case 'edit_message':
        $messageId = (int)($input['message_id'] ?? 0);
        $text = $input['text'] ?? null;
        $hasMarkup = array_key_exists('reply_markup', $input);

        if ($messageId <= 0 || $text === null || $text === '') {
            sendResponse(false, null, "Missing message_id or text");
        }

        $row = fetchBotMessage($pdo, $botId, $messageId);
        if (!$row || (int)$row['sender_id'] !== $botId || $row['sender_type'] !== 'bot') {
            sendResponse(false, null, "Message not found or not owned by this bot");
        }

        try {
            if ($hasMarkup) {
                $markup = sanitizeReplyMarkup($input['reply_markup']);
                $markupJson = $markup ? json_encode($markup, JSON_UNESCAPED_UNICODE) : null;
                $upd = $pdo->prepare("UPDATE messages SET content = ?, reply_markup = ?, is_edited = 1 WHERE id = ?");
                $upd->execute([$text, $markupJson, $messageId]);
            } else {
                $upd = $pdo->prepare("UPDATE messages SET content = ?, is_edited = 1 WHERE id = ?");
                $upd->execute([$text, $messageId]);
            }

            $stmt = $pdo->prepare("SELECT * FROM messages WHERE id = ?");
            $stmt->execute([$messageId]);
            $messageData = $stmt->fetch();
            $decodedMarkup = (!empty($messageData['reply_markup'])) ? json_decode($messageData['reply_markup'], true) : null;
            if ($messageData) {
                $messageData['message_id'] = (int)$messageData['id'];
                $messageData['reply_markup'] = $decodedMarkup;
            }

            enqueueBotEvent($pdo, $botId, 'message_edited', (int)$row['receiver_id'], [
                'type' => 'message_edited',
                'message_id' => $messageId,
                'content' => $text,
                'reply_markup' => $decodedMarkup
            ]);

            sendResponse(true, $messageData);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to edit message: " . $e->getMessage());
        }
        break;

    case 'edit_message_reply_markup':
        $messageId = (int)($input['message_id'] ?? 0);
        if ($messageId <= 0) {
            sendResponse(false, null, "Missing message_id");
        }
        $row = fetchBotMessage($pdo, $botId, $messageId);
        if (!$row || (int)$row['sender_id'] !== $botId || $row['sender_type'] !== 'bot') {
            sendResponse(false, null, "Message not found or not owned by this bot");
        }
        try {
            $markup = sanitizeReplyMarkup($input['reply_markup'] ?? null);
            $markupJson = $markup ? json_encode($markup, JSON_UNESCAPED_UNICODE) : null;
            $upd = $pdo->prepare("UPDATE messages SET reply_markup = ? WHERE id = ?");
            $upd->execute([$markupJson, $messageId]);

            enqueueBotEvent($pdo, $botId, 'message_edited', (int)$row['receiver_id'], [
                'type' => 'message_edited',
                'message_id' => $messageId,
                'content' => $row['content'],
                'reply_markup' => $markup
            ]);
            sendResponse(true, true);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to edit reply markup: " . $e->getMessage());
        }
        break;

    case 'delete_message':
        $ids = [];
        if (isset($input['message_ids']) && is_array($input['message_ids'])) {
            foreach ($input['message_ids'] as $mid) { $ids[] = (int)$mid; }
        } elseif (isset($input['message_id'])) {
            $ids[] = (int)$input['message_id'];
        }
        $ids = array_values(array_unique(array_filter($ids, fn($v) => $v > 0)));
        if (empty($ids)) {
            sendResponse(false, null, "Missing message_id(s)");
        }

        try {
            $placeholders = implode(',', array_fill(0, count($ids), '?'));
            $sel = $pdo->prepare("SELECT id, receiver_id FROM messages WHERE id IN ($placeholders) AND sender_id = ? AND sender_type = 'bot'");
            $sel->execute(array_merge($ids, [$botId]));
            $owned = $sel->fetchAll();

            if (empty($owned)) {
                sendResponse(false, null, "No deletable messages found for this bot");
            }

            $ownedIds = array_map(fn($r) => (int)$r['id'], $owned);
            $delPlaceholders = implode(',', array_fill(0, count($ownedIds), '?'));
            $del = $pdo->prepare("DELETE FROM messages WHERE id IN ($delPlaceholders) AND sender_id = ?");
            $del->execute(array_merge($ownedIds, [$botId]));

            // Group deleted ids by receiver to notify each affected chat.
            $byReceiver = [];
            foreach ($owned as $r) {
                $byReceiver[(int)$r['receiver_id']][] = (int)$r['id'];
            }
            foreach ($byReceiver as $receiverId => $deletedIds) {
                enqueueBotEvent($pdo, $botId, 'messages_deleted', $receiverId, [
                    'type' => 'messages_deleted',
                    'message_ids' => $deletedIds
                ]);
            }

            sendResponse(true, ['deleted_message_ids' => $ownedIds]);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to delete message(s): " . $e->getMessage());
        }
        break;

    case 'forward_message':
    case 'copy_message':
        $userId = (int)($input['user_id'] ?? 0);
        $fromMessageId = (int)($input['from_message_id'] ?? 0);
        if ($userId <= 0 || $fromMessageId <= 0) {
            sendResponse(false, null, "Missing user_id or from_message_id");
        }

        $src = fetchBotMessage($pdo, $botId, $fromMessageId);
        if (!$src) {
            sendResponse(false, null, "Source message not found or not accessible");
        }

        try {
            $isForward = ($method === 'forward_message');
            $forwardedFrom = $isForward ? (int)$src['sender_id'] : null;

            $rawReplyMarkup = $input['reply_markup'] ?? null;
            $replyMarkup = sanitizeReplyMarkup($rawReplyMarkup);
            $replyMarkupJson = $replyMarkup ? json_encode($replyMarkup, JSON_UNESCAPED_UNICODE) : null;

            $ins = $pdo->prepare("INSERT INTO messages (sender_id, receiver_id, sender_type, content, attachments, forwarded_from_id, reply_markup) VALUES (?, ?, 'bot', ?, ?, ?, ?)");
            $ins->execute([
                $botId,
                $userId,
                $src['content'],
                $src['attachments'],
                $forwardedFrom,
                $replyMarkupJson
            ]);
            $newId = (int)$pdo->lastInsertId();

            $stmt = $pdo->prepare("SELECT * FROM messages WHERE id = ?");
            $stmt->execute([$newId]);
            $messageData = $stmt->fetch();
            if ($messageData) {
                $messageData['message_id'] = (int)$messageData['id'];
                if (!empty($messageData['reply_markup'])) {
                    $messageData['reply_markup'] = json_decode($messageData['reply_markup'], true);
                }
            }
            sendResponse(true, $messageData);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to forward/copy message: " . $e->getMessage());
        }
        break;

    case 'set_reaction':
        $messageId = (int)($input['message_id'] ?? 0);
        $emoji = trim((string)($input['emoji'] ?? ''));
        if ($messageId <= 0) {
            sendResponse(false, null, "Missing message_id");
        }
        $row = fetchBotMessage($pdo, $botId, $messageId);
        if (!$row) {
            sendResponse(false, null, "Message not found or not accessible");
        }

        try {
            $currentReactions = !empty($row['reactions']) ? json_decode($row['reactions'], true) : [];
            if (!is_array($currentReactions)) $currentReactions = [];

            $userPreviousEmoji = null;
            foreach ($currentReactions as $r) {
                $uIds = isset($r['userIds']) && is_array($r['userIds']) ? $r['userIds'] : [];
                if (in_array($botId, $uIds)) {
                    $userPreviousEmoji = $r['emoji'] ?? '';
                }
            }

            $nowMillis = (int)(microtime(true) * 1000);
            $updatedReactions = [];
            foreach ($currentReactions as $r) {
                $em = $r['emoji'] ?? '';
                $uIds = isset($r['userIds']) && is_array($r['userIds']) ? $r['userIds'] : [];
                $usersArr = isset($r['users']) && is_array($r['users']) ? $r['users'] : [];

                $uIds = array_values(array_filter($uIds, fn($id) => (int)$id !== $botId));
                $usersArr = array_values(array_filter($usersArr, function ($u) use ($botId) {
                    $uId = is_array($u) ? ($u['userId'] ?? $u['user_id'] ?? 0) : $u;
                    return (int)$uId !== $botId;
                }));

                if ($em === $emoji && $userPreviousEmoji !== $emoji && $emoji !== '') {
                    $uIds[] = $botId;
                    $usersArr[] = ['userId' => $botId, 'timestamp' => $nowMillis];
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

            if ($emoji !== '' && $userPreviousEmoji !== $emoji) {
                $exists = false;
                foreach ($updatedReactions as $ur) {
                    if ($ur['emoji'] === $emoji) { $exists = true; break; }
                }
                if (!$exists) {
                    $updatedReactions[] = [
                        'emoji' => $emoji,
                        'count' => 1,
                        'userIds' => [$botId],
                        'users' => [['userId' => $botId, 'timestamp' => $nowMillis]]
                    ];
                }
            }

            $reactionsJson = !empty($updatedReactions) ? json_encode($updatedReactions, JSON_UNESCAPED_UNICODE) : null;
            $upd = $pdo->prepare("UPDATE messages SET reactions = ? WHERE id = ?");
            $upd->execute([$reactionsJson, $messageId]);

            $partnerId = ((int)$row['sender_id'] === $botId) ? (int)$row['receiver_id'] : (int)$row['sender_id'];
            enqueueBotEvent($pdo, $botId, 'message_reaction', $partnerId, [
                'type' => 'message_reaction',
                'message_id' => $messageId,
                'user_id' => $botId,
                'emoji' => $emoji,
                'reactions' => $updatedReactions
            ]);

            sendResponse(true, ['reactions' => $updatedReactions]);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to set reaction: " . $e->getMessage());
        }
        break;

    case 'pin_message':
        $messageId = (int)($input['message_id'] ?? 0);
        if ($messageId <= 0) {
            sendResponse(false, null, "Missing message_id");
        }
        $row = fetchBotMessage($pdo, $botId, $messageId);
        if (!$row) {
            sendResponse(false, null, "Message not found or not accessible");
        }
        try {
            $partnerId = ((int)$row['sender_id'] === $botId) ? (int)$row['receiver_id'] : (int)$row['sender_id'];
            $ins = $pdo->prepare("INSERT IGNORE INTO pinned_messages (message_id, pinned_by_id, pinned_for_id) VALUES (?, ?, ?)");
            $ins->execute([$messageId, $botId, $partnerId]);

            enqueueBotEvent($pdo, $botId, 'message_pinned', $partnerId, [
                'type' => 'message_pinned',
                'message_id' => $messageId,
                'with_user_id' => $botId
            ]);
            sendResponse(true, true);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to pin message: " . $e->getMessage());
        }
        break;

    case 'unpin_message':
        $messageId = (int)($input['message_id'] ?? 0);
        if ($messageId <= 0) {
            sendResponse(false, null, "Missing message_id");
        }
        $row = fetchBotMessage($pdo, $botId, $messageId);
        if (!$row) {
            sendResponse(false, null, "Message not found or not accessible");
        }
        try {
            $partnerId = ((int)$row['sender_id'] === $botId) ? (int)$row['receiver_id'] : (int)$row['sender_id'];
            $del = $pdo->prepare("DELETE FROM pinned_messages WHERE message_id = ? AND pinned_for_id = ?");
            $del->execute([$messageId, $partnerId]);

            enqueueBotEvent($pdo, $botId, 'message_unpinned', $partnerId, [
                'type' => 'message_unpinned',
                'message_id' => $messageId,
                'with_user_id' => $botId
            ]);
            sendResponse(true, true);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to unpin message: " . $e->getMessage());
        }
        break;

    case 'unpin_all_messages':
        $userId = (int)($input['user_id'] ?? 0);
        if ($userId <= 0) {
            sendResponse(false, null, "Missing user_id");
        }
        try {
            $del = $pdo->prepare("
                DELETE p FROM pinned_messages p
                JOIN messages m ON p.message_id = m.id
                WHERE p.pinned_for_id = ?
                  AND ((m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?))
            ");
            $del->execute([$userId, $botId, $userId, $userId, $botId]);

            enqueueBotEvent($pdo, $botId, 'all_messages_unpinned', $userId, [
                'type' => 'all_messages_unpinned',
                'with_user_id' => $botId
            ]);
            sendResponse(true, true);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to unpin all messages: " . $e->getMessage());
        }
        break;

    case 'answer_callback_query':
        $callbackQueryId = (int)($input['callback_query_id'] ?? 0);
        $userId = (int)($input['user_id'] ?? 0);
        $text = $input['text'] ?? null;
        $showAlert = !empty($input['show_alert']) ? 1 : 0;
        $refId = $callbackQueryId;

        if ($callbackQueryId > 0) {
            try {
                $stmtUpd = $pdo->prepare("SELECT reference_id, payload FROM bot_updates WHERE id = ? LIMIT 1");
                $stmtUpd->execute([$callbackQueryId]);
                $updRow = $stmtUpd->fetch();
                if ($updRow) {
                    $refId = (int)$updRow['reference_id'];
                    if ($userId <= 0 && !empty($updRow['payload'])) {
                        $p = json_decode($updRow['payload'], true);
                        if (isset($p['sender_id'])) $userId = (int)$p['sender_id'];
                    }
                }
            } catch (PDOException $e) {}

            if ($userId <= 0) {
                try {
                    $stmtCb = $pdo->prepare("SELECT user_id FROM bot_callbacks WHERE id = ? LIMIT 1");
                    $stmtCb->execute([$refId]);
                    $userId = (int)$stmtCb->fetchColumn();
                } catch (PDOException $e) {}
            }
        }

        if ($userId > 0) {
            try {
                $stmt = $pdo->prepare("INSERT INTO bot_callback_answers (callback_id, user_id, text, show_alert, is_delivered) VALUES (?, ?, ?, ?, 0)");
                $stmt->execute([$refId, $userId, $text, $showAlert]);
            } catch (PDOException $e) {
                // Ignore
            }
        }
        sendResponse(true, true);
        break;

    case 'get_me':
        sendResponse(true, [
            'id' => (int)$bot['id'],
            'is_bot' => true,
            'first_name' => $bot['name'],
            'name' => $bot['name'],
            'username' => $bot['username'],
            'is_verified' => (bool)$bot['is_verified'],
            'about' => $bot['about']
        ]);
        break;

    case 'get_user':
        $userId = (int)($input['user_id'] ?? 0);
        if ($userId <= 0) {
            sendResponse(false, null, "Missing user_id");
        }
        $info = getUserInfo($pdo, $userId);
        if (!$info) {
            sendResponse(false, null, "User not found");
        }
        sendResponse(true, $info);
        break;

    case 'get_message':
        $messageId = (int)($input['message_id'] ?? 0);
        if ($messageId <= 0) {
            sendResponse(false, null, "Missing message_id");
        }
        $row = fetchBotMessage($pdo, $botId, $messageId);
        if (!$row) {
            sendResponse(false, null, "Message not found or not accessible");
        }
        $row['message_id'] = (int)$row['id'];
        $row['reply_markup'] = (!empty($row['reply_markup'])) ? json_decode($row['reply_markup'], true) : null;
        $row['attachments'] = (!empty($row['attachments'])) ? json_decode($row['attachments'], true) : null;
        $row['reactions'] = (!empty($row['reactions'])) ? json_decode($row['reactions'], true) : null;
        sendResponse(true, $row);
        break;

    case 'get_chat_history':
        $userId = (int)($input['user_id'] ?? 0);
        $limit = (int)($input['limit'] ?? 50);
        $offset = (int)($input['offset'] ?? 0);
        if ($userId <= 0) {
            sendResponse(false, null, "Missing user_id");
        }
        $limit = max(1, min($limit, 100));
        $offset = max(0, $offset);
        try {
            $stmt = $pdo->prepare("
                SELECT * FROM messages
                WHERE (sender_id = :bot AND receiver_id = :usr) OR (sender_id = :usr2 AND receiver_id = :bot2)
                ORDER BY id DESC
                LIMIT $limit OFFSET $offset
            ");
            $stmt->execute([':bot' => $botId, ':usr' => $userId, ':usr2' => $userId, ':bot2' => $botId]);
            $rows = $stmt->fetchAll();
            $result = [];
            foreach ($rows as $r) {
                $result[] = [
                    'message_id' => (int)$r['id'],
                    'sender_id' => (int)$r['sender_id'],
                    'receiver_id' => (int)$r['receiver_id'],
                    'is_bot' => ($r['sender_type'] === 'bot') ? 1 : 0,
                    'content' => $r['content'],
                    'timestamp' => $r['timestamp'],
                    'reply_to_id' => $r['reply_to_id'] !== null ? (int)$r['reply_to_id'] : null,
                    'is_edited' => (int)$r['is_edited'],
                    'attachments' => (!empty($r['attachments'])) ? json_decode($r['attachments'], true) : null,
                    'reply_markup' => (!empty($r['reply_markup'])) ? json_decode($r['reply_markup'], true) : null,
                    'reactions' => (!empty($r['reactions'])) ? json_decode($r['reactions'], true) : null,
                ];
            }
            sendResponse(true, $result);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to fetch chat history: " . $e->getMessage());
        }
        break;

    case 'set_my_name':
        $name = trim((string)($input['name'] ?? ''));
        if ($name === '') {
            sendResponse(false, null, "Missing name");
        }
        try {
            $upd = $pdo->prepare("UPDATE bots SET name = ? WHERE id = ?");
            $upd->execute([mb_substr($name, 0, 32), $botId]);
            sendResponse(true, true);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to update name: " . $e->getMessage());
        }
        break;

    case 'set_my_description':
        $description = (string)($input['description'] ?? '');
        try {
            $upd = $pdo->prepare("UPDATE bots SET about = ? WHERE id = ?");
            $upd->execute([mb_substr($description, 0, 64), $botId]);
            sendResponse(true, true);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to update description: " . $e->getMessage());
        }
        break;

    case 'get_updates':
        $offset = isset($input['offset']) ? (int)$input['offset'] : 0;
        $skipUpdates = !empty($input['skip_updates']);

        // If skip_updates requested on first start, instantly watermark to skip all history
        if ($skipUpdates) {
            try {
                $stmtMsgMax = $pdo->prepare("SELECT MAX(id) FROM messages WHERE receiver_id = ?");
                $stmtMsgMax->execute([$botId]);
                $currentMaxMsg = (int)$stmtMsgMax->fetchColumn();

                $stmtCbMax = $pdo->prepare("SELECT MAX(id) FROM bot_callbacks WHERE bot_id = ?");
                $stmtCbMax->execute([$botId]);
                $currentMaxCb = (int)$stmtCbMax->fetchColumn();

                if ($currentMaxMsg > 0) {
                    $stmtCheck = $pdo->prepare("SELECT 1 FROM bot_updates WHERE bot_id = ? AND update_type = 'message' AND reference_id >= ? LIMIT 1");
                    $stmtCheck->execute([$botId, $currentMaxMsg]);
                    if (!$stmtCheck->fetch()) {
                        $stmtIns = $pdo->prepare("INSERT INTO bot_updates (bot_id, update_type, reference_id, payload) VALUES (?, 'message', ?, '{}')");
                        $stmtIns->execute([$botId, $currentMaxMsg]);
                    }
                }
                if ($currentMaxCb > 0) {
                    $stmtCheck = $pdo->prepare("SELECT 1 FROM bot_updates WHERE bot_id = ? AND update_type = 'callback_query' AND reference_id >= ? LIMIT 1");
                    $stmtCheck->execute([$botId, $currentMaxCb]);
                    if (!$stmtCheck->fetch()) {
                        $stmtIns = $pdo->prepare("INSERT INTO bot_updates (bot_id, update_type, reference_id, payload) VALUES (?, 'callback_query', ?, '{}')");
                        $stmtIns->execute([$botId, $currentMaxCb]);
                    }
                }

                $stmtMax = $pdo->prepare("SELECT MAX(id) FROM bot_updates WHERE bot_id = ?");
                $stmtMax->execute([$botId]);
                $maxId = (int)$stmtMax->fetchColumn();
                $offset = max($offset, $maxId);
            } catch (PDOException $e) {}
        }

        // Helper: sync unsynced messages and callbacks into bot_updates
        $syncAllUpdates = function () use ($pdo, $botId) {
            try {
                // 1. Messages
                $stmtMaxMsg = $pdo->prepare("SELECT MAX(reference_id) FROM bot_updates WHERE bot_id = ? AND update_type = 'message'");
                $stmtMaxMsg->execute([$botId]);
                $lastSyncedMsgId = (int)$stmtMaxMsg->fetchColumn();

                $stmtUnsyncedMsgs = $pdo->prepare("
                    SELECT * FROM messages
                    WHERE receiver_id = :bot_id AND id > :last_id
                    ORDER BY id ASC LIMIT 50
                ");
                $stmtUnsyncedMsgs->execute([':bot_id' => $botId, ':last_id' => $lastSyncedMsgId]);
                $unsyncedMsgs = $stmtUnsyncedMsgs->fetchAll();

                if (!empty($unsyncedMsgs)) {
                    $stmtInsertMsg = $pdo->prepare("INSERT INTO bot_updates (bot_id, update_type, reference_id, message_id, payload) VALUES (?, 'message', ?, ?, ?)");
                    $userCache = [];
                    foreach ($unsyncedMsgs as $row) {
                        $senderId = (int)$row['sender_id'];
                        if (!isset($userCache[$senderId])) {
                            $userCache[$senderId] = getUserInfo($pdo, $senderId);
                        }
                        $payload = [
                            'id' => (int)$row['id'],
                            'message_id' => (int)$row['id'],
                            'type' => 'message',
                            'sender_id' => $senderId,
                            'sender_info' => $userCache[$senderId],
                            'content' => $row['content'],
                            'timestamp' => $row['timestamp'],
                            'reply_to_id' => $row['reply_to_id'] !== null ? (int)$row['reply_to_id'] : null,
                            'is_bot' => ($row['sender_type'] === 'bot') ? 1 : 0,
                            'is_media' => (!empty($row['attachments'])) ? 1 : 0,
                            'attachments' => $row['attachments'] ? json_decode($row['attachments'], true) : null,
                            'reply_markup' => (!empty($row['reply_markup'])) ? (is_array($row['reply_markup']) ? $row['reply_markup'] : json_decode($row['reply_markup'], true)) : null
                        ];
                        $stmtInsertMsg->execute([$botId, (int)$row['id'], (int)$row['id'], json_encode($payload, JSON_UNESCAPED_UNICODE)]);
                    }
                }

                // 2. Callbacks
                $stmtMaxCb = $pdo->prepare("SELECT MAX(reference_id) FROM bot_updates WHERE bot_id = ? AND update_type = 'callback_query'");
                $stmtMaxCb->execute([$botId]);
                $lastSyncedCbId = (int)$stmtMaxCb->fetchColumn();

                $stmtUnsyncedCbs = $pdo->prepare("
                    SELECT * FROM bot_callbacks
                    WHERE bot_id = :bot_id AND id > :last_id
                    ORDER BY id ASC LIMIT 50
                ");
                $stmtUnsyncedCbs->execute([':bot_id' => $botId, ':last_id' => $lastSyncedCbId]);
                $unsyncedCbs = $stmtUnsyncedCbs->fetchAll();

                if (!empty($unsyncedCbs)) {
                    $stmtInsertCb = $pdo->prepare("INSERT INTO bot_updates (bot_id, update_type, reference_id, message_id, payload) VALUES (?, 'callback_query', ?, ?, ?)");
                    foreach ($unsyncedCbs as $cb) {
                        $payload = [
                            'id' => (int)$cb['id'],
                            'type' => 'callback_query',
                            'callback_query_id' => (int)$cb['id'],
                            'sender_id' => (int)$cb['user_id'],
                            'message_id' => (int)$cb['message_id'],
                            'data' => $cb['data']
                        ];
                        $stmtInsertCb->execute([$botId, (int)$cb['id'], (int)$cb['message_id'], json_encode($payload, JSON_UNESCAPED_UNICODE)]);
                    }
                }
            } catch (PDOException $e) {}
        };

        if (!$skipUpdates) {
            $syncAllUpdates();
        }

        // --------------------------------------------------------------
        // Single-instance polling lock (DB-based, no leftover files).
        // The newest poller claims the lock; older instances detect the
        // change on their next iteration and stop with a Conflict.
        // --------------------------------------------------------------
        try {
            $clientId = bin2hex(random_bytes(16));
        } catch (\Exception $e) {
            $clientId = uniqid('', true);
        }
        try {
            $claim = $pdo->prepare("INSERT INTO bot_polling_locks (bot_id, client_id) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE client_id = VALUES(client_id), updated_at = CURRENT_TIMESTAMP");
            $claim->execute([$botId, $clientId]);
        } catch (PDOException $e) {}
        $lockCheck = $pdo->prepare("SELECT client_id FROM bot_polling_locks WHERE bot_id = ? LIMIT 1");

        $timeout = 25; // 25 seconds long polling
        $startTime = time();

        try {
            $stmtUpdates = $pdo->prepare("SELECT * FROM bot_updates WHERE bot_id = :bot_id AND id > :offset AND payload != '{}' ORDER BY id ASC LIMIT 50");

            while (time() - $startTime < $timeout) {
                // Conflict resolution: another get_updates request took over.
                try {
                    $lockCheck->execute([$botId]);
                    $currentClient = (string)$lockCheck->fetchColumn();
                    if ($currentClient !== '' && $currentClient !== $clientId) {
                        sendResponse(false, null, "Conflict: terminated by another get_updates request");
                    }
                } catch (PDOException $e) {}

                $syncAllUpdates();

                try {
                    $stmtUpdates->execute([':bot_id' => $botId, ':offset' => $offset]);
                    $updates = $stmtUpdates->fetchAll();

                    if (!empty($updates)) {
                        $formatted = [];
                        foreach ($updates as $row) {
                            $payload = json_decode($row['payload'], true);
                            if (!is_array($payload) || empty($payload)) continue;
                            $payload['id'] = (int)$row['id']; // update id (used as polling offset)
                            if ($row['message_id'] !== null && !isset($payload['message_id'])) {
                                $payload['message_id'] = (int)$row['message_id'];
                            }
                            if ($row['update_type'] === 'callback_query') {
                                $payload['type'] = 'callback_query';
                                $payload['callback_query_id'] = (int)$row['id'];
                            }
                            $formatted[] = $payload;
                        }
                        if (!empty($formatted)) {
                            sendResponse(true, $formatted);
                        }
                    }
                } catch (PDOException $e) {
                    // Table might be pending creation
                }

                usleep(100000); // 100ms
            }

            // Timeout reached
            sendResponse(true, [], null, ['new_offset' => $offset]);
        } catch (PDOException $e) {
            sendResponse(false, null, "Failed to fetch updates: " . $e->getMessage());
        }
        break;

    default:
        sendResponse(false, null, "Method not found: " . $method);
}