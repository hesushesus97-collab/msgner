package com.flasskdev.vibe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

@Database(
    entities = [
        ChatEntity::class,
        UserCacheEntity::class,
        MessageEntity::class,
        FileCacheEntity::class,
        StickerPackCacheEntity::class,
        RecentItemEntity::class,
        MediaCacheEntity::class,
        OutboxEntity::class,
        CacheMetaEntity::class,
        E2eeSessionEntity::class
    ],
    version = 23,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * PERF: свой пул под запросы Room.
         *
         * По умолчанию Room берёт внутренний пул ArchTaskExecutor и создаёт его потоки
         * ЛЕНИВО — то есть при первом запросе, а первый запрос у нас случался ровно на
         * входе в переписку. Тут пул создаётся вместе с БД (её открывает AppWarmup на
         * старте приложения), потоки получают внятные имена и низкий приоритет, чтобы
         * не конкурировать с main thread за CPU во время анимации открытия чата.
         */
        private val queryExecutor = Executors.newFixedThreadPool(
            4,
            object : ThreadFactory {
                private val counter = AtomicInteger(1)
                override fun newThread(r: Runnable): Thread =
                    Thread(r, "vibe-room-" + counter.getAndIncrement()).apply {
                        priority = Thread.NORM_PRIORITY - 1
                    }
            }
        )

        /* ------------------------------------------------------------------
         * Старые миграции 5..22 оставлены как есть (см. историю проекта).
         * Ниже — только новая 22 -> 23.
         * ---------------------------------------------------------------- */
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- новые колонки ---
                db.execSQL("ALTER TABLE messages ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN decryptFailed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN circleDurationMs INTEGER")
                db.execSQL("ALTER TABLE chats ADD COLUMN lastMessageEncrypted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users_cache ADD COLUMN e2eeFingerprint TEXT")
                db.execSQL("ALTER TABLE users_cache ADD COLUMN cachedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE file_cache ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")

                // --- индексы (главный выигрыш по скорости) ---
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_senderId_receiverId_timestamp ON messages (senderId, receiverId, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_receiverId_senderId_timestamp ON messages (receiverId, senderId, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_receiverId_isRead ON messages (receiverId, isRead)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_id_timestamp ON messages (id, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_isPinned ON messages (isPinned)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_pinned_timestamp ON chats (pinned, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_timestamp ON chats (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_users_cache_username ON users_cache (username)")

                // --- новые таблицы ---
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sticker_packs_cache (
                        packId INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        title TEXT NOT NULL,
                        thumbUrl TEXT,
                        stickers TEXT NOT NULL,
                        isInstalled INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recent_items (
                        kind TEXT NOT NULL,
                        value TEXT NOT NULL,
                        usedAt INTEGER NOT NULL DEFAULT 0,
                        useCount INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(kind, value)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS media_cache (
                        key TEXT NOT NULL PRIMARY KEY,
                        remoteUrl TEXT NOT NULL,
                        localPath TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        mimeType TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        lastAccess INTEGER NOT NULL DEFAULT 0,
                        pinned INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_cache_lastAccess ON media_cache (lastAccess)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_media_cache_remoteUrl ON media_cache (remoteUrl)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS outbox (
                        localId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        opType TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        attempts INTEGER NOT NULL DEFAULT 0,
                        lastError TEXT,
                        localMessageId INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_createdAt ON outbox (createdAt)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cache_meta (
                        key TEXT NOT NULL PRIMARY KEY,
                        longValue INTEGER NOT NULL DEFAULT 0,
                        stringValue TEXT
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS e2ee_sessions (
                        peerId INTEGER NOT NULL PRIMARY KEY,
                        peerPublicKey TEXT NOT NULL,
                        rootKeyWrapped TEXT NOT NULL,
                        peerFingerprint TEXT NOT NULL,
                        establishedAt INTEGER NOT NULL DEFAULT 0,
                        verified INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Переносим недавние из SharedPreferences нельзя из миграции —
                // это делает RecentsStore.migrateFromPrefs() при первом запуске.
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vibe_database"
                )
                    .addMigrations(
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                        MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                        MIGRATION_21_22, MIGRATION_22_23
                    )
                    // ОСТОРОЖНО: это тихо сносит таблицы, если для перехода версий не
                    // найдена миграция. Пользователь при этом теряет локальный кэш
                    // сообщений, и первый вход в каждый чат снова идёт через сеть —
                    // ровно тот тормоз, который мы лечим. Держите миграции полными.
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    // WAL + асинхронный fsync: запись сообщений перестаёт блокировать чтения.
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .setQueryExecutor(queryExecutor)
                    .setTransactionExecutor(queryExecutor)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /* ---- существующие миграции: вставьте сюда без изменений ---- */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN replyToId INTEGER")
                db.execSQL("ALTER TABLE messages ADD COLUMN replyToContent TEXT")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE messages ADD COLUMN replyToSenderName TEXT") }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE messages ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0") }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE chats ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0") }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN forwardedFromId INTEGER")
                db.execSQL("ALTER TABLE messages ADD COLUMN forwardedFromName TEXT")
            }
        }
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE chats ADD COLUMN draft TEXT") }
        }
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE messages ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0") }
        }
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE users_cache ADD COLUMN lastSeenStatus TEXT") }
        }
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chats ADD COLUMN canMessage INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE users_cache ADD COLUMN canMessage INTEGER NOT NULL DEFAULT 1")
            }
        }
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN attachments TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN uploadStatus TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN uploadProgress INTEGER")
            }
        }
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE chats ADD COLUMN lastMessageAttachments TEXT") }
        }
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chats ADD COLUMN isBanned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chats ADD COLUMN isFreezed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users_cache ADD COLUMN isBanned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users_cache ADD COLUMN isFreezed INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE chats ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0") }
        }
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `file_cache` (`hash` TEXT NOT NULL, `url` TEXT NOT NULL, PRIMARY KEY(`hash`))")
            }
        }
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE messages ADD COLUMN reactions TEXT") }
        }
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users_cache ADD COLUMN isBlockedByMe INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users_cache ADD COLUMN isBlockedByUser INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chats ADD COLUMN isBlockedByMe INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chats ADD COLUMN isBlockedByUser INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE messages ADD COLUMN replyMarkup TEXT") }
        }
    }
}