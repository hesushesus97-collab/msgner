package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeStrings
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SessionInfo(
    val deviceId: String,
    val deviceName: String,
    val osVersion: String,
    val location: String,
    val lastActive: Long
)

/** Состояние загрузки экрана: раньше был только флаг isLoading без обработки таймаута. */
private enum class DevicesLoadState { Loading, Loaded, Failed }

/** Тип устройства определяется эвристикой по имени/ОС и влияет только на иконку. */
private enum class DeviceKind { Phone, Desktop }

private const val SESSIONS_TIMEOUT_MS = 10_000L

@Composable
fun DevicesScreenContent(
    webSocket: VibeWebSocket,
    userPreferences: UserPreferences,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current

    var sessions by remember { mutableStateOf<List<SessionInfo>>(emptyList()) }
    var loadState by remember { mutableStateOf(DevicesLoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var pendingTerminate by remember { mutableStateOf<SessionInfo?>(null) }
    var confirmTerminateAll by remember { mutableStateOf(false) }

    DisposableEffect(webSocket) {
        val listener = object : VibeWebSocketListener {
            override fun onSessionsResult(sessionsArray: JSONArray) {
                val list = ArrayList<SessionInfo>(sessionsArray.length())
                for (i in 0 until sessionsArray.length()) {
                    val obj = sessionsArray.optJSONObject(i) ?: continue
                    list.add(
                        SessionInfo(
                            deviceId = obj.optString("device_id", ""),
                            // Пустая строка вместо англоязычного фолбэка: подпись подставляет UI из Strings.
                            deviceName = obj.optString("device_name", ""),
                            osVersion = obj.optString("os_version", ""),
                            location = obj.optString("location", ""),
                            lastActive = obj.optLong("last_active", 0L)
                        )
                    )
                }
                sessions = list
                loadState = DevicesLoadState.Loaded
            }

            override fun onSessionTerminated(deviceId: String) {
                sessions = sessions.filter { it.deviceId != deviceId }
            }
        }
        webSocket.addListener(listener)
        onDispose { webSocket.removeListener(listener) }
    }

    // Запрос вынесен из DisposableEffect, чтобы работала кнопка "Повторить".
    LaunchedEffect(reloadKey) {
        loadState = DevicesLoadState.Loading
        webSocket.getSessions(userPreferences.userId)
        delay(SESSIONS_TIMEOUT_MS)
        if (loadState == DevicesLoadState.Loading) loadState = DevicesLoadState.Failed
    }

    val currentSession = sessions.find { it.deviceId == userPreferences.deviceId }
    val otherSessions = sessions.filter { it.deviceId != userPreferences.deviceId }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        VibeTopGlow(height = 380.dp)

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        DevicesTopBar(
            strings = strings,
            sessionCount = sessions.size,
            showCount = loadState == DevicesLoadState.Loaded,
            onBack = onBack,
            onRefresh = { reloadKey++ }
        )

        when (loadState) {
            DevicesLoadState.Loading -> DevicesSkeleton(strings)

            DevicesLoadState.Failed -> DevicesPlaceholder(
                icon = Icons.Rounded.Warning,
                accent = MaterialTheme.colorScheme.error,
                title = strings.devicesLoadFailedTitle,
                subtitle = strings.devicesLoadFailedSubtitle,
                actionLabel = strings.toastActionRetry,
                onAction = { reloadKey++ }
            )

            DevicesLoadState.Loaded -> if (sessions.isEmpty()) {
                DevicesPlaceholder(
                    icon = Icons.Rounded.Info,
                    accent = MaterialTheme.colorScheme.primary,
                    title = strings.devicesEmptyTitle,
                    subtitle = strings.devicesEmptySubtitle,
                    actionLabel = strings.toastActionRetry,
                    onAction = { reloadKey++ }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentSession != null) {
                        item(key = "current-header") {
                            SectionHeader(strings.devicesSectionCurrent)
                        }
                        item(key = "current-${currentSession.deviceId}") {
                            SessionCard(
                                session = currentSession,
                                isCurrent = true,
                                strings = strings,
                                onTerminate = null
                            )
                        }
                    }

                    if (otherSessions.isNotEmpty()) {
                        item(key = "other-header") {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    text = strings.devicesSectionOther,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { confirmTerminateAll = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = strings.devicesTerminateAll,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        items(otherSessions, key = { it.deviceId }) { session ->
                            SessionCard(
                                session = session,
                                isCurrent = false,
                                strings = strings,
                                onTerminate = { pendingTerminate = session }
                            )
                        }
                        item(key = "security-hint") {
                            SecurityHint(strings.devicesSecurityHint)
                        }
                    } else {
                        item(key = "no-others") {
                            Spacer(Modifier.height(14.dp))
                            NoOtherSessions(strings)
                        }
                    }
                }
            }
        }
    }

    pendingTerminate?.let { session ->
        val name = session.deviceName.ifBlank { strings.devicesUnknownDevice }
        DestructiveDialog(
            title = strings.devicesTerminateTitle,
            text = strings.devicesTerminateText(name),
            confirmLabel = strings.devicesTerminateConfirm,
            cancelLabel = strings.cancelBtn,
            onConfirm = {
                webSocket.terminateSession(userPreferences.userId, session.deviceId)
                pendingTerminate = null
            },
            onDismiss = { pendingTerminate = null }
        )
    }

    if (confirmTerminateAll) {
        DestructiveDialog(
            title = strings.devicesTerminateAllTitle,
            text = strings.devicesTerminateAllText(otherSessions.size),
            confirmLabel = strings.devicesTerminateConfirm,
            cancelLabel = strings.cancelBtn,
            onConfirm = {
                otherSessions.forEach { session ->
                    webSocket.terminateSession(userPreferences.userId, session.deviceId)
                }
                confirmTerminateAll = false
            },
            onDismiss = { confirmTerminateAll = false }
        )
    }
}
}

// ---------------------------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------------------------

@Composable
private fun DevicesTopBar(
    strings: VibeStrings,
    sessionCount: Int,
    showCount: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // statusBarsPadding вместо хардкода top = 48.dp: корректно на любом устройстве.
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = strings.backBtn }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.semantics { contentDescription = strings.devicesRefreshCd }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
            }
        }

        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp)) {
            Text(
                text = strings.devicesTitle,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (showCount && sessionCount > 0) {
                    strings.devicesSessionsCount(sessionCount)
                } else {
                    strings.devicesSubtitle
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = modifier.padding(start = 6.dp, top = 6.dp, bottom = 4.dp)
    )
}

// ---------------------------------------------------------------------------------------------
// Session card
// ---------------------------------------------------------------------------------------------

@Composable
private fun SessionCard(
    session: SessionInfo,
    isCurrent: Boolean,
    strings: VibeStrings,
    onTerminate: (() -> Unit)?
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(22.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isCurrent) 6.dp else 1.dp,
        border = if (isCurrent) {
            BorderStroke(1.5.dp, accent.copy(alpha = 0.35f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
        }
    ) {
        Box {
            // Текущее устройство подсвечено мягким градиентом, а не только цветом текста.
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.copy(alpha = 0.10f), Color.Transparent)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeviceAvatar(session = session, isCurrent = isCurrent, accent = accent)

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.deviceName.ifBlank { strings.devicesUnknownDevice },
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isCurrent) {
                            Spacer(Modifier.width(8.dp))
                            CurrentDeviceBadge(text = strings.devicesCurrentBadge, accent = accent)
                        }
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = listOf(
                            session.osVersion.ifBlank { strings.statusUnknown },
                            session.location.ifBlank { strings.devicesUnknownLocation }
                        ).joinToString(" • "),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrent) {
                            PulsingDot(color = accent)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isCurrent) {
                                strings.devicesOnlineNow
                            } else {
                                lastActiveLabel(strings, session.lastActive)
                            },
                            color = if (isCurrent) {
                                accent
                            } else {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (onTerminate != null) {
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onTerminate,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                            .semantics { contentDescription = strings.devicesTerminateCd }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAvatar(session: SessionInfo, isCurrent: Boolean, accent: Color) {
    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.08f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        )
        Icon(
            imageVector = session.deviceIcon(),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(26.dp)
        )
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CurrentDeviceBadge(text: String, accent: Color) {
    Text(
        text = text,
        color = accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

/** Пульсирующая точка «в сети»: единственная бесконечная анимация на экране. */
@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "onlineDot")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "onlineDotAlpha"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .alpha(pulse)
            .background(color, CircleShape)
    )
}

// ---------------------------------------------------------------------------------------------
// States: skeleton, placeholder, hints
// ---------------------------------------------------------------------------------------------

/** Скелетон вместо голого спиннера: пользователь сразу видит структуру списка. */
@Composable
private fun DevicesSkeleton(strings: VibeStrings) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "skeletonShimmer"
    )
    val base = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f * shimmer + 0.03f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(strings.devicesLoading)
        repeat(4) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, base)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(base, RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(14.dp)
                                .background(base, RoundedCornerShape(7.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(11.dp)
                                .background(base, RoundedCornerShape(6.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .height(11.dp)
                                .background(base, RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicesPlaceholder(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 12.dp)
        ) {
            Text(text = actionLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun NoOtherSessions(strings: VibeStrings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.devicesNoOtherSessions,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = strings.devicesNoOtherSessionsHint,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                fontSize = 13.5.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SecurityHint(text: String) {
    Row(
        modifier = Modifier.padding(top = 14.dp, start = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier
                .padding(top = 1.dp)
                .size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun DestructiveDialog(
    title: String,
    text: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(text = title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = text,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = cancelLabel,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    )
}

/**
 * Совместимость с прежним публичным API экрана: старый SessionItem рендерил карточку сам,
 * теперь просто делегирует в SessionCard.
 */
@Deprecated(
    message = "Используйте приватный SessionCard внутри DevicesScreenContent",
    replaceWith = ReplaceWith("SessionCard(session, isCurrent, LocalVibeStrings.current, onTerminate)")
)
@Composable
fun SessionItem(
    session: SessionInfo,
    isCurrent: Boolean,
    onTerminate: () -> Unit
) {
    SessionCard(
        session = session,
        isCurrent = isCurrent,
        strings = LocalVibeStrings.current,
        onTerminate = if (isCurrent) null else onTerminate
    )
}

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

private fun SessionInfo.kind(): DeviceKind {
    val haystack = "$deviceName $osVersion".lowercase(Locale.ROOT)
    val desktop = listOf("windows", "macos", "mac os", "linux", "ubuntu", "desktop", "web", "chrome", "firefox")
    return if (desktop.any { haystack.contains(it) }) DeviceKind.Desktop else DeviceKind.Phone
}

private fun SessionInfo.deviceIcon(): ImageVector = when (kind()) {
    DeviceKind.Desktop -> Icons.Rounded.Computer
    DeviceKind.Phone -> {
        val haystack = "$deviceName $osVersion".lowercase(Locale.ROOT)
        if (haystack.contains("android")) Icons.Rounded.PhoneAndroid else Icons.Rounded.Smartphone
    }
}

/**
 * Относительное время вместо всегда полной даты: «Только что», «12 минут назад», «Вчера»,
 * и лишь для старых сеансов - дата по локализованному паттерну.
 */
private fun lastActiveLabel(strings: VibeStrings, lastActive: Long): String {
    if (lastActive <= 0L) return strings.statusUnknown

    val diff = System.currentTimeMillis() - lastActive
    if (diff < 0L) return strings.devicesLastActiveNow

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1L -> strings.devicesLastActiveNow
        minutes < 60L -> strings.devicesLastActiveMinutes(minutes.toInt())
        hours < 24L -> strings.devicesLastActiveHours(hours.toInt())
        days < 2L -> strings.devicesLastActiveYesterday
        else -> {
            val formatter = SimpleDateFormat(strings.devicesDateTimePattern, Locale(strings.locale))
            strings.devicesLastActiveDate(formatter.format(Date(lastActive)))
        }
    }
}