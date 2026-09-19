package com.flasskdev.vibe.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

data class ProcessedImage(
    val originalUri: Uri,
    val cachedFilePath: String,
    val hash: String,
    val size: Long
)

object ImageProcessor {
    private const val MAX_DIMENSION = 1080

    suspend fun processAndCacheImages(context: Context, uris: List<Uri>): List<ProcessedImage> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ProcessedImage>()
        
        for (uri in uris) {
            try {
                // Read original bitmap
                val inputStream = context.contentResolver.openInputStream(uri) ?: continue
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                
                var inSampleSize = 1
                if (options.outHeight > MAX_DIMENSION || options.outWidth > MAX_DIMENSION) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while (halfHeight / inSampleSize >= MAX_DIMENSION && halfWidth / inSampleSize >= MAX_DIMENSION) {
                        inSampleSize *= 2
                    }
                }
                
                val decodeOptions = BitmapFactory.Options()
                decodeOptions.inSampleSize = inSampleSize
                val stream = context.contentResolver.openInputStream(uri) ?: continue
                val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
                stream.close()
                
                if (bitmap != null) {
                    // Create cached file
                    val cacheDir = File(context.cacheDir, "vibe_images")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    
                    val cachedFile = File(cacheDir, "img_${UUID.randomUUID()}.jpg")
                    val outStream = FileOutputStream(cachedFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
                    outStream.flush()
                    outStream.close()
                    bitmap.recycle()
                    
                    // Calculate SHA-256 content hash
                    val hash = calculateSha256(cachedFile)
                    val size = cachedFile.length()
                    
                    result.add(ProcessedImage(uri, cachedFile.absolutePath, hash, size))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        result
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use {
            val buffer = ByteArray(8192)
            var read: Int
            while (it.read(buffer).also { r -> read = r } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val bytes = digest.digest()
        val sb = java.lang.StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}