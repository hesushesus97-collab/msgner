package com.flasskdev.vibe.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.flasskdev.vibe.ui.theme.VibeError
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.theme.luminanceIsDark
import kotlin.math.roundToInt

/* ============================================================================
 *  ЕДИНОЕ КОНТЕКСТНОЕ МЕНЮ VIBE
 *  ---------------------------------------------------------------------------
 *  Заменяет: DropdownMenu в ChatInputBar (вложения), DropdownMenu в ChatHeader
 *  ("три точки"), ModalBottomSheet в ChatListScreen (зажатие чата) и меню
 *  сообщения. Одна реализация => одна анимация, одно стекло, одни отступы.
 *
 *  Отличия от Material DropdownMenu:
 *   - затемняющий скрим на весь экран, как в iOS, а не «висящая карточка»;
 *   - меню растёт ИЗ ТОЧКИ ЯКОРЯ (transformOrigin считается от bounds якоря),
 *     а не из фиксированного угла;
 *   - сам решает, открываться вверх или вниз, и не вылезает за края экрана;
 *   - деструктивные пункты и «выбранное состояние» встроены в модель, а не
 *     раскрашиваются вручную в каждом вызове.
 * ========================================================================== */

@Immutable
data class VibeMenuAction(
    val label: String,
    val icon: ImageVector? = null,
    /** Красный пункт: блокировка, удаление, выход. */
    val destructive: Boolean = false,
    /** Пункт-тумблер во включённом состоянии (подсвечивается акцентом). */
    val selected: Boolean = false,
    val enabled: Boolean = true,
    /** Для «булавки»: тот же вектор, повёрнутый на 45°. */
    val rotateIcon: Boolean = false,
    /** Отбить пункт разделителем-группой сверху. */
    val startsGroup: Boolean = false,
    val onClick: () -> Unit
)

/** Хранит позицию элемента, от которого меню должно «вырасти». */
@Stable
class VibeMenuAnchor {
    /**
     * Прямоугольник в координатах ОКНА. Ставится либо модификатором
     * [vibeMenuAnchor], либо вручную из onGloballyPositioned, когда меню
     * вызывается по долгому нажатию на элемент списка (там нужен именно
     * тот элемент, который зажали, а не единственный общий якорь).
     */
    var bounds: Rect by mutableStateOf(Rect.Zero)

    /**
     * Область, которая остаётся НЕзатемнённой и чёткой (вырез в скриме).
     * Если null, используется [bounds].
     */
    var highlightBounds: Rect? by mutableStateOf(null)

    /** Дополнительные области для выреза (например, при мультивыделении сообщений). */
    var additionalHighlightBounds: List<Rect> by mutableStateOf(emptyList())

    /** Радиус скругления выреза в скриме. */
    var cornerRadius: Dp by mutableStateOf(16.dp)
}

@Composable
fun rememberVibeMenuAnchor(): VibeMenuAnchor = remember { VibeMenuAnchor() }

/**
 * Повесить на кнопку/строку, которая вызывает меню.
 * boundsInWindow, а не boundsInRoot: Popup живёт в отдельном окне и меряет
 * координаты именно в оконной системе, иначе меню уезжает на высоту статусбара.
 */
fun Modifier.vibeMenuAnchor(anchor: VibeMenuAnchor): Modifier =
    this.onGloballyPositioned { anchor.bounds = it.boundsInWindow() }

private val PanelCorner = 16.dp
private val PanelMargin = 10.dp
private val AnchorGap = 8.dp

@Composable
fun VibeContextMenu(
    expanded: Boolean,
    anchor: VibeMenuAnchor,
    actions: List<VibeMenuAction>,
    onDismiss: () -> Unit,
    menuWidth: Dp = 250.dp,
    /** Шапка меню: аватар + имя для списка чатов, превью сообщения и т.п. */
    header: (@Composable () -> Unit)? = null
) {
    // Popup нельзя просто снять по expanded = false, иначе исчезновение
    // происходит мгновенно и без анимации. Держим окно смонтированным до
    // конца обратной анимации.
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) { if (expanded) mounted = true }

    val transition = updateTransition(targetState = expanded, label = "vibeContextMenu")
    val progress by transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMedium)
            else tween(130)
        },
        label = "menuProgress"
    ) { if (it) 1f else 0f }

    LaunchedEffect(expanded, progress) {
        if (!expanded && progress <= 0.001f) mounted = false
    }

    if (!mounted) return

    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    // View окна приложения: нужен, чтобы перевести boundsInWindow якоря в координаты
    // ОКНА ПОПАПА. Popup живёт в отдельном окне, и его (0,0) не обязано совпадать
    // с (0,0) окна активности: на части устройств оно сдвинуто на высоту статусбара,
    // из-за чего вырез в скриме и панель «уезжали» относительно зажатого элемента.
    val hostView = LocalView.current

    val positionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset.Zero
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        var menuSize by remember { mutableStateOf(IntSize.Zero) }
        val popupView = LocalView.current
        var windowOffset by remember { mutableStateOf(Offset.Zero) }

        val marginPx = with(density) { PanelMargin.toPx() }
        val gapPx = with(density) { AnchorGap.toPx() }
        // Якорь в координатах окна попапа.
        val a = anchor.bounds.translate(-windowOffset.x, -windowOffset.y)

        // Вверх открываемся только если внизу реально не хватает места
        // И сверху его достаточно, иначе меню упрётся в статусбар.
        val fitsBelow = a.bottom + gapPx + menuSize.height <= containerSize.height - marginPx
        val fitsAbove = a.top - gapPx - menuSize.height >= marginPx
        val openUpward = !fitsBelow && fitsAbove

        val targetY = if (openUpward) a.top - gapPx - menuSize.height else a.bottom + gapPx
        val maxY = (containerSize.height - menuSize.height - marginPx).coerceAtLeast(marginPx)
        val y = targetY.coerceIn(marginPx, maxY)

        val rawX = a.center.x - menuSize.width / 2f
        val maxX = (containerSize.width - menuSize.width - marginPx).coerceAtLeast(marginPx)
        val x = rawX.coerceIn(marginPx, maxX)

        // Пивот трансформации: точка, ближайшая к якорю. Именно это даёт
        // ощущение «меню выехало из-под пальца».
        val pivotX = if (menuSize.width == 0) 0.5f
        else ((a.center.x - x) / menuSize.width).coerceIn(0f, 1f)
        val pivotY = if (openUpward) 1f else 0f

        Box(
            modifier = Modifier
                .size(
                    width = with(density) { containerSize.width.toDp() },
                    height = with(density) { containerSize.height.toDp() }
                )
                .onGloballyPositioned {
                    // Смещение окна попапа относительно окна приложения в экранных пикселях.
                    val host = IntArray(2).also(hostView::getLocationOnScreen)
                    val hostInWindow = IntArray(2).also(hostView::getLocationInWindow)
                    val popup = IntArray(2).also(popupView::getLocationOnScreen)
                    val next = Offset(
                        (popup[0] - (host[0] - hostInWindow[0])).toFloat(),
                        (popup[1] - (host[1] - hostInWindow[1])).toFloat()
                    )
                    if (next != windowOffset) windowOffset = next
                }
        ) {
            // ─── Скрим с вырезом под активный элемент ───
            val scrimColor = Color.Black.copy(alpha = if (isDark) 0.46f else 0.22f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
                    .drawWithContent {
                        val shift = windowOffset
                        val targetHighlight = (anchor.highlightBounds ?: anchor.bounds)
                            .translate(-shift.x, -shift.y)
                        val allRects = buildList {
                            if (targetHighlight.width > 0f && targetHighlight.height > 0f) {
                                add(targetHighlight)
                            }
                            anchor.additionalHighlightBounds.forEach { raw ->
                                val r = raw.translate(-shift.x, -shift.y)
                                if (r.width > 0f && r.height > 0f) add(r)
                            }
                        }

                        if (allRects.isNotEmpty()) {
                            var path = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
                            val cr = CornerRadius(anchor.cornerRadius.toPx(), anchor.cornerRadius.toPx())
                            allRects.forEach { rect ->
                                val cutout = Path().apply { addRoundRect(RoundRect(rect = rect, cornerRadius = cr)) }
                                path = Path.combine(PathOperation.Difference, path, cutout)
                            }
                            drawPath(path, color = scrimColor)
                        } else {
                            drawRect(color = scrimColor)
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            // ─── Панель ───
            Box(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .width(minOf(menuWidth, with(density) { (containerSize.width - 2 * marginPx).coerceAtLeast(1f).toDp() }))
                    .heightIn(max = with(density) { (containerSize.height - 2 * marginPx).coerceAtLeast(1f).toDp() })
                    .onSizeChanged { menuSize = it }
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(pivotX, pivotY)
                        // 0.86 -> 1.0 даёт «пружинку», заметную но не мультяшную
                        val s = 0.86f + 0.14f * progress
                        scaleX = s
                        scaleY = s
                        alpha = progress.coerceIn(0f, 1f)
                        // Небольшой сдвиг навстречу якорю усиливает связь с пальцем
                        translationY = (1f - progress) * (if (openUpward) 14f else -14f)
                    }
                    .shadow(
                        elevation = 26.dp,
                        shape = RoundedCornerShape(PanelCorner),
                        ambientColor = Color.Black.copy(alpha = 0.22f),
                        spotColor = Color.Black.copy(alpha = 0.34f)
                    )
                    .clip(RoundedCornerShape(PanelCorner))
                    .background(
                        if (isDark) Color(0xFF1F1F22).copy(alpha = 0.97f)
                        else Color(0xFFFCFCFD).copy(alpha = 0.98f)
                    )
                    .background(
                        // Верхний блик: имитация стекла, ловящего свет
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.07f else 0.55f),
                                Color.Transparent
                            ),
                            endY = 90f
                        )
                    )
                    .border(
                        width = 0.7.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.16f else 0.75f),
                                Color.White.copy(alpha = if (isDark) 0.04f else 0.18f)
                            )
                        ),
                        shape = RoundedCornerShape(PanelCorner)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    if (header != null) {
                        header()
                        VibeMenuDivider(inset = 0.dp)
                    }
                    actions.forEachIndexed { index, action ->
                        if (index > 0) {
                            VibeMenuDivider(
                                inset = if (action.startsGroup) 0.dp else 46.dp,
                                strong = action.startsGroup
                            )
                        }
                        VibeMenuRow(
                            action = action,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VibeMenuDivider(inset: Dp, strong: Boolean = false) {
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    HorizontalDivider(
        modifier = Modifier.padding(start = inset),
        thickness = if (strong) 5.dp else 0.5.dp,
        color = if (strong) {
            if (isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.05f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.14f else 0.10f)
        }
    )
}

@Composable
private fun VibeMenuRow(
    action: VibeMenuAction,
    onDismiss: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()

    val contentColor = when {
        !action.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        action.destructive -> VibeError
        action.selected -> VibePrimary
        else -> MaterialTheme.colorScheme.onBackground
    }
    val pressOverlay = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    // Подсветка нажатия анимируется: быстро появляется, плавно гаснет. Раньше
    // фон переключался мгновенно и мигал при коротком тапе.
    val overlay by animateColorAsState(
        targetValue = if (pressed) pressOverlay else Color.Transparent,
        animationSpec = tween(if (pressed) 50 else 220),
        label = "menuRowPress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(overlay)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = action.enabled
            ) {
                action.onClick()
                onDismiss()
            }
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (action.icon != null) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .then(if (action.rotateIcon) Modifier.rotate(45f) else Modifier)
            )
            Spacer(modifier = Modifier.width(11.dp))
        } else {
            Spacer(modifier = Modifier.width(31.dp))
        }
        Text(
            text = action.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}