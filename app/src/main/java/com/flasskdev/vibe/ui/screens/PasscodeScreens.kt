package com.flasskdev.vibe.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.theme.luminanceIsDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PASSCODE_LENGTH = 4

/* ------------------------------------------------------------------------- */
/*  Lock screen (app launch)                                                 */
/* ------------------------------------------------------------------------- */

@Composable
fun PasscodeAuthScreen(
    userPreferences: UserPreferences,
    onSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val strings = LocalVibeStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(enabled = true) {
        (context as? android.app.Activity)?.moveTaskToBack(true)
    }

    val savedPin = userPreferences.passcode

    if (savedPin == null) {
        // Fallback in case navigated here but no pin is set
        LaunchedEffect(Unit) { onSuccess() }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            PasscodeContent(
                title = strings.passcodeEnterTitle,
                subtitle = strings.passcodeEnterSubtitle,
                errorText = if (isError) strings.passcodeWrongCode else null,
                showLockBadge = true,
                pin = pin,
                isError = isError,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onPinChange = { newPin ->
                    if (isError) isError = false
                    pin = newPin
                    if (pin.length == PASSCODE_LENGTH) {
                        if (pin == savedPin) {
                            onSuccess()
                        } else {
                            isError = true
                            scope.launch {
                                delay(600)
                                pin = ""
                                isError = false
                            }
                        }
                    }
                }
            )

            TextButton(
                onClick = onLogout,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = strings.btnLogout,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Settings page (same layout language as the two-factor page)              */
/* ------------------------------------------------------------------------- */

private enum class Step {
    INFO, ENTER_CURRENT, ENTER_NEW, CONFIRM_NEW
}

@Composable
fun PasscodeSetupScreen(
    userPreferences: UserPreferences,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val scope = rememberCoroutineScope()
    val revision = preferenceRevision(userPreferences)
    val hasPasscode = remember(revision) { userPreferences.passcode != null }

    var step by remember { mutableStateOf(Step.INFO) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>(null) }

    fun resetFlow() {
        step = Step.INFO
        pin = ""
        firstPin = ""
        isError = false
    }

    val inFlow = step != Step.INFO
    BackHandler(enabled = true) { if (inFlow) resetFlow() else onBack() }

    LaunchedEffect(banner) {
        if (banner != null) {
            delay(3500)
            banner = null
        }
    }

    SettingsSubPage(
        title = strings.privacyPasscodeLogin,
        subtitle = if (hasPasscode) strings.twoFactorStatusEnabled else strings.twoFactorStatusDisabled,
        onBack = { if (inFlow) resetFlow() else onBack() }
    ) {
        banner?.let {
            SettingsStatusBanner(
                text = it,
                tint = Color(0xFF4CAF50),
                icon = Icons.Rounded.CheckCircle
            )
            Spacer(Modifier.height(16.dp))
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally { it / 6 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally { -it / 6 })
            },
            label = "passcodeStep"
        ) { current ->
            if (current == Step.INFO) {
                PasscodeStatusContent(
                    isEnabled = hasPasscode,
                    strings = strings,
                    isDark = isDark,
                    onEnable = { step = Step.ENTER_NEW },
                    onChange = { step = Step.ENTER_CURRENT },
                    onDisable = { showRemoveDialog = true }
                )
            } else {
                val title = when (current) {
                    Step.ENTER_CURRENT -> strings.passcodeEnterCurrentTitle
                    Step.ENTER_NEW -> strings.passcodeCreateTitle
                    else -> strings.passcodeRepeatTitle
                }
                val subtitle = when (current) {
                    Step.ENTER_CURRENT -> strings.passcodeStepCurrentSubtitle
                    Step.ENTER_NEW -> strings.passcodeStepNewSubtitle
                    else -> strings.passcodeStepRepeatSubtitle
                }
                // Неверный текущий код и несовпадение повтора — разные ошибки с разным текстом.
                val errorText = when {
                    !isError -> null
                    current == Step.ENTER_CURRENT -> strings.passcodeWrongCode
                    else -> strings.passcodeMismatch
                }

                PasscodeContent(
                    title = title,
                    subtitle = subtitle,
                    errorText = errorText,
                    showLockBadge = true,
                    badgeSize = 56.dp,
                    pin = pin,
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.Top,
                    onPinChange = { newPin ->
                        if (isError) isError = false
                        pin = newPin
                        if (pin.length != PASSCODE_LENGTH) return@PasscodeContent

                        when (current) {
                            Step.ENTER_CURRENT -> {
                                if (pin == userPreferences.passcode) {
                                    step = Step.ENTER_NEW
                                    pin = ""
                                } else {
                                    isError = true
                                    scope.launch {
                                        delay(600)
                                        pin = ""
                                        isError = false
                                    }
                                }
                            }
                            Step.ENTER_NEW -> {
                                firstPin = pin
                                step = Step.CONFIRM_NEW
                                pin = ""
                            }
                            Step.CONFIRM_NEW -> {
                                if (pin == firstPin) {
                                    userPreferences.passcode = pin
                                    banner = strings.passcodeSavedToast
                                    resetFlow()
                                } else {
                                    isError = true
                                    scope.launch {
                                        delay(600)
                                        pin = ""
                                        firstPin = ""
                                        step = Step.ENTER_NEW
                                        isError = false
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }
                )
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = strings.passcodeRemoveTitle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    text = strings.passcodeRemoveText,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    userPreferences.passcode = null
                    showRemoveDialog = false
                    banner = strings.passcodeRemovedToast
                    resetFlow()
                }) {
                    Text(
                        text = strings.passcodeDisableShort,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(
                        text = strings.cancelBtn,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Статусная страница код-пароля. Визуально повторяет TwoFactorStatusContent:
 * hero-карточка с градиентом, список преимуществ пока защита выключена, список действий
 * когда включена.
 */
@Composable
private fun PasscodeStatusContent(
    isEnabled: Boolean,
    strings: VibeStrings,
    isDark: Boolean,
    onEnable: () -> Unit,
    onChange: () -> Unit,
    onDisable: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = if (isEnabled) Color(0xFF4CAF50) else primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = if (isEnabled) {
                            listOf(
                                Color(0xFF4CAF50).copy(alpha = if (isDark) 0.22f else 0.16f),
                                primary.copy(alpha = if (isDark) 0.16f else 0.12f)
                            )
                        } else {
                            listOf(
                                primary.copy(alpha = if (isDark) 0.22f else 0.16f),
                                Color(0xFF9C27B0).copy(alpha = if (isDark) 0.16f else 0.12f)
                            )
                        }
                    )
                )
                .border(
                    width = 0.9.dp,
                    brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.50f), Color.Transparent)),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.10f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (isEnabled) strings.passcodeStatusEnabledBadge else strings.passcodeInfoTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isEnabled) strings.passcodeStatusEnabledDesc else strings.passcodeInfoText,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }

        Spacer(Modifier.height(24.dp))

        if (!isEnabled) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TwoFactorBulletRow(
                        icon = Icons.Rounded.Lock,
                        tint = primary,
                        title = strings.passcodeBullet1Title,
                        desc = strings.passcodeBullet1Desc
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorBulletRow(
                        icon = Icons.Rounded.PhoneAndroid,
                        tint = Color(0xFF9C27B0),
                        title = strings.passcodeBullet2Title,
                        desc = strings.passcodeBullet2Desc
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorBulletRow(
                        icon = Icons.Rounded.Security,
                        tint = Color(0xFFFF9800),
                        title = strings.passcodeBullet3Title,
                        desc = strings.passcodeBullet3Desc
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onEnable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = strings.passcodeEnableBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    TwoFactorActionRow(
                        icon = Icons.Rounded.Pin,
                        iconTint = primary,
                        text = strings.passcodeChangeBtn,
                        onClick = onChange
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorActionRow(
                        icon = Icons.Rounded.LockOpen,
                        iconTint = Color(0xFFF44336),
                        text = strings.passcodeDisableBtn,
                        textColor = Color(0xFFF44336),
                        onClick = onDisable
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Shared keypad                                                            */
/* ------------------------------------------------------------------------- */

/** Gradient squircle holding the lock glyph, shared by the auth and setup screens. */
@Composable
private fun PasscodeLockBadge(
    size: Dp,
    iconSize: Dp,
    locked: Boolean = true
) {
    val strings = LocalVibeStrings.current
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.size(size + 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.24f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 3))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.22f),
                            primary.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.5f),
                            primary.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(size / 3)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = strings.a11yPasscodeLock,
                modifier = Modifier.size(iconSize),
                tint = primary
            )
        }
    }
}

@Composable
private fun PasscodeContent(
    title: String,
    subtitle: String?,
    errorText: String?,
    showLockBadge: Boolean,
    pin: String,
    isError: Boolean,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    badgeSize: Dp = 72.dp
) {
    val haptics = LocalHapticFeedback.current

    // Wrong code shakes the dot row instead of only tinting it red: motion is read much
    // faster than colour, especially on the lock screen.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0 using FastOutSlowInEasing
                    -14f at 60 using FastOutSlowInEasing
                    12f at 130 using FastOutSlowInEasing
                    -8f at 200 using FastOutSlowInEasing
                    4f at 280 using FastOutSlowInEasing
                    0f at 400
                }
            )
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = verticalArrangement
    ) {
        if (showLockBadge) {
            PasscodeLockBadge(size = badgeSize, iconSize = badgeSize * 0.45f)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.graphicsLayer { translationX = shake.value }
        ) {
            for (i in 0 until PASSCODE_LENGTH) {
                val isFilled = i < pin.length
                val dotSize by animateDpAsState(
                    targetValue = if (isFilled) 18.dp else 14.dp,
                    animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
                    label = "pinDot_$i"
                )
                val dotColor = when {
                    isError -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
                }
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor)
                            .then(
                                if (isFilled && !isError) {
                                    Modifier.border(
                                        width = 4.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        shape = CircleShape
                                    )
                                } else Modifier
                            )
                    )
                }
            }
        }

        // Reserved height so the keypad never jumps when the error appears.
        Box(
            modifier = Modifier
                .height(30.dp)
                .padding(top = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            this@Column.AnimatedVisibility(
                visible = errorText != null,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(140))
            ) {
                Text(
                    text = errorText.orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val buttonSpacing = 22.dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                    row.forEach { digit ->
                        NumButton(digit) {
                            if (pin.length < PASSCODE_LENGTH) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPinChange(pin + digit)
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(74.dp)) // Empty space for alignment
                NumButton("0") {
                    if (pin.length < PASSCODE_LENGTH) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPinChange(pin + "0")
                    }
                }

                val backspaceEnabled = pin.isNotEmpty()
                val backspaceAlpha by animateFloatAsState(
                    targetValue = if (backspaceEnabled) 1f else 0.3f,
                    animationSpec = tween(160),
                    label = "backspaceAlpha"
                )
                val strings = LocalVibeStrings.current
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = backspaceEnabled,
                            onClickLabel = strings.a11yPasscodeBackspace
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPinChange(pin.dropLast(1))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Backspace,
                        contentDescription = strings.a11yPasscodeBackspace,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = backspaceAlpha),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NumButton(
    number: String,
    onClick: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()

    // Physical key feel: the button dips and brightens under the finger
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "keyScale_$number"
    )

    Box(
        modifier = Modifier
            .size(74.dp)
            .scale(pressScale)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.75f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.55f)
                        )
                    }
                )
            )
            .border(
                width = 0.7.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = strings.a11yPasscodeDigit(number),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 31.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
