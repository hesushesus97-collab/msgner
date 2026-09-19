package com.flasskdev.vibe.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import org.json.JSONObject
import com.flasskdev.vibe.ui.components.VibeToast
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import com.flasskdev.vibe.ui.theme.luminanceIsDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TwoFactorStep {
    STATUS,
    ENTER_CURRENT,
    ENTER_NEW,
    CONFIRM_NEW,
    ENTER_HINT,
    RECOVERY_EMAIL,
    RECOVERY_CODE,
    RESET_CODE
}

private enum class CurrentPasswordTarget {
    CHANGE_PASSWORD,
    CHANGE_HINT,
    DISABLE,
    RECOVERY_EMAIL,
    REMOVE_RECOVERY
}

@Composable
private fun TwoFactorStatusContent(
    isEnabled: Boolean,
    currentHint: String?,
    strings: VibeStrings,
    isDark: Boolean,
    onSetPassword: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeHint: () -> Unit,
    onDisableClick: () -> Unit,
    recoveryEmail: String?,
    onRecoveryEmailClick: () -> Unit,
    onRemoveRecoveryEmail: () -> Unit,
    onResetClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Icon Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = if (isEnabled) {
                                listOf(
                                    Color(0xFF4CAF50).copy(alpha = if (isDark) 0.22f else 0.16f),
                                    Color(0xFF2196F3).copy(alpha = if (isDark) 0.16f else 0.12f)
                                )
                            } else {
                                listOf(
                                    Color(0xFF2196F3).copy(alpha = if (isDark) 0.22f else 0.16f),
                                    Color(0xFF9C27B0).copy(alpha = if (isDark) 0.16f else 0.12f)
                                )
                            }
                        )
                    )
                    .border(
                        width = 0.9.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isEnabled) Color(0xFF4CAF50).copy(alpha = 0.50f) else Color(0xFF2196F3).copy(alpha = 0.50f),
                                Color.Transparent
                            )
                        ),
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
                            Brush.radialGradient(
                                listOf(
                                    (if (isEnabled) Color(0xFF4CAF50) else Color(0xFF2196F3)).copy(alpha = 0.35f),
                                    (if (isEnabled) Color(0xFF4CAF50) else Color(0xFF2196F3)).copy(alpha = 0.10f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = if (isEnabled) Color(0xFF4CAF50) else Color(0xFF2196F3),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = if (isEnabled) strings.twoFactorEnabledBadge else strings.twoFactorTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isEnabled) strings.twoFactorEnabledDesc else strings.twoFactorDescription,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )

                if (isEnabled && !currentHint.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TipsAndUpdates,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.twoFactorCurrentHintPill(currentHint),
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!isEnabled) {
            // Feature bullets
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TwoFactorBulletRow(
                        icon = Icons.Rounded.Security,
                        tint = Color(0xFF2196F3),
                        title = strings.twoFactorBullet1Title,
                        desc = strings.twoFactorBullet1Desc
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorBulletRow(
                        icon = Icons.Rounded.VpnKey,
                        tint = Color(0xFF9C27B0),
                        title = strings.twoFactorBullet2Title,
                        desc = strings.twoFactorBullet2Desc
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorBulletRow(
                        icon = Icons.Rounded.TipsAndUpdates,
                        tint = Color(0xFFFF9800),
                        title = strings.twoFactorBullet3Title,
                        desc = strings.twoFactorBullet3Desc
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onSetPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = strings.twoFactorSetPasswordBtn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            }
        } else {
            // Actions when enabled
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    TwoFactorActionRow(
                        icon = Icons.Rounded.Key,
                        iconTint = Color(0xFF2196F3),
                        text = strings.twoFactorChangePasswordBtn,
                        onClick = onChangePassword
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorActionRow(
                        icon = Icons.Rounded.TipsAndUpdates,
                        iconTint = Color(0xFFFF9800),
                        text = strings.twoFactorChangeHintBtn,
                        onClick = onChangeHint
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorActionRow(
                        icon = Icons.Rounded.LockOpen,
                        iconTint = Color(0xFFF44336),
                        text = strings.twoFactorDisableBtn,
                        textColor = Color(0xFFF44336),
                        onClick = onDisableClick
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Резервная почта: только на неё уходит код сброса пароля защиты.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
            border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                TwoFactorActionRow(
                    icon = Icons.Rounded.AlternateEmail,
                    iconTint = Color(0xFF00BCD4),
                    text = strings.twoFactorRecoveryEmailBtn,
                    value = recoveryEmail ?: strings.twoFactorRecoveryEmailNotSet,
                    onClick = onRecoveryEmailClick
                )
                if (recoveryEmail != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        iconTint = Color(0xFF9E9E9E),
                        text = strings.twoFactorRecoveryEmailRemoveBtn,
                        onClick = onRemoveRecoveryEmail
                    )
                }
                if (isEnabled) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp),
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    )
                    TwoFactorActionRow(
                        icon = Icons.Rounded.LockReset,
                        iconTint = Color(0xFFFF9800),
                        text = strings.twoFactorResetBtn,
                        onClick = onResetClick
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        SettingsFootnote(strings.twoFactorRecoveryEmailFootnote)
    }
}

@Composable
private fun TwoFactorNewPasswordStep(
    strings: VibeStrings,
    isDark: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String?,
    onNext: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val strength = remember(password) { calculatePasswordStrength(password) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = strings.twoFactorEnterNewPasswordTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = strings.twoFactorEnterNewPasswordSubtitle,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(strings.twoFactorPasswordFieldLabel) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { if (password.length >= 6) onNext() }),
            shape = RoundedCornerShape(16.dp),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        if (password.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            TwoFactorStrengthIndicator(strength = strength, strings = strings)
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onNext,
            enabled = password.length >= 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = strings.twoFactorNextBtn,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
            )
        }
    }
}

@Composable
private fun TwoFactorConfirmPasswordStep(
    strings: VibeStrings,
    isDark: Boolean,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    errorMessage: String?,
    onNext: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = strings.twoFactorRepeatPasswordTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = strings.twoFactorRepeatPasswordSubtitle,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(strings.twoFactorConfirmFieldLabel) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (confirmPassword.isNotEmpty()) onNext() }),
            shape = RoundedCornerShape(16.dp),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onNext,
            enabled = confirmPassword.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = strings.twoFactorNextBtn,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
            )
        }
    }
}

@Composable
private fun TwoFactorCurrentPasswordStep(
    strings: VibeStrings,
    isDark: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String?,
    onNext: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Password,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = strings.twoFactorEnterCurrentPasswordTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = strings.twoFactorEnterCurrentPasswordSubtitle,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(strings.twoFactorCurrentFieldLabel) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (password.isNotEmpty()) onNext() }),
            shape = RoundedCornerShape(16.dp),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onNext,
            enabled = password.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = strings.twoFactorNextBtn,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
            )
        }
    }
}

@Composable
private fun TwoFactorHintStep(
    strings: VibeStrings,
    isDark: Boolean,
    hint: String,
    onHintChange: (String) -> Unit,
    passwordToAvoid: String,
    errorMessage: String?,
    onSave: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val hintTooLong = hint.length > 32
    val hintContainsPassword = passwordToAvoid.isNotBlank() && hint.isNotBlank() && hint.contains(passwordToAvoid, ignoreCase = true)

    val validationError = when {
        errorMessage != null -> errorMessage
        hintTooLong -> strings.twoFactorHintTooLong
        hintContainsPassword -> strings.twoFactorHintContainsPassword
        else -> null
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.TipsAndUpdates,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = strings.twoFactorHintTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = strings.twoFactorHintSubtitle,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = hint,
            onValueChange = { if (it.length <= 40) onHintChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(strings.twoFactorHintFieldLabel) },
            placeholder = { Text(strings.twoFactorHintPlaceholder) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            isError = validationError != null,
            supportingText = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = validationError ?: strings.twoFactorHintPublicWarning,
                        color = if (validationError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${hint.length}/32",
                        color = if (hintTooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }
            }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { onSave(hint.trim()) },
            enabled = !hintTooLong && !hintContainsPassword,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = strings.twoFactorSaveBtn,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp
            )
        }

        if (hint.isBlank()) {
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = { onSave("") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.twoFactorSkipBtn,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.5.sp
                )
            }
        }
    }
}

/**
 * Ввод резервной почты. Пароль защиты, если она включена, собирается предыдущим шагом
 * (ENTER_CURRENT) — сервер требует его в set_recovery_email/request.
 */
@Composable
private fun TwoFactorRecoveryEmailStep(
    strings: VibeStrings,
    email: String,
    onEmailChange: (String) -> Unit,
    currentEmail: String?,
    errorMessage: String?,
    onSend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val emailPattern = remember { Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") }
    val trimmed = email.trim()
    val emailValid = emailPattern.matches(trimmed)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Rounded.AlternateEmail,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = strings.twoFactorRecoveryEmailTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = strings.twoFactorRecoveryEmailSubtitle,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
        if (!currentEmail.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.twoFactorRecoveryEmailCurrent(currentEmail),
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(strings.twoFactorRecoveryEmailFieldLabel) },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (emailValid) onSend() }),
            shape = RoundedCornerShape(16.dp),
            isError = errorMessage != null || (email.isNotBlank() && !emailValid),
            supportingText = when {
                errorMessage != null -> { { Text(errorMessage, color = MaterialTheme.colorScheme.error) } }
                email.isNotBlank() && !emailValid -> {
                    { Text(strings.twoFactorRecoveryEmailInvalid, color = MaterialTheme.colorScheme.error) }
                }
                else -> null
            }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onSend,
            enabled = emailValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(strings.twoFactorRecoveryEmailSendCodeBtn, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
        }
    }
}

/** Ввод кода из письма: подтверждение резервной почты и сброс защиты. */
@Composable
private fun TwoFactorCodeStep(
    strings: VibeStrings,
    title: String,
    subtitle: String,
    warning: String?,
    confirmText: String,
    code: String,
    onCodeChange: (String) -> Unit,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onResend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Rounded.MarkEmailRead,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        if (warning != null) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFF9800).copy(alpha = 0.10f),
                border = BorderStroke(0.8.dp, Color(0xFFFF9800).copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.WarningAmber, null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = warning,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { onCodeChange(it.filter(Char::isDigit).take(6)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(strings.verificationTitle) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (code.length == 6) onConfirm() }),
            shape = RoundedCornerShape(16.dp),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onConfirm,
            enabled = code.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(confirmText, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
        }

        Spacer(Modifier.height(6.dp))

        TextButton(onClick = onResend) {
            Text(strings.twoFactorResendCodeBtn, fontSize = 13.5.sp)
        }
    }
}

@Composable
private fun TwoFactorStrengthIndicator(strength: Int, strings: VibeStrings) {
    val label = when (strength) {
        0, 1 -> strings.twoFactorStrengthWeak
        2 -> strings.twoFactorStrengthMedium
        3 -> strings.twoFactorStrengthStrong
        else -> strings.twoFactorStrengthVeryStrong
    }
    val color = when (strength) {
        0, 1 -> Color(0xFFF44336)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFF4CAF50)
        else -> Color(0xFF00C853)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(4) { i ->
                val filled = i < strength
                val animColor by animateColorAsState(
                    targetValue = if (filled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    label = "strength_seg_$i"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(animColor)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun TwoFactorBulletRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
internal fun TwoFactorActionRow(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    value: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            value?.let { right ->
                Text(
                    text = right,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier
                        .widthIn(max = 150.dp)
                        .padding(end = 6.dp)
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TwoFactorDisableDialog(
    strings: VibeStrings,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.GppBad,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = strings.twoFactorDisableConfirmTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.twoFactorDisableConfirmDesc,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(strings.twoFactorCurrentFieldLabel) },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(strings.twoFactorDisableAction)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn)
            }
        }
    )
}

private fun calculatePasswordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    val weak = listOf("password", "123456", "qwerty", "111111", "vibe", "пароль")
    if (weak.any { password.contains(it, ignoreCase = true) }) score = minOf(score, 1)
    return score.coerceIn(0, 4)
}

/* ------------------------------------------------------------------------- */
/*  Server-backed flow (get_two_factor / set_two_factor)                      */
/* ------------------------------------------------------------------------- */

private data class PendingTwoFactorOp(val operation: String, val wasEnabled: Boolean)

/**
 * Экран второго фактора в настройках. Дизайн общий с «Приватностью»
 * ([SettingsSubPage]), а пошаговые формы выше переиспользуются как есть.
 * Все операции подтверждает сервер: локальное состояние обновляется только
 * после `two_factor_result`.
 */
@Composable
fun ServerTwoFactorSettingsContent(prefs: UserPreferences, ws: VibeWebSocket, onBack: () -> Unit) {
    val strings = LocalVibeStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val scope = rememberCoroutineScope()
    val revision = preferenceRevision(prefs)
    val enabled = remember(revision) { prefs.twoFactorEnabled }
    val savedHint = remember(revision) { prefs.twoFactorHint }

    var step by remember { mutableStateOf(TwoFactorStep.STATUS) }
    var target by remember { mutableStateOf(CurrentPasswordTarget.CHANGE_PASSWORD) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var stepError by remember { mutableStateOf<String?>(null) }

    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(true) }
    var pending by remember { mutableStateOf<PendingTwoFactorOp?>(null) }
    var banner by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var showDisableDialog by remember { mutableStateOf(false) }

    val savedRecoveryEmail = remember(revision) { prefs.twoFactorRecoveryEmail }
    var recoveryEmailInput by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var pendingRecoveryEmail by remember { mutableStateOf<String?>(null) }
    var resetEmailMasked by remember { mutableStateOf<String?>(null) }

    fun resetFlow() {
        step = TwoFactorStep.STATUS
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
        hint = ""
        stepError = null
        recoveryEmailInput = ""
        code = ""
        pendingRecoveryEmail = null
        resetEmailMasked = null
    }

    DisposableEffect(ws) {
        val listener = object : VibeWebSocketListener {
            override fun onSettingsResponse(message: JSONObject) {
                when (message.optString("type")) {
                    "two_factor_result" -> scope.launch {
                        busy = false
                        val op = pending
                        pending = null
                        if (message.optBoolean("success")) {
                            ready = true
                            when {
                                // Сброс через резервную почту: сервер уже выключил защиту.
                                message.optBoolean("reset") -> {
                                    banner = strings.twoFactorResetDoneToast to false
                                    resetFlow()
                                }
                                op != null -> {
                                    banner = when (op.operation) {
                                        "disable" -> strings.twoFactorSuccessDisabledToast
                                        "hint" -> strings.twoFactorSuccessChangedToast
                                        else -> if (op.wasEnabled) strings.twoFactorSuccessChangedToast else strings.twoFactorSuccessSetToast
                                    } to false
                                    resetFlow()
                                }
                            }
                        } else {
                            val text = message.optString("message").ifBlank { strings.twoFactorPasswordWrong }
                            when {
                                op != null && op.wasEnabled -> {
                                    currentPassword = ""
                                    step = TwoFactorStep.ENTER_CURRENT
                                    stepError = text
                                }
                                op != null -> {
                                    step = TwoFactorStep.ENTER_NEW
                                    stepError = text
                                }
                                else -> banner = text to true
                            }
                        }
                    }

                    "recovery_email_result" -> scope.launch {
                        busy = false
                        pending = null
                        if (message.optBoolean("success")) {
                            ready = true
                            when (message.optString("step")) {
                                "code_sent" -> {
                                    pendingRecoveryEmail = message.optString("email").ifBlank { recoveryEmailInput.trim() }
                                    code = ""
                                    stepError = null
                                    step = TwoFactorStep.RECOVERY_CODE
                                }
                                "confirmed" -> {
                                    banner = strings.twoFactorRecoveryEmailSavedToast to false
                                    resetFlow()
                                }
                                "removed" -> {
                                    banner = strings.twoFactorRecoveryEmailRemovedToast to false
                                    resetFlow()
                                }
                            }
                        } else {
                            val text = message.optString("message").ifBlank { strings.twoFactorRecoveryEmailInvalid }
                            when {
                                // Неверный пароль защиты — возвращаем на шаг его ввода.
                                message.optString("error") == "wrong_password" -> {
                                    currentPassword = ""
                                    step = TwoFactorStep.ENTER_CURRENT
                                    stepError = text
                                }
                                step == TwoFactorStep.STATUS -> banner = text to true
                                else -> stepError = text
                            }
                        }
                    }

                    "two_factor_reset_result" -> scope.launch {
                        busy = false
                        pending = null
                        if (message.optBoolean("success")) {
                            ready = true
                            resetEmailMasked = message.optString("email_masked").ifBlank { "***" }
                            code = ""
                            stepError = null
                            step = TwoFactorStep.RESET_CODE
                        } else {
                            val text = when (message.optString("error")) {
                                "no_recovery_email" -> strings.twoFactorResetNoEmail
                                else -> message.optString("message").ifBlank { strings.twoFactorResetFailed }
                            }
                            if (step == TwoFactorStep.RESET_CODE) stepError = text else banner = text to true
                        }
                    }
                }
            }

            override fun onDisconnected() {
                scope.launch { busy = false; ready = false; banner = strings.twoFactorDisconnected to true }
            }

            override fun onConnected() {
                ws.sendRawJson("{\"type\":\"get_two_factor\"}")
            }
        }
        ws.addListener(listener)
        ws.sendRawJson("{\"type\":\"get_two_factor\"}")
        onDispose { ws.removeListener(listener) }
    }

    LaunchedEffect(busy) {
        if (busy) {
            delay(15_000)
            busy = false
            pending = null
            banner = strings.twoFactorNoServerResponse to true
        }
    }

    fun send(operation: String) {
        busy = true
        banner = null
        stepError = null
        pending = PendingTwoFactorOp(operation, enabled)
        ws.sendRawJson(
            JSONObject()
                .put("type", "set_two_factor")
                .put("operation", operation)
                .put("current_password", currentPassword)
                .put("password", newPassword)
                .put("hint", hint)
                .toString()
        )
    }

    fun sendRecoveryRequest() {
        busy = true; banner = null; stepError = null
        ws.requestRecoveryEmail(recoveryEmailInput.trim(), currentPassword)
    }

    fun sendRecoveryConfirm() {
        busy = true; banner = null; stepError = null
        ws.confirmRecoveryEmail(code)
    }

    fun sendRecoveryRemove() {
        busy = true; banner = null; stepError = null
        ws.removeRecoveryEmail(currentPassword)
    }

    fun sendResetRequest() {
        busy = true; banner = null; stepError = null
        ws.requestTwoFactorReset()
    }

    fun sendResetConfirm() {
        busy = true; banner = null; stepError = null
        ws.confirmTwoFactorReset(code)
    }

    val inFlow = step != TwoFactorStep.STATUS
    val interactive = ready && !busy
    BackHandler(enabled = inFlow) { resetFlow() }

    SettingsSubPage(
        title = strings.twoFactorTitle,
        subtitle = when {
            !ready -> strings.twoFactorCheckingServer
            enabled -> strings.twoFactorStatusEnabled
            else -> strings.twoFactorStatusDisabled
        },
        onBack = { if (inFlow) resetFlow() else onBack() }
    ) {
        AnimatedVisibility(
            visible = busy,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }

        banner?.let { (text, isError) ->
            SettingsStatusBanner(
                text = text,
                tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                icon = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle
            )
            Spacer(Modifier.height(16.dp))
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally { it / 6 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally { -it / 6 })
            },
            label = "twoFactorStep"
        ) { current ->
            when (current) {
                TwoFactorStep.STATUS -> Box(modifier = Modifier.alpha(if (ready) 1f else 0.55f)) {
                    TwoFactorStatusContent(
                        isEnabled = enabled,
                        currentHint = savedHint,
                        strings = strings,
                        isDark = isDark,
                        onSetPassword = {
                            if (interactive) {
                                target = CurrentPasswordTarget.CHANGE_PASSWORD
                                step = TwoFactorStep.ENTER_NEW
                            }
                        },
                        onChangePassword = {
                            if (interactive) {
                                target = CurrentPasswordTarget.CHANGE_PASSWORD
                                step = TwoFactorStep.ENTER_CURRENT
                            }
                        },
                        onChangeHint = {
                            if (interactive) {
                                target = CurrentPasswordTarget.CHANGE_HINT
                                hint = savedHint.orEmpty()
                                step = TwoFactorStep.ENTER_CURRENT
                            }
                        },
                        onDisableClick = { if (interactive) showDisableDialog = true },
                        recoveryEmail = savedRecoveryEmail,
                        onRecoveryEmailClick = {
                            if (interactive) {
                                stepError = null
                                if (enabled) {
                                    target = CurrentPasswordTarget.RECOVERY_EMAIL
                                    currentPassword = ""
                                    step = TwoFactorStep.ENTER_CURRENT
                                } else {
                                    recoveryEmailInput = savedRecoveryEmail.orEmpty()
                                    step = TwoFactorStep.RECOVERY_EMAIL
                                }
                            }
                        },
                        onRemoveRecoveryEmail = {
                            if (interactive) {
                                stepError = null
                                if (enabled) {
                                    target = CurrentPasswordTarget.REMOVE_RECOVERY
                                    currentPassword = ""
                                    step = TwoFactorStep.ENTER_CURRENT
                                } else {
                                    currentPassword = ""
                                    sendRecoveryRemove()
                                }
                            }
                        },
                        onResetClick = { if (interactive) sendResetRequest() }
                    )
                }

                TwoFactorStep.ENTER_CURRENT -> TwoFactorCurrentPasswordStep(
                    strings = strings,
                    isDark = isDark,
                    password = currentPassword,
                    onPasswordChange = { currentPassword = it; stepError = null },
                    errorMessage = stepError,
                    onNext = {
                        when (target) {
                            CurrentPasswordTarget.CHANGE_HINT -> step = TwoFactorStep.ENTER_HINT
                            CurrentPasswordTarget.RECOVERY_EMAIL -> {
                                recoveryEmailInput = savedRecoveryEmail.orEmpty()
                                stepError = null
                                step = TwoFactorStep.RECOVERY_EMAIL
                            }
                            CurrentPasswordTarget.REMOVE_RECOVERY -> sendRecoveryRemove()
                            else -> step = TwoFactorStep.ENTER_NEW
                        }
                    }
                )

                TwoFactorStep.ENTER_NEW -> TwoFactorNewPasswordStep(
                    strings = strings,
                    isDark = isDark,
                    password = newPassword,
                    onPasswordChange = { newPassword = it; stepError = null },
                    errorMessage = stepError,
                    onNext = {
                        when {
                            newPassword.length < 6 -> stepError = strings.twoFactorPasswordTooShort
                            newPassword.toByteArray().size > 72 -> stepError = strings.twoFactorPasswordTooLong
                            else -> {
                                confirmPassword = ""
                                step = TwoFactorStep.CONFIRM_NEW
                            }
                        }
                    }
                )

                TwoFactorStep.CONFIRM_NEW -> TwoFactorConfirmPasswordStep(
                    strings = strings,
                    isDark = isDark,
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it; stepError = null },
                    errorMessage = stepError,
                    onNext = {
                        if (confirmPassword != newPassword) {
                            stepError = strings.twoFactorPasswordMismatch
                        } else {
                            hint = ""
                            step = TwoFactorStep.ENTER_HINT
                        }
                    }
                )

                TwoFactorStep.ENTER_HINT -> TwoFactorHintStep(
                    strings = strings,
                    isDark = isDark,
                    hint = hint,
                    onHintChange = { hint = it; stepError = null },
                    passwordToAvoid = if (target == CurrentPasswordTarget.CHANGE_HINT) "" else newPassword,
                    errorMessage = stepError,
                    onSave = { finalHint ->
                        hint = finalHint
                        send(if (target == CurrentPasswordTarget.CHANGE_HINT) "hint" else "set")
                    }
                )

                TwoFactorStep.RECOVERY_EMAIL -> TwoFactorRecoveryEmailStep(
                    strings = strings,
                    email = recoveryEmailInput,
                    onEmailChange = { recoveryEmailInput = it; stepError = null },
                    currentEmail = savedRecoveryEmail,
                    errorMessage = stepError,
                    onSend = { sendRecoveryRequest() }
                )

                TwoFactorStep.RECOVERY_CODE -> TwoFactorCodeStep(
                    strings = strings,
                    title = strings.twoFactorRecoveryCodeTitle,
                    subtitle = strings.twoFactorRecoveryCodeSubtitle(pendingRecoveryEmail.orEmpty()),
                    warning = null,
                    confirmText = strings.twoFactorRecoveryConfirmBtn,
                    code = code,
                    onCodeChange = { code = it; stepError = null },
                    errorMessage = stepError,
                    onConfirm = { sendRecoveryConfirm() },
                    onResend = { sendRecoveryRequest() }
                )

                TwoFactorStep.RESET_CODE -> TwoFactorCodeStep(
                    strings = strings,
                    title = strings.twoFactorResetTitle,
                    subtitle = strings.twoFactorResetSubtitle(resetEmailMasked.orEmpty()),
                    warning = strings.twoFactorResetWarning,
                    confirmText = strings.twoFactorResetSettingsConfirmBtn,
                    code = code,
                    onCodeChange = { code = it; stepError = null },
                    errorMessage = stepError,
                    onConfirm = { sendResetConfirm() },
                    onResend = { sendResetRequest() }
                )
            }
        }
    }

    if (showDisableDialog) {
        TwoFactorDisableDialog(
            strings = strings,
            onConfirm = { password ->
                currentPassword = password
                showDisableDialog = false
                send("disable")
            },
            onDismiss = { showDisableDialog = false }
        )
    }
}
