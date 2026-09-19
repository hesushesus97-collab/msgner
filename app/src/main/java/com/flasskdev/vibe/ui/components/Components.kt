package com.flasskdev.vibe.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.*
import kotlinx.coroutines.delay

/* ==================================================================
 *  ДИЗАЙН-ТОКЕНЫ
 *  Радиусы/высоты/длительности в одном месте: правится стиль, а не 20 мест.
 * ================================================================== */
private object VibeUi {
    val radiusButton = 24.dp
    val radiusField = 20.dp
    val radiusOtp = 18.dp
    val radiusToast = 20.dp
    val radiusInlineBtn = 18.dp

    val heightButton = 56.dp
    val heightField = 56.dp
    val heightOtp = 62.dp

    const val PRESS_SCALE = 0.965f
    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/* ==================================================================
 *  ФОН
 * ================================================================== */

/**
 * Раньше это был просто залитый фоном Box (название "Mesh" ничего не значило).
 * Теперь — настоящий mesh: три мягких радиальных пятна, медленно дышащих.
 * Рисуется в drawWithCache (кисти пересобираются только при смене размера/фазы),
 * так что это дешевле любого layered-Box с градиентами.
 */
@Composable
fun VibeBackgroundMesh(
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

    val drift = if (animated) {
        val t = rememberInfiniteTransition(label = "meshDrift")
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16_000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "meshPhase"
        )
        v
    } else 0.5f

    val warm = VibePrimary.copy(alpha = if (isDark) 0.20f else 0.12f)
    val cool = (if (isDark) Color(0xFF6C5CE7) else Color(0xFF63B3FF))
        .copy(alpha = if (isDark) 0.16f else 0.10f)
    val soft = scheme.tertiary.copy(alpha = if (isDark) 0.12f else 0.08f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val r = size.maxDimension * 0.75f

                val b1 = Brush.radialGradient(
                    colors = listOf(warm, Color.Transparent),
                    center = Offset(w * (0.12f + 0.10f * drift), h * (0.10f + 0.04f * drift)),
                    radius = r
                )
                val b2 = Brush.radialGradient(
                    colors = listOf(cool, Color.Transparent),
                    center = Offset(w * (0.92f - 0.12f * drift), h * (0.32f + 0.06f * drift)),
                    radius = r * 0.85f
                )
                val b3 = Brush.radialGradient(
                    colors = listOf(soft, Color.Transparent),
                    center = Offset(w * (0.45f + 0.08f * drift), h * (0.95f - 0.05f * drift)),
                    radius = r * 0.9f
                )
                onDrawBehind {
                    drawRect(b1)
                    drawRect(b2)
                    drawRect(b3)
                }
            }
    )
}

/* ==================================================================
 *  КНОПКА
 * ================================================================== */

@Composable
fun VibeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val strings = LocalVibeStrings.current
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val clickable = enabled && !loading

    val scale by animateFloatAsState(
        targetValue = if (isPressed && clickable) VibeUi.PRESS_SCALE else 1f,
        animationSpec = VibeUi.pressSpring,
        label = "buttonScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.55f,
        animationSpec = tween(180),
        label = "buttonAlpha"
    )
    val pressOverlay by animateFloatAsState(
        targetValue = if (isPressed && clickable) 0.10f else 0f,
        animationSpec = tween(120),
        label = "buttonOverlay"
    )

    val shape = RoundedCornerShape(VibeUi.radiusButton)

    // Объёмная заливка вместо плоского цвета: светлее сверху, глубже снизу.
    val fill = remember(enabled) {
        if (enabled) {
            Brush.verticalGradient(
                listOf(
                    lerp(VibePrimary, Color.White, 0.16f),
                    VibePrimary,
                    lerp(VibePrimary, Color.Black, 0.12f)
                )
            )
        } else {
            Brush.verticalGradient(listOf(VibePrimary, VibePrimary))
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(VibeUi.heightButton)
            .shadow(
                // Тень цветная и живая: гаснет на нажатии, исчезает у disabled
                elevation = if (!enabled) 0.dp else if (isPressed) 3.dp else 12.dp,
                shape = shape,
                spotColor = VibePrimary.copy(alpha = 0.45f),
                ambientColor = VibePrimary.copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(fill)
            // FIX: раньше disabled затемнялся плёнкой Color.Black 35% — на светлой теме
            // это выглядело грязно. Теперь гасим цветом поверхности темы.
            .then(
                if (!enabled) Modifier.background(scheme.surface.copy(alpha = 0.62f))
                else Modifier
            )
            .then(
                if (pressOverlay > 0f) Modifier.background(Color.Black.copy(alpha = pressOverlay))
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = clickable,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                if (loading) stateDescription = strings.a11yLoading
            },
        contentAlignment = Alignment.Center
    ) {
        // Верхний блик стекла
        if (enabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
            )
        }

        Crossfade(targetState = loading, animationSpec = tween(180), label = "buttonContent") { isLoading ->
            if (isLoading) {
                LoadingDots(color = Color.White)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.alpha(contentAlpha)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}

/** Три пульсирующие точки: спокойнее и дешевле, чем CircularProgressIndicator. */
@Composable
private fun LoadingDots(
    color: Color,
    dotSize: androidx.compose.ui.unit.Dp = 7.dp
) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val a by transition.animateFloat(
                initialValue = 0.30f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(560, delayMillis = i * 140, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color.copy(alpha = a))
            )
        }
    }
}

/* ==================================================================
 *  ПОЛЕ ВВОДА
 * ================================================================== */

@Composable
fun VibeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    showClearButton: Boolean = false
) {
    val strings = LocalVibeStrings.current
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(VibeUi.radiusField)

    val accent = when {
        error != null -> VibeError
        isFocused -> VibePrimary
        else -> scheme.onSurface.copy(alpha = 0.55f)
    }
    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> VibeError
            isFocused -> VibePrimary
            else -> scheme.outline.copy(alpha = 0.20f)
        },
        animationSpec = tween(180),
        label = "fieldBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || error != null) 1.5.dp else 1.dp,
        animationSpec = tween(180),
        label = "fieldBorderWidth"
    )
    val labelColor by animateColorAsState(accent, tween(180), label = "fieldLabel")

    // Ошибка встряхивает поле — заметно, но не агрессивно
    val shake = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error != null) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 320
                    0f at 0
                    -8f at 60
                    8f at 130
                    -4f at 210
                    0f at 320
                }
            )
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                color = labelColor,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Black
            )
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(tween(140)) + slideInVertically { -it / 2 },
                exit = fadeOut(tween(120)) + slideOutVertically { -it / 2 }
            ) {
                Text(
                    text = error.orEmpty(),
                    color = VibeError,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics {
                        contentDescription = strings.a11yFieldError(error.orEmpty())
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { androidx.compose.ui.unit.IntOffset(shake.value.toInt(), 0) }
                .heightIn(min = VibeUi.heightField)
                .shadow(
                    elevation = if (isFocused) 8.dp else 2.dp,
                    shape = shape,
                    spotColor = if (isFocused) VibePrimary.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.06f),
                    ambientColor = Color.Black.copy(alpha = 0.04f)
                )
                .clip(shape)
                .background(scheme.surface.copy(alpha = if (enabled) 0.94f else 0.55f))
                .border(borderWidth, borderColor, shape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.padding(end = 12.dp)) { leadingIcon() }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    // Плейсхолдер: раньше пустое поле было полностью немым
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            color = scheme.onSurface.copy(alpha = 0.38f),
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        interactionSource = interactionSource,
                        textStyle = TextStyle(
                            color = scheme.onBackground,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(if (error != null) VibeError else VibePrimary),
                        keyboardOptions = keyboardOptions,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AnimatedVisibility(
                    visible = showClearButton && value.isNotEmpty() && enabled,
                    enter = fadeIn() + scaleIn(initialScale = 0.7f),
                    exit = fadeOut() + scaleOut(targetScale = 0.7f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurface.copy(alpha = 0.10f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onValueChange("") }
                            .semantics { contentDescription = strings.a11yClearField }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = scheme.onSurface.copy(alpha = 0.65f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

/* ==================================================================
 *  OTP
 * ================================================================== */

@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    onComplete: ((String) -> Unit)? = null
) {
    val strings = LocalVibeStrings.current
    val scheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    LaunchedEffect(value) {
        if (value.length == length) onComplete?.invoke(value)
    }

    // Мигающий курсор в активной ячейке
    val caretTransition = rememberInfiniteTransition(label = "otpCaret")
    val caretAlpha by caretTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "otpCaretAlpha"
    )

    val shake = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 340
                    0f at 0
                    -10f at 60
                    10f at 140
                    -5f at 230
                    0f at 340
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = strings.a11yOtpInput }
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                if (it.length <= length && it.all { c -> c.isDigit() }) onValueChange(it)
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = Color.Transparent)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { androidx.compose.ui.unit.IntOffset(shake.value.toInt(), 0) }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(length) { index ->
                val char = value.getOrNull(index)?.toString() ?: ""
                val isActive = value.length == index
                val isFilled = char.isNotEmpty()

                val cellShape = RoundedCornerShape(VibeUi.radiusOtp)
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isError -> VibeError
                        isActive -> VibePrimary
                        isFilled -> VibePrimary.copy(alpha = 0.45f)
                        else -> scheme.outline.copy(alpha = 0.18f)
                    },
                    animationSpec = tween(160),
                    label = "otpBorder$index"
                )
                val borderWidth by animateDpAsState(
                    targetValue = if (isActive || isError) 2.dp else 1.dp,
                    animationSpec = tween(160),
                    label = "otpBorderW$index"
                )
                val cellScale by animateFloatAsState(
                    targetValue = if (isActive) 1.05f else 1f,
                    animationSpec = VibeUi.pressSpring,
                    label = "otpScale$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(VibeUi.heightOtp)
                        .scale(cellScale)
                        .shadow(
                            elevation = if (isActive) 8.dp else 2.dp,
                            shape = cellShape,
                            spotColor = if (isActive) VibePrimary.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.06f),
                            ambientColor = Color.Black.copy(alpha = 0.04f)
                        )
                        .clip(cellShape)
                        .background(
                            if (isFilled) lerp(scheme.surface, VibePrimary, 0.07f).copy(alpha = 0.96f)
                            else scheme.surface.copy(alpha = 0.92f)
                        )
                        .border(borderWidth, borderColor, cellShape)
                        .semantics {
                            contentDescription = if (isFilled) {
                                strings.a11yOtpDigit(index + 1, length)
                            } else {
                                strings.a11yOtpDigitEmpty(index + 1, length)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Цифра появляется с лёгким «подскоком», а не мгновенно
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            (fadeIn(tween(120)) + scaleIn(initialScale = 0.6f))
                                .togetherWith(fadeOut(tween(90)) + scaleOut(targetScale = 0.6f))
                        },
                        label = "otpDigit$index"
                    ) { c ->
                        if (c.isEmpty()) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 2.dp, height = 26.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(VibePrimary.copy(alpha = caretAlpha))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(width = 10.dp, height = 2.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(scheme.onSurface.copy(alpha = 0.14f))
                                )
                            }
                        } else {
                            Text(
                                text = c,
                                color = if (isError) VibeError else scheme.onBackground,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ==================================================================
 *  INTERACTION HELPERS
 * ================================================================== */

@Composable
fun MutableInteractionSource.collectIsPressedAsState(): State<Boolean> {
    val pressCount = remember { mutableIntStateOf(0) }
    val isPressed = remember { derivedStateOf { pressCount.intValue > 0 } }
    LaunchedEffect(this) {
        pressCount.intValue = 0
        interactions.collect { interaction ->
            // FIX: считаем нажатия счётчиком. Раньше при двух пальцах отпускание одного
            // сбрасывало состояние, и кнопка «отлипала» под всё ещё нажатым пальцем.
            when (interaction) {
                is androidx.compose.foundation.interaction.PressInteraction.Press ->
                    pressCount.intValue++
                is androidx.compose.foundation.interaction.PressInteraction.Release ->
                    pressCount.intValue = (pressCount.intValue - 1).coerceAtLeast(0)
                is androidx.compose.foundation.interaction.PressInteraction.Cancel ->
                    pressCount.intValue = (pressCount.intValue - 1).coerceAtLeast(0)
            }
        }
    }
    return isPressed
}

@Composable
fun MutableInteractionSource.collectIsFocusedAsState(): State<Boolean> {
    val isFocused = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.FocusInteraction.Focus -> isFocused.value = true
                is androidx.compose.foundation.interaction.FocusInteraction.Unfocus -> isFocused.value = false
            }
        }
    }
    return isFocused
}

/* ==================================================================
 *  ТОСТ
 * ================================================================== */

enum class VibeToastType { Info, Success, Error }

@Composable
fun VibeToast(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    type: VibeToastType = VibeToastType.Info,
    autoHideMillis: Long? = null
) {
    val strings = LocalVibeStrings.current
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(isVisible, message, autoHideMillis) {
        if (isVisible && autoHideMillis != null) {
            delay(autoHideMillis)
            onDismiss()
        }
    }

    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            mounted = true
        } else {
            delay(250)
            mounted = false
        }
    }

    if (mounted) {
        Popup(
            alignment = Alignment.BottomCenter,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            val accent = when (type) {
                VibeToastType.Info -> VibePrimary
                VibeToastType.Success -> Color(0xFF2FB463)
                VibeToastType.Error -> VibeError
            }
            val icon = when (type) {
                VibeToastType.Info -> Icons.Default.Info
                VibeToastType.Success -> Icons.Default.CheckCircle
                VibeToastType.Error -> Icons.Default.ErrorOutline
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(tween(160)) + scaleIn(initialScale = 0.92f),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) +
                    fadeOut(tween(160)) + scaleOut(targetScale = 0.94f),
                modifier = modifier
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(VibeUi.radiusToast),
                            spotColor = Color.Black.copy(alpha = 0.28f),
                            ambientColor = Color.Black.copy(alpha = 0.14f)
                        )
                        .clip(RoundedCornerShape(VibeUi.radiusToast))
                        .background(scheme.inverseSurface)
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(VibeUi.radiusToast)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(start = 14.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = strings.a11yToast(message)
                            stateDescription = strings.a11yToastDismiss
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.20f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = message,
                        color = scheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/* ==================================================================
 *  УТИЛИТЫ
 * ================================================================== */

fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val clean = hex.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        when (clean.length) {
            3 -> {
                val r = clean[0].toString().repeat(2).toInt(16)
                val g = clean[1].toString().repeat(2).toInt(16)
                val b = clean[2].toString().repeat(2).toInt(16)
                Color(r, g, b, 255)
            }
            6 -> {
                val colorInt = clean.toLong(16)
                Color(
                    red = ((colorInt shr 16) and 0xFF).toInt(),
                    green = ((colorInt shr 8) and 0xFF).toInt(),
                    blue = (colorInt and 0xFF).toInt(),
                    alpha = 255
                )
            }
            8 -> {
                val colorInt = clean.toLong(16)
                Color(
                    alpha = ((colorInt shr 24) and 0xFF).toInt(),
                    red = ((colorInt shr 16) and 0xFF).toInt(),
                    green = ((colorInt shr 8) and 0xFF).toInt(),
                    blue = (colorInt and 0xFF).toInt()
                )
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/* ==================================================================
 *  INLINE-КЛАВИАТУРА
 * ================================================================== */

@Composable
fun InlineKeyboard(
    replyMarkup: com.flasskdev.vibe.data.local.ReplyMarkup,
    onButtonClick: (com.flasskdev.vibe.data.local.InlineKeyboardButton) -> Unit,
    modifier: Modifier = Modifier,
    liquidState: io.github.fletchmckee.liquid.LiquidState? = null,
    isInteractionEnabled: Boolean = true,
    pendingCallbackData: String? = null
) {
    val rows = replyMarkup.inlineKeyboard.take(10) // Limit max 10 rows
    if (rows.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { row ->
            val buttons = row.take(5) // Limit max 5 per row
            if (buttons.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    buttons.forEach { button ->
                        InlineKeyboardButtonItem(
                            button = button,
                            onClick = { onButtonClick(button) },
                            liquidState = liquidState,
                            enabled = isInteractionEnabled,
                            isLoading = pendingCallbackData != null && button.callbackData == pendingCallbackData,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InlineKeyboardButtonItem(
    button: com.flasskdev.vibe.data.local.InlineKeyboardButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    liquidState: io.github.fletchmckee.liquid.LiquidState? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val strings = LocalVibeStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val isDark = MaterialTheme.colorScheme.background == VibeBackgroundDark
    val isLink = !button.url.isNullOrBlank()

    // Custom or default colors
    val parsedBgColor = remember(button.bgColor) { parseHexColor(button.bgColor) }
    val parsedTextColor = remember(button.textColor) { parseHexColor(button.textColor) }

    val defaultTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textColor = parsedTextColor ?: defaultTextColor

    val shape = RoundedCornerShape(VibeUi.radiusInlineBtn)

    // Inline keyboards can appear many times in a lazy chat list. Avoid per-button liquid
    // distortion here: it is expensive during scroll and can delay hit testing on lower-end devices.

    // A calm, high-contrast Material-like surface remains readable over both chat bubbles.
    val defaultSurfaceColor = if (isDark) {
        Color(0xFF34343A).copy(alpha = if (isPressed) 0.96f else 0.90f)
    } else {
        Color.White.copy(alpha = if (isPressed) 0.98f else 0.94f)
    }

    val glassTint = if (parsedBgColor != null) {
        parsedBgColor.copy(alpha = if (isPressed) 0.92f else 0.82f)
    } else {
        defaultSurfaceColor
    }
    val waitingTransition = rememberInfiniteTransition(label = "inlineButtonWaitingPulse")
    val waitingPulse by waitingTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inlineButtonWaitingPulseAlpha"
    )
    val shimmerSweep by waitingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inlineButtonShimmer"
    )
    val targetSurfaceColor = when {
        isLoading -> VibePrimary.copy(alpha = 0.52f + (waitingPulse * 0.24f))
        !enabled -> glassTint.copy(alpha = 0.42f)
        else -> glassTint
    }
    val surfaceColor by animateColorAsState(
        targetValue = targetSurfaceColor,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "inlineButtonSurface"
    )
    val waitingBarProgress by animateFloatAsState(
        targetValue = if (isLoading) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "inlineButtonWaitingBar"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.975f else 1f,
        animationSpec = VibeUi.pressSpring,
        label = "inlineButtonScale"
    )
    val displayedTextColor = textColor.copy(alpha = if (enabled) 1f else 0.52f)

    // Luminous glass edge highlight
    val borderBrush = if (parsedBgColor != null) {
        Brush.verticalGradient(
            colors = listOf(
                parsedBgColor.copy(alpha = 0.75f),
                parsedBgColor.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.38f else 0.65f),
                Color.White.copy(alpha = if (isDark) 0.10f else 0.20f)
            )
        )
    }

    // Internal glass specular gloss highlight
    val glossBrush = remember(isDark, parsedBgColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.14f else 0.22f),
                Color.Transparent
            )
        )
    }
    val shimmerBrush = remember {
        Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = 0.18f),
            1f to Color.Transparent
        )
    }

    // Material Button owns both measurement and pointer handling, so its visual bounds and
    // touch target are identical even inside a scrolling lazy list.
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .scale(pressScale)
            // FIX: borderBrush и glossBrush раньше вычислялись, но НИКУДА не применялись —
            // весь «стеклянный» эффект был мёртвым кодом. Теперь он реально рисуется.
            .clip(shape)
            .drawWithContent {
                drawContent()
                // Верхний блик
                drawRect(brush = glossBrush, size = Size(size.width, size.height * 0.55f))
                // Бегущий отсвет во время ожидания ответа бота
                if (waitingBarProgress > 0f) {
                    val band = size.width * 0.55f
                    translate(left = (shimmerSweep * (size.width + band)) - band) {
                        drawRect(brush = shimmerBrush, size = Size(band, size.height))
                    }
                }
            }
            .border(BorderStroke(1.dp, borderBrush), shape)
            .semantics {
                if (isLink) contentDescription = strings.a11yInlineButtonLink
                if (isLoading) stateDescription = strings.a11yInlineButtonLoading
            },
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = surfaceColor,
            contentColor = displayedTextColor,
            disabledContainerColor = surfaceColor,
            disabledContentColor = displayedTextColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = button.text,
                    color = if (isLoading) Color.White else displayedTextColor,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
                if (isLink) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = displayedTextColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            // A quiet progress accent: no spinner and no text replacement.
            if (waitingBarProgress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(waitingBarProgress)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(VibeWaitingAccent.copy(alpha = 0.42f + (waitingPulse * 0.40f)))
                )
            }
        }
    }
}