package com.flasskdev.vibe.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Обновляем SQL запрос
    @Query("""
        SELECT chats.interlocutorId, chats.lastMessage, chats.timestamp, chats.unreadCount, 
               chats.isLastMessageMine, chats.isLastMessageRead, chats.isMuted, chats.draft, chats.canMessage, chats.lastMessageAttachments, chats.pinned, chats.lastMessageEncrypted,
               COALESCE(users_cache.isBlockedByMe, chats.isBlockedByMe) AS isBlockedByMe, 
               COALESCE(users_cache.isBlockedByUser, chats.isBlockedByUser) AS isBlockedByUser,
               COALESCE(users_cache.isDeveloper, chats.isDeveloper) AS isDeveloper, 
               COALESCE(users_cache.isVerified, chats.isVerified) AS isVerified, 
               COALESCE(users_cache.isBanned, chats.isBanned) AS isBanned, 
               COALESCE(users_cache.isFreezed, chats.isFreezed) AS isFreezed,
               users_cache.name, users_cache.username, users_cache.avatarUrl, 
               users_cache.isOnline, users_cache.lastSeen, users_cache.isBot, users_cache.about 
        FROM chats 
        LEFT JOIN users_cache ON chats.interlocutorId = users_cache.id 
        ORDER BY chats.pinned DESC, chats.timestamp DESC
    """)
    fun getChatsWithUserInfo(): Flow<List<ChatWithUser>>

    @Query("SELECT * FROM users_cache WHERE id = :id")
    fun getUserById(id: Int): Flow<UserCacheEntity?>

    @Query("SELECT * FROM users_cache")
    fun getAllUsersFlow(): Flow<List<UserCacheEntity>>

    @Query("SELECT * FROM users_cache WHERE id = :id")
    suspend fun getUserByIdSync(id: Int): UserCacheEntity?

    @Query("SELECT * FROM users_cache WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserCacheEntity>)

    @Query("SELECT * FROM chats WHERE (draft IS NOT NULL AND draft != '') OR pinned = 1")
    suspend fun getChatsWithDraftsOrPinned(): List<ChatEntity>

    @Query("SELECT * FROM chats")
    suspend fun getAllChats(): List<ChatEntity>

    /* ======================================================================
     *  ДИФФ-ЗАПИСЬ (PERF)
     *
     *  @Insert(onConflict = REPLACE) перезаписывает строку даже если данные
     *  не изменились ни на байт. Каждая такая запись инвалидирует таблицу
     *  в Room, а значит ВСЕ Flow над ней пересобирают список заново:
     *  для messages это getMessagesByPartner + getMessagesWithAttachments +
     *  searchMessagesInChat, для chats/users_cache — getChatsWithUserInfo
     *  с JOIN. Отсюда и лаги при входе в переписку: сервер отдавал те же
     *  15 сообщений, что уже лежали в кеше, а UI перестраивался целиком.
     *
     *  Ниже — сравнение с кешем и запись ТОЛЬКО изменившихся строк.
     * ====================================================================== */

    @Transaction
    suspend fun upsertMessagesIfChanged(messages: List<MessageEntity>) {
        if (messages.isEmpty()) return

        // SQLite ограничивает число переменных в IN (...), поэтому читаем чанками.
        val existing = HashMap<Int, MessageEntity>(messages.size)
        messages.map { it.id }.chunked(500).forEach { chunk ->
            getMessagesByIds(chunk).forEach { existing[it.id] = it }
        }

        val toWrite = messages.mapNotNull { incoming ->
            val old = existing[incoming.id]
            if (old == null) {
                incoming
            } else {
                // uploadStatus/uploadProgress живут только локально, сервер их
                // не присылает — переносим из кеша, иначе прогресс аплоада сбросится.
                val merged = incoming.copy(
                    uploadStatus = old.uploadStatus,
                    uploadProgress = old.uploadProgress
                )
                if (merged == old) null else merged
            }
        }

        if (toWrite.isNotEmpty()) insertMessages(toWrite)
    }

    @Transaction
    suspend fun upsertUsersIfChanged(users: List<UserCacheEntity>) {
        if (users.isEmpty()) return
        val toWrite = users.filter { it != getUserByIdSync(it.id) }
        if (toWrite.isNotEmpty()) insertUsers(toWrite)
    }

    /**
     * Замена replaceAllChats. Тот делал DELETE FROM chats + полную вставку на
     * КАЖДОЕ входящее сообщение и на каждый реконнект: список чатов полностью
     * пересобирался, включая JOIN с users_cache. Здесь удаляем только реально
     * исчезнувшие чаты и пишем только изменившиеся строки.
     */
    @Transaction
    suspend fun syncChats(chats: List<ChatEntity>, users: List<UserCacheEntity>) {
        val existing = getAllChats().associateBy { it.interlocutorId }
        val incomingIds = chats.mapTo(HashSet()) { it.interlocutorId }

        // Черновики и закреплённые чаты не выбрасываем, даже если сервер их не прислал.
        existing.values
            .filter { it.interlocutorId !in incomingIds && it.draft.isNullOrEmpty() && !it.pinned }
            .forEach { deleteChat(it.interlocutorId) }

        val toWrite = chats.mapNotNull { incoming ->
            val old = existing[incoming.interlocutorId]
            val merged = incoming.copy(
                draft = old?.draft,
                pinned = old?.pinned ?: false
            )
            if (merged == old) null else merged
        }

        if (toWrite.isNotEmpty()) insertChats(toWrite)
        upsertUsersIfChanged(users)
    }

    @Transaction
    suspend fun replaceAllChats(chats: List<ChatEntity>, users: List<UserCacheEntity>) {
        val existing = getChatsWithDraftsOrPinned().associateBy { it.interlocutorId }
        val incomingIds = chats.map { it.interlocutorId }.toSet()
        val missingChats = existing.values.filter { it.interlocutorId !in incomingIds }
        
        val updatedChats = chats.map { chat ->
            val draft = existing[chat.interlocutorId]?.draft
            val pinned = existing[chat.interlocutorId]?.pinned ?: false
            chat.copy(draft = draft, pinned = pinned)
        }
        
        deleteAllChats()
        insertChats(updatedChats + missingChats)
        insertUsers(users)
    }

    @Query("DELETE FROM chats WHERE interlocutorId = :id")
    suspend fun deleteChat(id: Int)

    @Query("""
        SELECT * FROM (
            SELECT * FROM messages 
            WHERE (senderId = :userId AND receiverId = :partnerId)
               OR (senderId = :partnerId AND receiverId = :userId)
            ORDER BY timestamp DESC, id DESC 
            LIMIT :limit
        ) ORDER BY timestamp ASC, id ASC
    """)
    fun getMessagesByPartner(userId: Int, partnerId: Int, limit: Int): Flow<List<MessageEntity>>

    /**
     * Тот же запрос, но одноразовый. Нужен для прогрева (AppWarmup): SQLite
     * компилирует statement и поднимает страницы индекса в кэш соединения ДО того,
     * как пользователь войдёт в чат. Flow тут не нужен и был бы вреден.
     */
    @Query("""
        SELECT * FROM (
            SELECT * FROM messages 
            WHERE (senderId = :userId AND receiverId = :partnerId)
               OR (senderId = :partnerId AND receiverId = :userId)
            ORDER BY timestamp DESC, id DESC 
            LIMIT :limit
        ) ORDER BY timestamp ASC, id ASC
    """)
    suspend fun getMessagesByPartnerOnce(userId: Int, partnerId: Int, limit: Int): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id IN (SELECT id FROM messages WHERE id < 0 AND content = :content AND receiverId = :receiverId ORDER BY timestamp ASC LIMIT 1)")
    suspend fun deleteOldestPendingMessage(content: String, receiverId: Int)

    @Query("SELECT * FROM messages WHERE id < 0 ORDER BY timestamp ASC")
    suspend fun getAllPendingMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = 1 WHERE senderId = :fromUserId AND receiverId = :userId AND isRead = 0")
    suspend fun markMessagesAsRead(userId: Int, fromUserId: Int)

    @Query("UPDATE messages SET isRead = 1 WHERE senderId = :fromUserId AND receiverId = :userId AND isRead = 0 AND id <= :upToMessageId")
    suspend fun markMessagesAsReadUpTo(userId: Int, fromUserId: Int, upToMessageId: Int)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateMessageReactions(messageId: Int, reactions: List<ReactionItem>?)

    @Query("DELETE FROM messages WHERE ((senderId = :userId AND receiverId = :partnerId) OR (senderId = :partnerId AND receiverId = :userId)) AND id > 0")
    suspend fun deleteMessagesByPartner(userId: Int, partnerId: Int)

    @Transaction
    suspend fun replaceMessagesByPartner(userId: Int, partnerId: Int, messages: List<MessageEntity>) {
        deleteMessagesByPartner(userId, partnerId)
        insertMessages(messages)
    }

    @Query("""
        SELECT * FROM messages 
        WHERE ((senderId = :userId AND receiverId = :partnerId)
           OR (senderId = :partnerId AND receiverId = :userId))
          AND attachments IS NOT NULL AND attachments != '[]'
        ORDER BY timestamp DESC
    """)
    fun getMessagesWithAttachments(userId: Int, partnerId: Int): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE ((senderId = :userId AND receiverId = :partnerId) OR (senderId = :partnerId AND receiverId = :userId))
          AND content LIKE '%' || :query || '%' 
          AND substr(content, 1, 2) != '${"$$"}'
        ORDER BY timestamp DESC, id DESC
    """)
    fun searchMessagesInChat(userId: Int, partnerId: Int, query: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE ((senderId = :userId AND receiverId = :partnerId) OR (senderId = :partnerId AND receiverId = :userId))
          AND timestamp >= :targetTimestamp
        ORDER BY timestamp ASC, id ASC 
        LIMIT 1
    """)
    suspend fun getFirstMessageOnOrAfterDate(userId: Int, partnerId: Int, targetTimestamp: Long): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE ((senderId = :userId AND receiverId = :partnerId) OR (senderId = :partnerId AND receiverId = :userId))
        ORDER BY ABS(timestamp - :targetTimestamp) ASC 
        LIMIT 1
    """)
    suspend fun getMessageClosestToTimestamp(userId: Int, partnerId: Int, targetTimestamp: Long): MessageEntity?

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("UPDATE chats SET isMuted = :isMuted WHERE interlocutorId = :interlocutorId")
    suspend fun updateMuteStatus(interlocutorId: Int, isMuted: Boolean)

    @Query("UPDATE chats SET pinned = :isPinned WHERE interlocutorId = :interlocutorId")
    suspend fun updatePinnedStatus(interlocutorId: Int, isPinned: Boolean)

    @Query("SELECT COUNT(*) FROM chats WHERE pinned = 1")
    suspend fun getPinnedChatsCount(): Int

    @Query("UPDATE chats SET draft = :draft WHERE interlocutorId = :interlocutorId")
    suspend fun updateDraft(interlocutorId: Int, draft: String?)

    @Transaction
    suspend fun saveDraft(interlocutorId: Int, draft: String?) {
        val chat = getChatById(interlocutorId)
        if (chat != null) {
            updateDraft(interlocutorId, draft)
        } else if (!draft.isNullOrBlank()) {
            val newChat = ChatEntity(
                interlocutorId = interlocutorId,
                lastMessage = "",
                timestamp = System.currentTimeMillis(),
                draft = draft
            )
            insertChat(newChat)
        }
    }

    @Query("SELECT * FROM chats WHERE interlocutorId = :interlocutorId LIMIT 1")
    suspend fun getChatById(interlocutorId: Int): ChatEntity?

    @Query("SELECT isMuted FROM chats WHERE interlocutorId = :userId")
    suspend fun isUserMuted(userId: Int): Boolean?

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("UPDATE messages SET isRead = 1 WHERE senderId = :myId AND receiverId = :partnerId AND isRead = 0")
    suspend fun markMyMessagesAsRead(myId: Int, partnerId: Int)

    @Query("UPDATE messages SET isRead = 1 WHERE senderId = :myId AND receiverId = :partnerId AND isRead = 0 AND id <= :upToMessageId")
    suspend fun markMyMessagesAsReadUpTo(myId: Int, partnerId: Int, upToMessageId: Int)

    @Query("UPDATE users_cache SET isOnline = :isOnline, lastSeen = :lastSeen, isDeveloper = :isDev, isVerified = :isVer, registerDate = :registerDate, isBot = :isBot, about = :about, canMessage = :canMessage, isBlockedByMe = :isBlockedByMe, isBlockedByUser = :isBlockedByUser WHERE id = :userId")
    suspend fun updateUserStatus(userId: Int, isOnline: Boolean, lastSeen: Long?, isDev: Boolean, isVer: Boolean, registerDate: Long?, isBot: Boolean, about: String?, canMessage: Boolean = true, isBlockedByMe: Boolean = false, isBlockedByUser: Boolean = false)

    @Query("UPDATE users_cache SET isBlockedByMe = :isBlocked WHERE id = :userId")
    suspend fun updateUserBlockedByMe(userId: Int, isBlocked: Boolean)

    @Query("UPDATE chats SET isBlockedByMe = :isBlocked WHERE interlocutorId = :userId")
    suspend fun updateChatBlockedByMe(userId: Int, isBlocked: Boolean)

    @Query("UPDATE users_cache SET isBlockedByUser = :isBlocked WHERE id = :userId")
    suspend fun updateUserBlockedByUser(userId: Int, isBlocked: Boolean)

    @Query("UPDATE chats SET isBlockedByUser = :isBlocked WHERE interlocutorId = :userId")
    suspend fun updateChatBlockedByUser(userId: Int, isBlocked: Boolean)

    @Query("UPDATE users_cache SET avatarUrl = :avatarUrl WHERE id = :userId")
    suspend fun updateUserAvatar(userId: Int, avatarUrl: String)

    @Query("UPDATE messages SET content = :newContent, isEdited = 1 WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: Int, newContent: String)

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteMessagesByIds(ids: List<Int>)

    @Query("SELECT * FROM messages WHERE id IN (:ids) ORDER BY id ASC")
    suspend fun getMessagesByIds(ids: List<Int>): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Int): MessageEntity?

    @Query("UPDATE messages SET uploadProgress = :progress WHERE id = :messageId")
    suspend fun updateUploadProgress(messageId: Int, progress: Int)

    @Query("UPDATE messages SET uploadStatus = :status, attachments = :finalAttachments WHERE id = :messageId")
    suspend fun updateUploadStatus(messageId: Int, status: String, finalAttachments: List<String>?)

    /** Статус без затирания attachments: updateUploadStatus() пишет ещё и вложения. */
    @Query("UPDATE messages SET uploadStatus = :status WHERE id = :messageId")
    suspend fun updateUploadStatusOnly(messageId: Int, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileCache(fileCache: FileCacheEntity)

    @Query("SELECT url FROM file_cache WHERE hash = :hash LIMIT 1")
    suspend fun getCachedFileUrl(hash: String): String?
}

// Обновляем саму модель в самом низу файла
data class ChatWithUser(
    @Embedded val chat: ChatEntity,
    val name: String?,
    val username: String?,
    val avatarUrl: String?,
    val isOnline: Boolean?,
    val lastSeen: Long?,
    val isBot: Boolean?,
    val about: String?
)