package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.VibeEffects
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.theme.VibeSystemGray
import com.flasskdev.vibe.ui.theme.luminanceIsDark
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import java.util.Locale

/* ============================================================================
 *  ТАББАР — LIQUID GLASS v2
 *  ---------------------------------------------------------------------------
 *  Что было не так в v1:
 *   1. Оболочка была НЕПРОЗРАЧНОЙ (surface с alpha 0.98 -> 0.90). Никакого
 *      стекла там физически не происходило: под ней ничего не просвечивало,
 *      это была просто серая плашка со скруглением.
 *   2. Refraction из библиотеки liquid применялся только к полю поиска в
 *      списке чатов, а к таббару — нет. Самый заметный элемент интерфейса
 *      не использовал эффект, который для него и подключали.
 *   3. Блоб дёргался: keyframes на 540 мс с четырьмя переломами поверх двух
 *      пружин — три анимации боролись за одно значение.
 *
 *  Что здесь:
 *   - настоящая refraction-подложка (liquid), поверх неё полупрозрачный tint,
 *     так что фон реально читается сквозь таббар;
 *   - многослойное стекло: refraction -> tint -> верхний блик -> нижняя
 *     тень-градиент -> градиентная рамка;
 *   - блоб на ОДНОЙ упругой модели (leader/tail для растяжения при перелёте
 *     плюс один короткий squash), без keyframes-борьбы;
 *   - deform-масштаб и подсветка иконки завязаны на одну и ту же фазу, так что
 *     капля и иконка двигаются синхронно;
 *   - fallback для API < 31: refraction выключается, tint становится плотнее,
 *     иначе на старых устройствах сквозь таббар просвечивал текст.
 * ========================================================================== */

private val BarHeight = 60.dp
private val BarShape = RoundedCornerShape(30.dp)

/** liquid() умеет работать только там, где есть RenderEffect (API 31+). */
private fun Modifier.vibeTabGlass(state: LiquidState?): Modifier =
    if (state != null && VibeEffects.liquid) {
        this.liquid(state) {
            refraction = 0.34f
            curve = 0.42f
            edge = 0.16f
        }
    } else this

@Composable
fun VibeTabBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    unreadCount: Int,
    strings: VibeStrings,
    liquidState: LiquidState?,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val glassActive = liquidState != null && VibeEffects.liquid

    // Со стеклом фон может быть лёгким, без стекла — обязан быть плотным.
    val tintTop = if (isDark) {
        Color(0xFF2A2A2E).copy(alpha = if (glassActive) 0.62f else 0.97f)
    } else {
        Color.White.copy(alpha = if (glassActive) 0.58f else 0.96f)
    }
    val tintBottom = if (isDark) {
        Color(0xFF16161A).copy(alpha = if (glassActive) 0.72f else 0.98f)
    } else {
        Color(0xFFF3F3F7).copy(alpha = if (glassActive) 0.70f else 0.97f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .shadow(
                elevation = 24.dp,
                shape = BarShape,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(BarShape)
            // Слой 1: рефракция — единственное, что даёт настоящее «стекло»
            .vibeTabGlass(liquidState)
            // Слой 2: тонировка
            .background(Brush.verticalGradient(listOf(tintTop, tintBottom)))
            // Слой 3: градиентная рамка, ярче сверху-слева
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.26f else 0.90f),
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.30f),
                        Color.White.copy(alpha = if (isDark) 0.03f else 0.10f)
                    ),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = BarShape
            )
    ) {
        // Слой 4: верхний специулярный блик — «кромка стекла»
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isDark) 0.30f else 0.95f),
                            Color.Transparent
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
        ) {
            val tabCount = MainTab.entries.size
            val tabWidth = maxWidth / tabCount

            // ── Одна упругая модель на две роли ──
            // leader обгоняет, tail отстаёт: разница между ними и есть
            // растяжение капли в полёте. Отдельного keyframes-«желе» не нужно.
            val leaderIndex by animateFloatAsState(
                targetValue = selectedTab.ordinal.toFloat(),
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
                label = "blobLeader"
            )
            val tailIndex by animateFloatAsState(
                targetValue = selectedTab.ordinal.toFloat(),
                animationSpec = spring(dampingRatio = 0.90f, stiffness = 210f),
                label = "blobTail"
            )

            val leaderX = tabWidth * leaderIndex
            val tailX = tabWidth * tailIndex
            val startX = minOf(leaderX, tailX)
            val endX = maxOf(leaderX, tailX) + tabWidth
            val blobWidth = endX - startX

            // 0 в покое, ~1 в середине перелёта
            val stretch = ((blobWidth.value - tabWidth.value) / tabWidth.value)
                .coerceIn(0f, 1f)

            // Короткий squash при смене вкладки: одна дуга, а не 4 перелома
            val squash = remember { Animatable(0f) }
            LaunchedEffect(selectedTab) {
                squash.snapTo(0f)
                squash.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 320
                        0f at 0 using FastOutSlowInEasing
                        1f at 130 using FastOutSlowInEasing
                        0f at 320
                    }
                )
            }
            val squashValue = squash.value

            LiquidBlob(
                startX = startX,
                blobWidth = blobWidth,
                stretch = stretch,
                squash = squashValue,
                isDark = isDark
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    TabItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        strings = strings,
                        unreadCount = if (tab == MainTab.CHATS) unreadCount else 0,
                        onClick = { if (selectedTab != tab) onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Капля-индикатор: тень -> тело -> блик -> внутреннее свечение. */
@Composable
private fun LiquidBlob(
    startX: androidx.compose.ui.unit.Dp,
    blobWidth: androidx.compose.ui.unit.Dp,
    stretch: Float,
    squash: Float,
    isDark: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val blobShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = Modifier
            .offset(x = startX)
            .width(blobWidth)
            .fillMaxHeight()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .graphicsLayer {
                // В полёте капля вытягивается по X и сплющивается по Y —
                // объём сохраняется, движение читается как жидкость.
                scaleX = (1f + stretch * 0.03f) * (1f + squash * 0.05f)
                scaleY = (1f - stretch * 0.16f) * (1f - squash * 0.10f)
            }
    ) {
        // Мягкое свечение под каплей
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 0.40f + stretch * 0.20f }
                .clip(blobShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.34f),
                            primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Тело капли
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(blobShape)
                .background(
                    Brush.linearGradient(
                        colors = if (isDark) listOf(
                            primary.copy(alpha = 0.34f),
                            primary.copy(alpha = 0.18f),
                            Color(0xFF5AC8FA).copy(alpha = 0.24f)
                        ) else listOf(
                            primary.copy(alpha = 0.22f),
                            primary.copy(alpha = 0.12f),
                            Color(0xFF5AC8FA).copy(alpha = 0.18f)
                        ),
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.34f else 0.62f),
                            primary.copy(alpha = 0.22f),
                            Color.White.copy(alpha = if (isDark) 0.05f else 0.14f)
                        )
                    ),
                    shape = blobShape
                )
        ) {
            // Блик на верхней половине
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.52f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.18f else 0.34f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: MainTab,
    isSelected: Boolean,
    strings: VibeStrings,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val label = tab.label(strings)

    // Нажатие даёт мгновенный отклик, выбор — упругий подъём.
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh),
        label = "press_${tab.name}"
    )
    val selectScale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "select_${tab.name}"
    )
    val iconLift by animateDpAsState(
        targetValue = if (isSelected) (-1).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "lift_${tab.name}"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.65f,
        animationSpec = tween(180),
        label = "labelAlpha_${tab.name}"
    )

    val tint = if (isSelected) MaterialTheme.colorScheme.primary else VibeSystemGray

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                transformOrigin = TransformOrigin.Center
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (tab) {
                    MainTab.CHATS -> if (isSelected) Icons.Filled.Forum else Icons.Outlined.Forum
                    MainTab.SETTINGS -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                    MainTab.PROFILE -> if (isSelected) Icons.Filled.Person else Icons.Outlined.PersonOutline
                },
                contentDescription = strings.a11yTab(label),
                modifier = Modifier
                    .offset(y = iconLift)
                    .size(24.dp)
                    .scale(selectScale),
                tint = tint
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.2.sp
                ),
                color = tint.copy(alpha = labelAlpha),
                maxLines = 1
            )
        }

        if (unreadCount > 0) {
            UnreadBadge(
                count = unreadCount,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 15.dp, y = (-12).dp)
            )
        }
    }
}

@Composable
private fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    val text = when {
        count >= 1_000_000 ->
            String.format(Locale.US, "%.1fm", count / 1_000_000.0).replace(".0", "")
        count >= 1_000 ->
            String.format(Locale.US, "%.1fk", count / 1_000.0).replace(".0", "")
        else -> count.toString()
    }
    Box(
        modifier = modifier
            .shadow(6.dp, CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    )
                ),
                CircleShape
            )
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .padding(horizontal = 5.dp, vertical = 1.dp)
            .defaultMinSize(minWidth = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}