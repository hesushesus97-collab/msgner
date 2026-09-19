package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flasskdev.vibe.data.VibeMessage
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import com.flasskdev.vibe.ui.auth.TwoFactorChallengeScreen
import com.flasskdev.vibe.ui.auth.TwoFactorResetScreen
import com.flasskdev.vibe.ui.components.*
import com.flasskdev.vibe.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Куда сервер отправил код входа (login_result.delivery). */
const val CODE_DELIVERY_APP = "app"
const val CODE_DELIVERY_EMAIL = "email"

/**
 * Ввод кода входа. Код приходит в чат с Vibe cat на другое устройство аккаунта; письмо
 * сервер отправляет только когда других активных сессий нет — поэтому кнопки
 * «Отправить код на почту» здесь больше нет, экран лишь показывает, куда ушёл код.
 *
 * Если включён второй фактор, после кода показывается экран пароля, а «Забыли пароль?»
 * запускает сброс через резервную почту (TwoFactorResetScreen).
 */
@Composable
fun VerificationScreen(
    email: String,
    webSocket: VibeWebSocket,
    via: String = CODE_DELIVERY_APP,
    onVerified: (Int, Boolean) -> Unit
) {
    var challenge by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    var otpCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    // Сброс второго фактора: null — обычный ввод пароля, иначе маскированный адрес
    // резервной почты, на который ушёл код сброса.
    var resetEmailMasked by remember { mutableStateOf<String?>(null) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var isResetBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val strings = LocalVibeStrings.current

    val listener = remember(strings) {
        object : VibeWebSocketListener {
            override fun onAuthResponse(message: VibeMessage) {
                if (message.type != "verify_code_result") return
                scope.launch {
                    isLoading = false
                    isResetBusy = false
                    when {
                        message.requires_two_factor -> {
                            challenge = message.challenge_token
                            hint = message.hint
                            errorMessage = null
                        }
                        message.challenge_expired -> {
                            challenge = null
                            resetEmailMasked = null
                            otpCode = ""
                            errorMessage = message.message
                        }
                        // Успех приходит и после обычного входа, и после сброса второго фактора.
                        message.success == true -> onVerified(message.user_id ?: 0, message.is_new_user == true)
                        else -> errorMessage = message.message ?: strings.codeInvalid
                    }
                }
            }

            // Ответы на запрос / подтверждение сброса второго фактора.
            override fun onSettingsResponse(message: JSONObject) {
                if (message.optString("type") != "two_factor_reset_result") return
                scope.launch {
                    isResetBusy = false
                    when {
                        message.optBoolean("success") -> {
                            resetEmailMasked = message.optString("email_masked").ifBlank { "***" }
                            resetError = null
                            errorMessage = null
                        }
                        message.optBoolean("challenge_expired") -> {
                            challenge = null
                            resetEmailMasked = null
                            otpCode = ""
                            errorMessage = message.optString("message").ifBlank { null }
                        }
                        else -> {
                            val text = when (message.optString("error")) {
                                "no_recovery_email" -> strings.twoFactorResetNoEmail
                                else -> message.optString("message").ifBlank { strings.twoFactorResetFailed }
                            }
                            if (resetEmailMasked != null) resetError = text else errorMessage = text
                        }
                    }
                }
            }

            override fun onConnected() {}
            override fun onDisconnected() {
                scope.launch {
                    isLoading = false
                    isResetBusy = false
                }
            }
            override fun onError(error: String) {
                scope.launch {
                    if (resetEmailMasked != null) resetError = error else errorMessage = error
                    isLoading = false
                    isResetBusy = false
                }
            }
        }
    }

    DisposableEffect(webSocket, listener) {
        webSocket.addListener(listener)
        onDispose {
            webSocket.removeListener(listener)
        }
    }

    val token = challenge
    if (token != null) {
        val masked = resetEmailMasked
        if (masked != null) {
            TwoFactorResetScreen(
                emailMasked = masked,
                isLoading = isResetBusy,
                errorMessage = resetError,
                onSubmit = { code ->
                    isResetBusy = true
                    resetError = null
                    webSocket.confirmTwoFactorReset(code, token)
                },
                onResend = {
                    isResetBusy = true
                    resetError = null
                    webSocket.requestTwoFactorReset(token)
                },
                onBack = { resetEmailMasked = null; resetError = null }
            )
        } else {
            TwoFactorChallengeScreen(
                hint = hint,
                attemptsLeft = null,
                isLoading = isLoading || isResetBusy,
                errorMessage = errorMessage,
                onSubmit = { password ->
                    isLoading = true
                    errorMessage = null
                    webSocket.verifyTwoFactor(token, password)
                },
                // «Забыли пароль?»: код сброса уходит на резервную почту, если она задана в настройках.
                onForgot = {
                    if (!isResetBusy) {
                        isResetBusy = true
                        errorMessage = null
                        webSocket.requestTwoFactorReset(token)
                    }
                },
                onBack = { challenge = null; otpCode = ""; errorMessage = null }
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VibeBackgroundMesh()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = strings.verificationTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Подзаголовок зависит от канала доставки, который сообщил сервер (login_result.delivery).
            Text(
                text = if (via == CODE_DELIVERY_APP) strings.verificationSubtitleApp else strings.verificationSubtitle(email),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            OtpInput(
                value = otpCode,
                onValueChange = { otpCode = it; errorMessage = null }
            )

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = VibeError,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            VibeButton(
                text = if (isLoading) strings.verifyLoading else strings.verifyBtn,
                onClick = {
                    isLoading = true
                    webSocket.verifyCode(email, otpCode)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && otpCode.length == 6
            )

            // Контент смещён чуть выше центра — симметрично экрану входа.
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
