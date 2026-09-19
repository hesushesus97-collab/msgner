package com.flasskdev.vibe.utils

import android.content.Context
import java.io.File

/**
 * Исходящие вложения нельзя держать в cacheDir: система чистит его в любой момент,
 * а CircleRecorder сам удаляет файлы старше часа. Если загрузка не прошла (нет сети,
 * приложение убили), файл исчезает и сообщение навсегда остаётся на "0%".
 */
object OutboxFiles {

    fun dir(context: Context): File = File(context.filesDir, "outbox").apply { mkdirs() }

    /** Переносит локальный файл в постоянную папку и возвращает новый путь. */
    fun persist(context: Context, path: String): String {
        if (!path.startsWith("/")) return path
        val source = File(path)
        if (!source.exists() || source.length() == 0L) return path
        val outbox = dir(context)
        if (source.parentFile?.absolutePath == outbox.absolutePath) return path
        val target = File(outbox, "${System.nanoTime()}_${source.name}")
        return runCatching {
            source.copyTo(target, overwrite = true)
            source.delete()
            target.absolutePath
        }.getOrDefault(path)
    }

    fun deleteIfOurs(context: Context, path: String) {
        if (!path.startsWith("/")) return
        val file = File(path)
        if (file.parentFile?.absolutePath == dir(context).absolutePath) runCatching { file.delete() }
    }

    /** Страховка от мусора: недоотправленное старше недели удаляем. */
    fun prune(context: Context, ageMs: Long = 7L * 24 * 60 * 60 * 1000) {
        runCatching {
            dir(context).listFiles()?.forEach {
                if (System.currentTimeMillis() - it.lastModified() > ageMs) it.delete()
            }
        }
    }
}
