package com.flasskdev.vibe.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.ui.theme.luminanceIsDark
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import java.util.Locale

@Composable
fun MainSettingsContent(
    webSocket: com.flasskdev.vibe.data.VibeWebSocket,
    onNavigateExtra: (String) -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToVibePro: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { com.flasskdev.vibe.data.local.AppDatabase.getDatabase(context) }
    val userPrefs = remember { com.flasskdev.vibe.data.UserPreferences(context) }
    val user by db.chatDao().getUserById(userPrefs.userId).collectAsState(initial = null)
    var liveAvatar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var liveName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.DisposableEffect(webSocket, userPrefs.userId) {
        val listener = object : com.flasskdev.vibe.data.VibeWebSocketListener {
            override fun onUserInfo(userId: Int, isOnline: Boolean, lastSeen: Long?, isDeveloper: Boolean, isVerified: Boolean, registerDate: Long?, isBot: Boolean, about: String?, username: String?, name: String?, avatarUrl: String?, lastSeenStatus: String?, canMessage: Boolean, isBanned: Boolean, isFreezed: Boolean, isBlockedByMe: Boolean, isBlockedByUser: Boolean) {
                if (userId == userPrefs.userId) scope.launch { liveAvatar = avatarUrl.orEmpty(); liveName = name }
            }
            override fun onAvatarUploaded(userId: Int, avatarUrl: String) { if (userId == userPrefs.userId) scope.launch { liveAvatar = avatarUrl } }
        }
        webSocket.addListener(listener); webSocket.getUserInfo(userPrefs.userId)
        onDispose { webSocket.removeListener(listener) }
    }
    val avatar = liveAvatar ?: user?.avatarUrl
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current

    // Version is read from the package instead of a hardcoded literal, so a release
    // bump can never leave a stale number on this screen.
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "1.0.6" }
    }

    val scrollState = rememberScrollState()
    val primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        VibeTopGlow(height = 380.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            Text(
                text = strings.tabSettings,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, bottom = 18.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp)
            ) {
                // ══ ACCOUNT HEADER CARD ══
                VibeSettingsGroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = strings.settingsAccount, onClick = onNavigateToAccount)
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayName = (liveName ?: user?.name)?.takeIf { it.isNotBlank() } ?: strings.userLabel
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(primary, primary.copy(alpha = 0.7f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatar?.let { if (it.startsWith("/")) "https://flasskdev.alwaysdata.net$it" else it })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = strings.a11yAvatar,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = displayName.take(1).uppercase(Locale.getDefault()),
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = user?.username?.let { "@$it" } ?: strings.settingsAccountSubtitle,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // ══ GENERAL ══
                Spacer(modifier = Modifier.height(28.dp))
                VibeSettingsGroupTitle(strings.settingsGroupGeneral)
                Spacer(modifier = Modifier.height(8.dp))
                VibeSettingsGroupCard {
                    Column {
                        VibeSettingsRow(
                            icon = Icons.Rounded.ChatBubble,
                            title = strings.settingsChats,
                            subtitle = strings.settingsChatsSubtitle,
                            iconTint = Color(0xFF4CAF50),
                            soonLabel = strings.settingsSoonBadge
                        )
                        VibeSettingsRowDivider()
                        VibeSettingsRow(
                            icon = Icons.Rounded.Lock,
                            title = strings.settingsPrivacy,
                            subtitle = strings.settingsPrivacySubtitle,
                            iconTint = Color(0xFF2196F3),
                            onClick = onNavigateToPrivacy
                        )
                        VibeSettingsRowDivider()
                        VibeSettingsRow(
                            icon = Icons.Rounded.Notifications,
                            title = strings.settingsNotifications,
                            subtitle = strings.settingsNotificationsSubtitle,
                            iconTint = Color(0xFFF44336),
                            onClick = { onNavigateExtra("notifications") }
                        )
                        VibeSettingsRowDivider()
                        VibeSettingsRow(
                            icon = Icons.Rounded.BatterySaver,
                            title = strings.settingsPowerSaving,
                            subtitle = strings.settingsPowerSavingSubtitle,
                            iconTint = Color(0xFFCDDC39),
                            onClick = { onNavigateExtra("power_saving") }
                        )
                        VibeSettingsRowDivider()
                        VibeSettingsRow(
                            icon = Icons.Rounded.Devices,
                            title = strings.settingsDevices,
                            subtitle = strings.settingsDevicesSubtitle,
                            iconTint = Color(0xFFFF9800),
                            onClick = onNavigateToDevices
                        )
                        VibeSettingsRowDivider()
                        VibeSettingsRow(
                            icon = Icons.Rounded.Language,
                            title = strings.btnLanguage,
                            subtitle = strings.languageName,
                            iconTint = Color(0xFF9C27B0),
                            onClick = { onNavigateExtra("language") }
                        )
                    }
                }

                // ══ EXTRAS ══
                Spacer(modifier = Modifier.height(28.dp))
                VibeSettingsGroupTitle(strings.settingsGroupExtras)
                Spacer(modifier = Modifier.height(8.dp))

                // Vibe Pro promo card: taps navigate directly to Vibe Pro tab
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFC107).copy(alpha = if (isDark) 0.22f else 0.18f),
                                        Color(0xFFFF6B6B).copy(alpha = if (isDark) 0.18f else 0.14f)
                                    )
                                )
                            )
                            .border(
                                width = 0.8.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFC107).copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable(onClickLabel = strings.settingsVibePro, onClick = onNavigateToVibePro)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFC107).copy(alpha = 0.22f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsVibePro,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.settingsVibeProSubtitle,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                VibeSettingsGroupCard {
                    VibeSettingsRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = strings.settingsVibes,
                        subtitle = strings.settingsVibesSubtitle,
                        iconTint = Color(0xFFE91E63),
                        soonLabel = strings.settingsSoonBadge
                    )
                }

                // ══ HELP ══
                Spacer(modifier = Modifier.height(28.dp))
                VibeSettingsGroupTitle(strings.settingsGroupHelp)
                Spacer(modifier = Modifier.height(8.dp))
                VibeSettingsGroupCard {
                    VibeSettingsRow(
                        icon = Icons.Rounded.SupportAgent,
                        title = strings.settingsSupport,
                        subtitle = strings.settingsSupportSubtitle,
                        iconTint = Color(0xFF00BCD4),
                        soonLabel = strings.settingsSoonBadge
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ══ VERSION FOOTER ══
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.appVersion(versionName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/* ────────────────────── Settings design-system helpers ────────────────────── */

/** Uppercase group caption above each card. */
@Composable
private fun VibeSettingsGroupTitle(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier = Modifier.padding(start = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.9.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    )
}

/** Grouped glass card container. */
@Composable
private fun VibeSettingsGroupCard(content: @Composable () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
        border = BorderStroke(
            width = 0.7.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        ),
        shadowElevation = 0.dp
    ) {
        content()
    }
}

/**
 * One settings row. Passing [soonLabel] marks the destination as unbuilt: the row
 * dims, loses its chevron and stops being clickable, which is honest about the state
 * instead of silently swallowing taps like the old empty-TODO rows did.
 */
@Composable
private fun VibeSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color,
    soonLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    val enabled = soonLabel == null && onClick != null
    val contentAlpha = if (enabled) 1f else 0.55f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(onClickLabel = title) { onClick?.invoke() }
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconTint.copy(alpha = 0.14f * contentAlpha), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f * contentAlpha),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (soonLabel != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
            ) {
                Text(
                    text = soonLabel,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Hairline divider inset to the text column. */
@Composable
private fun VibeSettingsRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 66.dp)
            .height(0.7.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
    )
}