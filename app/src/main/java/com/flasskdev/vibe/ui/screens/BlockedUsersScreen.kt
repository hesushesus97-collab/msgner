package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.data.BlockedUserItem
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import com.flasskdev.vibe.data.local.AppDatabase
import com.flasskdev.vibe.ui.components.UserBadgesRow
import com.flasskdev.vibe.ui.components.VibeToast
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------------- */
/*  Design tokens                                                            */
/* ------------------------------------------------------------------------- */

private val RowRadius = 20.dp
private val SearchRadius = 999.dp
private val ScreenPadding = 16.dp
private val BlockedRed = Color(0xFFFF3B30)

private val AvatarPalette = listOf(
    listOf(Color(0xFF7C5CFF), Color(0xFF5B8DEF)),
    listOf(Color(0xFF00C2A8), Color(0xFF0A84FF)),
    listOf(Color(0xFFFF8A5C), Color(0xFFFF5C8A)),
    listOf(Color(0xFF34C759), Color(0xFF00B3A4)),
    listOf(Color(0xFFFFB020), Color(0xFFFF6B57)),
    listOf(Color(0xFF5B8DEF), Color(0xFF7C5CFF))
)

/* ------------------------------------------------------------------------- */
/*  Screen                                                                   */
/* ------------------------------------------------------------------------- */

@Composable
fun BlockedUsersScreen(
    webSocket: VibeWebSocket,
    onBack: () -> Unit,
    onProfileClick: (userId: Int, username: String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalVibeStrings.current
    val currentUserId = remember { UserPreferences(context).userId }
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<BlockedUserItem>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }

    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }

    // Подтверждение разблокировки
    var pendingUnblock by remember { mutableStateOf<BlockedUserItem?>(null) }

    LaunchedEffect(showToast) {
        if (showToast) {
            delay(2500)
            showToast = false
        }
    }

    val listState = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4
        }
    }
    val dividerAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(180),
        label = "headerDivider"
    )

    DisposableEffect(webSocket, currentUserId, strings) {
        val listener = object : VibeWebSocketListener {
            override fun onBlockedUsersResult(
                newUsers: List<BlockedUserItem>,
                total: Int,
                p: Int,
                more: Boolean
            ) {
                totalCount = total
                page = p
                hasMore = more
                isLoading = false
                isLoadingMore = false

                if (p == 1) {
                    users = newUsers
                } else {
                    val existingIds = users.map { it.id }.toSet()
                    users = users + newUsers.filter { it.id !in existingIds }
                }
            }

            override fun onUnblockUserSuccess(blockedId: Int) {
                users = users.filter { it.id != blockedId }
                totalCount = maxOf(0, totalCount - 1)
                toastMessage = strings.blockedUnblockedToast
                showToast = true

                scope.launch(Dispatchers.IO) {
                    db.chatDao().updateUserBlockedByMe(blockedId, false)
                    db.chatDao().updateChatBlockedByMe(blockedId, false)
                }
            }
        }
        webSocket.addListener(listener)
        onDispose { webSocket.removeListener(listener) }
    }

    // Debounced search / initial load
    LaunchedEffect(searchQuery, currentUserId) {
        if (currentUserId <= 0) return@LaunchedEffect
        delay(if (searchQuery.isEmpty()) 0 else 300)
        isLoading = true
        page = 1
        webSocket.getBlockedUsers(currentUserId, page = 1, limit = 30, query = searchQuery.trim())
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        VibeTopGlow(height = 380.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

        /* ---------------- Top bar ---------------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = ScreenPadding, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
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
                text = strings.blockedTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f, fill = false)
            )
            AnimatedVisibility(
                visible = totalCount > 0,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.7f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f)
            ) {
                CountBadge(count = totalCount)
            }
        }

        /* ---------------- Search ---------------- */
        SearchField(
            query = searchQuery,
            placeholder = strings.blockedSearchPlaceholder,
            clearDescription = strings.blockedClearSearch,
            onQueryChange = { searchQuery = it },
            onClear = { searchQuery = "" }
        )

        HorizontalDivider(
            modifier = Modifier
                .padding(top = 10.dp)
                .alpha(dividerAlpha),
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
        )

        /* ---------------- Content ---------------- */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading && users.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = ScreenPadding, vertical = 12.dp)
                    ) {
                        repeat(6) {
                            SkeletonRow()
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                users.isEmpty() -> {
                    EmptyState(
                        isSearch = searchQuery.isNotBlank(),
                        title = if (searchQuery.isNotBlank())
                            strings.blockedSearchEmptyTitle else strings.blockedEmptyTitle,
                        description = if (searchQuery.isNotBlank())
                            strings.blockedSearchEmptyDesc(searchQuery.trim())
                        else
                            strings.blockedEmptyDesc,
                        actionText = strings.blockedClearSearch,
                        onAction = { searchQuery = "" }
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(
                            start = ScreenPadding,
                            end = ScreenPadding,
                            top = 12.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = users,
                            key = { _, item -> item.id }
                        ) { index, user ->
                            // Pagination trigger
                            if (index >= users.size - 4 && hasMore && !isLoadingMore) {
                                LaunchedEffect(index) {
                                    isLoadingMore = true
                                    webSocket.getBlockedUsers(
                                        currentUserId,
                                        page = page + 1,
                                        limit = 30,
                                        query = searchQuery.trim()
                                    )
                                }
                            }

                            BlockedUserRow(
                                user = user,
                                unblockText = strings.blockedUnblockBtn,
                                fallbackName = strings.blockedUserFallback(user.id),
                                onProfileClick = { onProfileClick(user.id, user.username ?: "") },
                                onUnblock = { pendingUnblock = user }
                            )
                        }

                        if (isLoadingMore) {
                            item(key = "loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = VibePrimary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Toast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                VibeToast(
                    message = toastMessage,
                    isVisible = showToast,
                    onDismiss = { showToast = false }
                )
            }
        }
    }
    }

    /* ---------------- Confirm dialog ---------------- */
    pendingUnblock?.let { target ->
        val name = target.name?.takeIf { it.isNotBlank() }
            ?: target.username
            ?: strings.blockedUserFallback(target.id)

        AlertDialog(
            onDismissRequest = { pendingUnblock = null },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(VibePrimary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = VibePrimary
                    )
                }
            },
            title = {
                Text(
                    text = strings.blockedUnblockConfirmTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = strings.blockedUnblockConfirmText(name),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingUnblock = null
                    webSocket.unblockUser(currentUserId, target.id)
                }) {
                    Text(
                        text = strings.blockedUnblockBtn,
                        color = VibePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnblock = null }) {
                    Text(strings.cancelBtn)
                }
            }
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Row                                                                      */
/* ------------------------------------------------------------------------- */

@Composable
private fun BlockedUserRow(
    user: BlockedUserItem,
    unblockText: String,
    fallbackName: String,
    onProfileClick: () -> Unit,
    onUnblock: () -> Unit
) {
    val displayName = user.name?.takeIf { it.isNotBlank() } ?: (user.username ?: fallbackName)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RowRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(RowRadius)
                )
                .pressable(onProfileClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(user = user, displayName = displayName)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(4.dp))
                    UserBadgesRow(
                        isVerified = user.isVerified,
                        isDeveloper = user.isDeveloper,
                        isBot = user.isBot,
                        isFreezed = user.isFreezed,
                        isBanned = user.isBanned,
                        badgeSize = 14.dp
                    )
                }

                if (!user.username.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "@${user.username}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            UnblockButton(text = unblockText, onClick = onUnblock)
        }
    }
}

@Composable
private fun UserAvatar(user: BlockedUserItem, displayName: String) {
    val gradient = remember(user.id) {
        Brush.linearGradient(AvatarPalette[user.id.mod(AvatarPalette.size)])
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            if (!user.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        // Заблокированные показываются приглушённо
                        .alpha(0.75f)
                )
            } else {
                Text(
                    text = displayName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Маркер блокировки
        Box(
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(2.dp)
                .clip(CircleShape)
                .background(BlockedRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(9.dp)
            )
        }
    }
}

@Composable
private fun UnblockButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SearchRadius))
            .background(VibePrimary.copy(alpha = 0.12f))
            .pressable(onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.LockOpen,
            contentDescription = null,
            tint = VibePrimary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = VibePrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Search                                                                   */
/* ------------------------------------------------------------------------- */

@Composable
private fun SearchField(
    query: String,
    placeholder: String,
    clearDescription: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (focused)
            VibePrimary.copy(alpha = 0.55f)
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
        animationSpec = tween(200),
        label = "searchBorder"
    )
    val iconTint by animateColorAsState(
        targetValue = if (focused)
            VibePrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(200),
        label = "searchIcon"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 4.dp)
            .clip(RoundedCornerShape(SearchRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(SearchRadius)
            )
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(VibePrimary),
            interactionSource = interaction,
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.6f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .pressable(onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = clearDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  States                                                                   */
/* ------------------------------------------------------------------------- */

@Composable
private fun EmptyState(
    isSearch: Boolean,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSearch) Icons.Rounded.Search else Icons.Outlined.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )

            if (isSearch) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(SearchRadius))
                        .background(VibePrimary.copy(alpha = 0.12f))
                        .pressable(onAction)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = null,
                        tint = VibePrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = actionText,
                        color = VibePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f * shimmer * 2)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RowRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(base)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(base)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.28f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(base)
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(SearchRadius))
                    .background(base)
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Small pieces                                                             */
/* ------------------------------------------------------------------------- */

@Composable
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SearchRadius))
            .background(BlockedRed.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val goingUp = targetState > initialState
                val enter = if (goingUp)
                    slideInVertically { it } + fadeIn(tween(180))
                else
                    slideInVertically { -it } + fadeIn(tween(180))
                val exit = if (goingUp)
                    slideOutVertically { -it } + fadeOut(tween(140))
                else
                    slideOutVertically { it } + fadeOut(tween(140))
                (enter togetherWith exit) using SizeTransform(clip = false)
            },
            label = "countBadge"
        ) { value ->
            Text(
                text = value.toString(),
                color = BlockedRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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