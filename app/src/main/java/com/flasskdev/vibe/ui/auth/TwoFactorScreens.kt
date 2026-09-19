package com.flasskdev.vibe.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.components.VibeToastHost
import com.flasskdev.vibe.ui.theme.*

/* ============================================================================
 *  ЭКРАНЫ ДВУХФАКТОРНОЙ АУТЕНТИФИКАЦИИ
 * ========================================================================== */

/**
 * Экран второго фактора при входе. Показывается ПОСЛЕ кода из письма, если 2FA включена.
 *
 * Дизайн приведён к остальному приложению: вместо фиолетового радиального «свечения»
 * в центре фона — общее синее сияние сверху (VibeTopGlow), акценты в цвете primary,
 * контент собран в карточку.
 */
@Composable
fun TwoFactorChallengeScreen(
    hint: String?,
    attemptsLeft: Int?,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: (password: String) -> Unit,
    onForgot: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()

    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var hintShown by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    // Тряска поля при неверном пароле — понятнее любой надписи.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            shake.animateTo(1f, keyframes {
                durationMillis = 380
                0f at 0; 1f at 60; -1f at 120; 0.6f at 190; -0.4f at 260; 0f at 380
            })
            shake.snapTo(0f)
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler { onBack() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        VibeTopGlow(height = 420.dp)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            /* ---------- top bar ---------- */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.backBtn,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.72f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = strings.twoFactorChallengeTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.twoFactorChallengeSubtitle,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                /* ---------- card ---------- */
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                    shadowElevation = 0.dp
                ) {
                    Column(Modifier.padding(18.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { if (it.length <= 128) password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focus)
                                .graphicsLayer { translationX = shake.value * 16f },
                            label = { Text(strings.twoFactorPasswordFieldLabel) },
                            singleLine = true,
                            isError = errorMessage != null,
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { if (password.isNotEmpty() && !isLoading) onSubmit(password) }),
                            leadingIcon = {
                                Icon(Icons.Rounded.Lock, null, tint = primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { visible = !visible }) {
                                    Icon(
                                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (visible) strings.a11yHidePassword else strings.a11yShowPassword,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primary,
                                focusedLabelColor = primary,
                                cursorColor = primary
                            )
                        )

                        /* ---------- подсказка ---------- */
                        if (!hint.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            // Подсказку не показываем сразу: она нужна, только когда
                            // пароль реально забыт, а на экране её видит любой, кто
                            // заглянет через плечо.
                            AnimatedContent(
                                targetState = hintShown,
                                transitionSpec = { fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically() },
                                label = "hint"
                            ) { shown ->
                                if (shown) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(primary.copy(alpha = 0.10f))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Rounded.TipsAndUpdates, null,
                                            tint = primary, modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = strings.twoFactorYourHint,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = primary
                                            )
                                            Text(
                                                text = hint,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                } else {
                                    TextButton(
                                        onClick = { hintShown = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.TipsAndUpdates, null, modifier = Modifier.size(18.dp), tint = primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(strings.twoFactorShowHint, color = primary)
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(errorMessage != null) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.ErrorOutline, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = buildString {
                                        append(errorMessage.orEmpty())
                                        if (attemptsLeft != null && attemptsLeft > 0) {
                                            append(" · ")
                                            append(strings.twoFactorAttemptsLeft(attemptsLeft))
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = { onSubmit(password) },
                            enabled = password.isNotEmpty() && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(strings.twoFactorSignInBtn, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        TextButton(onClick = onForgot, modifier = Modifier.fillMaxWidth()) {
                            Text(strings.twoFactorForgotPassword, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Смещаем блок чуть выше центра: так же, как на экране входа.
                Spacer(Modifier.height(88.dp))
            }
        }

        VibeToastHost(Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Сброс пароля второго фактора при входе: код ушёл на резервную почту (адрес показан
 * маскированным — владение аккаунтом ещё не доказано). Успех приходит как
 * verify_code_result и обрабатывается VerificationScreen; этот экран сам не навигирует.
 */
@Composable
fun TwoFactorResetScreen(
    emailMasked: String,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: (code: String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    var code by remember { mutableStateOf("") }

    BackHandler { onBack() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        VibeTopGlow(height = 420.dp)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, enabled = !isLoading) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.backBtn,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.72f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.LockReset, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = strings.twoFactorResetTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.twoFactorResetSubtitle(emailMasked),
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                    shadowElevation = 0.dp
                ) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        com.flasskdev.vibe.ui.components.OtpInput(
                            value = code,
                            onValueChange = { code = it }
                        )

                        AnimatedVisibility(errorMessage != null) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.ErrorOutline, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // После сброса защита выключается — пользователь должен видеть это до нажатия.
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(primary.copy(alpha = 0.10f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Rounded.Info, null, tint = primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.twoFactorResetWarning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = { onSubmit(code) },
                            enabled = code.length == 6 && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(strings.twoFactorResetConfirmBtn, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        TextButton(onClick = onResend, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
                            Text(strings.twoFactorResendCodeBtn, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(88.dp))
            }
        }

        VibeToastHost(Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Настройка 2FA: пароль + подтверждение + подсказка (макс. 32 символа).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorSetupScreen(
    isEnabled: Boolean,
    currentHint: String?,
    isLoading: Boolean,
    onSave: (password: String, hint: String, currentPassword: String?) -> Unit,
    onDisable: (currentPassword: String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    var currentPassword by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf(currentHint.orEmpty()) }
    var showDisable by remember { mutableStateOf(false) }
    val glass = glassStyle()

    val strength = remember(password) { passwordStrength(password) }
    val hintTooLong = hint.length > 32
    val hintContainsPassword = password.isNotEmpty() && hint.contains(password, ignoreCase = true)
    val mismatch = confirm.isNotEmpty() && confirm != password

    val canSave = password.length >= 6 && !mismatch && confirm.isNotEmpty() &&
                  !hintTooLong && !hintContainsPassword &&
                  (!isEnabled || currentPassword.isNotEmpty())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEnabled) strings.twoFactorChangePasswordBtn else strings.twoFactorTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.backBtn) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollCompat()
                .padding(horizontal = VibeSpacing.lg)
                .imePadding(),
        ) {
            InfoCard(glass)

            Spacer(Modifier.height(VibeSpacing.xl))

            if (isEnabled) {
                SecureField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = strings.twoFactorCurrentPasswordLabel
                )
                Spacer(Modifier.height(VibeSpacing.md))
            }

            SecureField(value = password, onValueChange = { password = it }, label = strings.twoFactorNewPasswordLabel)

            if (password.isNotEmpty()) {
                Spacer(Modifier.height(VibeSpacing.sm))
                StrengthBar(strength)
            }

            Spacer(Modifier.height(VibeSpacing.md))

            SecureField(
                value = confirm,
                onValueChange = { confirm = it },
                label = strings.twoFactorRepeatPasswordLabel,
                isError = mismatch,
                supporting = if (mismatch) strings.twoFactorPasswordMismatch else null
            )

            Spacer(Modifier.height(VibeSpacing.xl))

            /* ---------- подсказка ---------- */
            OutlinedTextField(
                value = hint,
                onValueChange = { if (it.length <= 40) hint = it },   // 40 чтобы показать ошибку, а не обрезать молча
                label = { Text(strings.twoFactorHintFieldLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = hintTooLong || hintContainsPassword,
                shape = RoundedCornerShape(VibeRadius.md),
                supportingText = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            when {
                                hintContainsPassword -> strings.twoFactorHintNoPassword
                                hintTooLong -> strings.twoFactorHintTooLongShort
                                else -> strings.twoFactorHintPublicDesc
                            },
                            color = if (hintTooLong || hintContainsPassword) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${hint.length}/32",
                            color = if (hintTooLong) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibeViolet, focusedLabelColor = VibeViolet, cursorColor = VibeViolet
                )
            )

            Spacer(Modifier.height(VibeSpacing.xxl))

            Button(
                onClick = { onSave(password, hint.trim(), currentPassword.takeIf { isEnabled }) },
                enabled = canSave && !isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(VibeRadius.md),
                colors = ButtonDefaults.buttonColors(containerColor = VibeViolet)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (isEnabled) strings.saveBtn else strings.twoFactorSetPasswordBtn)
            }

            if (isEnabled) {
                Spacer(Modifier.height(VibeSpacing.md))
                TextButton(
                    onClick = { showDisable = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.twoFactorDisableButton, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(VibeSpacing.section))
        }
    }

    if (showDisable) {
        DisableDialog(
            onConfirm = { pwd -> showDisable = false; onDisable(pwd) },
            onDismiss = { showDisable = false }
        )
    }
}

@Composable
private fun InfoCard(glass: VibeGlassStyle) {
    val strings = LocalVibeStrings.current
    Row(
        Modifier.fillMaxWidth().vibeCard(glass, VibeRadius.lg).padding(VibeSpacing.lg),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.Shield, null, tint = VibeViolet, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(VibeSpacing.md))
        Column {
            Text(
                strings.twoFactorAdditionalPasswordTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(VibeSpacing.xs))
            Text(
                strings.twoFactorAdditionalPasswordDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SecureField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    supporting: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 128) onValueChange(it) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(VibeRadius.md),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = supporting?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VibeViolet, focusedLabelColor = VibeViolet, cursorColor = VibeViolet
        )
    )
}

@Composable
private fun StrengthBar(strength: Int) {
    val strings = LocalVibeStrings.current
    val label = when (strength) {
        0, 1 -> strings.passwordStrengthWeak
        2 -> strings.passwordStrengthMedium
        3 -> strings.passwordStrengthGood
        else -> strings.passwordStrengthStrong
    }
    val color = when (strength) {
        0, 1 -> VibeError
        2 -> VibeWarning
        3 -> VibeSuccess
        else -> VibeSuccess
    }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(4) { i ->
                val filled = i < strength
                val animColor by animateColorAsState(
                    if (filled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    label = "seg$i"
                )
                Box(
                    Modifier
                        .weight(1f).height(4.dp)
                        .clip(RoundedCornerShape(VibeRadius.pill))
                        .background(animColor)
                )
            }
        }
        Spacer(Modifier.height(VibeSpacing.xs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun DisableDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalVibeStrings.current
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(VibeRadius.lg),
        icon = { Icon(Icons.Rounded.GppBad, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(strings.twoFactorDisableDialogTitle) },
        text = {
            Column {
                Text(strings.twoFactorDisableDialogDesc)
                Spacer(Modifier.height(VibeSpacing.md))
                SecureField(pwd, { pwd = it }, strings.twoFactorCurrentPasswordLabel)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pwd) }, enabled = pwd.isNotEmpty()) {
                Text(strings.twoFactorDisableAction, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancelBtn) } }
    )
}

/** Оценка стойкости 0..4. Без zxcvbn: она бы утянула 1.5 МБ словарей в APK. */
private fun passwordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    // Явно слабые пароли не должны показывать зелёное.
    val weak = listOf("password", "123456", "qwerty", "111111", "vibe", "пароль")
    if (weak.any { password.contains(it, ignoreCase = true) }) score = minOf(score, 1)
    return score.coerceIn(0, 4)
}

@Composable
private fun Modifier.verticalScrollCompat(): Modifier =
    this.then(Modifier.verticalScroll(rememberScrollState()))
