package com.flasskdev.vibe.util

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import okhttp3.OkHttpClient

/**
 * Единый ImageLoader на всё приложение.
 *
 * Было: ImageLoader.Builder(this) в MainActivity без disk-кэша, плюс
 * rememberAnimatedImageLoader() в EmojiStickerGifPanel создавал ВТОРОЙ лоадер
 * со своим memory-кэшем. Два кэша делили одну и ту же память вдвое, а
 * стикеры/гифки перекачивались после каждого закрытия панели.
 */
object VibeImageLoader {

    @Volatile private var instance: ImageLoader? = null

    fun get(context: Context, okHttp: OkHttpClient): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext, okHttp).also { instance = it }
        }

    private fun build(context: Context, okHttp: OkHttpClient): ImageLoader =
        ImageLoader.Builder(context)
            .okHttpClient(okHttp)
            .memoryCache {
                MemoryCache.Builder(context)
                    // 25% кучи. По умолчанию Coil берёт 20%, но у нас плотные
                    // сетки стикеров — иначе постоянные промахи при скролле.
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
                add(VideoFrameDecoder.Factory())   // превью видео без своего MediaMetadataRetriever
            }
            // Аппаратные битмапы: меньше нагрузки на GC при скролле лент.
            .allowHardware(true)
            .allowRgb565(false)
            .crossfade(false)                       // включаем точечно там, где нужно
            .respectCacheHeaders(false)             // наш CDN не отдаёт корректные заголовки
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .apply { if (BuildConfigProxy.debug) logger(DebugLogger()) }
            .build()

    /** Чтобы модуль не зависел от сгенерированного BuildConfig. */
    object BuildConfigProxy { var debug: Boolean = false }
}