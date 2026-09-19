package com.flasskdev.vibe.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Создаёт и кэширует JPEG-обложку видео.
 *
 * Порядок: системный декодер миниатюр -> MediaMetadataRetriever (несколько sync-кадров)
 * -> MediaMetadataRetriever без OPTION_CLOSEST_SYNC (для видео без ключевых кадров в начале).
 *
 * Все обложки складываются в <cacheDir>/video_covers/<stableKey>.jpg, поэтому
 * UI может мгновенно найти готовую обложку через [cached] без повторного декодирования.
 */
object VideoCoverGenerator {

    private val thumbnailSize = Size(1_280, 720)
    private const val DIR = "video_covers"

    // ---------------------------------------------------------------- ключи

    /** Стабильный ключ для любого источника: File, Uri, String (путь или URL). */
    fun stableKey(source: Any): String {
        val raw = when (source) {
            is File -> "file:${source.absolutePath}:${source.length()}"
            is Uri -> "uri:$source"
            else -> source.toString().substringBefore('?')
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun coverDir(context: Context): File =
        File(context.cacheDir, DIR).apply { if (!exists()) mkdirs() }

    private fun coverFile(context: Context, key: String) = File(coverDir(context), "$key.jpg")

    /** Уже готовая обложка на диске, либо null. Дешёвая синхронная проверка. */
    fun cached(context: Context, key: String): File? =
        coverFile(context, key).takeIf { it.exists() && it.length() > 0L }

    fun cached(context: Context, source: Any): File? = cached(context, stableKey(source))

    // ------------------------------------------------------------- генерация

    fun create(context: Context, videoFile: File): File? {
        if (!videoFile.exists() || videoFile.length() == 0L) return null
        val key = stableKey(videoFile)
        cached(context, key)?.let { return it }

        val system = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { ThumbnailUtils.createVideoThumbnail(videoFile, thumbnailSize, null) }.getOrNull()
        } else null

        return persist(context, key, system)
            ?: persist(context, key, extractWithRetriever { it.setDataSource(videoFile.absolutePath) })
    }

    fun create(context: Context, videoUri: Uri): File? {
        val key = stableKey(videoUri)
        cached(context, key)?.let { return it }

        val system = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { context.contentResolver.loadThumbnail(videoUri, thumbnailSize, null) }.getOrNull()
        } else null

        return persist(context, key, system)
            ?: persist(context, key, extractWithRetriever { it.setDataSource(context, videoUri) })
    }

    /**
     * Обложка для удалённого видео. MediaMetadataRetriever умеет читать HTTP-источник
     * по диапазонам, полное скачивание файла не требуется.
     */
    fun createFromUrl(context: Context, url: String): File? {
        val key = stableKey(url)
        cached(context, key)?.let { return it }
        return persist(context, key, extractWithRetriever { it.setDataSource(url, HashMap()) })
    }

    /** Единая точка входа для UI: сам разбирается, что за источник. */
    suspend fun createAsync(context: Context, source: Any): File? = withContext(Dispatchers.IO) {
        runCatching {
            when {
                source is File -> create(context, source)
                source is Uri -> create(context, source)
                source is String && source.startsWith("http") -> createFromUrl(context, source)
                source is String && source.startsWith("content://") -> create(context, Uri.parse(source))
                source is String -> create(context, File(source))
                else -> null
            }
        }.getOrNull()
    }

    // ----------------------------------------------------------------- внутр.

    private fun extractWithRetriever(prepare: (MediaMetadataRetriever) -> Unit): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            prepare(retriever)

            // 1) ближайший ключевой кадр на нескольких таймкодах
            val sync = sequenceOf(1_000_000L, 500_000L, 100_000L, 0L)
                .mapNotNull { ts ->
                    runCatching {
                        retriever.getFrameAtTime(ts, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }.getOrNull()
                }
                .firstOrNull()
            if (sync != null) return sync

            // 2) любой ближайший кадр (видео без sync-кадра в начале)
            val closest = runCatching {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST)
            }.getOrNull()
            if (closest != null) return closest

            // 3) масштабированный кадр (быстрее и надёжнее на слабых кодеках)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                runCatching {
                    retriever.getScaledFrameAtTime(
                        0L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        thumbnailSize.width,
                        thumbnailSize.height
                    )
                }.getOrNull()
            } else null
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun persist(context: Context, key: String, source: Bitmap?): File? {
        source ?: return null
        val cover = source.scaleDown(maxSide = 1_280)
        return try {
            val target = coverFile(context, key)
            val tmp = File(target.parentFile, "${key}.jpg.tmp")
            FileOutputStream(tmp).use { out ->
                check(cover.compress(Bitmap.CompressFormat.JPEG, 88, out))
            }
            if (tmp.length() > 0L && tmp.renameTo(target)) target else null
        } catch (_: Throwable) {
            null
        } finally {
            if (cover !== source) cover.recycle()
            source.recycle()
        }
    }

    private fun Bitmap.scaleDown(maxSide: Int): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return this
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}