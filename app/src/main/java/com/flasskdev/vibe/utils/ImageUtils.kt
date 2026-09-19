package com.flasskdev.vibe.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtils {
    
    private const val MAX_IMAGE_DIMENSION = 512

    private fun getExifRotation(context: Context, uri: Uri): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun compressAndEncodeImage(
        context: Context, 
        uri: Uri,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        boxWidthPx: Float = 0f,
        boxHeightPx: Float = 0f,
        circleRadiusPx: Float = 0f
    ): String? {
        return try {
            val rotation = getExifRotation(context, uri)
            
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            var originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(
                    originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                )
                if (rotatedBitmap != originalBitmap) {
                    originalBitmap.recycle()
                    originalBitmap = rotatedBitmap
                }
            }

            // If crop parameters are provided, crop the original bitmap first
            val targetBitmap = if (boxWidthPx > 0 && boxHeightPx > 0 && circleRadiusPx > 0) {
                val w = originalBitmap.width
                val h = originalBitmap.height
                
                // Calculate ContentScale.Fit scale factor
                val fitScale = kotlin.math.min(boxWidthPx / w.toFloat(), boxHeightPx / h.toFloat())
                
                // Calculate the crop rectangle in the original image
                val cropLeft = (((-circleRadiusPx - offsetX) / (fitScale * scale)) + w / 2f).toInt()
                val cropTop = (((-circleRadiusPx - offsetY) / (fitScale * scale)) + h / 2f).toInt()
                val cropSize = ((2 * circleRadiusPx) / (fitScale * scale)).toInt()
                
                if (cropSize > 0) {
                    val finalSize = kotlin.math.min(cropSize, MAX_IMAGE_DIMENSION)
                    val squareBitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(squareBitmap)
                    
                    val drawScale = finalSize.toFloat() / cropSize.toFloat()
                    val matrix = Matrix()
                    matrix.postTranslate(-cropLeft.toFloat(), -cropTop.toFloat())
                    matrix.postScale(drawScale, drawScale)
                    
                    canvas.drawBitmap(originalBitmap, matrix, null)
                    squareBitmap
                } else {
                    originalBitmap
                }
            } else {
                originalBitmap
            }
            
            var width = targetBitmap.width
            var height = targetBitmap.height
            
            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                val resizeScale = kotlin.math.max(width.toFloat() / MAX_IMAGE_DIMENSION, height.toFloat() / MAX_IMAGE_DIMENSION)
                width = (width / resizeScale).toInt()
                height = (height / resizeScale).toInt()
            }

            val resizedBitmap = if (targetBitmap.width != width || targetBitmap.height != height) {
                Bitmap.createScaledBitmap(targetBitmap, width, height, true)
            } else {
                targetBitmap
            }
            
            val outputStream = ByteArrayOutputStream()
            // Using WEBP for better compression if available, fallback to JPEG
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                resizedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 70, outputStream)
            } else {
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            }
            
            val byteArray = outputStream.toByteArray()
            
            // Clean up bitmaps to free memory
            if (resizedBitmap != originalBitmap && resizedBitmap != targetBitmap) {
                resizedBitmap.recycle()
            }
            if (targetBitmap != originalBitmap) {
                targetBitmap.recycle()
            }
            originalBitmap.recycle()

            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
