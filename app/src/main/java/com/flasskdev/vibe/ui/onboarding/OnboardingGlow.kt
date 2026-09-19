package com.flasskdev.vibe.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flasskdev.vibe.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/* ============================================================================
 *  ПУНКТ 10 — СВЕТЯЩИЙСЯ КРУГ ПОЗАДИ КАРТИНОК ТУТОРИАЛА
 * ============================================================================
 *
 *  Фиолетово-сине-белое свечение, живое, но ненавязчивое.
 *
 *  Как это сделано и почему именно так:
 *   - три радиальных градиента разного радиуса, вращающиеся с РАЗНОЙ
 *     скоростью и в разные стороны. За счёт этого рисунок никогда точно
 *     не повторяется, и глаз не замечает цикла анимации;
 *   - Modifier.blur вместо десятка полупрозрачных окружностей: одна
 *     операция на GPU вместо оверлея слоёв (важно для слабых устройств);
 *   - CompositingStrategy.Offscreen нужен, иначе blur на Android 12-
 *     не применяется к содержимому слоя;
 *   - вся анимация в одном rememberInfiniteTransition: три отдельных
 *     заставили бы Compose держать три независимых кадровых цикла.
 *
 *  Использование в OnboardingScreen:
 *
 *      Box(contentAlignment = Alignment.Center) {
 *          OnboardingGlow(intensity = if (isCurrentPage) 1f else 0.4f)
 *          Image(painter = ..., contentDescription = null)   // картинка страницы
 *      }
 * ========================================================================== */

@Composable
fun BoxScope.OnboardingGlow(
    modifier: Modifier = Modifier,
    /** 0f..1f — насколько ярко светится. Удобно гасить для соседних страниц пейджера. */
    intensity: Float = 1f,
    /** Смещение фазы, чтобы у каждой страницы туториала свой рисунок свечения. */
    phase: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "glow")

    val slow by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "slow"
    )
    val medium by transition.animateFloat(
        initialValue = 360f, targetValue = 0f,      // против часовой
        animationSpec = infiniteRepeatable(tween(17_000, easing = LinearEasing)),
        label = "medium"
    )
    // «Дыхание»: круг то расширяется, то сжимается.
    val breathe by transition.animateFloat(
        initialValue = 0.88f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(6_500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    Box(
        modifier
            .matchParentSize()
            .align(Alignment.Center)
            .graphicsLayer {
                alpha = intensity.coerceIn(0f, 1f)
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .blur(48.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = minOf(size.width, size.height) * 0.42f * breathe

            fun orbit(angleDeg: Float, distance: Float): Offset {
                val rad = Math.toRadians((angleDeg + phase * 137f).toDouble())
                return Offset(cx + cos(rad).toFloat() * distance, cy + sin(rad).toFloat() * distance)
            }

            // --- слой 1: белое ядро (самое яркое, самое маленькое) ---
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        VibeGlowWhite.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = base * 0.62f
                ),
                radius = base * 0.62f,
                center = Offset(cx, cy)
            )

            // --- слой 2: фиолетовое гало, вращается по часовой ---
            val violetCenter = orbit(slow, base * 0.20f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VibeViolet.copy(alpha = 0.72f),
                        VibeVioletDeep.copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    center = violetCenter,
                    radius = base * 1.02f
                ),
                radius = base * 1.02f,
                center = violetCenter,
                blendMode = BlendMode.Plus     // сложение света, а не перекрытие
            )

            // --- слой 3: синее гало, вращается против часовой ---
            val blueCenter = orbit(medium, base * 0.26f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VibeIndigo.copy(alpha = 0.66f),
                        VibeSky.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = blueCenter,
                    radius = base * 1.14f
                ),
                radius = base * 1.14f,
                center = blueCenter,
                blendMode = BlendMode.Plus
            )

            // --- слой 4: тонкий внешний ободок, чтобы круг читался как круг ---
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(VibeViolet, VibeIndigo, VibeSky, VibeGlowWhite, VibeViolet),
                    center = Offset(cx, cy)
                ),
                radius = base * 1.18f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = base * 0.055f),
                alpha = 0.30f
            )
        }
    }
}

/**
 * Готовая обёртка: свечение + картинка страницы туториала.
 * Заменяет в OnboardingScreen конструкцию `Image(...)` на эту.
 */
@Composable
fun GlowingOnboardingImage(
    pageIndex: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Соседние страницы светятся тускло — взгляд тянется к активной.
    val intensity by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.35f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "glowIntensity"
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        OnboardingGlow(intensity = intensity, phase = pageIndex * 0.37f)
        content()
    }
}