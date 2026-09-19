package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import android.os.Build
import com.flasskdev.vibe.ui.theme.vibeOptionalBlur
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.zIndex
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.flasskdev.vibe.ui.components.VibeContextMenu
import com.flasskdev.vibe.ui.components.VibeMenuAction
import com.flasskdev.vibe.ui.components.rememberVibeMenuAnchor
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.local.ChatWithUser
import com.flasskdev.vibe.ui.components.TypingIndicator
import com.flasskdev.vibe.ui.theme.*
import com.flasskdev.vibe.ui.viewmodels.ChatViewModel
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.exp
import kotlin.math.roundToInt

/* ---------------------------------------------------------------------------
 *  iOS "Inset Grouped" design tokens
 *
 *  Layout mirrors UITableView.Style.insetGrouped: a grouped page background,
 *  rows collected into cards with 16dp side margins and 14dp corners, headers
 *  and footers aligned to the card content inset.
 * ------------------------------------------------------------------------- */

private val IOSOrange = Color(0xFFFF9500)
private val IOSRed = Color(0xFFFF3B30)
private val IOSGray = Color(0xFF8E8E93)
private val IOSFill = Color(0xFF767680)
private val IOSGroupedLight = Color(0xFFF2F2F7)

private val PageInset = 16.dp          // card side margin
private val CardCorner = 14.dp
private val CardContentInset = 16.dp   // padding inside a card
private val AvatarSize = 60.dp
private val AvatarGap = 12.dp
private val SearchAvatarSize = 46.dp
private val SwipeActionWidth = 78.dp
private const val UNKNOWN_ARTIST_TAG = "Unknown Artist"

// Separator starts where the row text starts.
private val SearchSeparatorInset = CardContentInset + SearchAvatarSize + AvatarGap

private val AvatarGradients = listOf(
    Color(0xFF64D2FF) to Color(0xFF0A84FF),
    Color(0xFFFF9F0A) to Color(0xFFFF375F),
    Color(0xFF30D158) to Color(0xFF0A84FF),
    Color(0xFFBF5AF2) to Color(0xFF5E5CE6),
    Color(0xFFFFD60A) to Color(0xFFFF9F0A),
    Color(0xFFFF6482) to Color(0xFFBF5AF2),
    Color(0xFF5AC8FA) to Color(0xFF34C759)
)

private fun avatarBrush(seed: Int): Brush {
    val pair = AvatarGradients[abs(seed) % AvatarGradients.size]
    return Brush.linearGradient(listOf(pair.first, pair.second))
}

private enum class ChatFilter { ALL, UNREAD }

@Composable
private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Grouped page background: near-black in dark mode, systemGroupedBackground in light mode. */
@Composable
private fun pageBackground(): Color =
    if (isDarkSurface()) MaterialTheme.colorScheme.background else IOSGroupedLight

@Composable
private fun separatorColor(): Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)

@Composable
private fun secondaryTextColor(): Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)

/** Card fill, composited so it adapts to whatever background the theme provides. */
@Composable
private fun Modifier.cardSurface(shape: Shape, pressOverlay: Color = Color.Transparent): Modifier {
    val dark = isDarkSurface()
    val base = MaterialTheme.colorScheme.background
    return this
        .clip(shape)
        .then(
            if (dark) Modifier.background(base).background(Color.White.copy(alpha = 0.08f))
            else Modifier.background(Color.White)
        )
        .background(pressOverlay)
}

/** Corner rounding for a row depending on its position inside the card. */
private fun groupShape(index: Int, count: Int): Shape = when {
    count <= 1 -> RoundedCornerShape(CardCorner)
    index == 0 -> RoundedCornerShape(topStart = CardCorner, topEnd = CardCorner)
    index == count - 1 -> RoundedCornerShape(bottomStart = CardCorner, bottomEnd = CardCorner)
    else -> RoundedCornerShape(0.dp)
}

/* ---------------------------------------------------------------------------
 *  Пункт 6: список чатов как центр уведомлений iOS
 *
 *  Раньше строки склеивались в одну карточку-группу (inset grouped) с
 *  разделителями. Теперь каждый чат - отдельная плашка-«уведомление»:
 *   - на первом показе приезжает снизу со сдвигом по индексу (staggered),
 *     как всплывающий баннер;
 *   - у верхнего и нижнего края вьюпорта уходит в стопку и возвращается из неё
 *     СТРОГО по позиции скролла: масштаб, прозрачность, отставание и наклон
 *     считаются от того, какая доля плашки заехала за край. Появление снизу и
 *     сверху - одна кривая, проигранная в разные стороны.
 *
 *  Оба эффекта считаются в graphicsLayer, то есть на фазе DRAW. Это принципиально:
 *  чтение listState.layoutInfo во время композиции инвалидировало бы LazyColumn
 *  каждый кадр скролла вместе со всеми видимыми строками.
 * ------------------------------------------------------------------------- */

private val NotificationCorner = 22.dp
private val NotificationGap = 2.5.dp        // половина зазора: между соседними плашками получается 5dp
private val NotificationSlide = 26.dp       // с какой высоты карточка «выезжает» на первой отрисовке

/* ---------------------------------------------------------------------------
 *  ПОЯВЛЕНИЕ ПРИ СКРОЛЛЕ: ОДИН ИСТОЧНИК ПРАВДЫ НА КАДР
 *
 *  Было: у края работали ДВА независимых слоя. notificationStack двигал плашку
 *  от позиции скролла, а notificationEntrance в это же время играл свой
 *  380-миллисекундный tween со сдвигом, наклоном и расфокусом. Слои
 *  перемножались, поэтому строка на входе успевала уехать ПРОТИВ направления
 *  пальца, мигнуть прозрачностью (0.25 у входа против 0.08 у стопки) и
 *  «догнать» ленту уже в воздухе. Ровно это и читается как дешёвая анимация:
 *  движение живёт по таймеру, а не по жесту.
 *
 *  Стало: всё, что происходит у краёв вьюпорта, считается ТОЛЬКО от позиции
 *  строки, поэтому вход и выход симметричны и намертво приклеены к пальцу,
 *  включая инерцию и оверскролл. Тайминговый каскад остался привилегией
 *  первой отрисовки экрана и гасится, как только палец тронул ленту.
 * ------------------------------------------------------------------------- */

/** Кромка, которую можно спрятать под панель без всякой реакции: иначе плашка «дышит» уже на 1px захода. */
private const val EdgeDeadZone = 0.12f
/** Глубина захода: ease-in-out, основная работа в середине, без рывка на старте. */
private val EdgeDepthEasing = CubicBezierEasing(0.32f, 0f, 0.36f, 1f)
/** Прозрачность держится, пока плашка наполовину видна, и падает уже под панелью. */
private val EdgeFadeEasing = CubicBezierEasing(0.62f, 0f, 0.9f, 0.35f)
private const val EdgeScaleDrop = 0.055f    // было 0.12 - на строке списка читалось как «схлопывание»
private const val EdgeLag = 0.15f           // было 0.42 - плашка отставала от пальца почти на полвысоты
private const val EdgeTiltDeg = 4.5f        // было 10 - меньше перспективных искажений на тексте
private const val EdgeCameraDistance = 34f  // было 20 - «длинный объектив» вместо широкоугольника

/* ---------------------------------------------------------------------------
 *  Первая отрисовка экрана: каскад.
 *
 *  Прогресс гоняется линейным tween'ом, а каждый канал (сдвиг, масштаб,
 *  прозрачность, наклон, расфокус) читает его через собственную кривую. Так
 *  каналы расходятся во времени - именно это читается как «дорого», в отличие
 *  от одной пружины на всё сразу.
 * ------------------------------------------------------------------------- */

/** Expo-out: мгновенный старт и длинный доводчик. Основное движение. */
private val EntranceMotionEasing = CubicBezierEasing(0.16f, 1f, 0.30f, 1f)
/** Едва заметный перелёт по масштабу - карточка «дышит» на посадке. */
private val EntranceScaleEasing = CubicBezierEasing(0.22f, 1.10f, 0.32f, 1f)
/** Прозрачность набирается раньше геометрии, чтобы не было эффекта «проявки». */
private val EntranceFadeEasing = CubicBezierEasing(0.32f, 0f, 0.24f, 1f)

private const val EntranceDurationMs = 560
private const val EntranceStaggerSpanMs = 300f   // весь каскад укладывается в этот бюджет
private const val EntranceStartScale = 0.94f
private const val EntranceTiltDeg = 6f           // наклон по X: карточка приподнимается из плоскости
private const val EntranceBlurPx = 10f           // расфокус на старте, снимается к середине
private const val EntranceSheenAlpha = 0.10f     // блик, пробегающий по плашке

/** Прогресс подотрезка [start; end], нормированный в 0..1. */
private fun segment(t: Float, start: Float, end: Float): Float =
    ((t - start) / (end - start)).coerceIn(0f, 1f)

/**
 * Каскад с затухающим шагом: между первыми карточками пауза заметная,
 * дальше она быстро сжимается. Линейный стаггер на длинном списке выглядит
 * как лаг, этот - как одна волна.
 */
private fun entranceDelayMs(index: Int): Long =
    (EntranceStaggerSpanMs * (1f - exp(-index / 3.2f))).toLong()

/** RenderEffect доступен только с Android 12; ниже расфокус просто не применяется. */
private val supportsEntranceBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Полный сброс слоя.
 *
 * graphicsLayer хранит значения между кадрами: если выйти из блока, не переписав
 * канал, на плашке останется значение от кадра у края. Старый код сбрасывал
 * только rotationX и renderEffect, поэтому строки иногда «залипали» подмасштабом
 * или полупрозрачными в середине ленты.
 */
private fun GraphicsLayerScope.resetEdgeDepth() {
    scaleX = 1f
    scaleY = 1f
    alpha = 1f
    translationY = 0f
    rotationX = 0f
    transformOrigin = TransformOrigin.Center
    renderEffect = null
}

/**
 * Стопка у краёв вьюпорта и появление из неё - один и тот же расчёт.
 *
 * Плашка не «анимируется», а находится в состоянии, однозначно заданном её
 * позицией: доля, ушедшая за край, гонит масштаб, прозрачность, отставание и
 * наклон. Поэтому появление снизу, появление сверху, уход за край, инерция и
 * оверскролл автоматически согласованы между собой и с пальцем, а обратный
 * скролл на середине жеста честно проигрывает анимацию назад.
 *
 * Расфокуса здесь больше нет: BlurEffect на каждой въезжающей строке и стоил
 * дорого (лишний RenderEffect на слой в каждом кадре скролла), и читался как
 * грязь по краям, особенно на тексте под полупрозрачной панелью.
 *
 * @param topEdgePx нижняя граница инлайн-навбара: всё, что заехало выше, уходит в стопку.
 * @param bottomEdgePx высота таббара, отсчитывается от низа вьюпорта.
 */
private fun Modifier.notificationScrollDepth(
    listState: LazyListState,
    itemKey: Any,
    topEdgePx: Float,
    bottomEdgePx: Float
): Modifier = this.graphicsLayer {
    val info = listState.layoutInfo
    // Ручной поиск вместо firstOrNull: лямбда-предикат на каждой строке в каждом
    // кадре скролла - это аллокация в самом горячем месте экрана.
    val visible = info.visibleItemsInfo
    var item: LazyListItemInfo? = null
    for (i in visible.indices) {
        val candidate = visible[i]
        if (candidate.key == itemKey) {
            item = candidate
            break
        }
    }
    val row = item
    if (row == null || row.size <= 0) {
        resetEdgeDepth()
        return@graphicsLayer
    }

    val height = row.size.toFloat()
    val itemTop = row.offset.toFloat()
    val itemBottom = itemTop + height
    val bottomEdge = info.viewportEndOffset.toFloat() - bottomEdgePx

    // Доля карточки, ушедшая за верхний / нижний край.
    val underTop = ((topEdgePx - itemTop) / height).coerceIn(0f, 1f)
    val underBottom = ((itemBottom - bottomEdge) / height).coerceIn(0f, 1f)
    val raw = max(underTop, underBottom)
    if (raw <= EdgeDeadZone) {
        // Карточка в «чистой» зоне: слой сбрасывается ПОЛНОСТЬЮ, иначе на нём
        // останутся значения от предыдущего кадра у края.
        resetEdgeDepth()
        return@graphicsLayer
    }

    val atTop = underTop >= underBottom
    val depth = ((raw - EdgeDeadZone) / (1f - EdgeDeadZone)).coerceIn(0f, 1f)
    val p = EdgeDepthEasing.transform(depth)
    val dir = if (atTop) 1f else -1f

    // Точка сжатия - тот край, к которому карточка прижимается.
    transformOrigin = TransformOrigin(0.5f, if (atTop) 0f else 1f)
    val scale = 1f - EdgeScaleDrop * p
    scaleX = scale
    scaleY = scale
    // Прозрачность по своей кривой: геометрия уже поехала, а плашка ещё читается.
    alpha = 1f - EdgeFadeEasing.transform(depth)
    // Отставание от скролла даёт эффект слоёв, но на большой амплитуде плашка
    // отрывается от пальца, поэтому берём от eased-прогресса и всего 0.15 высоты.
    translationY = dir * height * p * EdgeLag
    // Наклон вокруг прижатого ребра: у верхнего края плашка уходит «вглубь»,
    // у нижнего - лежит и распрямляется по мере выезда во вьюпорт.
    rotationX = dir * EdgeTiltDeg * p
    // Перспектива по умолчанию (8 * density) слишком «широкоугольная» для строки списка.
    cameraDistance = EdgeCameraDistance * density
    renderEffect = null
}

/**
 * Запоминает уже показанные чаты.
 *
 * LazyColumn уничтожает строки за пределами экрана, поэтому по возвращении строки
 * в вид композиция всегда свежая. Этот набор нужен, чтобы отличить первую
 * отрисовку экрана (каскад со стаггером и бликом) от появления при прокрутке,
 * которое анимируется от позиции скролла и никаких таймеров не заводит.
 */
@Stable
private class NotificationEntrance {
    private val seen = HashSet<Int>()

    /**
     * Каскад по таймеру уместен только на первой отрисовке экрана. Как только
     * палец тронул ленту, строки обязаны появляться от позиции скролла: иначе
     * новая строка, впервые попавшая во вьюпорт на флинге, поедет по своему
     * 560-миллисекундному таймлайну и лента начнёт «догонять» палец.
     *
     * Обычное поле, а не mutableStateOf: claim() читается из remember { } на
     * фазе композиции, и snapshot-состояние подписало бы каждую строку на смену
     * этого флага, то есть на первый же скролл.
     */
    private var armed = true

    fun disarm() {
        armed = false
    }

    /** true ровно один раз на каждый id и только пока экран не проскроллен. */
    fun claim(id: Int): Boolean = seen.add(id) && armed
}

/**
 * Каскад первой отрисовки: плашка приезжает снизу, распрямляется из наклона,
 * доводится по масштабу с микро-перелётом, снимает расфокус и ловит блик.
 *
 * Играется РОВНО ОДИН раз за жизнь экрана и только для строк, попавших в кадр
 * до первого касания. Всё, что появляется при прокрутке, живёт на
 * [notificationScrollDepth], то есть на позиции, а не на таймере: два
 * независимых источника движения на одной строке и давали ту самую дешёвую,
 * «резиновую» картинку у краёв.
 *
 * Все каналы читают прогресс внутри graphicsLayer / drawWithContent, то есть на
 * фазах DRAW: композиция строки за время анимации не перезапускается ни разу.
 */
@Composable
private fun Modifier.notificationEntrance(
    entrance: NotificationEntrance,
    chatId: Int,
    index: Int,
    slidePx: Float
): Modifier {
    val firstShow = remember(chatId) { entrance.claim(chatId) }

    // Строка, которая въезжает при скролле, стартует сразу с завершённого
    // прогресса: слои ниже становятся no-op и не спорят с расчётом от позиции.
    val progress = remember(chatId) { Animatable(if (firstShow) 0f else 1f) }

    LaunchedEffect(chatId) {
        if (!firstShow) return@LaunchedEffect
        kotlinx.coroutines.delay(entranceDelayMs(index))
        progress.animateTo(
            targetValue = 1f,
            // Линейный носитель: форму задают кривые ниже, каждая своему каналу.
            animationSpec = tween(durationMillis = EntranceDurationMs, easing = LinearEasing)
        )
    }

    return this
        .graphicsLayer {
            val t = progress.value
            if (t >= 1f) return@graphicsLayer

            val motion = EntranceMotionEasing.transform(t)
            val settle = EntranceScaleEasing.transform(t)
            // Прозрачность закрывается на 55% таймлайна - дальше едет только геометрия.
            val fade = EntranceFadeEasing.transform(segment(t, 0f, 0.55f))

            alpha = fade
            translationY = (1f - motion) * slidePx

            val scale = EntranceStartScale + (1f - EntranceStartScale) * settle
            scaleX = scale
            scaleY = scale

            // Наклон вокруг нижнего ребра: плашка не просто едет, а раскрывается
            // в плоскость экрана.
            rotationX = (1f - motion) * EntranceTiltDeg
            cameraDistance = EdgeCameraDistance * density
            transformOrigin = TransformOrigin(0.5f, 1f)

            if (supportsEntranceBlur) {
                val radius = (1f - segment(t, 0f, 0.62f)) * EntranceBlurPx
                renderEffect =
                    if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Decal) else null
            }
        }
        .drawWithContent {
            drawContent()

            // Блик - привилегия первой отрисовки: на каждом возврате при скролле
            // он превратился бы в мигание.
            if (!firstShow) return@drawWithContent

            val t = progress.value
            // Блик стартует, когда карточка уже видна, и гаснет к концу движения.
            val sheen = segment(t, 0.18f, 0.95f)
            if (sheen <= 0f || sheen >= 1f) return@drawWithContent

            val insetX = PageInset.toPx()
            val insetY = NotificationGap.toPx()
            val cardWidth = size.width - insetX * 2f
            val cardHeight = size.height - insetY * 2f
            if (cardWidth <= 0f || cardHeight <= 0f) return@drawWithContent

            val band = cardWidth * 0.55f
            val travel = cardWidth + band * 2f
            val x = insetX - band + travel * sheen
            val strength = EntranceSheenAlpha * (1f - sheen)

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = strength),
                        Color.Transparent
                    ),
                    start = Offset(x, insetY),
                    end = Offset(x + band, insetY + cardHeight)
                ),
                topLeft = Offset(insetX, insetY),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(NotificationCorner.toPx())
            )
        }
}

/** Плашка отдельного «уведомления»: тень в светлой теме, приподнятая заливка в тёмной. */
@Composable
private fun Modifier.notificationCardSurface(shape: Shape, pressOverlay: Color): Modifier {
    val dark = isDarkSurface()
    val base = MaterialTheme.colorScheme.background
    return this
        .shadow(
            elevation = if (dark) 0.dp else 7.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.10f),
            spotColor = Color.Black.copy(alpha = 0.14f)
        )
        .clip(shape)
        .then(
            if (dark) Modifier.background(base).background(Color.White.copy(alpha = 0.10f))
            else Modifier.background(Color.White)
        )
        .background(pressOverlay)
}

/* ---------------------------------------------------------------------------
 *  Топбар: крупный заголовок передаёт эстафету инлайн-строке
 *
 *  Раньше крупный заголовок просто уезжал вместе со списком, а инлайн-строка
 *  линейно проявлялась поверх. Теперь это один жест: заголовок ужимается к
 *  ведущему краю и гаснет, инлайн-строка встречно подъезжает снизу. Обе фазы
 *  считаются из одного collapseProgress и читаются на фазе DRAW.
 * ------------------------------------------------------------------------- */

private val LargeTitleCollapseShift = 14.dp   // насколько крупный заголовок уползает вверх
private const val LargeTitleCollapseScale = 0.88f
private val InlineTitleRise = 9.dp            // встречный подъём инлайн-заголовка
private val TopBarHeight = 44.dp
private val TopBarShadowHeight = 10.dp        // мягкий хвост под разделителем

/** Крупный заголовок уходит раньше, чем появляется инлайн: иначе они наложатся. */
private val LargeTitleFadeEasing = CubicBezierEasing(0.4f, 0f, 0.7f, 0.2f)
/** Инлайн-строка, наоборот, ждёт и приходит на второй половине жеста. */
private val InlineBarFadeEasing = CubicBezierEasing(0.3f, 0.8f, 0.5f, 1f)

/** Счётчик непрочитанных: только цифра, поэтому не требует локализации. */
@Composable
private fun UnreadTotalBadge(
    count: Int,
    height: Dp,
    fontSize: TextUnit,
    // Не contentDescription: внутри semantics {} одноимённое свойство ресивера
    // перекрыло бы параметр, и чтение упало бы на нечитаемом геттере.
    description: String
) {
    Box(
        modifier = Modifier
            .height(height)
            .widthIn(min = height)
            .background(VibePrimary, CircleShape)
            .padding(horizontal = 7.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

/* ---------------------------------------------------------------------------
 *  Chat list
 * ------------------------------------------------------------------------- */

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    liquidState: LiquidState,
    webSocket: VibeWebSocket,
    onChatClick: (interlocutorId: Int, interlocutorName: String) -> Unit,
    viewModel: ChatViewModel = viewModel(),
    /**
     * Пункт 7: пока открыто контекстное меню чата, таббар должен уехать вниз.
     * Экран не знает про таббар, поэтому просто сообщает наружу факт открытия.
     */
    onTabBarSuppressedChange: (Boolean) -> Unit = {}
) {
    val chats by viewModel.chats.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    var hasInternet by remember { mutableStateOf(true) }
    val strings = LocalVibeStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ChatFilter.ALL) }
    // Раньше здесь был ModalBottomSheet. Теперь якорное контекстное меню:
    // menuTarget живёт дольше menuOpen, иначе панель исчезала бы мгновенно,
    // не доиграв обратную анимацию.
    var menuTarget by remember { mutableStateOf<ChatWithUser?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val chatMenuAnchor = rememberVibeMenuAnchor()
    // Раньше здесь был animateDpAsState(0.dp): анимация в ноль, которая всё равно
    // протаскивала blur-модификаторы через все элементы. Скрим и вырез рисует
    // само меню, списку делать ничего не нужно.
    val bgBlur = 0.dp

    val openChatMenu: (ChatWithUser, Rect) -> Unit = { target, bounds ->
        chatMenuAnchor.bounds = bounds
        chatMenuAnchor.highlightBounds = bounds
        chatMenuAnchor.cornerRadius = NotificationCorner
        menuTarget = target
        menuOpen = true
    }

    val matchedChats = remember(chats, searchQuery) {
        chats.filter {
            it.name?.contains(searchQuery, ignoreCase = true) == true ||
                    it.username?.contains(searchQuery, ignoreCase = true) == true
        }
    }
    val visibleChats = remember(matchedChats, filter) {
        if (filter == ChatFilter.UNREAD) matchedChats.filter { it.chat.unreadCount > 0 }
        else matchedChats
    }
    val pinnedChats = remember(visibleChats) { visibleChats.filter { it.chat.pinned } }
    val regularChats = remember(visibleChats) { visibleChats.filterNot { it.chat.pinned } }
    val unreadTotal = remember(chats) { chats.count { it.chat.unreadCount > 0 } }

    // Таббар прячем/возвращаем ровно по состоянию меню, и обязательно
    // возвращаем при уходе с экрана — иначе он остался бы скрытым навсегда.
    LaunchedEffect(menuOpen) { onTabBarSuppressedChange(menuOpen) }
    DisposableEffect(Unit) { onDispose { onTabBarSuppressedChange(false) } }

    menuTarget?.let { target ->
        val targetChat = target.chat
        VibeContextMenu(
            expanded = menuOpen,
            anchor = chatMenuAnchor,
            onDismiss = { menuOpen = false },
            actions = listOf(
                VibeMenuAction(
                    label = if (targetChat.pinned) strings.unpin else strings.pin,
                    icon = Icons.Filled.PushPin,
                    rotateIcon = true,
                    onClick = {
                        if (targetChat.pinned) viewModel.unpinChat(targetChat.interlocutorId)
                        else viewModel.pinChat(targetChat.interlocutorId)
                    }
                ),
                VibeMenuAction(
                    label = if (targetChat.isMuted) strings.unmuteNotifications
                    else strings.muteNotifications,
                    icon = if (targetChat.isMuted) Icons.Default.NotificationsActive
                    else Icons.Default.VolumeOff,
                    selected = targetChat.isMuted,
                    onClick = {
                        if (targetChat.isMuted) viewModel.unmuteUser(targetChat.interlocutorId)
                        else viewModel.muteUser(targetChat.interlocutorId)
                    }
                )
            ),
            header = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VibeAvatar(
                        name = displayNameOf(target, strings),
                        avatarUrl = target.avatarUrl,
                        seed = targetChat.interlocutorId,
                        size = 38.dp,
                        monogramFontSize = 16.sp,
                        showImage = !targetChat.isBanned && !targetChat.isFreezed,
                        contentDescription = strings.chatAvatar
                    )
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayNameOf(target, strings),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!target.username.isNullOrBlank()) {
                            Text(
                                text = "@" + target.username,
                                fontSize = 13.sp,
                                color = secondaryTextColor(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        )
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            kotlinx.coroutines.delay(300)
            viewModel.searchUsers(searchQuery)
        } else {
            viewModel.clearSearchResults()
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            hasInternet = true
        } else {
            // Connectivity checks do not belong on the main thread and do not need a 1-second cadence.
            while (!isConnected) {
                hasInternet = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    viewModel.hasInternet()
                }
                kotlinx.coroutines.delay(4_000)
            }
        }
    }

    LaunchedEffect(webSocket) {
        viewModel.attachWebSocket(webSocket)
    }

    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }

    LaunchedEffect(showToast) {
        if (showToast) {
            kotlinx.coroutines.delay(2500)
            showToast = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is com.flasskdev.vibe.ui.viewmodels.ChatUiEvent.ToastEvent -> {
                    toastMessage = event.message
                    showToast = true
                }
                is com.flasskdev.vibe.ui.viewmodels.ChatUiEvent.SpamblockError -> {
                    // Optional: Handle Spamblock error here
                }
            }
        }
    }

    val listState = rememberLazyListState()
    // iOS large-title behavior: the inline nav bar fades in while the large title scrolls away.
    val collapseProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 90f).coerceIn(0f, 1f)
        }
    }

    val navTitle = when {
        isConnected -> strings.chatsTitle
        hasInternet -> strings.connecting
        else -> strings.waitingForNetwork
    }

    val audioPlayerViewModel = com.flasskdev.vibe.LocalGlobalAudioPlayer.current
    val hazeState = remember { dev.chrisbanes.haze.HazeState() }
    var showExpandedPlayer by remember { mutableStateOf(false) }

    // Пункт 6: геометрия стопки. Считается один раз в композиции, дальше читается
    // только внутри graphicsLayer, то есть на фазе draw.
    val density = LocalDensity.current
    val statusBarInsets = WindowInsets.statusBars
    val topEdgePx = remember(density, statusBarInsets) {
        // Граница стопки = низ инлайн-строки, поэтому берём ровно её высоту.
        with(density) { statusBarInsets.getTop(this).toFloat() + TopBarHeight.toPx() }
    }
    val bottomEdgePx = remember(density) { with(density) { 88.dp.toPx() } }
    val slidePx = remember(density) { with(density) { NotificationSlide.toPx() } }
    val entrance = remember { NotificationEntrance() }
    LaunchedEffect(listState) {
        // Первое касание ленты закрывает окно тайминговых входов: дальше строки
        // появляются исключительно от позиции скролла.
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) entrance.disarm() }
    }

    val statusBarTopPx = remember(density, statusBarInsets) {
        with(density) { statusBarInsets.getTop(this).toFloat() }
    }
    val largeTitleHeightPx = remember(density) {
        with(density) { 90.dp.toPx() }
    }
    val fadeDistancePx = remember(density) {
        with(density) { 200.dp.toPx() }
    }

    Box(modifier = Modifier.fillMaxSize().background(pageBackground())) {

        // Фоновое сияние сверху (синее в темной теме, индиго в светлой).
        // При скролле вниз плавно угасает, при скролле обратно вверх — проявляется.
        VibeTopGlow(
            height = 380.dp,
            modifier = Modifier.graphicsLayer {
                val info = listState.layoutInfo
                val visible = info.visibleItemsInfo
                val scrollOffset = if (visible.isNotEmpty()) {
                    val first = visible[0]
                    when (first.index) {
                        0 -> -first.offset.toFloat()
                        1 -> statusBarTopPx - first.offset.toFloat()
                        2 -> statusBarTopPx + largeTitleHeightPx - first.offset.toFloat()
                        else -> fadeDistancePx + 50f
                    }
                } else {
                    0f
                }
                val progress = (scrollOffset / fadeDistancePx).coerceIn(0f, 1f)
                alpha = 1f - (progress * progress)
                translationY = -scrollOffset * 0.2f
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item(key = "status_bar_inset") {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }

            // Large title
            item(key = "large_title") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Передача эстафеты инлайн-строке: заголовок ужимается
                            // к ведущему краю, а не просто уезжает под неё.
                            val p = collapseProgress
                            if (p <= 0f) return@graphicsLayer
                            alpha = 1f - LargeTitleFadeEasing.transform(p)
                            val scale = 1f - (1f - LargeTitleCollapseScale) * p
                            scaleX = scale
                            scaleY = scale
                            translationY = -p * LargeTitleCollapseShift.toPx()
                            // Опора у ведущего края и базовой линии: заголовок
                            // «садится» в позицию инлайн-строки, а не в центр.
                            transformOrigin = TransformOrigin(0f, 1f)
                        }
                        .vibeOptionalBlur(bgBlur)
                        .padding(horizontal = PageInset)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = navTitle,
                            fontSize = 34.sp,
                            lineHeight = 41.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.37.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (unreadTotal > 0) {
                            Spacer(modifier = Modifier.width(9.dp))
                            UnreadTotalBadge(
                                count = unreadTotal,
                                height = 24.dp,
                                fontSize = 14.sp,
                                description = strings.filterUnread
                            )
                        }
                        if (!isConnected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TypingIndicator(
                                dotSize = 6.dp,
                                dotColor = secondaryTextColor()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Search field with Cancel
            item(key = "search_field") {
                Box(modifier = Modifier.fillMaxWidth().vibeOptionalBlur(bgBlur)) {
                    IOSSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        liquidState = liquidState,
                        strings = strings
                    )
                }
            }

            // Segmented control: All / Unread
            if (chats.isNotEmpty()) {
                item(key = "segmented_filter") {
                    Box(modifier = Modifier.fillMaxWidth().vibeOptionalBlur(bgBlur)) {
                        IOSSegmentedControl(
                            selected = filter,
                            onSelect = { filter = it },
                            allLabel = strings.filterAll,
                            unreadLabel = if (unreadTotal > 0) {
                                strings.filterUnreadCount(unreadTotal)
                            } else {
                                strings.filterUnread
                            }
                        )
                    }
                }
            }

            item(key = "mini_player") {
                com.flasskdev.vibe.ui.components.GlobalMiniPlayer(
                    viewModel = audioPlayerViewModel,
                    hazeState = hazeState,
                    onExpand = { showExpandedPlayer = true }
                )
            }

            // Empty states, presented as a grouped card
            if (chats.isEmpty() && searchQuery.isBlank()) {
                item(key = "empty_state") {
                    GroupedPlaceholderCard(
                        title = strings.chatsEmptyTitle,
                        subtitle = strings.chatsEmptySubtitle,
                        hint = strings.chatsEmptyHint,
                        showIcon = true
                    )
                }
            } else if (visibleChats.isEmpty() && filter == ChatFilter.UNREAD && searchQuery.isBlank()) {
                item(key = "empty_unread") {
                    GroupedPlaceholderCard(
                        title = strings.chatsNoUnreadTitle,
                        subtitle = strings.chatsNoUnreadSubtitle,
                        hint = null,
                        showIcon = true
                    )
                }
            } else if (searchQuery.isNotBlank() && visibleChats.isEmpty() && searchResults.isEmpty()) {
                item(key = "no_results") {
                    GroupedPlaceholderCard(
                        title = strings.chatsSearchNoResultsTitle,
                        subtitle = strings.chatsSearchNoResultsSubtitle(searchQuery),
                        hint = null,
                        showIcon = false
                    )
                }
            }

            // Pinned card
            if (pinnedChats.isNotEmpty()) {
                item(key = "header_pinned") { Box(Modifier.vibeOptionalBlur(bgBlur)) { GroupHeader(title = strings.chatsSectionPinned) } }
                itemsIndexed(
                    pinnedChats,
                    key = { _, item -> "pinned_" + item.chat.interlocutorId }
                ) { index, chatWithUser ->
                    val itemKey = remember(chatWithUser.chat.interlocutorId) {
                        "pinned_" + chatWithUser.chat.interlocutorId
                    }
                    // Выделение зажатого чата делает вырез в скриме контекстного меню.
                    // Прежний shadowElevation + zIndex на самой строке рисовал тень ПОД скримом,
                    // и она вылазила из выреза тёмным ореолом.
                    ChatItemView(
                        modifier = Modifier
                            .notificationEntrance(
                                entrance = entrance,
                                chatId = chatWithUser.chat.interlocutorId,
                                index = index,
                                slidePx = slidePx
                            )
                            .notificationScrollDepth(listState, itemKey, topEdgePx, bottomEdgePx),
                        chatWithUser = chatWithUser,
                        isTyping = typingUsers[chatWithUser.chat.interlocutorId] == true,
                        onClick = {
                            onChatClick(
                                chatWithUser.chat.interlocutorId,
                                displayNameOf(chatWithUser, strings)
                            )
                        },
                        onLongClick = { bounds -> openChatMenu(chatWithUser, bounds) },
                        onTogglePin = {
                            if (chatWithUser.chat.pinned) viewModel.unpinChat(chatWithUser.chat.interlocutorId)
                            else viewModel.pinChat(chatWithUser.chat.interlocutorId)
                        },
                        onToggleMute = {
                            if (chatWithUser.chat.isMuted) viewModel.unmuteUser(chatWithUser.chat.interlocutorId)
                            else viewModel.muteUser(chatWithUser.chat.interlocutorId)
                        }
                    )
                }
            }

            // Main card
            if (regularChats.isNotEmpty()) {
                item(key = "header_all") {
                    Box(Modifier.vibeOptionalBlur(bgBlur)) {
                        GroupHeader(
                            title = if (filter == ChatFilter.UNREAD) strings.filterUnread
                            else strings.chatsSectionAll
                        )
                    }
                }
                itemsIndexed(
                    regularChats,
                    key = { _, item -> "chat_" + item.chat.interlocutorId }
                ) { index, chatWithUser ->
                    val itemKey = remember(chatWithUser.chat.interlocutorId) {
                        "chat_" + chatWithUser.chat.interlocutorId
                    }
                    ChatItemView(
                        modifier = Modifier
                            .notificationEntrance(
                                entrance = entrance,
                                chatId = chatWithUser.chat.interlocutorId,
                                index = index + pinnedChats.size,
                                slidePx = slidePx
                            )
                            .notificationScrollDepth(listState, itemKey, topEdgePx, bottomEdgePx),
                        chatWithUser = chatWithUser,
                        isTyping = typingUsers[chatWithUser.chat.interlocutorId] == true,
                        onClick = {
                            onChatClick(
                                chatWithUser.chat.interlocutorId,
                                displayNameOf(chatWithUser, strings)
                            )
                        },
                        onLongClick = { bounds -> openChatMenu(chatWithUser, bounds) },
                        onTogglePin = {
                            if (chatWithUser.chat.pinned) viewModel.unpinChat(chatWithUser.chat.interlocutorId)
                            else viewModel.pinChat(chatWithUser.chat.interlocutorId)
                        },
                        onToggleMute = {
                            if (chatWithUser.chat.isMuted) viewModel.unmuteUser(chatWithUser.chat.interlocutorId)
                            else viewModel.muteUser(chatWithUser.chat.interlocutorId)
                        }
                    )
                }
                item(key = "footer_count") { Box(Modifier.vibeOptionalBlur(bgBlur)) { GroupFooter(text = strings.chatsCountFooter(chats.size)) } }
            }

            // Global search card
            if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                item(key = "header_global") { GroupHeader(title = strings.globalSearchResults) }
                itemsIndexed(
                    searchResults,
                    key = { _, item -> "search_" + item.id }
                ) { index, result ->
                    UserSearchResultView(
                        user = result,
                        shape = groupShape(index, searchResults.size),
                        showDivider = index != searchResults.lastIndex,
                        onClick = {
                            val name = result.name
                                ?: result.username
                                ?: strings.userFallback(result.id)
                            onChatClick(result.id, name)
                        }
                    )
                }
            }
        }

        // Inline navigation bar that fades in on scroll
        val barBase = pageBackground()
        val barTailColor = if (isDarkSurface()) Color.Black.copy(alpha = 0.30f)
        else Color.Black.copy(alpha = 0.055f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // Инлайн-строка приходит на второй половине жеста, когда
                    // крупный заголовок уже почти растворился.
                    alpha = InlineBarFadeEasing.transform(collapseProgress)
                }
                .vibeOptionalBlur(bgBlur)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Вертикальный градиент вместо плоской заливки: у статус-бара
                    // подложка плотная, к разделителю чуть отпускает, поэтому
                    // карточки уходят под неё, а не упираются в стену.
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                barBase,
                                barBase.copy(alpha = 0.97f),
                                barBase.copy(alpha = 0.90f)
                            )
                        )
                    )
            ) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TopBarHeight)
                        .padding(horizontal = PageInset)
                        .graphicsLayer {
                            // Встречное движение: заголовок подъезжает снизу,
                            // ровно оттуда, куда «сел» крупный.
                            val p = InlineBarFadeEasing.transform(collapseProgress)
                            translationY = (1f - p) * InlineTitleRise.toPx()
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = navTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        // Тайтлы навбара идут с отрицательным трекингом, иначе
                        // на 17sp semibold строка выглядит разреженной.
                        letterSpacing = (-0.24).sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (unreadTotal > 0) {
                        Spacer(modifier = Modifier.width(7.dp))
                        UnreadTotalBadge(
                            count = unreadTotal,
                            height = 19.dp,
                            fontSize = 12.sp,
                            description = strings.filterUnread
                        )
                    }
                    if (!isConnected) {
                        Spacer(modifier = Modifier.width(7.dp))
                        TypingIndicator(dotSize = 5.dp, dotColor = secondaryTextColor())
                    }
                }
            }

            // Разделитель и хвост включаются в самом конце схлопывания: пока
            // заголовок ещё виден, линия под ним выглядела бы лишней.
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = segment(collapseProgress, 0.55f, 1f)
                }
            ) {
                HorizontalDivider(thickness = 0.5.dp, color = separatorColor())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TopBarShadowHeight)
                        .background(
                            Brush.verticalGradient(listOf(barTailColor, Color.Transparent))
                        )
                )
            }
        }

        if (showExpandedPlayer) {
            com.flasskdev.vibe.ui.components.ExpandedAudioPlayerSheet(
                viewModel = audioPlayerViewModel,
                hazeState = hazeState,
                onDismiss = { showExpandedPlayer = false }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            com.flasskdev.vibe.ui.components.VibeToast(
                message = toastMessage,
                isVisible = showToast,
                onDismiss = { showToast = false }
            )
        }
    }
}

/* ---------------------------------------------------------------------------
 *  Search field
 * ------------------------------------------------------------------------- */

@Composable
private fun IOSSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    liquidState: LiquidState,
    strings: VibeStrings
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val showCancel = focused || query.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PageInset)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp
            ),
            cursorBrush = SolidColor(VibePrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .then(if (com.flasskdev.vibe.ui.theme.VibeEffects.liquid) Modifier.liquid(liquidState) {
                    refraction = 0.2f
                    curve = 0.2f
                    edge = 0.08f
                } else Modifier)
                .background(IOSFill.copy(alpha = if (isDarkSurface()) 0.20f else 0.10f))
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = IOSGray,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = strings.searchPlaceholder,
                                color = IOSGray,
                                fontSize = 17.sp,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = strings.chatsSearchClearField,
                            tint = IOSGray,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { onQueryChange("") }
                        )
                    }
                }
            }
        )

        AnimatedVisibility(
            visible = showCancel,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Text(
                text = strings.chatsSearchCancel,
                color = VibePrimary,
                fontSize = 17.sp,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clickable {
                        onQueryChange("")
                        focusManager.clearFocus()
                    }
            )
        }
    }
}

/* ---------------------------------------------------------------------------
 *  Segmented control
 * ------------------------------------------------------------------------- */

@Composable
private fun IOSSegmentedControl(
    selected: ChatFilter,
    onSelect: (ChatFilter) -> Unit,
    allLabel: String,
    unreadLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PageInset)
            .padding(bottom = 6.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(IOSFill.copy(alpha = if (isDarkSurface()) 0.24f else 0.12f))
            .padding(2.dp)
    ) {
        SegmentedItem(
            label = allLabel,
            active = selected == ChatFilter.ALL,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(ChatFilter.ALL) }
        )
        SegmentedItem(
            label = unreadLabel,
            active = selected == ChatFilter.UNREAD,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(ChatFilter.UNREAD) }
        )
    }
}

@Composable
private fun SegmentedItem(
    label: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(7.dp)
    val dark = isDarkSurface()
    val pillColor by animateColorAsState(
        targetValue = when {
            !active -> Color.Transparent
            dark -> Color(0xFF636366)
            else -> Color.White
        },
        animationSpec = tween(180),
        label = "segmentPill"
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(if (active) Modifier.shadow(2.dp, pillShape) else Modifier)
            .clip(pillShape)
            .background(pillColor)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) MaterialTheme.colorScheme.onBackground else secondaryTextColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ---------------------------------------------------------------------------
 *  Grouped headers, footers, placeholder card
 * ------------------------------------------------------------------------- */

@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp,
        color = secondaryTextColor(),
        modifier = Modifier.padding(
            start = PageInset + CardContentInset,
            end = PageInset,
            top = 18.dp,
            bottom = 7.dp
        )
    )
}

@Composable
private fun GroupFooter(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = secondaryTextColor(),
        modifier = Modifier.padding(
            start = PageInset + CardContentInset,
            end = PageInset,
            top = 8.dp
        )
    )
}

@Composable
private fun GroupedPlaceholderCard(
    title: String,
    subtitle: String,
    hint: String?,
    showIcon: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PageInset, vertical = 8.dp)
            .cardSurface(RoundedCornerShape(CardCorner))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showIcon) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(IOSFill.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = secondaryTextColor(),
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = secondaryTextColor(),
            textAlign = TextAlign.Center
        )
        if (hint != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = hint,
                fontSize = 15.sp,
                color = VibePrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ---------------------------------------------------------------------------
 *  Avatar
 * ------------------------------------------------------------------------- */

@Composable
private fun VibeAvatar(
    name: String,
    avatarUrl: String?,
    seed: Int,
    size: Dp,
    monogramFontSize: TextUnit,
    showImage: Boolean,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBrush(seed)),
        contentAlignment = Alignment.Center
    ) {
        if (showImage && !avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontSize = monogramFontSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/* ---------------------------------------------------------------------------
 *  Global search result row
 * ------------------------------------------------------------------------- */

@Composable
fun UserSearchResultView(
    user: com.flasskdev.vibe.data.UserSearchResult,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(CardCorner),
    showDivider: Boolean = true
) {
    val strings = LocalVibeStrings.current
    val name = when {
        user.isBanned -> strings.accountDeleted
        user.isFreezed -> strings.accountFrozen
        else -> user.name ?: user.username ?: strings.userFallback(user.id)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressOverlay by animateColorAsState(
        targetValue = if (pressed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
        else Color.Transparent,
        animationSpec = tween(120),
        label = "searchRowPress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PageInset)
            .cardSurface(shape, pressOverlay)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CardContentInset, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VibeAvatar(
                name = name,
                avatarUrl = user.avatarUrl,
                seed = user.id,
                size = SearchAvatarSize,
                monogramFontSize = 19.sp,
                showImage = !user.isBanned && !user.isFreezed,
                contentDescription = strings.chatAvatar
            )

            Spacer(modifier = Modifier.width(AvatarGap))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user.isBanned) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = strings.accountDeleted,
                            tint = IOSGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (user.isFreezed) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = strings.accountFrozen,
                            tint = Color(0xFF87CEEB),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    com.flasskdev.vibe.ui.components.UserBadgesRow(
                        isVerified = user.isVerified,
                        isDeveloper = user.isDeveloper,
                        isBot = user.isBot,
                        isBanned = user.isBanned,
                        isFreezed = user.isFreezed,
                        badgeSize = 14.dp
                    )
                }
                if (!user.username.isNullOrBlank()) {
                    Text(
                        text = "@" + user.username,
                        color = secondaryTextColor(),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = SearchSeparatorInset),
                thickness = 0.5.dp,
                color = separatorColor()
            )
        }
    }
}

/* ---------------------------------------------------------------------------
 *  Chat row: grouped card cell with iOS swipe actions
 * ------------------------------------------------------------------------- */

private fun displayNameOf(chatWithUser: ChatWithUser, strings: VibeStrings): String = when {
    chatWithUser.chat.isBanned -> strings.accountDeleted
    chatWithUser.chat.isFreezed -> strings.accountFrozen
    else -> chatWithUser.name
        ?: chatWithUser.username
        ?: strings.userFallback(chatWithUser.chat.interlocutorId)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItemView(
    chatWithUser: ChatWithUser,
    isTyping: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (Rect) -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleMute: () -> Unit = {}
) {
    // Пункт 6: форма больше не зависит от позиции в группе - каждый чат
    // самостоятельная плашка, поэтому groupShape здесь не нужен.
    val shape = RoundedCornerShape(NotificationCorner)
    val chat = chatWithUser.chat
    val strings = LocalVibeStrings.current
    val name = displayNameOf(chatWithUser, strings)
    val hasUnread = chat.unreadCount > 0
    val timeFormatted = remember(chat.timestamp) { formatChatTimestamp(chat.timestamp, strings) }

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val revealPx = with(density) { (SwipeActionWidth * 2).toPx() }
    val offsetX = remember { Animatable(0f) }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressOverlay by animateColorAsState(
        targetValue = if (pressed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
        else Color.Transparent,
        animationSpec = tween(120),
        label = "chatRowPress"
    )
    // Границы именно этой строки: меню должно вырасти из зажатого чата,
    // а не из общего для всего списка якоря.
    var rowBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PageInset, vertical = NotificationGap)
            .clip(shape)
    ) {
        // Swipe action layer, revealed by dragging the row to the left.
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End
        ) {
            SwipeActionButton(
                label = if (chat.isMuted) strings.actionUnmuteShort else strings.actionMuteShort,
                icon = if (chat.isMuted) Icons.Default.NotificationsActive else Icons.Default.VolumeOff,
                background = IOSOrange,
                onClick = {
                    onToggleMute()
                    scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.9f)) }
                }
            )
            SwipeActionButton(
                label = if (chat.pinned) strings.unpin else strings.pin,
                icon = Icons.Filled.PushPin,
                background = VibePrimary,
                rotateIcon = true,
                onClick = {
                    onTogglePin()
                    scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.9f)) }
                }
            )
        }

        // Card cell
        Column(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                .notificationCardSurface(shape, pressOverlay)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f))
                        }
                    },
                    onDragStopped = {
                        val target = if (offsetX.value < -revealPx / 2f) -revealPx else 0f
                        offsetX.animateTo(target, spring(dampingRatio = 0.9f))
                    }
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (offsetX.value != 0f) {
                            scope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.9f)) }
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick(rowBounds)
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CardContentInset, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(AvatarSize)) {
                    VibeAvatar(
                        name = name,
                        avatarUrl = chatWithUser.avatarUrl,
                        seed = chat.interlocutorId,
                        size = AvatarSize,
                        monogramFontSize = 25.sp,
                        showImage = !chat.isBanned && !chat.isFreezed && !chat.isBlockedByUser,
                        contentDescription = strings.chatAvatar
                    )

                    if (chatWithUser.isOnline == true && chatWithUser.isBot != true &&
                        !chat.isBanned && !chat.isFreezed && !chat.isBlockedByUser && !chat.isBlockedByMe
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .background(VibeOnlineGreen, CircleShape)
                                .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AvatarGap))

                Column(modifier = Modifier.weight(1f)) {

                    // Title line: name, mute glyph, badges, timestamp
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (chat.isMuted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = strings.a11yMutedChat,
                                tint = secondaryTextColor(),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        com.flasskdev.vibe.ui.components.UserBadgesRow(
                            isVerified = chat.isVerified,
                            isDeveloper = chat.isDeveloper,
                            isBot = chatWithUser.isBot == true,
                            isBanned = chat.isBanned,
                            isFreezed = chat.isFreezed,
                            badgeSize = 15.dp
                        )

                        }

                        Text(
                            text = timeFormatted,
                            fontSize = 13.sp,
                            color = secondaryTextColor(),
                            maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Preview line and trailing indicators
                    Row(verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (isTyping) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.typing,
                                        color = VibePrimary,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TypingIndicator()
                                }
                            } else {
                                ChatPreviewLine(chatWithUser = chatWithUser, strings = strings)
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .height(22.dp)
                                        .widthIn(min = 22.dp)
                                        .background(
                                            if (chat.isMuted) IOSGray else VibePrimary,
                                            CircleShape
                                        )
                                        .padding(horizontal = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                            }
                            if (chat.pinned) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = strings.a11yPinnedChat,
                                    tint = secondaryTextColor(),
                                    modifier = Modifier
                                        .padding(top = if (hasUnread) 4.dp else 2.dp)
                                        .size(14.dp)
                                        .rotate(45f)
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun SwipeActionButton(
    label: String,
    icon: ImageVector,
    background: Color,
    onClick: () -> Unit,
    rotateIcon: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(SwipeActionWidth)
            .fillMaxHeight()
            .background(background)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = if (rotateIcon) Modifier.size(22.dp).rotate(45f) else Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/* ---------------------------------------------------------------------------
 *  Message preview (single icon per type, fully localized text)
 * ------------------------------------------------------------------------- */

@Composable
private fun ChatPreviewLine(chatWithUser: ChatWithUser, strings: VibeStrings) {
    val chat = chatWithUser.chat
    val previewColor = secondaryTextColor()

    if (!chat.draft.isNullOrBlank()) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = IOSRed)) {
                    append(strings.draftLabel)
                }
                append(chat.draft)
            },
            color = previewColor,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            // Пункт 6: превью строго в одну строку, как в баннере уведомления.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val attachments = chat.lastMessageAttachments
    val hasAttachments = !attachments.isNullOrEmpty()
    val isVoiceMessage = chat.lastMessage.startsWith("duration:")
    val isVideoMessage = chat.lastMessage.startsWith("video_message:")
    val isAudio = hasAttachments && !isVoiceMessage && !isVideoMessage &&
            com.flasskdev.vibe.utils.AttachmentUtils.isPlayableAudio(attachments!![0])
    val isFile = hasAttachments && !isVoiceMessage && !isVideoMessage && !isAudio &&
            !com.flasskdev.vibe.utils.AttachmentUtils.isImage(attachments!![0]) &&
            !com.flasskdev.vibe.utils.AttachmentUtils.isPlayableVideo(attachments!![0])

    // Do not start network/metadata extraction from every recycled lazy row.
    // A cached value keeps previews informative while the detailed chat player owns full metadata loading.
    val audioPreviewUrl = remember(attachments) {
        attachments?.firstOrNull()?.let { firstAtt -> attachmentUrl(firstAtt) }
    }
    val cachedAudioMetadata = remember(audioPreviewUrl) {
        audioPreviewUrl?.let { com.flasskdev.vibe.utils.AudioMetadataHelper.getCachedMetadata(it) }
    }
    val audioTitle = cachedAudioMetadata?.displayTitle
    val audioArtist = cachedAudioMetadata?.displayArtist

    val displayText = when {
        chat.lastMessage.startsWith("\$\$SYSTEM\$\$PINNED_MESSAGE|") -> {
            val parts = chat.lastMessage.substringAfter("\$\$SYSTEM\$\$PINNED_MESSAGE|").split("|")
            val senderN = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: strings.someoneLabel
            val msgContent = parts.getOrNull(1) ?: ""
            strings.pinnedMessageSystemText(senderN, msgContent)
        }
        isVoiceMessage -> {
            val ms = chat.lastMessage.substringAfter("duration:").toLongOrNull() ?: 0L
            strings.previewVoiceMessage(formatDuration(ms))
        }
        isVideoMessage -> {
            val ms = chat.lastMessage.substringAfter("video_message:").toLongOrNull() ?: 0L
            strings.previewVideoMessage(formatDuration(ms))
        }
        isAudio -> {
            when {
                chat.lastMessage.isNotBlank() -> chat.lastMessage
                audioTitle != null && audioArtist != null && audioArtist != UNKNOWN_ARTIST_TAG ->
                    strings.previewAudioTrack(audioArtist, audioTitle)
                audioTitle != null -> audioTitle
                else -> strings.previewAudioLoading
            }
        }
        isFile -> {
            val fn = com.flasskdev.vibe.utils.AttachmentUtils.getFilename(attachments!![0])
            if (chat.lastMessage.isNotBlank()) chat.lastMessage else fn
        }
        hasAttachments -> {
            val count = attachments!!.size
            val hasCaption = chat.lastMessage.isNotBlank()
            val firstIsVideo = com.flasskdev.vibe.utils.AttachmentUtils.isPlayableVideo(attachments[0])
            if (count == 1) {
                when {
                    hasCaption -> chat.lastMessage
                    firstIsVideo -> strings.typeVideo
                    else -> strings.typePhoto
                }
            } else {
                val rem = count - 1
                when {
                    hasCaption -> strings.previewMoreWithCaption(rem, chat.lastMessage)
                    firstIsVideo -> strings.previewMoreVideos(rem)
                    else -> strings.previewMorePhotos(rem)
                }
            }
        }
        chat.lastMessage.isBlank() -> strings.chatHistoryEmpty
        else -> chat.lastMessage
    }

    // Переводы строк убиваются, иначе однострочный Text обрежется на первом же \n
    // и вместо текста получится пустая строка.
    val previewText = displayText.replace("\n", " ").trim()

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (chat.isLastMessageMine) {
            Icon(
                imageVector = if (chat.isLastMessageRead) Icons.Default.DoneAll else Icons.Default.Check,
                contentDescription = null,
                tint = if (chat.isLastMessageRead) VibePrimary else previewColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        when {
            isVoiceMessage -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = strings.typeVoice,
                    tint = VibePrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            isVideoMessage -> {
                val videoAttachment = attachments?.firstOrNull()
                if (videoAttachment != null) {
                    com.flasskdev.vibe.ui.components.VideoCover(
                        source = attachmentModel(videoAttachment),
                        modifier = Modifier.size(20.dp).clip(CircleShape),
                        showPlayIcon = false
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = strings.typeVideoMessage,
                        tint = VibePrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
            isAudio -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = strings.typeAudio,
                    tint = VibePrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            isFile -> {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = strings.typeFile,
                    tint = VibePrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            hasAttachments -> {
                val firstAtt = attachments!![0]
                val model = attachmentModel(firstAtt)
                if (com.flasskdev.vibe.utils.AttachmentUtils.isPlayableVideo(firstAtt)) {
                    com.flasskdev.vibe.ui.components.VideoCover(
                        source = model,
                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)),
                        showPlayIcon = false
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(model).crossfade(true).build(),
                        contentDescription = strings.inputAttachmentPreview,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
        }

        if (com.flasskdev.vibe.utils.TextFormatting.hasFormatting(previewText)) {
            com.flasskdev.vibe.ui.components.FormattedText(
                text = previewText,
                baseColor = previewColor,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                interactive = false,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = previewText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = previewColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun attachmentUrl(attachment: String): String {
    val isLocal = attachment.startsWith("/") ||
            attachment.startsWith("content://") ||
            attachment.contains("cacheDir")
    return if (isLocal || attachment.startsWith("http")) attachment
    else "https://flasskdev.alwaysdata.net/api/upload/file/" + attachment
}

private fun attachmentModel(attachment: String): Any {
    val isLocal = attachment.startsWith("/") ||
            attachment.startsWith("content://") ||
            attachment.contains("cacheDir")
    return if (isLocal) java.io.File(attachment) else attachmentUrl(attachment)
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return String.format(Locale.US, "%d:%02d", totalSec / 60, totalSec % 60)
}

/* ---------------------------------------------------------------------------
 *  Timestamp
 * ------------------------------------------------------------------------- */

private fun formatChatTimestamp(timestamp: Long, strings: VibeStrings): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    return when {
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) -> {
            timeFormat.format(Date(timestamp))
        }
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1 -> {
            strings.dateYesterday
        }
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == msgTime.get(Calendar.WEEK_OF_YEAR) -> {
            val dayFormat = SimpleDateFormat("EEE", Locale(strings.locale))
            dayFormat.format(Date(timestamp))
        }
        else -> {
            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}