package com.flasskdev.vibe.utils

import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.theme.VibeStringsHolder

object MessageUtils {
    fun formatMessagePreview(
        content: String,
        attachments: List<String>?,
        strings: VibeStrings = VibeStringsHolder.current
    ): String {
        val hasAttachments = !attachments.isNullOrEmpty()

        // System messages
        if (content.startsWith("\$\$SYSTEM\$\$PINNED_MESSAGE|")) {
            val parts = content.substringAfter("\$\$SYSTEM\$\$PINNED_MESSAGE|").split("|")
            val senderN = parts.getOrNull(0) ?: "Someone"
            val msgContent = parts.getOrNull(1) ?: ""
            return strings.pinnedMessageSystemText(senderN, msgContent)
        }

        // Video messages (кружочки)
        if (content.startsWith("video_message:")) {
            val ms = content.substringAfter("video_message:").toLongOrNull() ?: 0L
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "📹 ${strings.previewVideoMessage(String.format("%d:%02d", minutes, seconds))}"
        }

        // Stickers
        if (content.startsWith("sticker:")) {
            return "🏷️ ${strings.typeSticker}"
        }

        // GIFs
        if (content.startsWith("gif:")) {
            return "🎞️ ${strings.typeGif}"
        }

        // Voice messages
        if (content.startsWith("duration:")) {
            val ms = content.substringAfter("duration:").toLongOrNull() ?: 0L
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "🎤 ${strings.previewVoiceMessage(String.format("%d:%02d", minutes, seconds))}"
        }

        if (hasAttachments) {
            val images = attachments!!.filter { AttachmentUtils.isImage(it) }
            val videos = attachments.filter { AttachmentUtils.isPlayableVideo(it) }
            val audios = attachments.filter { AttachmentUtils.isPlayableAudio(it) }
            val files = attachments.filter {
                !AttachmentUtils.isImage(it) && !AttachmentUtils.isPlayableVideo(it) && !AttachmentUtils.isPlayableAudio(it)
            }

            val hasCaption = content.isNotBlank()
            val count = attachments.size

            // Single attachment
            if (count == 1) {
                val att = attachments[0]
                return when {
                    AttachmentUtils.isImage(att) -> if (hasCaption) "🖼 $content" else "🖼 ${strings.typePhoto}"
                    AttachmentUtils.isPlayableVideo(att) -> if (hasCaption) "🎬 $content" else "🎬 ${strings.typeVideo}"
                    AttachmentUtils.isPlayableAudio(att) -> {
                        val filename = AttachmentUtils.getFilename(att)
                        if (hasCaption) "🎵 $content" else "🎵 $filename"
                    }
                    else -> {
                        val filename = AttachmentUtils.getFilename(att)
                        if (hasCaption) "📎 $content" else "📎 $filename"
                    }
                }
            }

            // Multiple attachments - mixed types
            if (images.isNotEmpty() && videos.isNotEmpty()) {
                val mediaCount = images.size + videos.size
                return if (hasCaption) "+${mediaCount - 1} $content" else "🖼 ${strings.previewMediaCount(mediaCount)}"
            }

            if (images.isNotEmpty() && videos.isEmpty() && audios.isEmpty() && files.isEmpty()) {
                val rem = count - 1
                return if (hasCaption) "+$rem $content" else strings.previewMorePhotos(rem)
            }

            if (videos.isNotEmpty() && images.isEmpty() && audios.isEmpty() && files.isEmpty()) {
                val rem = count - 1
                return if (hasCaption) "+$rem $content" else "🎬 ${strings.previewMoreVideos(count)}"
            }

            if (audios.isNotEmpty() && images.isEmpty() && videos.isEmpty() && files.isEmpty()) {
                return if (hasCaption) "🎵 $content" else "🎵 ${strings.previewMoreAudio(count)}"
            }

            if (files.isNotEmpty() && images.isEmpty() && videos.isEmpty() && audios.isEmpty()) {
                return if (hasCaption) "📎 $content" else "📎 ${strings.previewMoreFiles(count)}"
            }

            // Mixed types
            val rem = count - 1
            return if (hasCaption) "+$rem $content" else "+$rem ${strings.previewMoreAttachments(rem)}"
        }

        return content
    }
}