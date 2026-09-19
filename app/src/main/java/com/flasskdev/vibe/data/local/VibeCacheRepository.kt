package com.flasskdev.vibe.data.local

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Единая точка входа в кэш. Раньше кэш был размазан по ChatScreenViewModel,
 * VibeWebSocket, RecentsStore (SharedPreferences) и GifCache — из-за чего
 * одни и те же данные писались по 2-3 раза и Flow дёргались лишний раз.
 */
class VibeCacheRepository private constructor(
    context: Context,
    val db: AppDatabase
) {
    private val appContext = context.applicationContext
    val chatDao: ChatDao = db.chatDao()
    val cacheDao: CacheDao = db.cacheDao()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        // Пул соединений переиспользуется между загрузками — раньше на каждый
        // файл создавался новый OkHttpClient (новый пул, новый TLS-хендшейк).
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .build()

    val media: MediaDiskCache = MediaDiskCache(appContext, cacheDao, http)

    companion object {
        private const val KEY_LAST_CHAT_SYNC = "last_chat_sync"
        private const val KEY_LAST_PACKS_SYNC = "last_packs_sync"
        private const val KEY_RECENTS_MIGRATED = "recents_migrated_v23"

        /** Стикерпаки перечитываем с сервера не чаще раза в 6 часов. */
        private const val PACKS_TTL_MS = 6 * 60 * 60 * 1000L
        /** Профиль пользователя считаем свежим 5 минут. */
        const val USER_TTL_MS = 5 * 60 * 1000L

        @Volatile private var INSTANCE: VibeCacheRepository? = null

        fun get(context: Context): VibeCacheRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VibeCacheRepository(
                    context,
                    AppDatabase.getDatabase(context)
                ).also {
                    INSTANCE = it
                    it.bootstrap()
                }
            }
    }

    /** Разовая работа при старте приложения: чистка, миграция recents, верификация кэша. */
    private fun bootstrap() = scope.launch {
        runCatching { migrateRecentsFromPrefs() }
        runCatching { cacheDao.dropDeadOutbox() }
        runCatching { media.verify() }
    }

    /* ---------------------------- users ------------------------------- */

    fun observeUser(id: Int): Flow<UserCacheEntity?> = chatDao.getUserById(id)

    /** true, если профиль нужно перезапросить у сервера. */
    suspend fun isUserStale(id: Int, ttlMs: Long = USER_TTL_MS): Boolean {
        val user = chatDao.getUserByIdSync(id) ?: return true
        return System.currentTimeMillis() - user.cachedAt > ttlMs
    }

    suspend fun cacheUsers(users: List<UserCacheEntity>) {
        val now = System.currentTimeMillis()
        chatDao.upsertUsersIfChanged(users.map { it.copy(cachedAt = now) })
    }

    /* ------------------------- sticker packs -------------------------- */

    val installedPacks: Flow<List<StickerPackCacheEntity>> = cacheDao.observeInstalledPacks()

    suspend fun packsNeedRefresh(): Boolean =
        System.currentTimeMillis() - cacheDao.getLong(KEY_LAST_PACKS_SYNC) > PACKS_TTL_MS

    suspend fun onPacksLoaded(packs: List<StickerPackCacheEntity>) {
        cacheDao.syncPacks(packs)
        cacheDao.setLong(KEY_LAST_PACKS_SYNC, System.currentTimeMillis())
    }

    /* ---------------------------- recents ----------------------------- */

    fun recentEmojis(limit: Int = 40): Flow<List<String>> =
        cacheDao.observeRecents("emoji", limit).map { list -> list.map { it.value } }

    fun recentStickers(limit: Int = 24): Flow<List<String>> =
        cacheDao.observeRecents("sticker", limit).map { list -> list.map { it.value } }

    fun recentGifs(limit: Int = 24): Flow<List<String>> =
        cacheDao.observeRecents("gif", limit).map { list -> list.map { it.value } }

    fun touchEmoji(value: String) = scope.launch { cacheDao.touchRecent("emoji", value, 40) }
    fun touchSticker(value: String) = scope.launch { cacheDao.touchRecent("sticker", value, 24) }
    fun touchGif(value: String) = scope.launch { cacheDao.touchRecent("gif", value, 24) }

    private suspend fun migrateRecentsFromPrefs() {
        if (cacheDao.getLong(KEY_RECENTS_MIGRATED) == 1L) return
        val prefs = appContext.getSharedPreferences("emoji_recents", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        prefs.getString("recent_emojis", "")?.split("\u0001")
            ?.filter { it.isNotBlank() }
            ?.forEachIndexed { i, v -> cacheDao.insertRecent(RecentItemEntity("emoji", v, now - i)) }

        prefs.getString("recent_stickers", "")?.split("\u0001")
            ?.filter { it.isNotBlank() }
            ?.forEachIndexed { i, v -> cacheDao.insertRecent(RecentItemEntity("sticker", v, now - i)) }

        cacheDao.setLong(KEY_RECENTS_MIGRATED, 1L)
        prefs.edit().clear().apply()
    }

    /* ----------------------------- outbox ----------------------------- */

    val pendingCount: Flow<Int> = cacheDao.observeOutboxSize()

    suspend fun enqueue(opType: String, payloadJson: String, localMessageId: Int? = null): Long =
        cacheDao.enqueue(OutboxEntity(opType = opType, payload = payloadJson, localMessageId = localMessageId))

    suspend fun drainOutbox(send: suspend (OutboxEntity) -> Boolean) = withContext(Dispatchers.IO) {
        cacheDao.dropDeadOutbox()
        for (op in cacheDao.peekOutbox()) {
            val ok = runCatching { send(op) }.getOrElse { false }
            if (ok) cacheDao.dequeue(op.localId)
            else {
                cacheDao.markOutboxFailure(op.localId, "delivery failed")
                break // сохраняем порядок: не пропускаем вперёд следующие операции
            }
        }
    }

    /* ------------------------- chat list sync ------------------------- */

    suspend fun lastChatSync(): Long = cacheDao.getLong(KEY_LAST_CHAT_SYNC)
    suspend fun markChatSync() = cacheDao.setLong(KEY_LAST_CHAT_SYNC, System.currentTimeMillis())

    /* --------------------------- wipe on logout ----------------------- */

    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        media.clear()
    }
}