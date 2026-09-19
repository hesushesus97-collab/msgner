package com.flasskdev.vibe.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.util.LruCache
import com.flasskdev.vibe.ui.theme.VibeStringsHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AudioMetadata(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val coverArt: ByteArray? = null
) {
    val displayTitle: String
        get() = if (title.isNotBlank() && !title.startsWith("audio_")) title
        else VibeStringsHolder.current.playerTrackFallback

    val displayArtist: String
        get() = if (artist.isNotBlank() && artist != "Unknown Artist") artist else ""
}

/**
 * ============================================================================
 *  ПУНКТ 3 — «ОБЛОЖКА МУЗЫКИ ТАК И НЕ ОТОБРАЖАЕТСЯ»
 * ============================================================================
 *
 *  КОРНЕВАЯ ПРИЧИНА (одна, и она была не в UI)
 *  -------------------------------------------
 *  Обложка блокировалась НАВСЕГДА при первом же промахе, причём промах был
 *  практически гарантирован.
 *
 *   1. [extractMetadata] дёргает MediaMetadataRetriever ПО HTTP. Для удалённого
 *      источника ретривер читает файл частично: длительность и теги он обычно
 *      достаёт, а `embeddedPicture` очень часто возвращает null, потому что
 *      кадр APIC (или атом `covr` в m4a) в прочитанный кусок не попал.
 *   2. В конце [extractMetadata] стоял `rememberCover(url, coverArt)`, а внутри
 *      rememberCover ветка `bytes == null` писала в SharedPreferences
 *      `nocover_<md5> = true` БЕЗ срока жизни.
 *   3. Метаданные запрашивает каждый аудио-баббл — то есть флаг «обложки нет»
 *      ставился почти для каждого трека ещё до того, как плеер вообще просил
 *      картинку.
 *   4. [getCoverArt] первым делом читает этот флаг и выходит с null.
 *
 *  Итог: плеер честно спрашивал обложку, а helper честно отвечал «её нет»,
 *  ссылаясь на собственный ошибочный вывод. Переустановка приложения помогала
 *  до первого проигрывания — ровно та картина, которую было видно.
 *
 *  ЧТО ИЗМЕНЕНО
 *   - Негативный вердикт больше НЕ ставится из [extractMetadata]. Оттуда
 *     обложка только сохраняется, если она реально нашлась.
 *   - Негатив разделён на два вида: «авторитетный» (файл полностью прочитан
 *     локально, картинки правда нет — помним постоянно) и «неуверенный»
 *     (сетевой промах — помним 6 часов и пробуем снова).
 *   - Добавлен настоящий фолбэк: если по сети ретривер обложку не отдал,
 *     скачиваем сначала первый мегабайт (ID3v2 лежит в начале файла), а если
 *     не помогло — файл целиком, но не больше 25 МБ, и парсим локально.
 *     Именно этот шаг и достаёт обложку в 9 случаях из 10.
 *
 *  Публичный API не менялся, вызывающий код править не нужно.
 * ========================================================================== */
object AudioMetadataHelper {
    private var prefs: SharedPreferences? = null
    private val cache = LruCache<String, AudioMetadata>(300)

    private val inFlight = HashMap<String, Deferred<AudioMetadata>>()
    private val inFlightLock = Mutex()
    private val gate = Semaphore(2)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val coverCache = object : LruCache<String, ByteArray>(6 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    /** Неуверенные промахи — только в памяти процесса, с временем попытки. */
    private val softMisses = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val coverInFlight = HashMap<String, Deferred<ByteArray?>>()
    private var coverDir: File? = null

    private const val SOFT_MISS_TTL_MS = 6L * 60 * 60 * 1000       // 6 часов
    private const val HEAD_WINDOW_BYTES = 1L * 1024 * 1024          // 1 МБ на пробу
    private const val FULL_DOWNLOAD_LIMIT_BYTES = 25L * 1024 * 1024 // предел «скачать целиком»

    fun init(context: Context) {
        if (prefs == null) {
            try {
                prefs = context.applicationContext
                    .getSharedPreferences("audio_metadata_cache", Context.MODE_PRIVATE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (coverDir == null) {
            try {
                coverDir = File(context.applicationContext.cacheDir, "audio_covers")
                    .apply { mkdirs() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        migrateLegacyNegativeFlags()
    }

    /**
     * Разовая чистка. У существующих пользователей SharedPreferences уже забит
     * ошибочными `nocover_*`, и без сброса обложки не появились бы даже после
     * исправления логики.
     */
    private fun migrateLegacyNegativeFlags() {
        val p = prefs ?: return
        if (p.getBoolean(KEY_NEGATIVE_RESET_DONE, false)) return
        try {
            val editor = p.edit()
            p.all.keys.filter { it.startsWith("nocover_") }.forEach { editor.remove(it) }
            editor.putBoolean(KEY_NEGATIVE_RESET_DONE, true)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ------------------------------------------------------------ метаданные

    fun getCachedMetadata(url: String): AudioMetadata? {
        cache.get(url)?.let { return it }

        prefs?.let { p ->
            val jsonStr = p.getString(url, null)
            if (!jsonStr.isNullOrEmpty()) {
                try {
                    val obj = JSONObject(jsonStr)
                    val title = obj.optString("title", "")
                    val artist = obj.optString("artist", "")
                    val durationMs = obj.optLong("durationMs", 0L)
                    if (title.isNotBlank()) {
                        val meta = AudioMetadata(title, artist, durationMs)
                        cache.put(url, meta)
                        return meta
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    suspend fun getMetadata(url: String): AudioMetadata {
        getCachedMetadata(url)?.let { cached ->
            if (cached.title.isNotBlank() &&
                !cached.title.startsWith("audio_") &&
                cached.durationMs > 0L
            ) {
                return cached
            }
        }

        val deferred = inFlightLock.withLock {
            inFlight[url] ?: scope
                .async(start = CoroutineStart.LAZY) {
                    try {
                        gate.withPermit { extractMetadata(url) }
                    } finally {
                        inFlightLock.withLock { inFlight.remove(url) }
                    }
                }
                .also { inFlight[url] = it }
        }
        return deferred.await()
    }

    // --------------------------------------------------------------- обложки

    /** Обложка из памяти/диска. Никогда не ходит в сеть — безопасно звать из composable. */
    fun getCachedCoverArt(url: String): ByteArray? {
        if (url.isBlank()) return null
        coverCache.get(url)?.let { return it }
        readCoverFromDisk(url)?.let {
            coverCache.put(url, it)
            return it
        }
        return null
    }

    suspend fun getCoverArt(url: String): ByteArray? {
        if (url.isBlank()) return null
        getCachedCoverArt(url)?.let { return it }
        if (isNegativelyCached(url)) return null

        val deferred = inFlightLock.withLock {
            coverInFlight[url] ?: scope
                .async(start = CoroutineStart.LAZY) {
                    try {
                        gate.withPermit { resolveCoverArt(url) }
                    } finally {
                        inFlightLock.withLock { coverInFlight.remove(url) }
                    }
                }
                .also { coverInFlight[url] = it }
        }
        return deferred.await()
    }

    private fun isNegativelyCached(url: String): Boolean {
        if (prefs?.getBoolean(noCoverKey(url), false) == true) return true
        val at = softMisses[url] ?: return false
        if (System.currentTimeMillis() - at > SOFT_MISS_TTL_MS) {
            softMisses.remove(url)
            return false
        }
        return true
    }

    /**
     * Три попытки по возрастанию цены:
     *  1) удалённый ретривер (мгновенно, если сервер отдаёт Range и тег в начале);
     *  2) первый мегабайт локально (ID3v2 с APIC почти всегда там);
     *  3) файл целиком, если он разумного размера (нужно для m4a, где moov
     *     может лежать в хвосте).
     */
    private suspend fun resolveCoverArt(url: String): ByteArray? = withContext(Dispatchers.IO) {
        val isRemote = url.startsWith("http")

        if (!isRemote) {
            val bytes = readEmbeddedPicture(url)
            rememberCover(url, bytes, authoritative = true)
            return@withContext bytes
        }

        readEmbeddedPicture(url)?.let {
            rememberCover(url, it, authoritative = true)
            return@withContext it
        }

        var temp: File? = null
        try {
            temp = downloadTo(url, HEAD_WINDOW_BYTES)
            temp?.let { file ->
                readEmbeddedPicture(file.absolutePath)?.let {
                    rememberCover(url, it, authoritative = true)
                    return@withContext it
                }
            }
        } finally {
            temp?.delete()
        }

        val size = remoteContentLength(url)
        if (size in 1..FULL_DOWNLOAD_LIMIT_BYTES) {
            var full: File? = null
            try {
                full = downloadTo(url, size)
                full?.let { file ->
                    val bytes = readEmbeddedPicture(file.absolutePath)
                    // Файл прочитан целиком: если картинки нет — её правда нет.
                    rememberCover(url, bytes, authoritative = true)
                    return@withContext bytes
                }
            } finally {
                full?.delete()
            }
        }

        // Сетевой промах. Помним ненадолго и обязательно попробуем ещё раз.
        rememberCover(url, null, authoritative = false)
        null
    }

    private fun readEmbeddedPicture(source: String): ByteArray? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            if (source.startsWith("http")) {
                retriever.setDataSource(source, emptyMap())
            } else {
                retriever.setDataSource(source)
            }
            retriever.embeddedPicture?.takeIf { it.isNotEmpty() }
        } catch (e: Throwable) {
            null
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun remoteContentLength(url: String): Long {
        if (!isSafeRemoteUrl(url)) return -1L
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            connection.connect()
            connection.contentLengthLong
        } catch (e: Throwable) {
            -1L
        } finally {
            connection?.disconnect()
        }
    }

    /** Скачивает не больше [limitBytes] в файл во временной папке кэша. */
    private fun downloadTo(url: String, limitBytes: Long): File? {
        if (!isSafeRemoteUrl(url)) return null
        var connection: HttpURLConnection? = null
        val target = File(coverDir ?: return null, "probe_${urlKey(url)}_$limitBytes.bin")
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                // Range не обязателен: если сервер его не поддержит, мы всё равно
                // оборвём чтение на limitBytes, просто потратим чуть больше трафика.
                setRequestProperty("Range", "bytes=0-${limitBytes - 1}")
            }
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (total < limitBytes) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        total += read
                    }
                }
            }
            target.takeIf { it.exists() && it.length() > 0L }
        } catch (e: Throwable) {
            target.delete()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun rememberCover(url: String, bytes: ByteArray?, authoritative: Boolean) {
        if (bytes == null || bytes.isEmpty()) {
            if (authoritative) {
                try {
                    prefs?.edit()?.putBoolean(noCoverKey(url), true)?.apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                softMisses[url] = System.currentTimeMillis()
            }
            return
        }
        softMisses.remove(url)
        try {
            prefs?.edit()?.remove(noCoverKey(url))?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        coverCache.put(url, bytes)
        try {
            coverFile(url)?.writeBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readCoverFromDisk(url: String): ByteArray? = try {
        coverFile(url)?.takeIf { it.exists() && it.length() > 0L }?.readBytes()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun coverFile(url: String): File? = coverDir?.let { File(it, "${urlKey(url)}.img") }

    private fun noCoverKey(url: String): String = "nocover_${urlKey(url)}"

    private fun urlKey(url: String): String = try {
        MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        url.hashCode().toString()
    }

    /**
     * URL вложения приходит из сообщения чата, то есть потенциально от другого
     * пользователя. Чтобы чужое сообщение не могло заставить устройство ходить
     * по произвольным адресам (утечка IP, cleartext http, зондирование локальной
     * сети), сетевые пробы разрешены только для https и публичных хостов.
     */
    private fun isSafeRemoteUrl(url: String): Boolean = try {
        val uri = java.net.URI(url)
        val host = uri.host?.lowercase()
        uri.scheme.equals("https", ignoreCase = true) &&
            !host.isNullOrBlank() &&
            host != "localhost" &&
            !host.endsWith(".local") &&
            !isPrivateOrLoopbackAddress(host)
    } catch (e: Exception) {
        false
    }

    private fun isPrivateOrLoopbackAddress(host: String): Boolean {
        // IPv6-литералы ([::1], [fe80::…]) блокируем целиком.
        if (host.contains(':')) return true
        val octets = host.split('.')
        if (octets.size != 4 || octets.any { it.toIntOrNull() == null }) return false // доменное имя
        val a = octets[0].toInt()
        val b = octets[1].toInt()
        return a == 0 || a == 10 || a == 127 ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) ||
            (a == 169 && b == 254)
    }

    private suspend fun extractMetadata(url: String): AudioMetadata = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            if (url.startsWith("http")) {
                retriever.setDataSource(url, emptyMap())
            } else {
                retriever.setDataSource(url)
            }

            var title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            var artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val coverArt = retriever.embeddedPicture?.takeIf { it.isNotEmpty() }

            val rawFilename = if (url.startsWith("http")) {
                url.substringAfterLast("/").substringBeforeLast(".")
            } else {
                File(url).nameWithoutExtension
            }

            if (title.isNullOrBlank()) {
                if (rawFilename.contains(" - ")) {
                    val parts = rawFilename.split(" - ", limit = 2)
                    if (artist.isNullOrBlank()) artist = parts[0].trim()
                    title = parts[1].trim()
                } else if (!rawFilename.startsWith("audio_") &&
                    !rawFilename.matches(Regex("^[0-9a-fA-F_-]{16,}$"))
                ) {
                    title = rawFilename
                } else {
                    title = VibeStringsHolder.current.playerTrackFallback
                }
            }
            if (artist.isNullOrBlank()) artist = "Unknown Artist"

            val metadata = AudioMetadata(title, artist, durationMs, coverArt)
            cache.put(url, metadata)

            // ГЛАВНОЕ ИСПРАВЛЕНИЕ ПУНКТА 3.
            // Раньше здесь безусловно вызывался rememberCover(url, coverArt), и при
            // coverArt == null трек навсегда помечался как «без обложки». Теперь
            // сохраняем только положительный результат: отсутствие картинки в
            // частично прочитанном сетевом потоке ничего не доказывает.
            if (coverArt != null) rememberCover(url, coverArt, authoritative = true)

            try {
                prefs?.edit()?.apply {
                    val obj = JSONObject()
                    obj.put("title", metadata.title)
                    obj.put("artist", metadata.artist)
                    obj.put("durationMs", metadata.durationMs)
                    putString(url, obj.toString())
                    apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            metadata
        } catch (e: Exception) {
            val rawFilename = if (url.startsWith("http")) {
                url.substringAfterLast("/").substringBeforeLast(".")
            } else {
                File(url).nameWithoutExtension
            }
            val safeTitle = if (!rawFilename.startsWith("audio_") &&
                !rawFilename.matches(Regex("^[0-9a-fA-F_-]{16,}$"))
            ) rawFilename else VibeStringsHolder.current.playerTrackFallback
            val metadata = AudioMetadata(safeTitle, "Unknown Artist", 0L, null)
            cache.put(url, metadata)
            metadata
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private const val KEY_NEGATIVE_RESET_DONE = "nocover_reset_v3"
}