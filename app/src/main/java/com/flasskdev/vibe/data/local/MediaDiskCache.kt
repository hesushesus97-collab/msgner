package com.flasskdev.vibe.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * LRU-кэш медиа на диске.
 *
 * Что было не так раньше:
 *  - каждое открытие чата заново качало фото/видео/голосовые из сети;
 *  - Coil кэшировал только картинки, видео и аудио — нет;
 *  - скачанное складывалось в cacheDir без учёта размера, приложение
 *    раздувалось на гигабайты, а Android потом молча чистил cacheDir целиком.
 *
 * Теперь: файлы лежат в filesDir/media, реестр в Room, вытеснение по LRU
 * до maxBytes, «закреплённые» файлы (свой аватар, кружочки в открытом чате)
 * не вытесняются.
 */
class MediaDiskCache(
    context: Context,
    private val dao: CacheDao,
    private val client: OkHttpClient,
    private val maxBytes: Long = DEFAULT_MAX_BYTES
) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "media").apply { mkdirs() }
    private val evictMutex = Mutex()
    /** Не даём двум корутинам качать один и тот же URL параллельно. */
    private val inFlight = HashMap<String, Mutex>()
    private val inFlightGuard = Mutex()

    companion object {
        const val DEFAULT_MAX_BYTES = 512L * 1024 * 1024 // 512 MB

        fun keyOf(url: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(url.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }

    /** Локальный файл, если он уже есть. Не ходит в сеть. Дёшево — можно звать из UI. */
    suspend fun peek(url: String): File? = withContext(Dispatchers.IO) {
        val row = dao.findMedia(url) ?: return@withContext null
        val file = File(row.localPath)
        if (!file.exists()) {
            dao.deleteMedia(listOf(row.key))
            return@withContext null
        }
        dao.touchMedia(row.key)
        file
    }

    /**
     * Возвращает локальный файл, скачивая его при необходимости.
     * @param onProgress 0..100, вызывается не чаще раза в 64 КБ.
     */
    suspend fun get(
        url: String,
        pinned: Boolean = false,
        onProgress: ((Int) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        peek(url)?.let { return@withContext it }

        val key = keyOf(url)
        val lock = inFlightGuard.withLock { inFlight.getOrPut(key) { Mutex() } }

        lock.withLock {
            // Пока ждали лок, файл мог скачать кто-то другой.
            peek(url)?.let { return@withLock it }

            val ext = url.substringAfterLast('.', "").substringBefore('?').take(5)
            val target = File(root, if (ext.isBlank()) key else "$key.$ext")
            val tmp = File(root, "$key.part")

            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withLock null
                    val body = response.body ?: return@withLock null
                    val total = body.contentLength()
                    var read = 0L
                    var lastReported = -1

                    body.byteStream().use { input ->
                        tmp.outputStream().buffered(64 * 1024).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buffer)
                                if (n <= 0) break
                                output.write(buffer, 0, n)
                                read += n
                                if (onProgress != null && total > 0) {
                                    val pct = ((read * 100) / total).toInt()
                                    if (pct != lastReported) {
                                        lastReported = pct
                                        onProgress(pct)
                                    }
                                }
                            }
                        }
                    }

                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }

                    dao.insertMedia(
                        MediaCacheEntity(
                            key = key,
                            remoteUrl = url,
                            localPath = target.absolutePath,
                            sizeBytes = target.length(),
                            mimeType = response.header("Content-Type"),
                            pinned = pinned
                        )
                    )
                    evictIfNeeded()
                    target
                }
            } catch (t: Throwable) {
                tmp.delete()
                null
            } finally {
                inFlightGuard.withLock { inFlight.remove(key) }
            }
        }
    }

    /** Кладём уже готовый локальный файл (например, только что записанный кружочек). */
    suspend fun put(url: String, source: File, pinned: Boolean = false): File =
        withContext(Dispatchers.IO) {
            val key = keyOf(url)
            val target = File(root, "$key.${source.extension}")
            if (source.absolutePath != target.absolutePath) source.copyTo(target, overwrite = true)
            dao.insertMedia(
                MediaCacheEntity(
                    key = key,
                    remoteUrl = url,
                    localPath = target.absolutePath,
                    sizeBytes = target.length(),
                    mimeType = null,
                    pinned = pinned
                )
            )
            evictIfNeeded()
            target
        }

    suspend fun currentSizeBytes(): Long = withContext(Dispatchers.IO) { dao.totalMediaBytes() }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dao.allMedia().forEach { runCatching { File(it.localPath).delete() } }
        dao.clearMedia()
        root.listFiles()?.forEach { it.delete() }
    }

    /** Сносим «сирот»: файлы на диске без записи в Room и записи без файлов. */
    suspend fun verify() = withContext(Dispatchers.IO) {
        val rows = dao.allMedia()
        val known = rows.mapTo(HashSet()) { it.localPath }
        rows.filter { !File(it.localPath).exists() }
            .map { it.key }
            .chunked(400)
            .forEach { dao.deleteMedia(it) }
        root.listFiles()?.forEach { f ->
            if (f.absolutePath !in known && !f.name.endsWith(".part")) f.delete()
        }
    }

    private suspend fun evictIfNeeded() = evictMutex.withLock {
        var total = dao.totalMediaBytes()
        if (total <= maxBytes) return@withLock
        val target = (maxBytes * 0.85).toLong()
        while (total > target) {
            val victims = dao.oldestMedia(50)
            if (victims.isEmpty()) break
            victims.forEach { runCatching { File(it.localPath).delete() } }
            dao.deleteMedia(victims.map { it.key })
            total -= victims.sumOf { it.sizeBytes }
        }
    }
}