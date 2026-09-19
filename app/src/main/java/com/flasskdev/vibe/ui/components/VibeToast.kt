package com.flasskdev.vibe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flasskdev.vibe.ui.theme.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow

/* ============================================================================
 *  ПУНКТ 11 — VibeToast ВМЕСТО СИСТЕМНЫХ УВЕДОМЛЕНИЙ ANDROID
 * ============================================================================
 *
 *  ЧЕМ ПЛОХ СИСТЕМНЫЙ Toast:
 *   1. На Android 12+ он выглядит как серая плашка с иконкой приложения —
 *      чужеродно в нашем интерфейсе и его нельзя стилизовать вообще никак.
 *   2. Начиная с Android 11 текст в фоне не показывается совсем: половина
 *      сообщений об ошибках просто не доходила до пользователя.
 *   3. Он не знает про клавиатуру и всплывает поверх неё или под ней.
 *   4. Его нельзя смахнуть, нельзя добавить кнопку действия («Отменить»).
 *   5. Он живёт в WindowManager, а не в нашей иерархии — портит скриншоты
 *      и не подхватывает тёмную тему приложения.
 *
 *  ЧТО ЗДЕСЬ:
 *   - глобальный контроллер (Channel, а не SharedFlow: гарантированная
 *     доставка без потери при отсутствии подписчика);
 *   - очередь: тосты не перекрывают друг друга, а показываются по очереди;
 *   - НЕОБЯЗАТЕЛЬНАЯ иконка слева (как просили) — вектор, эмодзи или URL;
 *   - кнопка действия;
 *   - смахивание вниз, чтобы убрать;
 *   - учёт клавиатуры и системных врезок;
 *   - haptic-отклик по типу события.
 * ========================================================================== */

enum class VibeToastKind { NEUTRAL, SUCCESS, ERROR, WARNING }

@Immutable
data class VibeToastData(
    val message: String,
    val kind: VibeToastKind = VibeToastKind.NEUTRAL,
    /** Иконка слева от текста. Не обязательна — можно не передавать вовсе. */
    val icon: ImageVector? = null,
    /** Альтернатива: эмодзи или короткий символ вместо векторной иконки. */
    val emoji: String? = null,
    /** Или URL картинки (например, аватар отправителя). */
    val iconUrl: String? = null,
    val durationMs: Long = 2600,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val id: Long = nextId()
) {
    companion object {
        private var counter = 0L
        private fun nextId(): Long = ++counter
    }
}

/**
 * Глобальный контроллер. Заменяет `Toast.makeText(...).show()`.
 *
 * Вызывать можно откуда угодно, включая ViewModel и корутины вне Compose:
 *
 *     VibeToaster.success("Сообщение отправлено")
 *     VibeToaster.error("Нет соединения", icon = Icons.Rounded.WifiOff)
 *     VibeToaster.show(VibeToastData("Удалено", actionLabel = "Отменить", onAction = { undo() }))
 */
object VibeToaster {
    // Channel с буфером: если тост отправили до того, как появился хост
    // (например, из onCreate), он не потеряется, а покажется сразу после.
    private val channel = Channel<VibeToastData>(capacity = 16)
    internal val flow = channel.receiveAsFlow()

    fun show(data: VibeToastData) { channel.trySend(data) }

    fun info(message: String, icon: ImageVector? = null, emoji: String? = null) =
        show(VibeToastData(message, VibeToastKind.NEUTRAL, icon, emoji))

    fun success(message: String, icon: ImageVector? = null, emoji: String? = null) =
        show(VibeToastData(message, VibeToastKind.SUCCESS, icon, emoji))

    fun error(message: String, icon: ImageVector? = null, emoji: String? = null) =
        show(VibeToastData(message, VibeToastKind.ERROR, icon, emoji, durationMs = 3600))

    fun warning(message: String, icon: ImageVector? = null, emoji: String? = null) =
        show(VibeToastData(message, VibeToastKind.WARNING, icon, emoji, durationMs = 3200))

    fun action(message: String, actionLabel: String, onAction: () -> Unit) =
        show(VibeToastData(message, actionLabel = actionLabel, onAction = onAction, durationMs = 5000))
}

/**
 * Хост. Ставится ОДИН раз в корне приложения (MainActivity), поверх NavHost:
 *
 *     Box(Modifier.fillMaxSize()) {
 *         VibeNavGraph(...)
 *         VibeToastHost(Modifier.align(Alignment.BottomCenter))
 *     }
 */
@Composable
fun VibeToastHost(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<VibeToastData?>(null) }
    val queue = remember { mutableStateListOf<VibeToastData>() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        VibeToaster.flow.collect { data ->
            // Схлопываем дубликаты подряд: раньше при потере сети
            // прилетало 5 одинаковых тостов подряд.
            if (current?.message == data.message) return@collect
            queue.add(data)
        }
    }

    LaunchedEffect(queue.size, current) {
        if (current == null && queue.isNotEmpty()) {
            current = queue.removeAt(0)
        }
    }

    LaunchedEffect(current?.id) {
        val data = current ?: return@LaunchedEffect
        haptics.performHapticFeedback(
            when (data.kind) {
                VibeToastKind.ERROR -> HapticFeedbackType.LongPress
                else -> HapticFeedbackType.TextHandleMove
            }
        )
        delay(data.durationMs)
        current = null
    }

    Box(
        modifier
            .fillMaxWidth()
            .imePadding()                 // не залезаем под клавиатуру
            .navigationBarsPadding()
            .padding(horizontal = VibeSpacing.lg, vertical = VibeSpacing.lg),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = current != null,
            enter = slideInVertically(VibeMotion.snappy()) { it / 2 } +
                    fadeIn(VibeMotion.quick()) + scaleIn(VibeMotion.snappy(), initialScale = 0.92f),
            exit = slideOutVertically(VibeMotion.quick()) { it / 3 } +
                   fadeOut(VibeMotion.quick()) + scaleOut(VibeMotion.quick(), targetScale = 0.94f)
        ) {
            current?.let { data ->
                ToastCard(
                    data = data,
                    onDismiss = { current = null },
                    onAction = { data.onAction?.invoke(); current = null }
                )
            }
        }
    }
}

@Composable
private fun ToastCard(data: VibeToastData, onDismiss: () -> Unit, onAction: () -> Unit) {
    val glass = glassStyle()
    var dragY by remember { mutableFloatStateOf(0f) }

    val accent = when (data.kind) {
        VibeToastKind.SUCCESS -> VibeSuccess
        VibeToastKind.ERROR -> VibeError
        VibeToastKind.WARNING -> VibeWarning
        VibeToastKind.NEUTRAL -> VibeViolet
    }

    val fallbackIcon = when (data.kind) {
        VibeToastKind.SUCCESS -> Icons.Rounded.CheckCircle
        VibeToastKind.ERROR -> Icons.Rounded.ErrorOutline
        VibeToastKind.WARNING -> Icons.Rounded.WarningAmber
        VibeToastKind.NEUTRAL -> null      // нейтральный тост может быть вообще без иконки
    }

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragY }
            .pointerInput(data.id) {
                detectVerticalDragGestures(
                    onDragEnd = { if (dragY > 60f) onDismiss() else dragY = 0f },
                    onVerticalDrag = { _, delta -> dragY = (dragY + delta).coerceAtLeast(0f) }
                )
            }
            .clip(RoundedCornerShape(VibeRadius.lg))
            .background(if (glass.fill.luminanceIsDark()) Color(0xFF232326) else Color(0xFF1C1C1E))
            .padding(start = VibeSpacing.lg, end = VibeSpacing.sm)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        /* ---------- иконка слева (необязательная) ---------- */
        val iconSlotVisible = data.icon != null || data.emoji != null ||
                              data.iconUrl != null || fallbackIcon != null

        if (iconSlotVisible) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    data.iconUrl != null -> coil.compose.AsyncImage(
                        model = data.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                    data.emoji != null -> Text(data.emoji, style = MaterialTheme.typography.bodyMedium)
                    else -> Icon(
                        imageVector = data.icon ?: fallbackIcon!!,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(Modifier.width(VibeSpacing.md))
        }

        Text(
            text = data.message,
            color = Color.White.copy(alpha = 0.95f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = VibeSpacing.md)
        )

        if (data.actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(
                    data.actionLabel.uppercase(),
                    color = accent,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            Spacer(Modifier.width(VibeSpacing.sm))
        }
    }
}