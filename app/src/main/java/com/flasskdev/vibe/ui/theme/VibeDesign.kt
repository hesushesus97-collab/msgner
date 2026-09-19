package com.flasskdev.vibe.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  ПУНКТ 6 — ЕДИНАЯ ДИЗАЙН-СИСТЕМА
 *  ---------------------------------------------------------------------------
 *  Проблема, которую это решает: по проекту раскиданы 40+ разных значений
 *  скруглений (8, 10, 12, 14, 16, 18, 20, 22, 24, 28...), у каждой панельки
 *  свой alpha у фона и своя тень. Из-за этого интерфейс выглядит собранным
 *  из кусков. Ниже — единая шкала, на которую переведены все переработанные
 *  экраны.
 * ========================================================================== */

object VibeRadius {
    val xs = 6.dp      // чипы, маленькие бейджи, иконки настроек
    val sm = 10.dp     // поля ввода, iOS grouped cells
    val md = 12.dp     // карточки, ячейки
    val lg = 14.dp     // панели, диалоги
    val xl = 20.dp     // bottom sheet, крупные карточки
    val pill = 100.dp  // капсулы (iOS-style pill)
}

object VibeSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val section = 32.dp
    /** Единый горизонтальный отступ контента на всех экранах. */
    val screen = 16.dp
}

object VibeElevation {
    val flat = 0.dp
    val raised = 2.dp
    val floating = 8.dp
    val modal = 16.dp
}

/** Пружины, а не линейные tween: интерфейс ощущается «живым», а не дёрганым. */
object VibeMotion {
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    fun <T> snappy() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    fun <T> gentle() = spring<T>(dampingRatio = 1f, stiffness = Spring.StiffnessLow)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

    fun <T> quick() = tween<T>(180, easing = standardEasing)
    fun <T> normal() = tween<T>(280, easing = emphasizedEasing)
    fun <T> slow() = tween<T>(420, easing = emphasizedEasing)
}

/* ---------------------------------------------------------------------------
 *  Палитра «фиолетово-сине-белый» — используется в онбординге (пункт 10),
 *  в развёрнутом плеере (пункт 6) и в акцентах 2FA (пункт 9).
 * ------------------------------------------------------------------------- */
val VibeViolet = Color(0xFF7C5CFF)
val VibeVioletDeep = Color(0xFF5B3FD9)
val VibeIndigo = Color(0xFF4C6FFF)
val VibeSky = Color(0xFF5AC8FA)
val VibeGlowWhite = Color(0xFFF3F0FF)

val VibeAuroraGradient = listOf(VibeViolet, VibeIndigo, VibeSky)
val VibeAuroraSoft = listOf(
    VibeViolet.copy(alpha = 0.55f),
    VibeIndigo.copy(alpha = 0.40f),
    VibeGlowWhite.copy(alpha = 0.22f)
)

@Immutable
data class VibeGlassStyle(
    val fill: Color,
    val border: Color,
    val highlight: Color
)

@Composable
@ReadOnlyComposable
fun glassStyle(dark: Boolean = MaterialTheme.colorScheme.background.luminanceIsDark()): VibeGlassStyle =
    if (dark) VibeGlassStyle(
        fill = Color(0xFF1C1C1E).copy(alpha = 0.86f),
        border = Color.White.copy(alpha = 0.10f),
        highlight = Color.White.copy(alpha = 0.06f)
    ) else VibeGlassStyle(
        fill = Color.White.copy(alpha = 0.90f),
        border = Color.Black.copy(alpha = 0.06f),
        highlight = Color.White.copy(alpha = 0.60f)
    )

fun Color.luminanceIsDark(): Boolean =
    (0.299 * red + 0.587 * green + 0.114 * blue) < 0.5

/**
 * Стандартная «стеклянная» поверхность. Раньше в каждом файле рисовали
 * свой Box с Brush.verticalGradient — отсюда разнобой.
 */
fun Modifier.vibeGlass(
    style: VibeGlassStyle,
    radius: Dp = VibeRadius.lg,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(
        Brush.verticalGradient(listOf(style.highlight, Color.Transparent))
    )
    .background(style.fill)
    .border(borderWidth, style.border, RoundedCornerShape(radius))

fun Modifier.vibeCard(
    style: VibeGlassStyle,
    radius: Dp = VibeRadius.md
): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(style.fill)
    .border(1.dp, style.border, RoundedCornerShape(radius))

/** Единые отступы содержимого экранов и листов. */
object VibeInsets {
    val screen = PaddingValues(horizontal = VibeSpacing.screen)
    val sheet = PaddingValues(horizontal = VibeSpacing.lg, vertical = VibeSpacing.xl)
    val listItem = PaddingValues(horizontal = VibeSpacing.lg, vertical = VibeSpacing.md)
}

/** Минимальный размер тач-таргета. Много где в проекте кнопки были 24-32 dp. */
val VibeTouchTarget = 48.dp

object VibeType {
    val titleLarge = 28.sp
    val title = 20.sp
    val body = 16.sp
    val caption = 13.sp
    val micro = 11.sp
}

/**
 * Верхнее фоновое сияние экранов (Чаты, Настройки, Профиль).
 * В тёмной теме — глубокое синее сияние (iOS Blue / 0xFF0A84FF).
 * В светлой теме — мягкое индиго/лавандовое сияние другого цвета (0xFF6366F1).
 */
@Composable
fun VibeTopGlow(
    modifier: Modifier = Modifier,
    height: Dp = 380.dp
) {
    if (!VibeEffects.glowEnabled) return
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    // Тёмная тема: насыщенный синий (#0A84FF)
    // Светлая тема: благородный индиго/фиолетовый (#6366F1)
    val glowColor = if (isDark) {
        Color(0xFF0A84FF)
    } else {
        Color(0xFF6366F1)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isDark) 0.28f else 0.16f),
                        glowColor.copy(alpha = if (isDark) 0.10f else 0.05f),
                        Color.Transparent
                    )
                )
            )
    )
}
