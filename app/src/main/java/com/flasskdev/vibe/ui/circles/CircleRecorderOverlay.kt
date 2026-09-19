package com.flasskdev.vibe.ui.circles

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flasskdev.vibe.ui.theme.VibeAuroraGradient
import com.flasskdev.vibe.ui.theme.VibeSpacing
import com.flasskdev.vibe.ui.theme.VibeStrings
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** Насколько нужно потянуть палец, чтобы зафиксировать запись. */
private val LOCK_DISTANCE = 88.dp

/** Насколько нужно сдвинуть палец влево, чтобы отменить запись. */
private val CANCEL_DISTANCE = 96.dp

/** Дорожка замка: высота в покое и в момент фиксации. */
private val LOCK_RAIL_HEIGHT = 108.dp
private val LOCK_RAIL_COLLAPSED = 46.dp
private val LOCK_RAIL_WIDTH = 46.dp

private val LockAccent = Color(0xFF7BF1A8)
private val CancelAccent = Color(0xFFFF6F81)

@Composable
fun CircleRecorderOverlay(
    visible: Boolean,
    strings: VibeStrings,
    onSend: (file: File, durationMs: Long) -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val recorder = remember(lifecycleOwner) { CircleRecorder(context, lifecycleOwner) }
    val state by recorder.state.collectAsState()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var hasPermission by remember { mutableStateOf(CircleRecorder.hasPermissions(context)) }
    var permissionAsked by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    val lockPx = with(density) { LOCK_DISTANCE.toPx() }
    val cancelPx = with(density) { CANCEL_DISTANCE.toPx() }
    val lockProgress = if (locked) 1f else (-dragY / lockPx).coerceIn(0f, 1f)
    val cancelProgress = if (locked) 0f else (-dragX / cancelPx).coerceIn(0f, 1f)
    val aboutToCancel = !locked && cancelProgress >= 0.92f

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = CircleRecorder.REQUIRED_PERMISSIONS.all { result[it] == true }
        hasPermission = granted
        if (!granted) {
            onError(strings.circlePermissionRequired)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !permissionAsked) {
            permissionAsked = true
            permissionLauncher.launch(CircleRecorder.REQUIRED_PERMISSIONS)
        }
    }

    DisposableEffect(recorder) { onDispose { recorder.release() } }

    LaunchedEffect(previewView, hasPermission) {
        if (hasPermission) previewView?.let { recorder.bind(it) }
    }

    LaunchedEffect(state) {
        when (val current = state) {
            is CircleRecorder.State.Finished -> {
                onSend(current.file, current.durationMs)
                onDismiss()
            }
            is CircleRecorder.State.Cancelled -> onDismiss()
            is CircleRecorder.State.Failed -> {
                if (current.reason != REASON_NO_PERMISSION) {
                    onError(current.reason)
                    onDismiss()
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(aboutToCancel) {
        if (aboutToCancel) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(locked) {
        if (locked) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val recordingState = state as? CircleRecorder.State.Recording
    val elapsed = recordingState?.elapsedMs ?: 0L
    val amplitude = recordingState?.amplitude ?: 0f
    val progress = (elapsed.toFloat() / CircleRecorder.MAX_DURATION_MS).coerceIn(0f, 1f)
    val isRecording = recordingState != null
    val isReady = state is CircleRecorder.State.Ready || isRecording

    Dialog(
        onDismissRequest = {
            recorder.cancel()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = VibeSpacing.lg)
            ) {
                /* ---------- превью + кольцо длительности ---------- */
                Box(contentAlignment = Alignment.Center) {
                    val ringPad by animateFloatAsState(
                        targetValue = 6f + amplitude * 10f,
                        animationSpec = spring(dampingRatio = 0.5f),
                        label = "ring"
                    )

                    Box(
                        Modifier
                            .size(268.dp)
                            .graphicsLayer {
                                // Превью следует за пальцем, но приглушённо:
                                // полный сдвиг выглядел как рывок всего экрана.
                                translationX = dragX * 0.35f
                                translationY = dragY * 0.22f
                                alpha = if (aboutToCancel) 0.5f else 1f
                            }
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 5.dp.toPx()
                            val diameter = size.minDimension - stroke
                            val topLeft = androidx.compose.ui.geometry.Offset(
                                x = (size.width - diameter) / 2f,
                                y = (size.height - diameter) / 2f
                            )

                            drawCircle(
                                color = Color.White.copy(alpha = 0.18f),
                                radius = diameter / 2f,
                                style = Stroke(stroke)
                            )

                            if (progress > 0f) {
                                // Дуга вписана внутрь Canvas: иначе половина
                                // обводки уходит за границы и срезается.
                                drawArc(
                                    brush = Brush.sweepGradient(VibeAuroraGradient, center),
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                                    style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                            }
                        }

                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }.also { previewView = it }
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size((240 + ringPad).dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(Modifier.height(VibeSpacing.lg))

                Text(
                    text = formatElapsed(elapsed),
                    color = if (isRecording) Color.White else Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium)
                )

                Spacer(Modifier.height(VibeSpacing.xs))

                Text(
                    text = when {
                        !hasPermission -> strings.circlePermissionRequired
                        !isReady -> strings.circleCameraPreparing
                        aboutToCancel -> strings.circleReleaseToCancel
                        locked -> strings.circleLockedHint
                        isRecording -> strings.circleRecordingHint
                        else -> strings.circleHoldOrTapHint
                    },
                    color = when {
                        aboutToCancel -> CancelAccent
                        locked -> LockAccent
                        else -> Color.White.copy(alpha = 0.62f)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(VibeSpacing.md))

                /* ---------- дорожка замка над кнопкой ----------
                 * Высота контейнера фиксирована, поэтому появление дорожки
                 * не сдвигает кнопку записи. */
                Box(
                    modifier = Modifier
                        .height(LOCK_RAIL_HEIGHT)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    LockRail(
                        active = isRecording || locked,
                        progress = lockProgress,
                        locked = locked
                    )
                }

                Spacer(Modifier.height(VibeSpacing.sm))

                /* ---------- ряд управления ---------- */
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            recorder.cancel()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = strings.circleCancel,
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(Modifier.width(VibeSpacing.xxl))

                    RecordButton(
                        isRecording = isRecording,
                        locked = locked,
                        enabled = isReady,
                        dragX = dragX,
                        aboutToCancel = aboutToCancel,
                        contentDescription = when {
                            locked && isRecording -> strings.circleSend
                            isRecording -> strings.circleTapToStop
                            else -> strings.circleHoldOrTapHint
                        },
                        onTap = {
                            if (isRecording) {
                                locked = false
                                recorder.stop()
                            } else {
                                recorder.start()
                                // Короткий тап = запись без удержания.
                                locked = true
                            }
                            dragX = 0f
                            dragY = 0f
                        },
                        onHoldStart = {
                            if (!locked) {
                                dragX = 0f
                                dragY = 0f
                                if (!isRecording) recorder.start()
                            }
                        },
                        onHoldDrag = { dx, dy ->
                            if (!locked) {
                                dragX = (dragX + dx).coerceIn(-cancelPx * 1.3f, 0f)
                                dragY = (dragY + dy).coerceIn(-lockPx * 1.3f, 0f)
                                if (-dragY >= lockPx) {
                                    locked = true
                                    dragX = 0f
                                    dragY = 0f
                                }
                            }
                        },
                        onHoldEnd = {
                            if (!locked) {
                                if (-dragX >= cancelPx) recorder.cancel() else recorder.stop()
                            }
                            dragX = 0f
                            dragY = 0f
                        }
                    )

                    Spacer(Modifier.width(VibeSpacing.xxl))

                    IconButton(
                        onClick = {
                            val pv = previewView ?: return@IconButton
                            scope.launch { recorder.switchCamera(pv) }
                        },
                        enabled = !isRecording && isReady
                    ) {
                        Icon(
                            Icons.Rounded.Cameraswitch,
                            contentDescription = strings.circleSwitchCamera,
                            tint = Color.White.copy(alpha = if (isRecording || !isReady) 0.3f else 0.8f)
                        )
                    }
                }

                /* ---------- дорожка отмены под кнопкой ---------- */
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CancelRail(
                        active = isRecording && !locked,
                        progress = cancelProgress,
                        armed = aboutToCancel,
                        text = if (aboutToCancel) strings.circleReleaseToCancel else strings.circleCancel
                    )
                }

                Text(
                    text = strings.circleMaxDurationHint,
                    color = Color.White.copy(alpha = if (isRecording) 0.35f else 0.18f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Вертикальная дорожка замка над кнопкой записи.
 *
 * Пока палец внизу — вытянутая капсула с открытым замком и подпрыгивающей
 * стрелкой. По мере подъёма пальца капсула сжимается к замку и подсвечивается,
 * после фиксации превращается в круглый бейдж с закрытым замком.
 */
@Composable
private fun LockRail(
    active: Boolean,
    progress: Float,
    locked: Boolean
) {
    val appear by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "lockAppear"
    )
    if (appear <= 0.01f) return

    val railHeight by animateDpAsState(
        targetValue = if (locked) {
            LOCK_RAIL_COLLAPSED
        } else {
            LOCK_RAIL_HEIGHT - (LOCK_RAIL_HEIGHT - LOCK_RAIL_COLLAPSED) * progress
        },
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "lockHeight"
    )
    val bounce by animateFloatAsState(
        targetValue = if (locked) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "lockBounce"
    )

    val accent = lerp(Color.White, LockAccent, if (locked) 1f else progress)

    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = appear
                scaleX = bounce
                scaleY = bounce
                translationY = (1f - appear) * 24f
            }
            .width(LOCK_RAIL_WIDTH)
            .height(railHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.25f + 0.45f * progress), RoundedCornerShape(percent = 50))
    ) {
        // Подсветка заполняется снизу вверх — «сколько осталось дотянуть».
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(railHeight * progress.coerceIn(0f, 1f))
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.05f), accent.copy(alpha = 0.30f))
                    )
                )
        )

        Icon(
            imageVector = if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 13.dp)
                .size(20.dp)
        )

        if (!locked) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f + 0.55f * (1f - progress)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(18.dp)
                    .graphicsLayer { translationY = -progress * 8f }
            )
        }
    }
}

/** Горизонтальная подсказка отмены: едет за пальцем и краснеет у порога. */
@Composable
private fun CancelRail(
    active: Boolean,
    progress: Float,
    armed: Boolean,
    text: String
) {
    val appear by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cancelAppear"
    )
    if (appear <= 0.01f) return

    val tint = lerp(Color.White.copy(alpha = 0.75f), CancelAccent, progress)
    val icon: ImageVector = if (armed) Icons.Rounded.Delete else Icons.Rounded.ChevronLeft

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer {
                alpha = appear
                translationX = -progress * 26.dp.toPx()
            }
            .clip(RoundedCornerShape(percent = 50))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(percent = 50))
            .padding(horizontal = VibeSpacing.md, vertical = VibeSpacing.sm)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(VibeSpacing.xs))
        Text(
            text = text,
            color = tint,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.width(VibeSpacing.sm))
        // Тонкая шкала прогресса до отмены.
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(tint)
            )
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    locked: Boolean,
    enabled: Boolean,
    dragX: Float,
    aboutToCancel: Boolean,
    contentDescription: String,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldDrag: (Float, Float) -> Unit,
    onHoldEnd: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.9f
            aboutToCancel -> 0.92f
            locked && isRecording -> 1.1f
            isRecording -> 1.16f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "recScale"
    )

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnHoldStart by rememberUpdatedState(onHoldStart)
    val currentOnHoldDrag by rememberUpdatedState(onHoldDrag)
    val currentOnHoldEnd by rememberUpdatedState(onHoldEnd)

    Box(
        Modifier
            .size(84.dp)
            .graphicsLayer {
                // Кнопка следует за пальцем: видно, что жест «взят».
                translationX = dragX * 0.5f
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(CircleShape)
            .background(
                if (aboutToCancel) {
                    Brush.linearGradient(listOf(CancelAccent, Color(0xFFB3324A)))
                } else {
                    Brush.linearGradient(VibeAuroraGradient)
                }
            )
            // Ключ намеренно только enabled: смена isRecording/locked больше не
            // перезапускает pointerInput посреди удержания, поэтому onHoldEnd
            // всегда доходит до конца.
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    val releasedBeforeLongPress = withTimeoutOrNull(
                        viewConfiguration.longPressTimeoutMillis
                    ) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id }
                                ?: return@withTimeoutOrNull true
                            if (!pointer.pressed) {
                                pointer.consume()
                                return@withTimeoutOrNull true
                            }
                            pointer.consume()
                        }
                    }

                    if (releasedBeforeLongPress != null) {
                        currentOnTap()
                        return@awaitEachGesture
                    }

                    currentOnHoldStart()
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) {
                            pointer.consume()
                            break
                        }
                        val change = pointer.positionChange()
                        currentOnHoldDrag(change.x, change.y)
                        pointer.consume()
                    }
                    currentOnHoldEnd()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when {
                aboutToCancel -> Icons.Rounded.Delete
                locked && isRecording -> Icons.Rounded.Send
                isRecording -> Icons.Rounded.Stop
                else -> Icons.Rounded.FiberManualRecord
            },
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

private fun formatElapsed(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0L)
    return "%d:%02d".format(total / 60, total % 60)
}