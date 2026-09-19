package com.flasskdev.vibe.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.utils.VideoCoverGenerator
import java.io.File

/**
 * Единая обложка для всех видеоповерхностей.
 *
 * Кандидаты пробуются по очереди, при ошибке загрузки переходим к следующему:
 *  1. [coverUrl] — обложка, явно пришедшая с сервера;
 *  2. локально сгенерированный и закэшированный JPEG (мгновенно, без сети);
 *  3. предполагаемый серверный `<видео>.cover.jpg`;
 *  4. кадр из самого видео силами Coil (требует VideoFrameDecoder в ImageLoader).
 *
 * Параллельно, если локальной обложки ещё нет, она генерируется в фоне
 * (MediaMetadataRetriever умеет и в HTTP), поэтому синий плейсхолдер живёт
 * максимум пару секунд и только при первом показе.
 */
@Composable
fun VideoCover(
    source: Any,
    modifier: Modifier = Modifier,
    coverUrl: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    showPlayIcon: Boolean = true,
    playIconSize: Dp = 38.dp,
    frameMillis: Long = 1_000L
) {
    val context = LocalContext.current
    val strings = LocalVibeStrings.current
    val key = remember(source) { VideoCoverGenerator.stableKey(source) }

    // PERF: VideoCoverGenerator.cached() читает диск, а вызывался он внутри remember,
    // то есть прямо в композиции на UI-потоке для КАЖДОЙ видео-плитки в чате.
    var localCover by remember(key) { mutableStateOf<Any?>(null) }
    var attempt by remember(key) { mutableStateOf(0) }
    var loaded by remember(key) { mutableStateOf(false) }

    val candidates: List<Any> = remember(key, coverUrl, localCover) {
        buildList {
            coverUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            localCover?.let { add(it) }
            source.asServerCoverUrl()?.let { add(it) }
            add(source)
        }
    }

    val model = candidates.getOrNull(attempt)
    val exhausted = model == null
    val needsFrameDecoding = model != null && model !is File && !model.isImageSource()

    // Фоновая генерация обложки — страховка на случай, когда ни серверная
    // обложка, ни Coil-декодер не сработали.
    LaunchedEffect(key) {
        // Сначала дешёвая проверка кэша на диске, потом (и только при промахе) генерация.
        val cached = withContext(Dispatchers.IO) { VideoCoverGenerator.cached(context, key) }
        if (cached != null) {
            localCover = cached
            attempt = 0
            loaded = false
            return@LaunchedEffect
        }
        VideoCoverGenerator.createAsync(context, source)?.let { generated ->
            localCover = generated
            attempt = 0
            loaded = false
        }
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFF1F2A44), Color(0xFF38598A)))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .apply { if (needsFrameDecoding) videoFrameMillis(frameMillis) }
                    .crossfade(180)
                    .build(),
                contentDescription = strings.videoCoverCd,
                contentScale = contentScale,
                onSuccess = { loaded = true },
                onError = {
                    // Пробуем следующий источник, а не сдаёмся сразу.
                    if (attempt < candidates.lastIndex) attempt++ else attempt = candidates.size
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        when {
            // Все источники исчерпаны — честный заглушечный значок.
            exhausted -> Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = strings.videoNoFrameCd,
                tint = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.size(36.dp)
            )
            // Обложка ещё грузится/генерируется — мягкий пульс вместо мёртвого синего фона.
            !loaded -> ShimmerVeil()
        }

        if (showPlayIcon) {
            Box(
                modifier = Modifier
                    .size(playIconSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = strings.videoPlayCd,
                    tint = Color.White,
                    modifier = Modifier.size(playIconSize * 0.63f)
                )
            }
        }
    }
}

@Composable
private fun ShimmerVeil() {
    val transition = rememberInfiniteTransition(label = "cover-shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "cover-shimmer-alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = alpha))
    )
}

private fun Any.isImageSource(): Boolean {
    val value = when (this) {
        is File -> name
        else -> toString()
    }.substringBefore('?').lowercase()
    return value.endsWith(".jpg") || value.endsWith(".jpeg") ||
            value.endsWith(".png") || value.endsWith(".webp")
}

private fun Any.asServerCoverUrl(): String? {
    val videoUrl = this as? String ?: return null
    if (!videoUrl.startsWith("http")) return null
    val withoutQuery = videoUrl.substringBefore('?')
    val dot = withoutQuery.lastIndexOf('.')
    if (dot <= withoutQuery.lastIndexOf('/')) return null
    return withoutQuery.substring(0, dot) + ".cover.jpg"
}