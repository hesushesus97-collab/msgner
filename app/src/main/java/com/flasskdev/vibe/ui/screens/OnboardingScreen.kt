package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.flasskdev.vibe.ui.components.*
import com.flasskdev.vibe.ui.theme.*
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/* ============================================================================
 *  ONBOARDING — РЕДИЗАЙН
 *  ---------------------------------------------------------------------------
 *  Что изменилось по сравнению с прошлой версией и ПОЧЕМУ:
 *
 *  1. Свечение. Здесь была главная проблема: свет выглядел КВАДРАТНЫМ, и при
 *     свайпе квадрат было отлично видно. Причина не в цветах, а в технике.
 *
 *     OnboardingGlow (ui/onboarding/OnboardingGlow.kt) строит свет так:
 *     Canvas -> graphicsLayer(CompositingStrategy.Offscreen) -> blur(48.dp).
 *     У Modifier.blur по умолчанию edgeTreatment = BlurredEdgeTreatment
 *     .Rectangle, то есть РАЗМЫТЫЙ РЕЗУЛЬТАТ ОБРЕЗАЕТСЯ ПО ПРЯМОУГОЛЬНИКУ
 *     слоя. Слой был размером с героя, свет дотягивался до его границы —
 *     и граница срезалась ровной линией. Вдобавок этот прямоугольник лежал
 *     ВНУТРИ страницы пейджера, а пейджер клипует страницы по своей ширине:
 *     при свайпе ехали два прямоугольных среза сразу.
 *
 *     Ставить BlurredEdgeTreatment.Unbounded смысла мало: blur остался бы
 *     дорогим (offscreen-слой каждый кадр) и всё равно недоступным до
 *     Android 12. Поэтому blur убран полностью. Свет рисуется в AuroraBackdrop
 *     чистыми радиальными градиентами: они круглые по построению и гаснут
 *     в Color.Transparent ДО края круга, так что обрезаться физически нечему.
 *     Ни offscreen-слоя, ни блюра, ни шейдера — дешевле старой версии.
 *
 *     Второе следствие: свет вынесен ИЗ страницы пейджера на весь экран, за
 *     контент. Он больше не едет вместе со страницей — он живёт отдельно и
 *     только слегка ведёт за свайпом и перекрашивается между страницами.
 *     Именно это убирает ощущение «убого при свайпах».
 *
 *  2. Токены вместо магических чисел. 40/56/18/68/32/20 dp заменены на
 *     VibeSpacing / VibeRadius / VibeMotion из единой дизайн-системы.
 *
 *  3. Контраст. Описание было onBackground.copy(alpha = 0.45f) — это ниже
 *     порога WCAG AA для 17sp. Теперь 0.68f + мягкий межстрочный ритм.
 *
 *  4. Безопасные зоны. Нижняя кнопка жила с vertical = 40.dp без
 *     navigationBarsPadding() и на устройствах с жестовой полосой залезала
 *     под неё. Добавлены statusBarsPadding/navigationBarsPadding.
 *
 *  5. Тач-таргеты. «Skip» и точки-индикаторы были 8–20 dp по высоте, то есть
 *     заметно меньше минимума 48 dp (VibeTouchTarget). Обёрнуты в кликабельные
 *     области нужного размера, визуал остался прежним.
 *
 *  6. Параллакс усмирён. Иконка уезжала на -pageOffset * 150f и физически
 *     вылетала за пределы своей плитки. Теперь смещения привязаны к размеру
 *     плитки и ограничены, плюс убран «пьяный» поворот всей плитки.
 *
 *  7. Компактные экраны. Жёсткие 240 dp плитки + Spacer(56.dp) на телефонах
 *     с малой высотой и при системном увеличении шрифта обрезали заголовок.
 *     Размеры героя и отступы адаптивные.
 *
 *  8. Иконка залита градиентом Aurora (BlendMode.SrcIn по offscreen-слою),
 *     а не плоским VibePrimary — плитка перестала выглядеть скриншотом
 *     системных настроек.
 *
 *  9. Тактильный отклик при смене страницы и на финальном CTA.
 * ========================================================================== */

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

val onboardingIcons = listOf(
    Icons.Outlined.Lock,
    Icons.Outlined.FlashOn,
    Icons.Outlined.Psychology,
    Icons.Outlined.Language,
    Icons.Outlined.Science
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val strings = LocalVibeStrings.current
    val haptics = LocalHapticFeedback.current

    val onboardingPages = remember(strings) {
        strings.onboardingPages.mapIndexed { index, pair ->
            OnboardingPage(
                title = pair.first,
                description = pair.second,
                icon = onboardingIcons.getOrElse(index) { Icons.Outlined.AutoAwesome }
            )
        }
    }

    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    // Адаптив: на невысоких экранах (и при крупном системном шрифте) герой
    // и отступы сжимаются, иначе заголовок с описанием не помещаются.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val compact = screenHeightDp < 720
    val heroTile: Dp = if (compact) 176.dp else 228.dp
    val heroGap: Dp = if (compact) VibeSpacing.xxl else 48.dp

    // Одна общая idle-анимация на весь экран: медленное «дыхание» + покачивание.
    // Держать её одну (а не по одной на страницу) важно — Compose иначе крутит
    // несколько независимых кадровых циклов.
    val idle = rememberInfiniteTransition(label = "onboardingIdle")
    val bob by idle.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroBob"
    )
    // Лёгкий тик при каждом перелистывании — страница «щёлкает» на место.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    // Непрерывная позиция пейджера: 0f..(pageCount-1). Именно она, а не
    // currentPage, красит и смещает свет, поэтому переход плавный.
    val pagerProgress = pagerState.currentPage + pagerState.currentPageOffsetFraction

    Box(modifier = Modifier.fillMaxSize()) {
        VibeBackgroundMesh()

        // Свет лежит НА ВСЁМ ЭКРАНЕ под контентом и вне пейджера: ничего не
        // клипуется, при свайпе не видно ни одной прямой границы.
        AuroraBackdrop(
            progress = pagerProgress,
            pageCount = onboardingPages.size,
            centerBias = if (compact) 0.32f else 0.36f,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Skip: живёт до последней страницы, потом плавно гаснет ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = VibeSpacing.sm),
                contentAlignment = Alignment.CenterEnd
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isLastPage,
                    enter = fadeIn(VibeMotion.quick()),
                    exit = fadeOut(VibeMotion.quick())
                ) {
                    SkipButton(
                        label = strings.onboardingSkip,
                        onClick = onFinished
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { pageIndex ->
                val page = onboardingPages[pageIndex]
                val pageOffset =
                    (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                val focus = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = VibeSpacing.section)
                        .semantics {
                            contentDescription =
                                strings.a11yOnboardingPage(pageIndex + 1, onboardingPages.size)
                        }
                        .graphicsLayer {
                            // Мягкий «карточный» заход страницы: сдвиг + масштаб + прозрачность.
                            translationX = pageOffset * size.width * 0.35f
                            alpha = 0.25f + focus * 0.75f
                            val scale = 0.94f + focus * 0.06f
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    OnboardingHero(
                        icon = page.icon,
                        focus = focus,
                        pageOffset = pageOffset,
                        bob = bob,
                        tileSize = heroTile
                    )

                    Spacer(modifier = Modifier.height(heroGap))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { translationX = -pageOffset * 48f }
                    )

                    Spacer(modifier = Modifier.height(VibeSpacing.md))

                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        // Было 0.45f — не проходило по контрасту. 0.68f читается
                        // и при этом не конкурирует с заголовком.
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(max = 340.dp)
                            .graphicsLayer { translationX = -pageOffset * 24f }
                    )
                }
            }

            OnboardingPageIndicator(
                pageCount = onboardingPages.size,
                currentPage = pagerState.currentPage,
                offsetFraction = pagerState.currentPageOffsetFraction,
                pageLabel = { index -> strings.a11yOnboardingPage(index + 1, onboardingPages.size) },
                onSelect = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = index,
                            animationSpec = VibeMotion.snappy()
                        )
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = VibeSpacing.xl,
                        vertical = if (compact) VibeSpacing.xl else VibeSpacing.xxl
                    )
            ) {
                VibeButton(
                    text = if (isLastPage) strings.onboardingGetStarted else strings.continueBtn,
                    onClick = {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage + 1,
                                    animationSpec = VibeMotion.snappy()
                                )
                            }
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFinished()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------
 *  ФОНОВЫЙ СВЕТ (AURORA)
 *  ---------------------------------------------------------------------------
 *  Принципы, из-за нарушения которых прошлая версия выглядела квадратной:
 *
 *   1. НИКАКОГО Modifier.blur и никаких offscreen-слоёв. Мягкость даётся
 *      многоступенчатым alpha-рампом самого градиента
 *      (0.42 -> 0.24 -> 0.07 -> 0), а не размытием растра.
 *   2. Каждое гало гаснет В ПРОЗРАЧНОСТЬ на своём радиусе, а радиус меньше
 *      половины холста. Свету просто негде упереться в границу.
 *   3. Холст — весь экран и вне пейджера, поэтому клипа страниц нет.
 *   4. Реакция на свайп непрерывная: свет чуть отклоняется в сторону жеста
 *      (drift) и плавно перекрашивается из палитры одной страницы в палитру
 *      следующей. Дискретных «перескоков» на границе страниц нет.
 *
 *  Стоимость кадра: три drawCircle с градиентной кистью. Дешевле и старого
 *  OnboardingGlow (blur + offscreen), и прежнего набора полупрозрачных Box.
 * ------------------------------------------------------------------------- */

@Composable
private fun AuroraBackdrop(
    progress: Float,
    pageCount: Int,
    centerBias: Float,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()

    // Свет можно выключить одним флагом вместе с остальными эффектами.
    if (!VibeEffects.glowEnabled) return

    val transition = rememberInfiniteTransition(label = "aurora")
    // Два гало вращаются с разной скоростью и в разные стороны: рисунок
    // никогда точно не повторяется, и глаз не считывает цикл анимации.
    val slow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(38_000, easing = LinearEasing)),
        label = "slow"
    )
    val medium by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(23_000, easing = LinearEasing)),
        label = "medium"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(7_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // У каждой страницы своя пара оттенков внутри одной палитры дизайн-системы.
    val palette = remember {
        listOf(
            VibeViolet to VibeIndigo,
            VibeIndigo to VibeSky,
            VibeVioletDeep to VibeViolet,
            VibeSky to VibeIndigo,
            VibeViolet to VibeSky
        )
    }
    val from = progress.toInt().coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val to = (from + 1).coerceAtMost((pageCount - 1).coerceAtLeast(0))
    val blend = (progress - from).coerceIn(0f, 1f)
    val warm = lerp(palette[from % palette.size].first, palette[to % palette.size].first, blend)
    val cool = lerp(palette[from % palette.size].second, palette[to % palette.size].second, blend)

    // В светлой теме то же свечение читается как грязь, если не приглушить.
    val gain = if (isDark) 1f else 0.62f

    Canvas(modifier = modifier) {
        val cy = size.height * centerBias
        // drift в диапазоне -0.5..0.5: свет «ведёт» за пальцем и возвращается.
        val drift = progress - progress.roundToInt()
        val cx = size.width / 2f + drift * size.width * 0.10f
        val base = size.minDimension * 0.62f * breathe

        fun halo(color: Color, center: Offset, radius: Float, peak: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    // Плавный хвост до полной прозрачности — за это отвечает
                    // именно набор стопов, а не blur.
                    0.00f to color.copy(alpha = peak * gain),
                    0.35f to color.copy(alpha = peak * 0.55f * gain),
                    0.68f to color.copy(alpha = peak * 0.18f * gain),
                    1.00f to Color.Transparent,
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        fun orbit(angleDeg: Float, distance: Float): Offset {
            val rad = Math.toRadians(angleDeg.toDouble())
            return Offset(
                cx + cos(rad).toFloat() * distance,
                cy + sin(rad).toFloat() * distance
            )
        }

        // Ядро: мягкий белый свет по центру героя.
        halo(
            color = VibeGlowWhite,
            center = Offset(cx, cy),
            radius = base * 0.58f,
            peak = if (isDark) 0.26f else 0.42f
        )
        // Тёплое (фиолетовое) гало — по часовой.
        halo(
            color = warm,
            center = orbit(slow, base * 0.16f),
            radius = base * 1.08f,
            peak = 0.40f
        )
        // Холодное (синее) гало — против часовой.
        halo(
            color = cool,
            center = orbit(medium, base * 0.22f),
            radius = base * 0.94f,
            peak = 0.34f
        )
    }
}

/* ---------------------------------------------------------------------------
 *  ГЕРОЙ СТРАНИЦЫ: стеклянная плитка + иконка в градиенте
 *  Свет теперь не его забота — он приходит из AuroraBackdrop под всем экраном.
 * ------------------------------------------------------------------------- */

@Composable
private fun OnboardingHero(
    icon: ImageVector,
    focus: Float,
    pageOffset: Float,
    bob: Float,
    tileSize: Dp,
    modifier: Modifier = Modifier
) {
    val glass = glassStyle()
    val tileShape = RoundedCornerShape(tileSize * 0.28f)   // squircle пропорционально размеру

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Плитка: стекло из дизайн-системы, аврора-хайрлайн, живая тень.
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = bob * focus
                }
                .size(tileSize)
                .shadow(
                    elevation = 24.dp,
                    shape = tileShape,
                    ambientColor = VibeViolet.copy(alpha = 0.30f),
                    spotColor = VibeIndigo.copy(alpha = 0.34f)
                )
                .clip(tileShape)
                .background(
                    Brush.verticalGradient(listOf(glass.highlight, Color.Transparent))
                )
                .background(glass.fill)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            VibeGlowWhite.copy(alpha = 0.55f),
                            VibeViolet.copy(alpha = 0.28f),
                            VibeSky.copy(alpha = 0.35f)
                        )
                    ),
                    shape = tileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Блик по верхней половине — плитка читается как физическое стекло.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
            )

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,                       // основа для маски SrcIn
                modifier = Modifier
                    .size(tileSize * 0.40f)
                    .graphicsLayer {
                        // Параллакс привязан к размеру плитки и ограничен, чтобы
                        // глиф не вылетал за её пределы при быстром свайпе.
                        translationX = -pageOffset.coerceIn(-1f, 1f) * size.width * 0.35f
                        rotationZ = pageOffset.coerceIn(-1f, 1f) * 6f
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.linearGradient(VibeAuroraGradient),
                            blendMode = BlendMode.SrcIn
                        )
                    }
            )
        }
    }
}

/* ---------------------------------------------------------------------------
 *  ИНДИКАТОР СТРАНИЦ
 *  Визуал — те же «жидкие» пилюли, но каждая обёрнута в тач-таргет 48 dp.
 * ------------------------------------------------------------------------- */

@Composable
private fun OnboardingPageIndicator(
    pageCount: Int,
    currentPage: Int,
    offsetFraction: Float,
    pageLabel: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .height(VibeTouchTarget)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val offset = (currentPage - index + offsetFraction).absoluteValue
            val focus = (1f - offset).coerceIn(0f, 1f)

            val width = 8.dp + (24.dp * focus)
            val idleColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
            val brush = if (focus > 0.05f) {
                Brush.horizontalGradient(
                    listOf(
                        VibeViolet.copy(alpha = 0.35f + 0.65f * focus),
                        VibeIndigo.copy(alpha = 0.35f + 0.65f * focus),
                        VibeSky.copy(alpha = 0.35f + 0.65f * focus)
                    )
                )
            } else {
                Brush.horizontalGradient(listOf(idleColor, idleColor))
            }

            Box(
                modifier = Modifier
                    .size(width = width + VibeSpacing.md, height = VibeTouchTarget)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = pageLabel(index)
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------
 *  SKIP
 *  Раньше это был просто текст alpha 0.45 без какой-либо аффордности.
 *  Теперь — капсула из стекла: видно, что это кнопка, и палец её находит.
 * ------------------------------------------------------------------------- */

@Composable
private fun SkipButton(
    label: String,
    onClick: () -> Unit
) {
    val glass = glassStyle()
    Box(
        modifier = Modifier
            .heightIn(min = VibeTouchTarget)
            .clip(RoundedCornerShape(VibeRadius.pill))
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = VibeSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(VibeRadius.pill))
                .background(glass.fill.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    glass.border,
                    RoundedCornerShape(VibeRadius.pill)
                )
                .padding(horizontal = VibeSpacing.lg, vertical = VibeSpacing.sm)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}