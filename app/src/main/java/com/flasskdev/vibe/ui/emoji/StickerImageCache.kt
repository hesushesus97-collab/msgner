package com.flasskdev.vibe.ui.emoji

import android.content.Context
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.flasskdev.vibe.ui.components.StickerRepository
import com.flasskdev.vibe.util.VibeImageLoader
import java.util.Collections

/**
 * ПУНКТ 12: причина, по которой стикеры моргали и панель лагала.
 *
 * Старый код:
 *     ImageRequest.Builder(context).data(...).crossfade(false).build()
 * вызывался ВНУТРИ каждой ячейки. AsyncImage сравнивает модель по equals,
 * а свежесобранный ImageRequest никогда не равен предыдущему, поэтому
 * при каждой рекомпозиции запускалась новая загрузка и сбрасывался кадр.
 *
 * Здесь ImageRequest создаётся один раз на путь и переиспользуется.
 */
object StickerImageCache {

    private val requests = Collections.synchronizedMap(HashMap<String, ImageRequest>(512))

    fun loader(context: Context): ImageLoader =
        VibeImageLoader.get(context, com.flasskdev.vibe.data.local.VibeCacheRepository.get(context).http)

    fun request(context: Context, path: String): ImageRequest =
        requests.getOrPut(path) {
            ImageRequest.Builder(context.applicationContext)
                .data(StickerRepository.resolve(path))
                .crossfade(false)                 // мгновенная подстановка из кэша
                .scale(Scale.FIT)
                // Стикеры маленькие и их много: держим их в памяти агрессивно.
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey(path)
                .diskCacheKey(path)
                // allowHardware отключаем только для анимированных webp:
                // аппаратные битмапы нельзя рисовать в софтверный canvas анимации.
                .allowHardware(!path.endsWith(".webp", ignoreCase = true))
                .build()
        }

    /** Прогрев кэша на несколько экранов вперёд. */
    fun preload(context: Context, path: String) {
        loader(context).enqueue(request(context, path))
    }

    fun clear() = requests.clear()
}