package com.flasskdev.vibe.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Всё, что раньше жило в SharedPreferences, в памяти ViewModel или вообще
 * не кэшировалось. Вынесено в отдельный DAO, чтобы не раздувать ChatDao.
 */
@Dao
interface CacheDao {

    /* ------------------------- sticker packs ------------------------- */

    @Query("SELECT * FROM sticker_packs_cache ORDER BY position ASC, packId ASC")
    fun observePacks(): Flow<List<StickerPackCacheEntity>>

    @Query("SELECT * FROM sticker_packs_cache WHERE isInstalled = 1 ORDER BY position ASC")
    fun observeInstalledPacks(): Flow<List<StickerPackCacheEntity>>

    @Query("SELECT * FROM sticker_packs_cache")
    suspend fun getPacks(): List<StickerPackCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacks(packs: List<StickerPackCacheEntity>)

    @Query("DELETE FROM sticker_packs_cache WHERE packId = :packId")
    suspend fun deletePack(packId: Int)

    /** Пишем только изменившиеся паки, чтобы не инвалидировать Flow зря. */
    @Transaction
    suspend fun syncPacks(incoming: List<StickerPackCacheEntity>) {
        val existing = getPacks().associateBy { it.packId }
        val incomingIds = incoming.mapTo(HashSet()) { it.packId }
        existing.keys.filter { it !in incomingIds }.forEach { deletePack(it) }
        val changed = incoming.filter { new ->
            val old = existing[new.packId]
            old == null || old.copy(cachedAt = new.cachedAt) != new
        }
        if (changed.isNotEmpty()) insertPacks(changed)
    }

    /* --------------------------- recents ----------------------------- */

    @Query("SELECT * FROM recent_items WHERE kind = :kind ORDER BY usedAt DESC LIMIT :limit")
    fun observeRecents(kind: String, limit: Int = 40): Flow<List<RecentItemEntity>>

    @Query("SELECT * FROM recent_items WHERE kind = :kind ORDER BY usedAt DESC LIMIT :limit")
    suspend fun getRecents(kind: String, limit: Int = 40): List<RecentItemEntity>

    @Query("SELECT useCount FROM recent_items WHERE kind = :kind AND value = :value")
    suspend fun recentUseCount(kind: String, value: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(item: RecentItemEntity)

    @Query("DELETE FROM recent_items WHERE kind = :kind AND value NOT IN (SELECT value FROM recent_items WHERE kind = :kind ORDER BY usedAt DESC LIMIT :keep)")
    suspend fun trimRecents(kind: String, keep: Int)

    @Transaction
    suspend fun touchRecent(kind: String, value: String, keep: Int = 40) {
        val count = (recentUseCount(kind, value) ?: 0) + 1
        insertRecent(RecentItemEntity(kind, value, System.currentTimeMillis(), count))
        trimRecents(kind, keep)
    }

    @Query("DELETE FROM recent_items WHERE kind = :kind")
    suspend fun clearRecents(kind: String)

    /* ------------------------- media LRU cache ------------------------ */

    @Query("SELECT * FROM media_cache WHERE remoteUrl = :url LIMIT 1")
    suspend fun findMedia(url: String): MediaCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(entity: MediaCacheEntity)

    @Query("UPDATE media_cache SET lastAccess = :now WHERE key = :key")
    suspend fun touchMedia(key: String, now: Long = System.currentTimeMillis())

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_cache")
    suspend fun totalMediaBytes(): Long

    @Query("SELECT * FROM media_cache WHERE pinned = 0 ORDER BY lastAccess ASC LIMIT :limit")
    suspend fun oldestMedia(limit: Int): List<MediaCacheEntity>

    @Query("DELETE FROM media_cache WHERE key IN (:keys)")
    suspend fun deleteMedia(keys: List<String>)

    @Query("DELETE FROM media_cache")
    suspend fun clearMedia()

    @Query("SELECT * FROM media_cache")
    suspend fun allMedia(): List<MediaCacheEntity>

    /* ----------------------------- outbox ----------------------------- */

    @Insert
    suspend fun enqueue(op: OutboxEntity): Long

    @Query("SELECT * FROM outbox ORDER BY createdAt ASC LIMIT :limit")
    suspend fun peekOutbox(limit: Int = 100): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM outbox")
    fun observeOutboxSize(): Flow<Int>

    @Query("DELETE FROM outbox WHERE localId = :localId")
    suspend fun dequeue(localId: Long)

    @Query("UPDATE outbox SET attempts = attempts + 1, lastError = :error WHERE localId = :localId")
    suspend fun markOutboxFailure(localId: Long, error: String?)

    /** Операции, которые не удалось доставить 20 раз, выбрасываем — иначе очередь встанет колом. */
    @Query("DELETE FROM outbox WHERE attempts >= 20")
    suspend fun dropDeadOutbox()

    @Query("DELETE FROM outbox")
    suspend fun clearOutbox()

    /* ---------------------------- meta ------------------------------- */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: CacheMetaEntity)

    @Query("SELECT * FROM cache_meta WHERE key = :key")
    suspend fun getMeta(key: String): CacheMetaEntity?

    suspend fun getLong(key: String, default: Long = 0L): Long = getMeta(key)?.longValue ?: default

    suspend fun setLong(key: String, value: Long) = putMeta(CacheMetaEntity(key, longValue = value))

    /* ---------------------------- e2ee -------------------------------- */

    @Query("SELECT * FROM e2ee_sessions WHERE peerId = :peerId")
    suspend fun getSession(peerId: Int): E2eeSessionEntity?

    @Query("SELECT * FROM e2ee_sessions WHERE peerId = :peerId")
    fun observeSession(peerId: Int): Flow<E2eeSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSession(session: E2eeSessionEntity)

    @Query("UPDATE e2ee_sessions SET verified = 1 WHERE peerId = :peerId")
    suspend fun markSessionVerified(peerId: Int)

    @Query("DELETE FROM e2ee_sessions WHERE peerId = :peerId")
    suspend fun dropSession(peerId: Int)

    @Query("DELETE FROM e2ee_sessions")
    suspend fun clearSessions()
}