package com.flasskdev.vibe.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/* ============================================================================
 *  ВАЖНО ПО ИНДЕКСАМ
 *  ---------------------------------------------------------------------------
 *  Раньше таблица messages не имела ни одного индекса кроме PK. Каждый
 *  getMessagesByPartner / getMessagesWithAttachments / searchMessagesInChat
 *  делал FULL TABLE SCAN. На 20 000 сообщений это ~40-70 мс на КАЖДУЮ эмиссию
 *  Flow — отсюда фризы при открытии чата и при скролле.
 *  Ниже добавлены составные индексы под реальные запросы DAO.
 * ========================================================================== */

@Entity(
    tableName = "chats",
    indices = [
        Index(value = ["pinned", "timestamp"]),
        Index(value = ["timestamp"])
    ]
)
data class ChatEntity(
    @PrimaryKey val interlocutorId: Int,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0,
    val isLastMessageMine: Boolean = false,
    val isLastMessageRead: Boolean = false,
    val isDeveloper: Boolean = false,
    val isVerified: Boolean = false,
    val isBanned: Boolean = false,
    val isFreezed: Boolean = false,
    val isMuted: Boolean = false,
    val draft: String? = null,
    val canMessage: Boolean = true,
    val lastMessageAttachments: List<String>? = null,
    val pinned: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isBlockedByUser: Boolean = false,
    /** true, если последнее сообщение зашифровано E2EE и его нельзя показать без ключа */
    val lastMessageEncrypted: Boolean = false
)

@Entity(
    tableName = "users_cache",
    indices = [Index(value = ["username"], unique = false)]
)
data class UserCacheEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val username: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val isDeveloper: Boolean = false,
    val isVerified: Boolean = false,
    val isBanned: Boolean = false,
    val isFreezed: Boolean = false,
    val registerDate: Long? = null,
    val isBot: Boolean = false,
    val about: String? = null,
    val lastSeenStatus: String? = null,
    val canMessage: Boolean = true,
    val isBlockedByMe: Boolean = false,
    val isBlockedByUser: Boolean = false,
    /** Отпечаток публичного ключа собеседника (E2EE), для экрана «Проверить шифрование» */
    val e2eeFingerprint: String? = null,
    /** Когда запись последний раз обновлялась с сервера — для TTL-инвалидации */
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    indices = [
        // Основной индекс под getMessagesByPartner (обе стороны переписки).
        Index(value = ["senderId", "receiverId", "timestamp"]),
        Index(value = ["receiverId", "senderId", "timestamp"]),
        // Под markMessagesAsRead / счётчики непрочитанного.
        Index(value = ["receiverId", "isRead"]),
        // Под getAllPendingMessages (id < 0) и очистку outbox.
        Index(value = ["id", "timestamp"]),
        Index(value = ["isPinned"])
    ]
)
@Immutable
data class MessageEntity(
    @PrimaryKey val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val senderType: String = "user",
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val replyToId: Int? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val forwardedFromId: Int? = null,
    val forwardedFromName: String? = null,
    val isEdited: Boolean = false,
    val isPinned: Boolean = false,
    val attachments: List<String>? = null,
    val uploadStatus: String? = null,
    val uploadProgress: Int? = null,
    val reactions: List<ReactionItem>? = null,
    val replyMarkup: ReplyMarkup? = null,

    /* ---------- E2EE ---------- */
    /** true — content хранится в открытом виде ПОСЛЕ локальной расшифровки */
    val isEncrypted: Boolean = false,
    /** true — расшифровать не удалось (нет ключа/битый шифротекст) */
    val decryptFailed: Boolean = false,

    /* ---------- кружочки ---------- */
    /** Длительность видеосообщения-кружочка в мс (дублируется из content для быстрых выборок) */
    val circleDurationMs: Long? = null
)

@Serializable
data class InlineKeyboardButton(
    val text: String,
    val callbackData: String? = null,
    val url: String? = null,
    val bgColor: String? = null,
    val textColor: String? = null
)

@Serializable
data class ReplyMarkup(
    val inlineKeyboard: List<List<InlineKeyboardButton>> = emptyList()
)

@Serializable
data class ReactionUser(
    val userId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ReactionItem(
    val emoji: String,
    val count: Int,
    val userIds: List<Int> = emptyList(),
    val users: List<ReactionUser> = emptyList()
)

/** Дедупликация загрузок: sha-256 файла -> уже загруженный URL. */
@Entity(tableName = "file_cache")
data class FileCacheEntity(
    @PrimaryKey val hash: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

/* ============================================================================
 *  НОВЫЕ ТАБЛИЦЫ КЭША
 * ========================================================================== */

/**
 * Кэш стикерпаков. Раньше список паков тянулся с сервера при каждом открытии
 * панели — панель открывалась с пустым экраном на 300-800 мс.
 */
@Entity(tableName = "sticker_packs_cache")
data class StickerPackCacheEntity(
    @PrimaryKey val packId: Int,
    val name: String,
    val title: String,
    val thumbUrl: String?,
    /** JSON-массив путей до стикеров */
    val stickers: List<String>,
    val isInstalled: Boolean,
    val position: Int = 0,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Недавние стикеры/эмодзи. Раньше жили в SharedPreferences и читались
 * синхронно на главном потоке при каждой рекомпозиции панели.
 */
@Entity(tableName = "recent_items", primaryKeys = ["kind", "value"])
data class RecentItemEntity(
    /** "emoji" | "sticker" | "gif" */
    val kind: String,
    val value: String,
    val usedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 1
)

/**
 * LRU-реестр локально скачанной медиа (фото/видео/голос/кружочки/gif).
 * Сам файл лежит в filesDir/media, тут — метаданные для вытеснения.
 */
@Entity(tableName = "media_cache", indices = [Index(value = ["lastAccess"]), Index(value = ["remoteUrl"], unique = true)])
data class MediaCacheEntity(
    @PrimaryKey val key: String,      // sha-256(remoteUrl)
    val remoteUrl: String,
    val localPath: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccess: Long = System.currentTimeMillis(),
    /** Закреплённые файлы (например, аватар себя) не вытесняются LRU */
    val pinned: Boolean = false
)

/**
 * Надёжная очередь исходящих действий (outbox). Раньше «неотправленные»
 * сообщения жили как messages с id < 0 и терялись при любой ошибке сериализации,
 * а редактирования/реакции/прочтения офлайн просто пропадали.
 */
@Entity(tableName = "outbox", indices = [Index(value = ["createdAt"])])
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    /** send_message | edit_message | delete_messages | send_reaction | mark_read | ... */
    val opType: String,
    /** JSON-пейлоад, который уйдёт в WebSocket как есть */
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null,
    /** id локального сообщения-плейсхолдера, если есть */
    val localMessageId: Int? = null
)

/**
 * Служебные метки кэша: когда какой раздел синхронизировался последний раз.
 * Позволяет делать дельта-синхронизацию вместо «скачать всё заново».
 */
@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val key: String,
    val longValue: Long = 0,
    val stringValue: String? = null
)

/** Ключи E2EE-сессий по собеседникам. */
@Entity(tableName = "e2ee_sessions")
data class E2eeSessionEntity(
    @PrimaryKey val peerId: Int,
    /** Публичный ключ собеседника (X25519, base64) */
    val peerPublicKey: String,
    /** Производный корневой секрет сессии (зашифрован Keystore-ключом, base64) */
    val rootKeyWrapped: String,
    val peerFingerprint: String,
    val establishedAt: Long = System.currentTimeMillis(),
    /** Пользователь лично сверил отпечаток */
    val verified: Boolean = false
)