package com.flasskdev.vibe.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.local.AppDatabase
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.theme.VibeTopGlow

/* ------------------------------------------------------------------------- */
/*  Design tokens                                                            */
/* ------------------------------------------------------------------------- */

private object AccountTokens {
    val ScreenPadding = 20.dp
    val CardRadius = 22.dp
    val TileRadius = 10.dp
    val RowVertical = 14.dp
    val GapS = 8.dp
    val GapM = 16.dp
    val GapL = 28.dp

    val Blue = Color(0xFF0A84FF)
    val Violet = Color(0xFF7C5CFF)
    val Teal = Color(0xFF00C2A8)
    val Red = Color(0xFFFF3B30)
}

private const val MEDIA_HOST = "https://flasskdev.alwaysdata.net"

/** Сервер отдаёт как абсолютные URL, так и пути вида `/uploads/...`. */
private fun resolveAvatarUrl(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    return if (value.startsWith("/")) MEDIA_HOST + value else value
}

/* ------------------------------------------------------------------------- */
/*  Screen                                                                   */
/* ------------------------------------------------------------------------- */

@Composable
fun AccountSettingsContent(
    userPreferences: UserPreferences,
    webSocket: VibeWebSocket,
    onLogout: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToEditUsername: () -> Unit,
    onNavigateToEditNickname: () -> Unit,
    onNavigateToEditBio: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val db = remember { AppDatabase.getDatabase(context) }
    val userId = userPreferences.userId
    val user by db.chatDao().getUserById(userId).collectAsState(initial = null)

    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val username = user?.username?.takeIf { it.isNotBlank() }
    val name = user?.name?.takeIf { it.isNotBlank() }
    val about = user?.about?.takeIf { it.isNotBlank() }
    val avatarUrl = resolveAvatarUrl(user?.avatarUrl)

    // Хедер "прилипает" и получает разделитель только при скролле
    val scrolled by remember { derivedStateOf { scrollState.value > 8 } }
    val dividerAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(180),
        label = "headerDivider"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        VibeTopGlow(height = 380.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {

        /* ---------------- Top bar ---------------- */
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.backBtn,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = strings.accountTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            HorizontalDivider(
                modifier = Modifier.alpha(dividerAlpha),
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            )
        }

        /* ---------------- Content ---------------- */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = AccountTokens.ScreenPadding)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Spacer(Modifier.height(AccountTokens.GapL))

            ProfileHero(
                name = name,
                username = username,
                avatarUrl = avatarUrl,
                strings = strings,
                onAvatarClick = onNavigateToEditNickname
            )

            Spacer(Modifier.height(AccountTokens.GapL))

            /* --- Идентификация --- */
            SectionLabel(strings.accountSectionProfile)
            AccountCard {
                AccountRow(
                    icon = Icons.Rounded.AlternateEmail,
                    tint = AccountTokens.Blue,
                    label = strings.accountUsernameLabel,
                    value = username?.let { "@$it" },
                    placeholder = strings.accountNotSet,
                    onClick = onNavigateToEditUsername,
                    trailing = {
                        if (username != null) {
                            GhostAction(
                                icon = Icons.Rounded.ContentCopy,
                                contentDescription = strings.a11yCopy
                            ) {
                                clipboard.setText(AnnotatedString("@$username"))
                                Toast
                                    .makeText(context, strings.usernameCopied, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                )
                RowDivider()
                AccountRow(
                    icon = Icons.Rounded.Person,
                    tint = AccountTokens.Violet,
                    label = strings.accountNicknameLabel,
                    value = name,
                    placeholder = strings.accountNoName,
                    onClick = onNavigateToEditNickname
                )
            }

            Spacer(Modifier.height(AccountTokens.GapL))

            /* --- О себе --- */
            SectionLabel(strings.accountSectionAbout)
            AccountCard {
                AccountRow(
                    icon = Icons.Rounded.Info,
                    tint = AccountTokens.Teal,
                    label = strings.accountBioLabel,
                    value = about,
                    placeholder = strings.accountBioPlaceholder,
                    maxValueLines = 3,
                    onClick = onNavigateToEditBio
                )
            }

            Spacer(Modifier.height(10.dp))
            FootnoteWithLink(
                prefix = strings.accountPrivacyFootPrefix,
                linkText = strings.accountPrivacyFootLink,
                suffix = strings.accountPrivacyFootSuffix,
                onLinkClick = onNavigateToPrivacy
            )

            Spacer(Modifier.height(AccountTokens.GapL))

            /* --- Опасная зона --- */
            DangerCard(
                title = strings.accountLogoutTitle,
                subtitle = strings.accountLogoutSubtitle,
                onClick = { showLogoutDialog = true }
            )

            Spacer(Modifier.height(AccountTokens.GapL))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AccountTokens.Red.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = null,
                        tint = AccountTokens.Red
                    )
                }
            },
            title = {
                Text(
                    text = strings.accountLogoutDialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = strings.accountLogoutDialogText,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text(strings.logoutConfirm, color = AccountTokens.Red, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(strings.logoutCancel)
                }
            }
        )
    }
}
}

/* ------------------------------------------------------------------------- */
/*  Profile hero                                                             */
/* ------------------------------------------------------------------------- */

@Composable
private fun ProfileHero(
    name: String?,
    username: String?,
    avatarUrl: String?,
    strings: VibeStrings,
    onAvatarClick: () -> Unit
) {
    val context = LocalContext.current
    val displayName = name ?: strings.accountNoName

    val initials = remember(name, username) {
        val source = name ?: username ?: "?"
        source.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
    }

    val gradient = remember(initials) {
        Brush.linearGradient(
            listOf(
                AccountTokens.Violet,
                AccountTokens.Blue,
                AccountTokens.Teal
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Внешнее «свечение»
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AccountTokens.Blue.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(gradient)
                    .pressable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    // Аватар поверх градиента: пока картинка грузится, под ней видны инициалы.
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = strings.a11yAvatar,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
            // Бейдж редактирования
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .pressable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = strings.a11yEditProfile,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(Modifier.height(AccountTokens.GapM))

        Text(
            text = displayName,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        if (username != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "@$username",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Building blocks                                                          */
/* ------------------------------------------------------------------------- */

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = AccountTokens.GapS)
    )
}

@Composable
private fun AccountCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AccountTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(AccountTokens.CardRadius)
                )
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        thickness = 0.7.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    )
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    maxValueLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null
) {
    val isEmpty = value == null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = AccountTokens.RowVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tint = tint)

        Spacer(Modifier.width(AccountTokens.GapM))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value ?: placeholder,
                color = if (isEmpty)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                else
                    MaterialTheme.colorScheme.onSurface,
                fontSize = 16.5.sp,
                fontStyle = if (isEmpty) FontStyle.Italic else FontStyle.Normal,
                fontWeight = if (isEmpty) FontWeight.Normal else FontWeight.SemiBold,
                lineHeight = 21.sp,
                maxLines = maxValueLines,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
            Spacer(Modifier.width(2.dp))
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(tint, tint.copy(alpha = 0.78f))
                ),
                shape = RoundedCornerShape(AccountTokens.TileRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun GhostAction(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun FootnoteWithLink(
    prefix: String,
    linkText: String,
    suffix: String,
    onLinkClick: () -> Unit
) {
    val text = buildAnnotatedString {
        append(prefix)
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        ) { append(linkText) }
        append(suffix)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pressable(onClick = onLinkClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
            modifier = Modifier
                .padding(top = 1.dp)
                .size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun DangerCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = if (pressed)
            AccountTokens.Red.copy(alpha = 0.12f)
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(140),
        label = "dangerBg"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AccountTokens.CardRadius),
        color = bg,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 0.8.dp,
                    color = AccountTokens.Red.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(AccountTokens.CardRadius)
                )
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = AccountTokens.RowVertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = Icons.Rounded.Logout, tint = AccountTokens.Red)
            Spacer(Modifier.width(AccountTokens.GapM))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AccountTokens.Red,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Interaction helper: ripple + лёгкое «нажатие»                            */
/* ------------------------------------------------------------------------- */

@Composable
private fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
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
