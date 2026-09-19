package com.flasskdev.vibe.data.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flasskdev.vibe.data.local.AppDatabase
import com.flasskdev.vibe.data.local.ChatDao
import com.flasskdev.vibe.data.local.FileCacheEntity
import com.flasskdev.vibe.utils.AttachmentUtils
import com.flasskdev.vibe.utils.OutboxFiles
import com.flasskdev.vibe.utils.VideoCoverGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class FileUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // 0 невозможен: временные id отрицательные, серверные положительные.
        // Старая сигнатура (-1) конфликтовала с реальным id.
        val messageId = inputData.getInt("messageId", 0)
        if (messageId == 0) return@withContext Result.failure()

        val dao = AppDatabase.getDatabase(applicationContext).chatDao()

        try {
            uploadMessage(dao, messageId)
        } catch (cancel: CancellationException) {
            // Воркера снял WorkManager. Нельзя оставлять сообщение в UPLOADING,
            // иначе в UI оно навсегда зависнет на 0%.
            withContext(NonCancellable) {
                runCatching { dao.updateUploadStatusOnly(messageId, "FAILED") }
            }
            throw cancel
        } catch (t: Throwable) {
            t.printStackTrace()
            runCatching { dao.updateUploadStatusOnly(messageId, "FAILED") }
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private suspend fun uploadMessage(dao: ChatDao, messageId: Int): Result = coroutineScope {
        val message = dao.getMessageById(messageId) ?: return@coroutineScope Result.failure()

        val attachments = message.attachments
        if (attachments.isNullOrEmpty()) {
            // Раньше здесь был Result.success() без смены статуса: сообщение
            // оставалось "UPLOADING 0%" навсегда.
            dao.updateUploadStatusOnly(messageId, "FAILED")
            return@coroutineScope Result.failure()
        }

        // CONFLATED: в БД уходит только последнее значение прогресса,
        // без лавины UPDATE на каждый чанк.
        val progressChannel = Channel<Int>(Channel.CONFLATED)
        val reporter = launch {
            for (percent in progressChannel) dao.updateUploadProgress(messageId, percent)
        }

        val finalAttachmentIds = mutableListOf<String>()
        var allSuccess = true

        try {
            for (i in attachments.indices) {
                val filePath = attachments[i]

                if (filePath.startsWith("http") || filePath.startsWith("att_")) {
                    finalAttachmentIds.add(filePath)
                    continue
                }

                val file = File(filePath)
                if (!file.exists() || file.length() == 0L) {
                    allSuccess = false
                    break
                }

                val hash = calculateMD5(file)
                val isVideo = AttachmentUtils.isPlayableVideo(file.name)
                val coverFile = if (isVideo) {
                    runCatching { VideoCoverGenerator.create(applicationContext, file) }.getOrNull()
                } else null

                val cachedUrl = dao.getCachedFileUrl(hash)
                if (cachedUrl != null) {
                    if (coverFile != null) linkCoverToUrl(coverFile, cachedUrl)
                    finalAttachmentIds.add(cachedUrl)
                    continue
                }

                val mimeType = AttachmentUtils.getMimeType(file.name)
                val endpoint = AttachmentUtils.getUploadEndpointForFile(file.name)

                val progressBody = ProgressRequestBody(file, mimeType.toMediaTypeOrNull()) { progress ->
                    val overall = ((i * 100) + progress) / attachments.size
                    progressChannel.trySend(overall)
                }

                val multipartBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name, progressBody)

                if (coverFile != null && coverFile.exists() && coverFile.length() > 0L) {
                    multipartBuilder.addFormDataPart(
                        "cover",
                        "${file.nameWithoutExtension}.cover.jpg",
                        coverFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                }

                val uploadRequest = Request.Builder()
                    .url(endpoint)
                    .post(multipartBuilder.build())
                    .build()

                client.newCall(uploadRequest).execute().use { response ->
                    val body = response.body?.string()
                    val url = if (response.isSuccessful && body != null) {
                        runCatching { JSONObject(body) }.getOrNull()
                            ?.takeIf { it.optString("status") == "SUCCESS" }
                            ?.optString("url")
                            ?.takeIf { it.isNotBlank() }
                    } else null

                    if (url != null) {
                        dao.insertFileCache(FileCacheEntity(hash, url))
                        if (coverFile != null) linkCoverToUrl(coverFile, url)
                        finalAttachmentIds.add(url)
                    } else {
                        allSuccess = false
                    }
                }

                if (!allSuccess) break
            }
        } finally {
            progressChannel.close()
            reporter.join()
        }

        if (allSuccess && finalAttachmentIds.size == attachments.size) {
            dao.updateUploadProgress(messageId, 100)
            dao.updateUploadStatus(messageId, "SUCCESS", finalAttachmentIds)
            // Локальные копии больше не нужны: в attachments лежат URL.
            attachments.forEach { OutboxFiles.deleteIfOurs(applicationContext, it) }
            Result.success()
        } else {
            // Вложения сохраняем: без них повтор невозможен.
            dao.updateUploadStatus(messageId, "FAILED", attachments)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun linkCoverToUrl(coverFile: File, url: String) {
        runCatching {
            val targetKey = VideoCoverGenerator.stableKey(url)
            val dir = File(applicationContext.cacheDir, "video_covers").apply { mkdirs() }
            val target = File(dir, "$targetKey.jpg")
            if (!target.exists() && coverFile.absolutePath != target.absolutePath) {
                coverFile.copyTo(target, overwrite = true)
            }
        }
    }

    private fun calculateMD5(file: File): String {
        val digest = java.security.MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buffer).also { r -> read = r } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}