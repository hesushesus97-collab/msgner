package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import kotlin.math.roundToInt

/**
 * Состояние строки ввода. Создаётся в ChatScreen, но ЧИТАЕТСЯ только внутри ChatInputBar,
 * поэтому нажатие клавиши больше не инвалидирует ChatScreen вместе со списком сообщений.
 */
@Stable
class ChatInputState {
    var value by mutableStateOf(TextFieldValue(""))
    var pendingPhotos by mutableStateOf<List<android.net.Uri>>(emptyList())
    var pendingFiles by mutableStateOf<List<android.net.Uri>>(emptyList())
    var pendingVideoCoverPaths by mutableStateOf<Map<String, String>>(emptyMap())

    var showEmojiPanel by mutableStateOf(false)
    var showAttachmentMenu by mutableStateOf(false)
    var showFormattingBar by mutableStateOf(false)
    var showPreviewMode by mutableStateOf(false)

    var showLinkInputDialog by mutableStateOf(false)
    var linkInputSelection by mutableStateOf(TextRange.Zero)
    var linkInputInitialText by mutableStateOf("")

    var showColorInputDialog by mutableStateOf(false)
    var colorInputSelection by mutableStateOf(TextRange.Zero)
    var colorInputInitialText by mutableStateOf("")

    /** Количество вложений в черновике: используется бейджем и валидацией лимита. */
    val attachmentCount: Int by derivedStateOf { pendingPhotos.size + pendingFiles.size }

    /** Есть ли хоть что-то, что можно отправить. Кнопка Send читает только это. */
    val canSend: Boolean by derivedStateOf {
        value.text.isNotBlank() || pendingPhotos.isNotEmpty() || pendingFiles.isNotEmpty()
    }

    /** Открыта ли какая-либо нижняя панель: нужно для перехвата системной кнопки "назад". */
    val hasOpenPanel: Boolean by derivedStateOf { showEmojiPanel || showAttachmentMenu }

    /**
     * Панели эмодзи и вложений взаимоисключающие: раньше их можно было открыть одновременно,
     * и они наезжали друг на друга, ломая высоту нижней панели.
     */
    fun toggleEmojiPanel() {
        showAttachmentMenu = false
        showEmojiPanel = !showEmojiPanel
    }

    fun toggleAttachmentMenu() {
        showEmojiPanel = false
        showAttachmentMenu = !showAttachmentMenu
    }

    /** Закрывает все всплывающие панели. Возвращает true, если что-то реально закрылось. */
    fun closePanels(): Boolean {
        val had = showEmojiPanel || showAttachmentMenu
        showEmojiPanel = false
        showAttachmentMenu = false
        return had
    }

    /** Полный сброс черновика после успешной отправки. */
    fun clear() {
        value = TextFieldValue("")
        pendingPhotos = emptyList()
        pendingFiles = emptyList()
        pendingVideoCoverPaths = emptyMap()
        showPreviewMode = false
        showFormattingBar = false
        closePanels()
    }
}

/** Геометрия чата: нижняя панель пишет свою высоту, список и кнопка "вниз" её читают. */
@Stable
class ChatLayoutState {
    /** Реальная высота нижней панели: поле ввода + reply/edit + вложения + панель эмодзи. */
    var bottomBarHeightPx by mutableIntStateOf(0)

    /** Счётчик новых сообщений на бейдже кнопки "вниз". */
    var newMessagesCount by mutableIntStateOf(0)

    val hasNewMessages: Boolean by derivedStateOf { newMessagesCount > 0 }

    /**
     * Пишется из onSizeChanged на каждом layout-проходе панели. Отсекаем одинаковые значения,
     * иначе snapshot-запись без изменения значения всё равно дёргает наблюдателей.
     */
    fun updateBottomBarHeight(px: Int) {
        if (px != bottomBarHeightPx) bottomBarHeightPx = px
    }

    fun resetNewMessages() {
        if (newMessagesCount != 0) newMessagesCount = 0
    }
}

/** Выделение сообщений (режим мультивыбора). */
@Stable
class ChatSelectionState {
    val ids: SnapshotStateList<Int> = mutableStateListOf()

    val count: Int by derivedStateOf { ids.size }
    val isActive: Boolean by derivedStateOf { ids.isNotEmpty() }

    operator fun contains(id: Int): Boolean = ids.contains(id)

    fun toggle(id: Int) {
        if (!ids.remove(id)) ids.add(id)
    }

    fun clear() {
        if (ids.isNotEmpty()) ids.clear()
    }
}

// ---------------------------------------------------------------------------------------------
// Toast
// ---------------------------------------------------------------------------------------------

/** Тип тоста: определяет иконку, акцентный цвет и заголовок по умолчанию. */
enum class ChatToastVariant { Info, Success, Warning, Error }

/** Иммутабельный снимок одного показа: хост читает его, а не «живые» поля состояния. */
private data class ChatToastData(
    val message: String,
    val variant: ChatToastVariant,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
    val durationMillis: Long
)

/**
 * Тост. show() можно звать откуда угодно: на текст подписан только ChatToastHost,
 * поэтому показ тоста не рекомпозит ни экран, ни список.
 */
@Stable
class ChatToastState {
    internal var message by mutableStateOf("")
    internal var variant by mutableStateOf(ChatToastVariant.Info)
    internal var actionLabel by mutableStateOf<String?>(null)
    internal var onAction by mutableStateOf<(() -> Unit)?>(null)
    internal var durationMillis by mutableLongStateOf(DEFAULT_DURATION_MS)
    internal var trigger by mutableLongStateOf(0L)
    internal var dismissTrigger by mutableLongStateOf(0L)

    /**
     * @param durationMillis 0 или отрицательное значение — время показа считается по длине текста.
     * @param actionLabel подпись кнопки действия (например «Повторить»); без onAction игнорируется.
     */
    fun show(
        text: String,
        variant: ChatToastVariant = ChatToastVariant.Info,
        actionLabel: String? = null,
        durationMillis: Long = 0L,
        onAction: (() -> Unit)? = null
    ) {
        if (text.isBlank()) return
        message = text
        this.variant = variant
        this.actionLabel = actionLabel
        this.onAction = onAction
        this.durationMillis = if (durationMillis > 0L) durationMillis else autoDuration(text)
        trigger = System.currentTimeMillis()
    }

    fun showSuccess(text: String) = show(text, ChatToastVariant.Success)

    fun showWarning(text: String) = show(text, ChatToastVariant.Warning)

    fun showError(
        text: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) = show(text, ChatToastVariant.Error, actionLabel, onAction = onAction)

    fun dismiss() {
        dismissTrigger = System.currentTimeMillis()
    }

    private fun autoDuration(text: String): Long =
        (DEFAULT_DURATION_MS + text.length * 45L).coerceAtMost(MAX_DURATION_MS)

    private companion object {
        const val DEFAULT_DURATION_MS = 2500L
        const val MAX_DURATION_MS = 6000L
    }
}

/**
 * Хост тостов: плавающая «пилюля» над нижней панелью.
 *
 * Отличия от прежнего варианта: анимация появления/скрытия (spring вместо мгновенной подмены),
 * акцентная иконка и цвет по типу события, опциональная кнопка действия, свайп вниз и тап
 * для закрытия, поддержка insets (клавиатура и системная навигация) вместо хардкода 80.dp,
 * и объявление для screen reader'а через liveRegion.
 *
 * @param bottomPadding отступ от нижней панели чата.
 */
@Composable
fun ChatToastHost(
    state: ChatToastState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp
) {
    val strings = LocalVibeStrings.current
    val dark = isSystemInDarkTheme()

    var visible by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<ChatToastData?>(null) }
    val dragOffset = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.trigger) {
        if (state.trigger <= 0L) return@LaunchedEffect
        shown = ChatToastData(
            message = state.message,
            variant = state.variant,
            actionLabel = state.actionLabel,
            onAction = state.onAction,
            durationMillis = state.durationMillis
        )
        dragOffset.floatValue = 0f
        visible = true
        kotlinx.coroutines.delay(state.durationMillis)
        visible = false
    }

    LaunchedEffect(state.dismissTrigger) {
        if (state.dismissTrigger > 0L) visible = false
    }

    val toast = shown ?: return
    val accent = toast.variant.accentColor(dark)

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialOffsetY = { it / 2 }
            ) + fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.92f),
            exit = slideOutVertically(tween(160), targetOffsetY = { it / 3 }) +
                    fadeOut(tween(140)) +
                    scaleOut(tween(160), targetScale = 0.96f)
        ) {
            val drag = dragOffset.floatValue
            val dragAlpha by animateFloatAsState(
                targetValue = (1f - (drag / 220f)).coerceIn(0f, 1f),
                label = "toastDragAlpha"
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = toastBackground(dark),
                shadowElevation = 12.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(0, drag.roundToInt()) }
                    .alpha(dragAlpha)
                    .pointerInput(toast) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset.floatValue > 48f) visible = false
                                else dragOffset.floatValue = 0f
                            },
                            onDragCancel = { dragOffset.floatValue = 0f },
                            onVerticalDrag = { _, delta ->
                                dragOffset.floatValue = (dragOffset.floatValue + delta)
                                    .coerceIn(-16f, 240f)
                            }
                        )
                    }
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = strings.a11yToast(toast.message)
                    }
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(
                            start = 12.dp,
                            end = 6.dp,
                            top = 12.dp,
                            bottom = 12.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(accent.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = toast.variant.icon(),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = toast.variant.title(strings),
                                color = toastTitleColor(dark),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = toast.message,
                                color = toastMessageColor(dark),
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val action = toast.onAction
                        val label = toast.actionLabel
                        if (action != null && !label.isNullOrBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = label,
                                color = accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        visible = false
                                        action.invoke()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { visible = false }
                                .semantics { contentDescription = strings.a11yToastDismiss },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = toastMessageColor(dark).copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Тонкая акцентная полоса вместо цветной рамки: читается и на светлой, и на тёмной теме.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(accent.copy(alpha = 0.9f))
                    )
                }
            }
        }
    }
}

private fun ChatToastVariant.icon(): ImageVector = when (this) {
    ChatToastVariant.Success -> Icons.Rounded.CheckCircle
    ChatToastVariant.Warning -> Icons.Rounded.Warning
    ChatToastVariant.Error -> Icons.Rounded.Warning
    ChatToastVariant.Info -> Icons.Rounded.Info
}

private fun ChatToastVariant.title(strings: com.flasskdev.vibe.ui.theme.VibeStrings): String =
    when (this) {
        ChatToastVariant.Success -> strings.toastTitleSuccess
        ChatToastVariant.Warning -> strings.toastTitleWarning
        ChatToastVariant.Error -> strings.toastTitleError
        ChatToastVariant.Info -> strings.toastTitleInfo
    }

private fun ChatToastVariant.accentColor(dark: Boolean): Color = when (this) {
    ChatToastVariant.Success -> if (dark) Color(0xFF34D399) else Color(0xFF059669)
    ChatToastVariant.Warning -> if (dark) Color(0xFFFBBF24) else Color(0xFFD97706)
    ChatToastVariant.Error -> if (dark) Color(0xFFFB7185) else Color(0xFFDC2626)
    ChatToastVariant.Info -> if (dark) Color(0xFF60A5FA) else Color(0xFF2563EB)
}

private fun toastBackground(dark: Boolean): Color =
    if (dark) Color(0xFF1C1F26) else Color(0xFFFFFFFF)

private fun toastTitleColor(dark: Boolean): Color =
    if (dark) Color(0xFFF3F4F6) else Color(0xFF111827)

private fun toastMessageColor(dark: Boolean): Color =
    if (dark) Color(0xFFB6BCC7) else Color(0xFF4B5563)

/**
 * Резервирует место под нижнюю панель, уменьшая вьюпорт списка на ФАЗЕ LAYOUT.
 *
 * Раньше высота панели попадала в contentPadding, то есть читалась во время композиции:
 * любое изменение высоты (панель эмодзи, перенос строки, блок reply) рекомпозило LazyColumn
 * и все видимые бабблы. Чтение snapshot-состояния внутри Modifier.layout приводит только
 * к повторному layout, без рекомпозиции.
 *
 * @param baseline постоянный нижний contentPadding, уже заданный у LazyColumn.
 */
fun Modifier.reserveBottomBarSpace(
    layoutState: ChatLayoutState,
    baseline: Dp,
    gap: Dp = 16.dp
): Modifier = this.layout { measurable, constraints ->
    val extra = if (constraints.hasBoundedHeight) {
        (layoutState.bottomBarHeightPx + gap.roundToPx() - baseline.roundToPx()).coerceAtLeast(0)
    } else {
        0
    }
    val available = (constraints.maxHeight - extra).coerceAtLeast(0)
    val placeable = measurable.measure(
        constraints.copy(
            minHeight = minOf(constraints.minHeight, available),
            maxHeight = available
        )
    )
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
    layout(placeable.width, height) {
        placeable.place(0, 0)
    }
}