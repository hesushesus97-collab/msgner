package com.flasskdev.vibe.ui.circles

import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.flasskdev.vibe.ui.components.VideoCover
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeAuroraGradient
import com.flasskdev.vibe.ui.theme.VibeAuroraSoft
import com.flasskdev.vibe.ui.theme.VibeRadius
import com.flasskdev.vibe.ui.theme.VibeSpacing
import java.io.File

private const val SERVER_FILE_BASE = "https://flasskdev.alwaysdata.net/api/upload/file/"

/** Один активный ExoPlayer на весь список сообщений. */
object ActiveCircle {
    var activeMessageId by mutableStateOf<Int?>(null)

    fun toggle(messageId: Int) {
        activeMessageId = if (activeMessageId == messageId) null else messageId
    }
}

/**
 * Откуда проигрывается кружок.
 *
 * attachments у исходящего сообщения до конца загрузки содержит путь внутри
 * cacheDir. Кэш вычищается системой и самим CircleRecorder (файлы старше часа),
 * поэтому после перезахода путь может указывать в пустоту. Угадывать из него
 * серверный URL нельзя: раньше так получался запрос вида
 * /api/upload/file//data/user/0/... и ExoPlayer отдавал ERROR_CODE_IO_FILE_NOT_FOUND.
 */
private sealed interface CircleSource {
    data class Remote(val url: String) : CircleSource
    data class Local(val file: File) : CircleSource
    data object Missing : CircleSource
}

private fun resolveCircleSource(raw: String): CircleSource {
    val value = raw.trim()
    return when {
        value.isBlank() -> CircleSource.Missing

        value.startsWith("http://") ||
            value.startsWith("https://") ||
            value.startsWith("content://") -> CircleSource.Remote(value)

        // Абсолютный путь: это ещё не загруженный локальный файл.
        value.startsWith("/") -> File(value).let { file ->
            if (file.exists() && file.length() > 0L) CircleSource.Local(file) else CircleSource.Missing
        }

        // att_xxx или просто имя файла, отданное сервером.
        else -> CircleSource.Remote(SERVER_FILE_BASE + value.trimStart('/'))
    }
}

private fun CircleSource.toUri(): Uri? = when (this) {
    is CircleSource.Remote -> Uri.parse(url)
    is CircleSource.Local -> Uri.fromFile(file)
    CircleSource.Missing -> null
}

private fun CircleSource.coverModel(): Any? = when (this) {
    is CircleSource.Remote -> url
    is CircleSource.Local -> file
    CircleSource.Missing -> null
}

/**
 * Голый TextureView натягивает кадр на границы вью: аспект и unappliedRotationDegrees
 * он не учитывает (этим занимался AspectRatioFrameLayout внутри PlayerView).
 * Матрица ниже повторяет RESIZE_MODE_ZOOM: центрированная обрезка без искажений.
 */
private fun applyCenterCrop(view: TextureView, videoSize: VideoSize?) {
    val vw = view.width.toFloat()
    val vh = view.height.toFloat()
    val matrix = Matrix()
    if (vw <= 0f || vh <= 0f || videoSize == null || videoSize.width <= 0 || videoSize.height <= 0) {
        view.setTransform(matrix)
        return
    }

    val cx = vw / 2f
    val cy = vh / 2f
    val rotation = videoSize.unappliedRotationDegrees

    if (rotation != 0) {
        matrix.postRotate(rotation.toFloat(), cx, cy)
        val src = RectF(0f, 0f, vw, vh)
        val dst = RectF()
        matrix.mapRect(dst, src)
        matrix.postScale(vw / dst.width(), vh / dst.height(), cx, cy)
    }

    val par = if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio else 1f
    var srcW = videoSize.width * par
    var srcH = videoSize.height.toFloat()
    if (rotation == 90 || rotation == 270) {
        val tmp = srcW; srcW = srcH; srcH = tmp
    }

    val videoAspect = srcW / srcH
    val viewAspect = vw / vh
    if (videoAspect > viewAspect) {
        matrix.postScale(videoAspect / viewAspect, 1f, cx, cy)
    } else {
        matrix.postScale(1f, viewAspect / videoAspect, cx, cy)
    }
    view.setTransform(matrix)
}

@OptIn(UnstableApi::class)
@Composable
fun CircleMessageBubble(
    videoUrl: String,
    thumbUrl: String?,
    durationMs: Long,
    isMine: Boolean,
    isActive: Boolean,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 208.dp,
    uploadStatus: String? = null,
    uploadProgress: Int? = null,
    onRetryUpload: () -> Unit = {}
) {
    val strings = LocalVibeStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var retryNonce by remember(videoUrl) { mutableIntStateOf(0) }

    // Пересчитываем источник и при ретрае: к этому моменту загрузка могла
    // завершиться и в attachments уже лежит нормальный URL.
    val source = remember(videoUrl, retryNonce) { resolveCircleSource(videoUrl) }
    val isMissing = source is CircleSource.Missing
    val playbackUri = remember(source) { source.toUri() }

    val isUploading = uploadStatus == "UPLOADING"
    val isFailed = uploadStatus == "FAILED"

    var videoSize by remember(videoUrl) { mutableStateOf<VideoSize?>(null) }
    val latestVideoSize = rememberUpdatedState(videoSize)

    var muted by remember(videoUrl) { mutableStateOf(true) }
    var positionMs by remember(videoUrl) { mutableLongStateOf(0L) }
    var totalMs by remember(videoUrl) { mutableLongStateOf(durationMs.coerceAtLeast(0L)) }
    var playing by remember(videoUrl) { mutableStateOf(false) }
    var buffering by remember(videoUrl) { mutableStateOf(false) }
    var firstFrameRendered by remember(videoUrl) { mutableStateOf(false) }
    var playerError by remember(videoUrl) { mutableStateOf<String?>(null) }

    val player = remember(playbackUri, isActive, retryNonce) {
        if (!isActive || playbackUri == null) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(playbackUri))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                prepare()
                playWhenReady = true
            }
        }
    }

    DisposableEffect(player) {
        val currentPlayer = player
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                videoSize = size
            }

            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
                buffering = false
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playerError = null
                    val duration = currentPlayer?.duration ?: C.TIME_UNSET
                    if (duration != C.TIME_UNSET && duration > 0) totalMs = duration
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playing = false
                buffering = false
                playerError = error.errorCodeName
            }
        }

        currentPlayer?.addListener(listener)
        onDispose {
            currentPlayer?.removeListener(listener)
            currentPlayer?.release()
        }
    }

    LaunchedEffect(player, muted) {
        player?.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(player) {
        val currentPlayer = player ?: return@LaunchedEffect
        while (true) {
            positionMs = currentPlayer.currentPosition.coerceAtLeast(0L)
            playing = currentPlayer.isPlaying
            buffering = currentPlayer.playbackState == Player.STATE_BUFFERING

            val duration = currentPlayer.duration
            if (duration != C.TIME_UNSET && duration > 0) totalMs = duration
            if (currentPlayer.playbackState == Player.STATE_READY) firstFrameRendered = true

            kotlinx.coroutines.delay(90)
        }
    }

    val effectiveTotalMs = totalMs.takeIf { it > 0 } ?: durationMs.coerceAtLeast(0L)
    val progress = if (effectiveTotalMs > 0) {
        (positionMs.toFloat() / effectiveTotalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            /* --- кольцо прогресса ---
             * drawArc раньше рисовался по полному размеру Canvas, а обводка
             * толщиной 3.5dp центрируется по контуру: половина линии уходила
             * за границы и обрезалась сверху/снизу/по бокам. Теперь дуга
             * вписана внутрь с отступом stroke/2, как и фоновая окружность. */
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 3.5.dp.toPx()
                val diameter = this.size.minDimension - stroke
                val topLeft = Offset(
                    x = (this.size.width - diameter) / 2f,
                    y = (this.size.height - diameter) / 2f
                )

                drawCircle(
                    color = Color.Gray.copy(alpha = 0.20f),
                    radius = diameter / 2f,
                    style = Stroke(stroke)
                )

                if (progress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = VibeAuroraGradient,
                            center = this.center
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }

            /* --- содержимое круга --- */
            Box(
                Modifier
                    .size(size - 12.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .pointerInput(isActive, player, isMissing, isUploading, isFailed) {
                        detectTapGestures(
                            onTap = {
                                when {
                                    // Файл не загружен или ошибка: плеер открывать нечем.
                                    isMissing || isUploading || isFailed -> Unit
                                    !isActive -> onActivate()
                                    else -> player?.let { if (it.isPlaying) it.pause() else it.play() }
                                }
                            },
                            onLongPress = { if (!isMissing && !isUploading && !isFailed) muted = !muted }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isFailed || isMissing -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(VibeAuroraSoft)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.CloudOff,
                                    contentDescription = strings.circleSendFailed,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.height(VibeSpacing.xs))
                                Text(
                                    text = strings.circleSendFailed,
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = VibeSpacing.lg)
                                )
                                IconButton(onClick = {
                                    onRetryUpload() // раньше был только retryNonce++, загрузка не перезапускалась
                                    retryNonce++
                                }) {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        contentDescription = strings.circleSendFailed,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    player != null -> {
                        AndroidView(
                            // SurfaceView не обрезается по CircleShape, поэтому нужен TextureView,
                            // но аспект ему приходится считать вручную (см. applyCenterCrop).
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                                        applyCenterCrop(v as TextureView, latestVideoSize.value)
                                    }
                                }
                            },
                            update = { textureView ->
                                player.setVideoTextureView(textureView)
                                applyCenterCrop(textureView, videoSize ?: player.videoSize)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (playerError != null) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = playerError.orEmpty(),
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = VibeSpacing.lg)
                                    )
                                    IconButton(onClick = {
                                        playerError = null
                                        firstFrameRendered = false
                                        retryNonce++
                                    }) {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            contentDescription = strings.circleSendFailed,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        } else if (!firstFrameRendered || buffering) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.30f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        } else if (!playing) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = strings.typeVideoMessage,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        VideoCover(
                            source = source.coverModel() ?: thumbUrl ?: "",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            showPlayIcon = !isUploading,
                            playIconSize = 56.dp
                        )
                    }
                }

                if (isUploading) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                progress = { (uploadProgress ?: 0) / 100f },
                                color = Color.White,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(VibeSpacing.xs))
                            Text(
                                text = "${uploadProgress ?: 0}%",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            if (isActive && !isMissing && !isUploading && !isFailed) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { muted = !muted },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = if (muted) strings.a11yUnmuteSound else strings.a11yMuteSound,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(VibeSpacing.xs))

        Text(
            text = formatDuration(
                if (isActive && !isMissing && !isUploading && !isFailed && effectiveTotalMs > 0) {
                    (effectiveTotalMs - positionMs).coerceAtLeast(0L)
                } else {
                    durationMs
                }
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(VibeRadius.pill))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .padding(horizontal = VibeSpacing.sm, vertical = 2.dp)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}