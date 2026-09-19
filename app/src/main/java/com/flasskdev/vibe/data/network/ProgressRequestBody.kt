package com.flasskdev.vibe.data.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.IOException

class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    private val listener: (progress: Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = file.length()

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val total = file.length().coerceAtLeast(1L)
        val buffer = ByteArray(BUFFER_SIZE)
        var uploaded = 0L
        var lastPercent = -1
        var lastEmitAt = 0L

        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                uploaded += read

                // Колбэк на каждые 8 КБ означал UPDATE в Room на каждые 8 КБ.
                val percent = ((uploaded * 100) / total).toInt().coerceIn(0, 100)
                val now = System.currentTimeMillis()
                if (percent != lastPercent && (now - lastEmitAt >= MIN_INTERVAL_MS || percent == 100)) {
                    lastPercent = percent
                    lastEmitAt = now
                    listener(percent)
                }
            }
        }
        if (lastPercent != 100) listener(100)
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val MIN_INTERVAL_MS = 150L
    }
}