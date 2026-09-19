package com.flasskdev.vibe.utils

import android.content.Context
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.flasskdev.vibe.R
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ПРОГРЕВ ПРОЦЕССА.
 *
 * ПОЧЕМУ ЭТО НУЖНО
 * ----------------
 * Симптом «первый вход в переписку тормозит 6 секунд, второй вход быстрый, после
 * перезапуска приложения снова тормозит» — это всегда ОДНОРАЗОВАЯ инициализация
 * на процесс. Второй вход быстрый не потому, что UI стал лучше, а потому что всё
 * тяжёлое уже лежит в памяти процесса. Перезапуск приложения = новый процесс =
 * все кэши пустые.
 *
 * Что именно инициализировалось ЛЕНИВО, ровно в момент открытия чата:
 *   1) Room: первое открытие файла БД, валидация схемы, компиляция SQL-запросов;
 *   2) шрифты Inter (4 файла) — парсятся при первой раскладке текста, на main;
 *   3) assets стикеров (StickerRepository.loadLocalPacks — рекурсивный обход assets);
 *   4) EmojiData (инициализация object с ~1000 строк);
 *   5) AudioMetadataHelper — его дисковый кэш вообще не работал, потому что
 *      init(context) никто не вызывал (prefs == null => putString/getString в никуда).
 *      Из-за этого метаданные каждого аудио тянулись из сети MediaMetadataRetriever'ом
 *      ЗАНОВО в каждом новом процессе;
 *   6) дисковый кэш Coil (256 МБ) — открывается и чистится при первом запросе картинки.
 *
 * Здесь всё это делается один раз, в фоне, сразу после старта приложения, пока
 * пользователь смотрит на список чатов. К моменту входа в переписку main thread
 * уже свободен, и клавиатура выезжает нормально.
 */
object AppWarmup {

    private const val TAG = "VibeWarmup"

    @Volatile
    private var started = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Идемпотентно. Безопасно звать из Activity.onCreate. */
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext

        scope.launch {
            val t0 = System.currentTimeMillis()

            // 1. Дисковый кэш метаданных аудио. БЕЗ ЭТОГО ВЫЗОВА он не работал совсем.
            runCatching { AudioMetadataHelper.init(app) }
                .onFailure { Log.e(TAG, "audio meta init", it) }

            // 2. Room: открываем файл БД и прогреваем реальные запросы чата,
            //    чтобы SQLite скомпилировал statements и поднял страницы в кэш.
            runCatching {
                val db = AppDatabase.getDatabase(app)
                db.openHelper.readableDatabase
                val dao = db.chatDao()
                val myId = UserPreferences(app).userId
                if (myId > 0) {
                    // Последние чаты по времени — почти наверняка пользователь
                    // откроет один из них.
                    dao.getAllChats()
                        .sortedByDescending { it.timestamp }
                        .take(3)
                        .forEach { chat ->
                            dao.getMessagesByPartnerOnce(myId, chat.interlocutorId, 50)
                        }
                }
            }.onFailure { Log.e(TAG, "room warmup", it) }

            // 3. Шрифты. Парсинг ttf при первой раскладке текста идёт на main thread.
            runCatching {
                intArrayOf(
                    R.font.inter_regular,
                    R.font.inter_medium,
                    R.font.inter_semibold,
                    R.font.inter_bold
                ).forEach { ResourcesCompat.getFont(app, it) }
                com.flasskdev.vibe.ui.theme.AppleFontProvider.warmup(app)
            }.onFailure { Log.e(TAG, "font warmup", it) }

            // 4. assets стикеров + таблица эмодзи (оба кэшируются на процесс).
            runCatching {
                com.flasskdev.vibe.ui.components.StickerRepository.loadLocalPacks(app)
                com.flasskdev.vibe.ui.components.EmojiData.categories.size
            }.onFailure { Log.e(TAG, "sticker/emoji warmup", it) }

            // 5. Дисковый кэш Coil: открытие и housekeeping вместо первого показа картинки.
            runCatching { coil.Coil.imageLoader(app).diskCache?.size }
                .onFailure { Log.e(TAG, "coil warmup", it) }

            Log.d(TAG, "warmup done in ${System.currentTimeMillis() - t0} ms")
        }
    }
}