package com.flasskdev.vibe.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.flasskdev.vibe.ui.theme.VibeStringsHolder

/**
 * Types of attachments supported in chat messages.
 */
enum class AttachmentType {
    IMAGE,          // .webp, .jpg, .png, .jpeg, .gif
    VIDEO,          // .mp4, .mov, .avi, .mkv, .webm, .3gp
    VIDEO_MESSAGE,  // video circle ("кружочек"), marked by content prefix "video_message:"
    AUDIO,          // .mp3, .m4a, .flac, .ogg, .wav, .aac, .opus, .wma (music files)
    VOICE,          // voice messages, marked by content prefix "duration:"
    FILE            // everything else
}

object AttachmentUtils {

    private val imageExtensions = setOf("webp", "jpg", "jpeg", "png", "gif")
    private val videoExtensions = setOf("mp4", "mov", "avi", "mkv", "webm", "3gp")
    private val audioExtensions = setOf("mp3", "m4a", "flac", "ogg", "wav", "aac", "opus", "wma")

    /**
     * Determines the type of attachment based on URL extension and message content.
     */
    fun getType(url: String, content: String? = null): AttachmentType {
        // Voice messages: content starts with "duration:" and attachment is audio
        if (content != null && content.startsWith("duration:")) {
            return AttachmentType.VOICE
        }
        // Video messages (кружочки): content starts with "video_message:"
        if (content != null && content.startsWith("video_message:")) {
            return AttachmentType.VIDEO_MESSAGE
        }

        val ext = url.substringAfterLast('.').lowercase().substringBefore('?')

        return when {
            ext in imageExtensions -> AttachmentType.IMAGE
            ext in videoExtensions -> AttachmentType.VIDEO
            ext in audioExtensions -> {
                // If content starts with "duration:", it's a voice message even if extension is audio
                if (content?.startsWith("duration:") == true) AttachmentType.VOICE
                else AttachmentType.AUDIO
            }
            else -> AttachmentType.FILE
        }
    }

    /**
     * Returns the correct server upload endpoint for a given attachment type.
     */
    fun getUploadEndpoint(type: AttachmentType): String {
        return when (type) {
            AttachmentType.IMAGE -> "https://flasskdev.alwaysdata.net/upload_photo.php"
            AttachmentType.VIDEO, AttachmentType.VIDEO_MESSAGE -> "https://flasskdev.alwaysdata.net/upload_video.php"
            AttachmentType.AUDIO, AttachmentType.VOICE -> "https://flasskdev.alwaysdata.net/upload_audio.php"
            AttachmentType.FILE -> "https://flasskdev.alwaysdata.net/upload_file.php"
        }
    }

    /**
     * Returns the upload endpoint based on file path/name extension.
     */
    fun getUploadEndpointForFile(filePath: String): String {
        val ext = filePath.substringAfterLast('.').lowercase()
        return when {
            ext in imageExtensions -> "https://flasskdev.alwaysdata.net/upload_photo.php"
            ext in videoExtensions -> "https://flasskdev.alwaysdata.net/upload_video.php"
            ext in audioExtensions -> "https://flasskdev.alwaysdata.net/upload_audio.php"
            else -> "https://flasskdev.alwaysdata.net/upload_file.php"
        }
    }

    /**
     * Returns an appropriate Material icon for the attachment type.
     */
    fun getTypeIcon(type: AttachmentType): ImageVector {
        return when (type) {
            AttachmentType.IMAGE -> Icons.Default.Image
            AttachmentType.VIDEO -> Icons.Default.Videocam
            AttachmentType.VIDEO_MESSAGE -> Icons.Default.VideoCall
            AttachmentType.AUDIO -> Icons.Default.MusicNote
            AttachmentType.VOICE -> Icons.Default.Mic
            AttachmentType.FILE -> Icons.Default.InsertDriveFile
        }
    }

    /**
     * Checks if the URL points to a playable audio file.
     */
    fun isPlayableAudio(url: String): Boolean {
        val ext = url.substringAfterLast('.').lowercase().substringBefore('?')
        return ext in audioExtensions
    }

    /**
     * Checks if the URL points to a playable video file.
     */
    fun isPlayableVideo(url: String): Boolean {
        val ext = url.substringAfterLast('.').lowercase().substringBefore('?')
        return ext in videoExtensions
    }

    /**
     * Checks if the URL points to an image file.
     */
    fun isImage(url: String): Boolean {
        val ext = url.substringAfterLast('.').lowercase().substringBefore('?')
        return ext in imageExtensions
    }

    /**
     * Returns a MIME type string based on file extension.
     */
    fun getMimeType(filePath: String): String {
        val ext = filePath.substringAfterLast('.').lowercase()
        return when (ext) {
            // Images
            "webp" -> "image/webp"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            // Videos
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            // Audio
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "opus" -> "audio/opus"
            "wma" -> "audio/x-ms-wma"
            // Default
            else -> "application/octet-stream"
        }
    }

    /**
     * Gets the filename from a URL or path.
     */
    fun getFilename(url: String): String {
        return url.substringAfterLast('/').substringBefore('?')
    }

    /**
     * Categorizes a list of attachments by type.
     */
    fun categorize(attachments: List<String>, content: String? = null): Map<AttachmentType, List<String>> {
        return attachments.groupBy { getType(it, content) }
    }

    /**
     * Checks if all attachments in the list are images or videos (media).
     */
    fun allMedia(attachments: List<String>): Boolean {
        return attachments.all {
            val ext = it.substringAfterLast('.').lowercase().substringBefore('?')
            ext in imageExtensions || ext in videoExtensions
        }
    }

    /**
     * PERF: размер файла запрашивался HEAD-ом на КАЖДУЮ композицию баббла с файлом
     * (вход в чат, скролл, поворот). Десяток файловых сообщений = десяток сетевых
     * запросов на входе в переписку. Теперь результат кэшируется на процесс.
     */
    private val fileSizeCache = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 300
        }
    )

    suspend fun getFileSizeAsync(url: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        fileSizeCache[url]?.let { return@withContext it }
        try {
            if (url.startsWith("/") || url.startsWith("content://") || url.contains("cacheDir")) {
                val file = java.io.File(url)
                if (file.exists()) {
                    return@withContext formatFileSize(file.length()).also { fileSizeCache[url] = it }
                }
            } else {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "HEAD"
                val size = connection.contentLengthLong
                connection.disconnect()
                if (size > 0) {
                    return@withContext formatFileSize(size).also { fileSizeCache[url] = it }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return@withContext VibeStringsHolder.current.typeFile
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}