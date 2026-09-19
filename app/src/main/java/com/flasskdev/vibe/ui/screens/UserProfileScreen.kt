package com.flasskdev.vibe.ui.screens

import com.flasskdev.vibe.ui.components.vibeMenuAnchor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.UserSearchResult
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import com.flasskdev.vibe.data.local.AppDatabase
import com.flasskdev.vibe.data.local.MessageEntity
import com.flasskdev.vibe.data.local.UserCacheEntity
import com.flasskdev.vibe.ui.components.ProfileMediaTabs
import com.flasskdev.vibe.ui.components.UserBadgesRow
import com.flasskdev.vibe.ui.components.VibeToast
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeError
import com.flasskdev.vibe.ui.theme.VibeOnlineGreen
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/* ------------------------------------------------------------------ */
/*  Design tokens                                                     */
/*  Minimal, Telegram-like: one accent, flat surfaces, no gradients,  */
/*  no shadows, no glass. Rhythm is built from spacing and type only. */
/* ------------------------------------------------------------------ */

private val AVATAR_SIZE = 112.dp
private val TOP_BAR_HEIGHT = 52.dp
private val SCREEN_PADDING = 20.dp
private val BLOCK_CORNER = 16.dp
private val CROP_CIRCLE_RADIUS = 76.dp
private const val USER_SEARCH_TIMEOUT_MS = 3500L

/** Flat, muted palette for initials. No gradients by design. */
private val profileAvatarPalette = listOf(
    Color(0xFF4B8FD8),
    Color(0xFF7E71C8),
    Color(0xFF3FA9A0),
    Color(0xFFD8695F),
    Color(0xFFD69B4A),
    Color(0xFF5FA867),
    Color(0xFFCC6C93)
)

private fun profileAvatarColor(id: Int): Color {
    val size = profileAvatarPalette.size
    return profileAvatarPalette[(id.hashCode() % size + size) % size]
}

/* ------------------------------------------------------------------ */
/*  Screen                                                            */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Int,
    userName: String,
    webSocket: VibeWebSocket,
    onBack: () -> Unit,
    onNavigateToChat: ((partnerId: Int, messageId: Int) -> Unit)? = null
) {
    BackHandler { onBack() }

    val strings = LocalVibeStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val currentUserId = remember(context) { UserPreferences(context).userId }
    var resolvedUserId by remember(userId) { mutableIntStateOf(userId) }
    var isSearchingUser by remember(userId, userName) {
        mutableStateOf(userId == 0 && userName.isNotBlank())
    }
    var isUserNotFound by remember(userId, userName) { mutableStateOf(false) }

    /* ---------- Resolving a profile opened by @username ---------- */

    DisposableEffect(webSocket, userName) {
        val listener = object : VibeWebSocketListener {
            override fun onUsersSearchResult(users: List<UserSearchResult>) {
                val cleanUsername = userName.removePrefix("@").trim()
                val match = users.firstOrNull {
                    it.username?.equals(cleanUsername, ignoreCase = true) == true
                }
                if (match == null) {
                    isSearchingUser = false
                    isUserNotFound = true
                    return
                }
                resolvedUserId = match.id
                isSearchingUser = false
                isUserNotFound = false
                scope.launch(Dispatchers.IO) {
                    db.chatDao().insertUser(
                        UserCacheEntity(
                            id = match.id,
                            name = match.name ?: cleanUsername,
                            username = match.username ?: cleanUsername,
                            avatarUrl = match.avatarUrl,
                            isVerified = match.isVerified,
                            isDeveloper = match.isDeveloper,
                            isBot = match.isBot,
                            isBanned = match.isBanned,
                            isFreezed = match.isFreezed
                        )
                    )
                }
            }
        }
        webSocket.addListener(listener)
        onDispose { webSocket.removeListener(listener) }
    }

    LaunchedEffect(userId, userName) {
        when {
            userId > 0 -> {
                isSearchingUser = false
                isUserNotFound = false
            }
            userName.isNotBlank() -> {
                val cleanUsername = userName.removePrefix("@").trim()
                val cached = db.chatDao().getUserByUsername(cleanUsername)
                if (cached != null) {
                    resolvedUserId = cached.id
                    isSearchingUser = false
                    isUserNotFound = false
                } else {
                    isSearchingUser = true
                    isUserNotFound = false
                    webSocket.searchUsers(cleanUsername, currentUserId)
                    delay(USER_SEARCH_TIMEOUT_MS)
                    if (resolvedUserId == 0 && isSearchingUser) {
                        isSearchingUser = false
                        isUserNotFound = true
                    }
                }
            }
        }
    }

    LaunchedEffect(resolvedUserId) {
        if (resolvedUserId > 0) webSocket.getUserInfo(resolvedUserId)
    }

    /* ---------- State ---------- */

    val user by db.chatDao().getUserById(resolvedUserId).collectAsState(initial = null)
    val isCurrentUser = resolvedUserId == currentUserId
    val messagesWithAttachments by db.chatDao()
        .getMessagesWithAttachments(currentUserId, resolvedUserId)
        .collectAsState(initial = emptyList())

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showAvatarViewer by remember { mutableStateOf(false) }
    var showCopyToast by remember { mutableStateOf(false) }
    var showUnblockToast by remember { mutableStateOf(false) }

    LaunchedEffect(showCopyToast) {
        if (showCopyToast) {
            delay(2000)
            showCopyToast = false
        }
    }
    LaunchedEffect(showUnblockToast) {
        if (showUnblockToast) {
            delay(2500)
            showUnblockToast = false
        }
    }

    val displayName = when {
        user?.isBanned == true -> strings.accountDeleted
        user?.isFreezed == true -> strings.accountFrozen
        else -> user?.name?.takeIf { it.isNotBlank() } ?: userName
    }
    val usernameText = "@" + (user?.username?.takeIf { it.isNotBlank() }
        ?: userName.removePrefix("@").lowercase(Locale.getDefault()).replace(" ", ""))

    val isRestricted = user?.isBlockedByUser == true || user?.isBanned == true || user?.isFreezed == true
    val isBlockedByMe = user?.isBlockedByMe == true
    val isOnline = user?.isOnline == true && !isRestricted && !isBlockedByMe && user?.isBot != true
    val canShowAvatar = !user?.avatarUrl.isNullOrEmpty() && !isRestricted

    val statusText = profileStatusText(user, strings, isRestricted, isBlockedByMe, isOnline)
    val statusColor = when {
        isBlockedByMe -> VibeError
        isOnline -> VibeOnlineGreen
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetValue ->
            if (sheetValue == SheetValue.Hidden) {
                scrollState.value == 0
            } else {
                true
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = sheetState,
        sheetGesturesEnabled = scrollState.value == 0,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        when {
            isUserNotFound || (resolvedUserId == 0 && !isSearchingUser && user == null) ->
                ProfileNotFoundState(userName = userName, strings = strings, onBack = onBack)

            user == null -> ProfileLoadingState(strings = strings)

            else -> ProfileContent(
                user = user,
                scrollState = scrollState,
                displayName = displayName,
                usernameText = usernameText,
                statusText = statusText,
                statusColor = statusColor,
                isCurrentUser = isCurrentUser,
                isBlockedByMe = isBlockedByMe,
                canShowAvatar = canShowAvatar,
                resolvedUserId = resolvedUserId,
                messagesWithAttachments = messagesWithAttachments,
                strings = strings,
                showCopyToast = showCopyToast,
                showUnblockToast = showUnblockToast,
                onBack = onBack,
                onOpenAvatar = { if (canShowAvatar) showAvatarViewer = true },
                onEditAvatar = { showAvatarPicker = true },
                onCopyUsername = {
                    clipboard.setText(AnnotatedString(usernameText))
                    showCopyToast = true
                },
                onWriteMessage = { onNavigateToChat?.invoke(resolvedUserId, 0) },
                onNavigateToMessage = { messageId, partnerId ->
                    onNavigateToChat?.invoke(partnerId, messageId)
                },
                onBlock = {
                    webSocket.blockUser(currentUserId, resolvedUserId)
                    scope.launch(Dispatchers.IO) {
                        db.chatDao().updateUserBlockedByMe(resolvedUserId, true)
                        db.chatDao().updateChatBlockedByMe(resolvedUserId, true)
                    }
                },
                onUnblock = {
                    webSocket.unblockUser(currentUserId, resolvedUserId)
                    scope.launch(Dispatchers.IO) {
                        db.chatDao().updateUserBlockedByMe(resolvedUserId, false)
                        db.chatDao().updateChatBlockedByMe(resolvedUserId, false)
                    }
                    showUnblockToast = true
                },
                onCopyToastDismiss = { showCopyToast = false },
                onUnblockToastDismiss = { showUnblockToast = false }
            )
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatarUrl = user?.avatarUrl,
            strings = strings,
            onDismiss = { showAvatarPicker = false },
            onConfirm = { base64 ->
                webSocket.uploadAvatar(resolvedUserId, base64)
                showAvatarPicker = false
            }
        )
    }

    if (showAvatarViewer && canShowAvatar) {
        AvatarViewerDialog(
            avatarUrl = user?.avatarUrl,
            strings = strings,
            onDismiss = { showAvatarViewer = false }
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Content                                                           */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileContent(
    user: UserCacheEntity?,
    scrollState: ScrollState,
    displayName: String,
    usernameText: String,
    statusText: String,
    statusColor: Color,
    isCurrentUser: Boolean,
    isBlockedByMe: Boolean,
    canShowAvatar: Boolean,
    resolvedUserId: Int,
    messagesWithAttachments: List<MessageEntity>,
    strings: VibeStrings,
    showCopyToast: Boolean,
    showUnblockToast: Boolean,
    onBack: () -> Unit,
    onOpenAvatar: () -> Unit,
    onEditAvatar: () -> Unit,
    onCopyUsername: () -> Unit,
    onWriteMessage: () -> Unit,
    onNavigateToMessage: (Int, Int) -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onCopyToastDismiss: () -> Unit,
    onUnblockToastDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // The old screen animated eight values against a hand-rolled collapse
    // distance. Here the header is static and only the top bar fades in, which
    // is both calmer and far less code.
    val fadeDistancePx = with(density) { 90.dp.toPx() }
    val topBarProgress by remember(fadeDistancePx) {
        derivedStateOf { (scrollState.value / fadeDistancePx).coerceIn(0f, 1f) }
    }

    val profileNestedScrollConnection = remember(scrollState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0 && scrollState.value > 0) {
                    val consumedY = -scrollState.dispatchRawDelta(-delta)
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(profileNestedScrollConnection)
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(TOP_BAR_HEIGHT + 8.dp))

            /* ---------------- Identity ---------------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SCREEN_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileAvatar(
                    avatarUrl = user?.avatarUrl,
                    canShowAvatar = canShowAvatar,
                    initial = displayName.trim().take(1).uppercase(),
                    accent = profileAvatarColor(resolvedUserId),
                    showCameraBadge = isCurrentUser,
                    strings = strings,
                    onClick = onOpenAvatar,
                    onEditAvatar = onEditAvatar
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = if (displayName.length > 22) Modifier.basicMarquee() else Modifier
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                val hasBadges = user?.isVerified == true || user?.isDeveloper == true ||
                        user?.isBot == true || user?.isBanned == true || user?.isFreezed == true
                if (hasBadges) {
                    Spacer(Modifier.height(14.dp))
                    UserBadgesRow(
                        isVerified = user?.isVerified == true,
                        isDeveloper = user?.isDeveloper == true,
                        isBot = user?.isBot == true,
                        isBanned = user?.isBanned == true,
                        isFreezed = user?.isFreezed == true
                    )
                }
            }

            /* ---------------- Actions ---------------- */
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SCREEN_PADDING),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isCurrentUser) {
                    ActionTile(
                        icon = Icons.Rounded.PhotoCamera,
                        label = strings.addAvatar,
                        onClick = onEditAvatar,
                        modifier = Modifier.weight(1f)
                    )
                } else if (!isBlockedByMe) {
                    ActionTile(
                        icon = Icons.Outlined.Chat,
                        label = strings.profileWriteBtn,
                        onClick = onWriteMessage,
                        modifier = Modifier.weight(1f)
                    )
                }

                ActionTile(
                    icon = Icons.Outlined.ContentCopy,
                    label = strings.profileCopyUsername,
                    onClick = onCopyUsername,
                    modifier = Modifier.weight(1f)
                )

                if (!isCurrentUser && user?.isBot != true) {
                    if (isBlockedByMe) {
                        ActionTile(
                            icon = Icons.Outlined.LockOpen,
                            label = strings.blockedUnblockBtn,
                            onClick = onUnblock,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        ActionTile(
                            icon = Icons.Outlined.Block,
                            label = strings.chatBlockUser,
                            tint = VibeError,
                            onClick = onBlock,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (isBlockedByMe) {
                Spacer(Modifier.height(20.dp))
                BlockedNotice(strings = strings)
            }

            /* ---------------- Info ---------------- */
            val about = user?.about?.takeIf {
                it.isNotBlank() && user?.isBanned != true && user?.isFreezed != true
            }
            val showRegisterDate = user?.isBot != true

            Spacer(Modifier.height(28.dp))
            SectionHeader(text = strings.profileSectionInfo)
            InfoBlock {
                InfoRow(
                    icon = Icons.Outlined.AlternateEmail,
                    value = usernameText,
                    label = strings.profileCopyUsername,
                    onClick = onCopyUsername
                )
                if (about != null) {
                    InfoSeparator()
                    InfoRow(
                        icon = Icons.Outlined.Info,
                        value = about,
                        label = strings.aboutLabel,
                        trailingIcon = when {
                            user?.isBanned == true -> Icons.Rounded.Delete
                            user?.isFreezed == true -> Icons.Rounded.AcUnit
                            else -> null
                        }
                    )
                }
                if (showRegisterDate) {
                    InfoSeparator()
                    InfoRow(
                        icon = Icons.Outlined.CalendarMonth,
                        value = formatRegisterDate(user?.registerDate, strings),
                        label = strings.registerDateLabel
                    )
                }
            }

            /* ---------------- Media ---------------- */
            if (messagesWithAttachments.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                ProfileMediaTabs(
                    messages = messagesWithAttachments,
                    partnerAvatarUrl = user?.avatarUrl,
                    onNavigateToMessage = onNavigateToMessage
                )
            }

            Spacer(Modifier.height(96.dp))
            Spacer(Modifier.navigationBarsPadding())
        }

        /* ---------------- Top bar ----------------
         * Transparent over the profile, opaque with the name once scrolled. */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = topBarProgress))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TOP_BAR_HEIGHT)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.backBtn,
                    onClick = onBack
                )

                Text(
                    text = displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .alpha(topBarProgress)
                )

                if (!isCurrentUser && user?.isBot != true) {
                    var showMenu by remember { mutableStateOf(false) }
                    val menuAnchor = com.flasskdev.vibe.ui.components.rememberVibeMenuAnchor()
                    Box(Modifier.vibeMenuAnchor(menuAnchor)) {
                        BarIconButton(icon = Icons.Rounded.MoreVert, contentDescription = strings.a11yProfileMenu, onClick = { showMenu = true })
                    }
                    com.flasskdev.vibe.ui.components.VibeContextMenu(
                        expanded = showMenu, anchor = menuAnchor, onDismiss = { showMenu = false },
                        actions = listOf(
                            com.flasskdev.vibe.ui.components.VibeMenuAction(label = strings.profileCopyUsername, icon = Icons.Rounded.ContentCopy, onClick = onCopyUsername),
                            com.flasskdev.vibe.ui.components.VibeMenuAction(
                                label = if (isBlockedByMe) strings.chatUnblockUser else strings.chatBlockUser,
                                icon = if (isBlockedByMe) Icons.Outlined.LockOpen else Icons.Outlined.Block,
                                destructive = !isBlockedByMe, onClick = { if (isBlockedByMe) onUnblock() else onBlock() }
                            )
                        )
                    )
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f * topBarProgress)
            )
        }

        /* ---------------- Toasts ---------------- */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            VibeToast(
                message = strings.usernameCopied,
                isVisible = showCopyToast,
                onDismiss = onCopyToastDismiss
            )
            VibeToast(
                message = strings.blockedUnblockedToast,
                isVisible = showUnblockToast,
                onDismiss = onUnblockToastDismiss
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Building blocks                                                   */
/* ------------------------------------------------------------------ */

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    canShowAvatar: Boolean,
    initial: String,
    accent: Color,
    showCameraBadge: Boolean,
    strings: VibeStrings,
    onClick: () -> Unit,
    onEditAvatar: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.size(AVATAR_SIZE)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (canShowAvatar) MaterialTheme.colorScheme.surfaceVariant else accent)
                .clickable(enabled = canShowAvatar, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (canShowAvatar) {
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
                    text = initial,
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (showCameraBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(VibePrimary)
                    .clickable(onClick = onEditAvatar),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = strings.a11yEditAvatar,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** Flat tile: icon over a short label. No gradient, no border, no elevation. */
@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = VibePrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BLOCK_CORNER))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SCREEN_PADDING + 4.dp, end = SCREEN_PADDING, bottom = 8.dp)
    )
}

@Composable
private fun InfoBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SCREEN_PADDING)
            .clip(RoundedCornerShape(BLOCK_CORNER))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)),
        content = content
    )
}

/**
 * Telegram-style row: the value reads first, the label sits under it as a hint.
 * Long values wrap instead of colliding with the label.
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    value: String,
    label: String,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
        if (trailingIcon != null) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
        }
    }
}

@Composable
private fun InfoSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

/** Quiet notice; the actual unblock action lives in the action row above. */
@Composable
private fun BlockedNotice(strings: VibeStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SCREEN_PADDING)
            .clip(RoundedCornerShape(BLOCK_CORNER))
            .background(VibeError.copy(alpha = 0.07f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = strings.profileBlockedTitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VibeError
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = strings.profileBlockedDesc,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Placeholder states                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun ProfileNotFoundState(userName: String, strings: VibeStrings, onBack: () -> Unit) {
    val targetUsername = if (userName.startsWith("@")) userName else "@$userName"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 48.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.PersonOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = strings.profileNotFoundTitle,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = strings.profileNotFoundDesc(targetUsername),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(28.dp))
        TextButton(onClick = onBack) {
            Text(
                text = strings.backBtn,
                color = VibePrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

/** Skeleton mirrors the final layout, with a slow fade instead of a spinner. */
@Composable
private fun ProfileLoadingState(strings: VibeStrings) {
    val transition = rememberInfiniteTransition(label = "profile-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f * pulse + 0.03f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SCREEN_PADDING)
            .padding(top = TOP_BAR_HEIGHT + 8.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(base)
        )
        Spacer(Modifier.height(18.dp))
        SkeletonBar(width = 170.dp, height = 22.dp, color = base)
        Spacer(Modifier.height(10.dp))
        SkeletonBar(width = 96.dp, height = 14.dp, color = base)
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(76.dp)
                        .clip(RoundedCornerShape(BLOCK_CORNER))
                        .background(base)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(BLOCK_CORNER))
                .background(base)
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = strings.profileLoading,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SkeletonBar(width: Dp, height: Dp, color: Color) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color)
    )
}

/* ------------------------------------------------------------------ */
/*  Avatar dialogs                                                    */
/* ------------------------------------------------------------------ */

@Composable
private fun AvatarPickerDialog(
    currentAvatarUrl: String?,
    strings: VibeStrings,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cropScale by remember { mutableFloatStateOf(1f) }
    var cropOffset by remember { mutableStateOf(Offset.Zero) }
    var boxWidthPx by remember { mutableFloatStateOf(0f) }
    var boxHeightPx by remember { mutableFloatStateOf(0f) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            cropScale = 1f
            cropOffset = Offset.Zero
        }
    }

    fun pickImage() {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.addAvatar,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (selectedImageUri != null) strings.avatarCropHint else strings.avatarPickPrompt,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(BLOCK_CORNER))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable(enabled = selectedImageUri == null) { pickImage() }
                        .onGloballyPositioned {
                            boxWidthPx = it.size.width.toFloat()
                            boxHeightPx = it.size.height.toFloat()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedImageUri != null -> {
                            val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                                cropScale = (cropScale * zoomChange).coerceIn(1f, 5f)
                                cropOffset += offsetChange
                            }
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = strings.a11yAvatarPreview,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures { _, dragAmount -> cropOffset += dragAmount }
                                    }
                                    .transformable(transformState)
                                    .graphicsLayer(
                                        scaleX = cropScale,
                                        scaleY = cropScale,
                                        translationX = cropOffset.x,
                                        translationY = cropOffset.y
                                    )
                            )
                        }

                        !currentAvatarUrl.isNullOrEmpty() -> AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentAvatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = strings.a11yAvatarPreview,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                        )

                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = strings.a11yChoosePhoto,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = strings.choosePhoto,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (selectedImageUri != null) {
                        // Dim everything outside the crop circle so the user sees
                        // exactly what will be uploaded.
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = CROP_CIRCLE_RADIUS.toPx()
                            val path = Path().apply {
                                addRect(Rect(0f, 0f, size.width, size.height))
                                addOval(
                                    Rect(
                                        center.x - radius,
                                        center.y - radius,
                                        center.x + radius,
                                        center.y + radius
                                    )
                                )
                                fillType = PathFillType.EvenOdd
                            }
                            drawPath(path, color = Color.Black.copy(alpha = 0.55f))
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = radius,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = strings.reportCancelBtn,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            val uri = selectedImageUri
                            if (uri == null) {
                                pickImage()
                            } else {
                                val base64 = ImageUtils.compressAndEncodeImage(
                                    context = context,
                                    uri = uri,
                                    scale = cropScale,
                                    offsetX = cropOffset.x,
                                    offsetY = cropOffset.y,
                                    boxWidthPx = boxWidthPx,
                                    boxHeightPx = boxHeightPx,
                                    circleRadiusPx = with(density) { CROP_CIRCLE_RADIUS.toPx() }
                                )
                                if (base64 != null) onConfirm(base64)
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedImageUri == null) strings.choosePhoto else strings.doneBtn,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VibePrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarViewerDialog(
    avatarUrl: String?,
    strings: VibeStrings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        var zoom by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
            zoom = (zoom * zoomChange).coerceIn(1f, 5f)
            offset += offsetChange
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(avatarUrl).crossfade(true).build(),
                contentDescription = strings.a11yAvatar,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            if (zoom > 1f) offset += dragAmount else onDismiss()
                        }
                    }
                    .transformable(transformState)
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 4.dp, start = 6.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.a11yAvatarViewerClose,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = zoom <= 1f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = strings.photoViewer,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Formatting                                                        */
/* ------------------------------------------------------------------ */

private fun profileStatusText(
    user: UserCacheEntity?,
    strings: VibeStrings,
    isRestricted: Boolean,
    isBlockedByMe: Boolean,
    isOnline: Boolean
): String = when {
    isRestricted -> strings.lastSeenLongAgo
    isBlockedByMe -> strings.chatStatusBlockedByMe
    user?.isBot == true -> strings.statusBot
    isOnline -> strings.statusOnline
    user?.lastSeenStatus in listOf("hidden", "approximate", "recently") -> strings.lastSeenRecently
    user?.lastSeenStatus == "long_ago" -> strings.lastSeenLongAgo
    user?.lastSeenStatus == "this_week" -> strings.lastSeenInWeek
    user?.lastSeenStatus == "this_month" -> strings.lastSeenInMonth
    else -> formatLastSeenProfile(user?.lastSeen, strings)
}

private fun formatRegisterDate(timestamp: Long?, strings: VibeStrings): String {
    if (timestamp == null) return strings.statusUnknown
    val now = Calendar.getInstance()
    val registered = Calendar.getInstance().apply { timeInMillis = timestamp }
    val day = registered.get(Calendar.DAY_OF_MONTH)
    val month = strings.monthsShort.getOrNull(registered.get(Calendar.MONTH)).orEmpty()
    val year = registered.get(Calendar.YEAR)
    val sameYear = now.get(Calendar.YEAR) == year
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - registered.get(Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayDiff == 0 -> strings.dateToday
        sameYear && dayDiff == 1 -> strings.dateYesterday
        sameYear -> "$day $month"
        else -> "$day $month $year"
    }
}

private fun formatLastSeenProfile(lastSeenTimestamp: Long?, strings: VibeStrings): String {
    if (lastSeenTimestamp == null) return strings.lastSeenRecently

    val now = Calendar.getInstance()
    val lastSeen = Calendar.getInstance().apply { timeInMillis = lastSeenTimestamp }
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSeenTimestamp))
    val sameYear = now.get(Calendar.YEAR) == lastSeen.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - lastSeen.get(Calendar.DAY_OF_YEAR)

    return when {
        sameYear && dayDiff == 0 -> strings.lastSeenToday(timeStr)
        sameYear && dayDiff == 1 -> strings.lastSeenYesterday(timeStr)
        !sameYear -> strings.lastSeenLongAgo
        else -> strings.lastSeenDate(
            SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(lastSeenTimestamp)),
            timeStr
        )
    }
}