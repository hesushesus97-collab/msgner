package com.flasskdev.vibe.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import okhttp3.OkHttpClient
import java.io.File

/**
 * Общий кэш ExoPlayer. Критично: SimpleCache на один и тот же каталог
 * можно создать ТОЛЬКО ОДИН РАЗ за жизнь процесса, иначе прилетает
 * IllegalStateException("Another SimpleCache instance uses the folder").
 * Именно на этом ловится большинство проектов, где плеер создаётся в двух местах.
 */
@UnstableApi
object VibeMediaCache {

    @Volatile private var cache: SimpleCache? = null
    @Volatile private var client: OkHttpClient? = null

    private const val MAX_BYTES = 384L * 1024 * 1024

    fun get(context: Context): SimpleCache =
        cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(context.applicationContext.cacheDir, "media3"),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { cache = it }
        }

    fun okHttp(context: Context): OkHttpClient =
        client ?: synchronized(this) {
            client ?: OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build()
                .also { client = it }
        }
}