package com.flasskdev.vibe.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.flasskdev.vibe.ui.theme.VibeStringsHolder
import java.io.File

object DownloadHelper {
    fun downloadFile(context: Context, url: String, fileName: String, onToast: ((String) -> Unit)? = null) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
            
            request.setTitle(fileName)
            request.setDescription(VibeStringsHolder.current.downloadFileDescription)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            
            // Setting the destination path
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "VibeMessenger/$fileName"
            )

            // Make sure the directory exists
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VibeMessenger")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            downloadManager.enqueue(request)
            onToast?.invoke(VibeStringsHolder.current.downloadStartedToast)
        } catch (e: Exception) {
            e.printStackTrace()
            onToast?.invoke(VibeStringsHolder.current.downloadErrorToast(e.message.orEmpty()))
        }
    }

    /**
     * Downloads multiple files at once.
     */
    fun downloadFiles(context: Context, files: List<Pair<String, String>>, onToast: ((String) -> Unit)? = null) {
        var count = 0
        files.forEach { (url, filename) ->
            try {
                downloadFile(context, url, filename, onToast = null)
                count++
            } catch (_: Exception) {}
        }
        onToast?.invoke(VibeStringsHolder.current.downloadMultipleToast(count))
    }

    /**
     * Resolves a hash/filename to a full download URL.
     */
    fun resolveUrl(hash: String): String {
        return if (hash.startsWith("http") || hash.startsWith("/") || hash.startsWith("content://")) hash
        else "https://flasskdev.alwaysdata.net/api/upload/file/$hash"
    }
}