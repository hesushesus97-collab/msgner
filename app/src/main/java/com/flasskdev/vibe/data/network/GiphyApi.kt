package com.flasskdev.vibe.data.network

import android.content.Context
import com.flasskdev.vibe.BuildConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient

import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder

import java.util.concurrent.TimeUnit

/**
 * One GIF result coming from the GIPHY API.
 *
 * @param previewUrl a lightweight, fixed-width animated preview for the picker grid
 * @param fullUrl    the URL that actually gets sent in the message
 * @param width      original width in pixels (0 if unknown)
 * @param height     original height in pixels (0 if unknown)
 */
data class GifItem(
    val id: String,
    val previewUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int
)

/**
 * Minimal GIPHY client built on the OkHttp instance already available in the project.
 *
 * The API key is read from BuildConfig.GIPHY_API_KEY (configured in build.gradle.kts +
 * local.properties). No extra dependency is required.
 */
object GiphyApi {

    private const val BASE = "https://api.giphy.com/v1/gifs"

    @Volatile
    private var client: OkHttpClient = createClient()
    private var isHttpCacheInitialized = false

    /**
     * Initializes the shared HTTP response cache once at application startup.
     * GifCache still owns parsed-result TTL caching; this cache avoids repeat
     * downloads when that higher-level cache is cold or has expired.
     */
    fun init(context: Context) {
        synchronized(this) {
            if (isHttpCacheInitialized) return
            client = createClient(
                cache = Cache(File(context.applicationContext.cacheDir, "giphy_http"), HTTP_CACHE_SIZE_BYTES)
            )
            isHttpCacheInitialized = true
        }
    }

    private fun createClient(cache: Cache? = null): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .apply {
                if (cache != null) {
                    cache(cache)
                    addNetworkInterceptor { chain ->
                        chain.proceed(chain.request())
                            .newBuilder()
                            // GIPHY marks responses as no-cache; the picker can safely reuse a list for 10 minutes.
                            .header("Cache-Control", "public, max-age=600")
                            .removeHeader("Pragma")
                            .build()
                    }
                }
            }
            .build()

    private const val HTTP_CACHE_SIZE_BYTES = 8L * 1024 * 1024

    private val apiKey: String get() = BuildConfig.GIPHY_API_KEY

    /** Trending GIFs, used as the default grid when the search box is empty. */
    suspend fun trending(offset: Int = 0, limit: Int = 27): List<GifItem> =
        request("$BASE/trending?api_key=$apiKey&limit=$limit&offset=$offset&rating=pg-13&bundle=messaging_non_clips")

    /** Search GIFs by query. */
    suspend fun search(query: String, offset: Int = 0, limit: Int = 27): List<GifItem> {
        if (query.isBlank()) return trending(offset, limit)
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        return request("$BASE/search?api_key=$apiKey&q=$q&limit=$limit&offset=$offset&rating=pg-13&lang=ru&bundle=messaging_non_clips")
    }

    private suspend fun request(url: String): List<GifItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw java.io.IOException("GIPHY_API_KEY не настроен в сборке приложения")
        try {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw java.io.IOException("GIPHY HTTP ${resp.code}")
                val body = resp.body?.string() ?: return@withContext emptyList()
                parse(body)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        }
    }

    private fun parse(json: String): List<GifItem> {
        val result = mutableListOf<GifItem>()
        val root = JSONObject(json)
        val data = root.optJSONArray("data") ?: return result
        for (i in 0 until data.length()) {
            val obj = data.optJSONObject(i) ?: continue
            val images = obj.optJSONObject("images") ?: continue

            fun rendition(vararg names: String): JSONObject? = names.asSequence()
                .mapNotNull { images.optJSONObject(it) }
                .firstOrNull { it.optString("url").startsWith("https://") }
            val preview = rendition("fixed_width_downsampled", "fixed_width", "preview_gif", "original")
            val fullObj = rendition("downsized_medium", "downsized", "original", "fixed_width") ?: preview
            val previewUrl = preview?.optString("url").orEmpty()
            val fullUrl = fullObj?.optString("url").orEmpty()
            if (previewUrl.isBlank() || fullUrl.isBlank()) continue

            result.add(
                GifItem(
                    id = obj.optString("id"),
                    previewUrl = previewUrl,
                    fullUrl = fullUrl,
                    width = fullObj?.optString("width")?.toIntOrNull() ?: 0,
                    height = fullObj?.optString("height")?.toIntOrNull() ?: 0
                )
            )
        }
        return result
    }
}