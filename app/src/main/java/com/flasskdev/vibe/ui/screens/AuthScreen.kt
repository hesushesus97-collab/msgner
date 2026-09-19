package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.data.VibeMessage
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import com.flasskdev.vibe.ui.components.*
import com.flasskdev.vibe.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------------- */
/*  Локальные токены экрана                                                  */
/* ------------------------------------------------------------------------- */

private val AuthSuccess = Color(0xFF22C55E)
private val CardRadius = 28.dp
private val PillRadius = 999.dp
private const val LanguagePillHoldMs = 1600L

/* ------------------------------------------------------------------------- */
/*  Screen                                                                   */
/* ------------------------------------------------------------------------- */

@Composable
fun AuthScreen(
    webSocket: VibeWebSocket,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    language: String,
    onLanguageToggle: () -> Unit,
    /** delivery: куда сервер отправил код — CODE_DELIVERY_APP (чат с Vibe cat) или CODE_DELIVERY_EMAIL. */
    onAuthSuccess: (email: String, delivery: String) -> Unit
) {
    val strings = LocalVibeStrings.current

    var isRegister by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    // Результат проверки доступности: null = ещё не проверяли
    var emailFree by remember { mutableStateOf<Boolean?>(null) }
    var usernameFree by remember { mutableStateOf<Boolean?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val emailPattern = remember { Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$") }
    val isEmailValid = email.isEmpty() || emailPattern.matches(email)

    // strings вынесены в ключ: раньше слушатель «замораживал» первый язык
    val listener = remember(strings) {
        object : VibeWebSocketListener {
            override fun onAuthResponse(message: VibeMessage) {
                scope.launch {
                    when (message.type) {
                        "check_availability_result" -> {
                            isChecking = false
                            val emailTaken = message.email_taken == true
                            val usernameTaken = message.username_taken == true
                            emailFree = if (email.isBlank()) null else !emailTaken
                            usernameFree = if (username.isBlank()) null else !usernameTaken
                            emailError = if (emailTaken) strings.authEmailTaken else null
                            usernameError = if (usernameTaken) strings.authUsernameTakenShort else null
                        }
                        "register_result" -> {
                            isLoading = false
                            // У нового аккаунта ещё нет устройств, код всегда идёт на почту.
                            if (message.success == true) onAuthSuccess(email, CODE_DELIVERY_EMAIL)
                            else generalError = message.message ?: strings.authRegisterFailed
                        }
                        "login_result" -> {
                            isLoading = false
                            // Сервер сообщает, куда реально ушёл код: в чат с Vibe cat на другом устройстве
                            // (delivery = "app") или, если других активных сессий нет, на почту ("email").
                            // Раньше экран всегда писал «код в приложении», даже когда ушло письмо.
                            if (message.success == true) onAuthSuccess(email, message.delivery ?: CODE_DELIVERY_APP)
                            else generalError = message.message ?: strings.authLoginFailed
                        }
                    }
                }
            }

            override fun onConnected() { scope.launch { isConnected = true } }
            override fun onDisconnected() { scope.launch { isConnected = false; isLoading = false } }
            override fun onError(error: String) { scope.launch { generalError = error; isLoading = false } }
        }
    }

    DisposableEffect(webSocket, listener) {
        webSocket.addListener(listener)
        onDispose { webSocket.removeListener(listener) }
    }

    LaunchedEffect(email, username, isRegister, isConnected) {
        if (isConnected && isRegister && (email.isNotEmpty() || username.isNotEmpty())) {
            isChecking = true
            delay(400)
            webSocket.checkAvailability(email, username)
        } else {
            isChecking = false
        }
    }

    // Появление контента
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(500),
        label = "appear"
    )
    val contentOffset by animateDpAsState(
        targetValue = if (appeared) 0.dp else 18.dp,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "appearOffset"
    )

    val canSubmit = email.isNotEmpty() &&
        isEmailValid &&
        (!isRegister || username.length in 4..32) &&
        emailError == null &&
        usernameError == null &&
        !isLoading

    Box(modifier = Modifier.fillMaxSize()) {
        VibeBackgroundMesh()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {

            /* ---------------- Top bar ---------------- */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCircleButton(onClick = onThemeToggle) {
                    AnimatedContent(
                        targetState = isDarkTheme,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.7f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(targetScale = 0.7f))
                        },
                        label = "themeIcon"
                    ) { dark ->
                        Icon(
                            imageVector = if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = strings.btnTheme,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                LanguagePill(
                    code = language,
                    fullName = strings.languageName,
                    contentDescription = strings.btnLanguage,
                    onClick = onLanguageToggle
                )
            }

            /* ---------------- Content ---------------- */
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = contentAlpha }
                    .offset(y = contentOffset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Vibe",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            )
                        ),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2.5).sp
                    )
                )

                Spacer(Modifier.height(6.dp))

                AnimatedContent(
                    targetState = isRegister,
                    transitionSpec = {
                        (fadeIn(tween(220, delayMillis = 60)) +
                            slideInVertically { it / 3 }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically { -it / 3 }) using
                            SizeTransform(clip = false) { _, _ -> tween(220) }
                    },
                    label = "subtitle"
                ) { register ->
                    Text(
                        text = if (register) strings.createAccount else strings.welcomeBack,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }

                Spacer(Modifier.height(28.dp))

                /* ---------------- Glass card ---------------- */
                GlassCard {
                    ModeSwitch(
                        isRegister = isRegister,
                        signUpText = strings.authTabSignUp,
                        signInText = strings.authTabSignIn,
                        onChange = {
                            if (isRegister != it) {
                                isRegister = it
                                emailError = null
                                usernameError = null
                                generalError = null
                                emailFree = null
                                usernameFree = null
                            }
                        }
                    )

                    Spacer(Modifier.height(22.dp))

                    VibeTextField(
                        value = email,
                        onValueChange = {
                            email = it.trim()
                            emailError = null
                            generalError = null
                            emailFree = null
                        },
                        label = strings.emailLabel,
                        error = if (!isEmailValid) strings.emailInvalidFormat else emailError,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = VibePrimary.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    StatusHint(
                        visible = isRegister && email.isNotBlank() && isEmailValid && emailError == null,
                        isPositive = emailFree == true,
                        text = when {
                            isChecking -> strings.authChecking
                            emailFree == true -> strings.authEmailAvailable
                            else -> ""
                        }
                    )

                    AnimatedVisibility(
                        visible = isRegister,
                        enter = expandVertically(tween(240)) + fadeIn(tween(240)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            VibeTextField(
                                value = username,
                                onValueChange = {
                                    username = sanitizeUsername(it)
                                    generalError = null
                                    usernameFree = null
                                    usernameError = if (username.isNotEmpty() && username.length < 4)
                                        strings.usernameMinLength else null
                                },
                                label = strings.usernameLabel,
                                error = usernameError,
                                leadingIcon = {
                                    Text(
                                        text = "@",
                                        color = VibePrimary.copy(alpha = 0.6f),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            StatusHint(
                                visible = username.length >= 4 && usernameError == null,
                                isPositive = usernameFree == true,
                                text = when {
                                    isChecking -> strings.authChecking
                                    usernameFree == true -> strings.authUsernameAvailable(username)
                                    else -> ""
                                }
                            )

                            // Живой счётчик длины
                            AnimatedVisibility(visible = username.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, start = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = strings.authUsernameCounter(username.length, 32),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(
                                            alpha = if (username.length in 4..32) 0.35f else 0.6f
                                        )
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = generalError != null,
                        enter = expandVertically(tween(200)) + fadeIn(),
                        exit = shrinkVertically(tween(150)) + fadeOut()
                    ) {
                        ErrorBanner(text = generalError.orEmpty())
                    }

                    Spacer(Modifier.height(24.dp))

                    VibeButton(
                        text = if (isLoading) strings.saveLoading else strings.continueBtn,
                        onClick = {
                            when {
                                !isConnected -> webSocket.connect()
                                canSubmit -> {
                                    isLoading = true
                                    generalError = null
                                    if (isRegister) webSocket.requestRegistration(email, username)
                                    else webSocket.requestLogin(email)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSubmit
                    )

                    AnimatedVisibility(visible = isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(3.dp)
                                .clip(PillShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isRegister = !isRegister
                            emailError = null
                            usernameError = null
                            generalError = null
                            emailFree = null
                            usernameFree = null
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isRegister,
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 60)) +
                                slideInVertically { it / 3 }) togetherWith
                                (fadeOut(tween(140)) + slideOutVertically { -it / 3 }) using
                                SizeTransform(clip = false) { _, _ -> tween(220) }
                        },
                        label = "switchMode"
                    ) { register ->
                        Text(
                            text = if (register) strings.switchSignIn else strings.switchSignUp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Больший нижний отступ при Arrangement.Center смещает весь блок вверх:
                // карточка стоит чуть выше центра и не упирается в клавиатуру.
                Spacer(Modifier.height(104.dp))
            }

            /* ---------------- Footer ---------------- */
            Text(
                text = "v1.0.6",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Компоненты                                                               */
/* ------------------------------------------------------------------------- */

private val PillShape = RoundedCornerShape(PillRadius)

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(CardRadius)
                )
                .padding(horizontal = 20.dp, vertical = 20.dp),
            content = content
        )
    }
}

@Composable
private fun ModeSwitch(
    isRegister: Boolean,
    signUpText: String,
    signInText: String,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(4.dp)
    ) {
        SegmentTab(
            text = signUpText,
            selected = isRegister,
            modifier = Modifier.weight(1f),
            onClick = { onChange(true) }
        )
        SegmentTab(
            text = signInText,
            selected = !isRegister,
            modifier = Modifier.weight(1f),
            onClick = { onChange(false) }
        )
    }
}

@Composable
private fun SegmentTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.surface
        else
            Color.Transparent,
        animationSpec = tween(200),
        label = "segBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "segFg"
    )

    Box(
        modifier = modifier
            .clip(PillShape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun StatusHint(
    visible: Boolean,
    isPositive: Boolean,
    text: String
) {
    AnimatedVisibility(
        visible = visible && text.isNotEmpty(),
        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(150))
    ) {
        Row(
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPositive) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = AuthSuccess,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isPositive) FontWeight.Medium else FontWeight.Normal,
                color = if (isPositive)
                    AuthSuccess
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VibeError.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = VibeError.copy(alpha = 0.28f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = VibeError,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = VibeError,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun GlassCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                shape = CircleShape
            )
            .pressable(onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Кнопка языка: после смены языка плавно расширяется, дописывая полное название
 * («Русский» / «English»), держит его пару секунд и так же плавно сворачивается
 * обратно до кода. Код (RU/EN) виден всегда, в том числе в раскрытом состоянии.
 */
@Composable
private fun LanguagePill(
    code: String,
    fullName: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    val a11yLabel = contentDescription
    var expanded by remember { mutableStateOf(false) }
    var skipInitial by remember { mutableStateOf(true) }

    LaunchedEffect(code) {
        if (skipInitial) {
            skipInitial = false
            return@LaunchedEffect
        }
        expanded = true
        delay(LanguagePillHoldMs)
        expanded = false
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(120),
        label = "langPress"
    )

    Row(
        modifier = Modifier
            .height(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                shape = PillShape
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 14.dp)
            .semantics { this.contentDescription = a11yLabel },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Код языка: остаётся на месте, меняется кроссфейдом
        AnimatedContent(
            targetState = code,
            transitionSpec = {
                fadeIn(tween(200, delayMillis = 60)) togetherWith
                    fadeOut(tween(120)) using
                    SizeTransform(clip = false) { _, _ -> tween(200) }
            },
            label = "langCode"
        ) { value ->
            Text(
                text = value,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                softWrap = false
            )
        }

        // Полное название: расширяет кнопку и сворачивается обратно
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Start
            ) + fadeIn(tween(240, delayMillis = 120)),
            exit = fadeOut(tween(140)) + shrinkHorizontally(
                animationSpec = tween(340, delayMillis = 60, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Start
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = fullName,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(120),
        label = "pressScale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

/* ------------------------------------------------------------------------- */
/*  Валидация юзернейма (вынесена из UI)                                     */
/* ------------------------------------------------------------------------- */

private fun sanitizeUsername(raw: String): String {
    var result = raw.filter { c ->
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_'
    }
    // Не может начинаться с цифры или подчёркивания
    result = result.dropWhile { it.isDigit() || it == '_' }
    // Максимум одно подчёркивание
    if (result.count { it == '_' } > 1) {
        val first = result.indexOf('_')
        result = result.filterIndexed { index, c -> c != '_' || index == first }
    }
    return result.take(32)
}