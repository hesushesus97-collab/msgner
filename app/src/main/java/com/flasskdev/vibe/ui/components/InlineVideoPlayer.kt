package com.flasskdev.vibe.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private const val CONTROLS_TIMEOUT_MS = 3_000L
private const val SEEK_STEP_MS = 10_000L
private val SPEEDS = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)

/**
 * Полноэкранный/встроенный плеер на Media3 с собственным Compose-оверлеем:
 * авто-скрытие контролов, тап по половинам экрана для перемотки ±10 c,
 * буферизованный прогресс, скорость, звук, переключение вписать/заполнить,
 * пауза на уходе в фон и обложка до первого кадра.
 */
@OptIn(UnstableApi::class)
@Composable
fun InlineVideoPlayer(
    attachmentPath: String,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true,
    coverUrl: String? = null,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalVibeStrings.current

    val mediaUri = remember(attachmentPath) {
        when {
            attachmentPath.startsWith("content://") || attachmentPath.startsWith("http") ->
                Uri.parse(attachmentPath)
            File(attachmentPath).exists() -> Uri.fromFile(File(attachmentPath))
            else -> Uri.parse("https://flasskdev.alwaysdata.net/api/upload/file/$attachmentPath")
        }
    }

    val player = remember(mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            this.playWhenReady = playWhenReady
        }
    }

    var firstFrameRendered by remember(attachmentPath) { mutableStateOf(false) }
    var isPlaying by remember(attachmentPath) { mutableStateOf(playWhenReady) }
    var isBuffering by remember(attachmentPath) { mutableStateOf(true) }
    var ended by remember(attachmentPath) { mutableStateOf(false) }
    var hasError by remember(attachmentPath) { mutableStateOf(false) }

    var durationMs by remember(attachmentPath) { mutableLongStateOf(0L) }
    var positionMs by remember(attachmentPath) { mutableLongStateOf(0L) }
    var bufferedMs by remember(attachmentPath) { mutableLongStateOf(0L) }
    var scrubbing by remember(attachmentPath) { mutableStateOf(false) }
    var scrubValue by remember(attachmentPath) { mutableFloatStateOf(0f) }

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsNonce by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var speedIndex by remember { mutableIntStateOf(1) }
    var fillMode by remember { mutableStateOf(false) }
    var playerWidthPx by remember { mutableIntStateOf(1) }
    var seekHint by remember { mutableStateOf<String?>(null) }

    fun showControls() {
        controlsVisible = true
        controlsNonce++
    }

    // --- слушатель плеера -----------------------------------------------
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrameRendered = true }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) ended = false
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                ended = state == Player.STATE_ENDED
                if (state == Player.STATE_READY) {
                    hasError = false
                    durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                }
                if (state == Player.STATE_ENDED) controlsVisible = true
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isBuffering = false
                controlsVisible = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // --- опрос позиции ---------------------------------------------------
    LaunchedEffect(player, isPlaying, scrubbing) {
        while (true) {
            if (!scrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                bufferedMs = player.bufferedPosition.coerceAtLeast(0L)
                player.duration.takeIf { it != C.TIME_UNSET && it > 0 }?.let { durationMs = it }
            }
            delay(200)
        }
    }

    // --- авто-скрытие контролов -----------------------------------------
    LaunchedEffect(controlsNonce, isPlaying, scrubbing, ended) {
        if (controlsVisible && isPlaying && !scrubbing && !ended) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }

    // --- подсказка перемотки ---------------------------------------------
    LaunchedEffect(seekHint) {
        if (seekHint != null) {
            delay(650)
            seekHint = null
        }
    }

    // --- жизненный цикл ---------------------------------------------------
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(speedIndex) {
        player.playbackParameters = PlaybackParameters(SPEEDS[speedIndex])
    }

    // --- UI ----------------------------------------------------------------
    Box(
        modifier = modifier
            .background(Color.Black)
            .onSizeChanged { playerWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(attachmentPath) {
                detectTapGestures(
                    onTap = {
                        if (controlsVisible) controlsVisible = false else showControls()
                    },
                    onDoubleTap = { offset: Offset ->
                        val forward = offset.x > playerWidthPx / 2f
                        val target = if (forward) {
                            (player.currentPosition + SEEK_STEP_MS)
                                .coerceAtMost(if (durationMs > 0) durationMs else Long.MAX_VALUE)
                        } else {
                            (player.currentPosition - SEEK_STEP_MS).coerceAtLeast(0L)
                        }
                        player.seekTo(target)
                        positionMs = target
                        seekHint = if (forward) "+10 ${strings.unitSecondShort}" else "−10 ${strings.unitSecondShort}"
                        showControls()
                    }
                )
            }
    ) {
        // Обложка держится до первого отрисованного кадра — без чёрной вспышки.
        if (!firstFrameRendered) {
            VideoCover(
                source = attachmentPath,
                coverUrl = coverUrl,
                modifier = Modifier.fillMaxSize(),
                showPlayIcon = false
            )
        }

        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (firstFrameRendered) 1f else 0f),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false            // контролы рисуем сами
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setKeepContentOnPlayerReset(true)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                view.player = player
                view.resizeMode = if (fillMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        // Буферизация
        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
            )
        }

        // Подсказка двойного тапа
        seekHint?.let { hint ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(hint, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (hasError) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(strings.videoPlaybackFailed, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                IconButton(
                    onClick = {
                        hasError = false
                        player.prepare()
                        player.play()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Replay, strings.toastActionRetry, tint = Color.White)
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {

                // ---------- верхняя панель ----------
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, strings.actionClose, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.weight(1f))

                    // Скорость
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    speedIndex = (speedIndex + 1) % SPEEDS.size
                                    showControls()
                                })
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = formatSpeed(SPEEDS[speedIndex]),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = { muted = !muted; showControls() }) {
                        Icon(
                            if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (muted) strings.a11yUnmuteSound else strings.a11yMuteSound,
                            tint = Color.White
                        )
                    }
                }

                // ---------- центральные кнопки ----------
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    RoundControl(Icons.Default.Replay10, strings.a11yPlayerRewind10, 46.dp) {
                        player.seekTo((player.currentPosition - SEEK_STEP_MS).coerceAtLeast(0L))
                        showControls()
                    }

                    RoundControl(
                        icon = when {
                            ended -> Icons.Default.Replay
                            isPlaying -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        description = if (isPlaying) strings.a11yPlayerPause else strings.a11yPlayerPlay,
                        size = 64.dp,
                        background = Color.Black.copy(alpha = 0.5f)
                    ) {
                        when {
                            ended -> { player.seekTo(0); player.play() }
                            isPlaying -> player.pause()
                            else -> player.play()
                        }
                        showControls()
                    }

                    RoundControl(Icons.Default.Forward10, strings.a11yPlayerForward10, 46.dp) {
                        val max = if (durationMs > 0) durationMs else Long.MAX_VALUE
                        player.seekTo((player.currentPosition + SEEK_STEP_MS).coerceAtMost(max))
                        showControls()
                    }
                }

                // ---------- нижняя панель ----------
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                        .padding(start = 14.dp, end = 6.dp, top = 18.dp, bottom = 8.dp)
                ) {
                    val safeDuration = durationMs.coerceAtLeast(1L)
                    val progress =
                        if (scrubbing) scrubValue
                        else (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
                    val buffered = (bufferedMs.toFloat() / safeDuration).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // фон дорожки + буфер
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(buffered)
                                    .fillMaxHeight()
                                    .background(Color.White.copy(alpha = 0.38f))
                            )
                        }

                        Slider(
                            value = progress,
                            onValueChange = {
                                scrubbing = true
                                scrubValue = it
                                positionMs = (it * safeDuration).toLong()
                                showControls()
                            },
                            onValueChangeFinished = {
                                player.seekTo((scrubValue * safeDuration).toLong())
                                scrubbing = false
                                showControls()
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.Transparent,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { fillMode = !fillMode; showControls() }) {
                            Icon(
                                if (fillMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = strings.videoScaleCd,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: androidx.compose.ui.unit.Dp,
    background: Color = Color.Black.copy(alpha = 0.38f),
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(size * 0.52f))
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}x"
    else "${speed.toString().trimEnd('0').trimEnd('.')}x"