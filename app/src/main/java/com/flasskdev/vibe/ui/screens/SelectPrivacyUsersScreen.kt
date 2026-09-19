package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flasskdev.vibe.data.local.AppDatabase
import com.flasskdev.vibe.data.local.UserCacheEntity
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.theme.VibeTopGlow

/* ------------------------------------------------------------------ */
/*  Avatar palette                                                    */
/* ------------------------------------------------------------------ */

private val avatarPalette = listOf(
    Color(0xFF2196F3) to Color(0xFF1565C0),
    Color(0xFF7E57C2) to Color(0xFF4527A0),
    Color(0xFF26A69A) to Color(0xFF00695C),
    Color(0xFFEF5350) to Color(0xFFC62828),
    Color(0xFFFFA726) to Color(0xFFEF6C00),
    Color(0xFF66BB6A) to Color(0xFF2E7D32),
    Color(0xFFEC407A) to Color(0xFFAD1457)
)

private fun avatarBrush(id: Int): Brush {
    val (start, end) = avatarPalette[(id.hashCode() % avatarPalette.size + avatarPalette.size) % avatarPalette.size]
    return Brush.linearGradient(listOf(start, end))
}

private fun UserCacheEntity.displayName(strings: VibeStrings): String =
    name.ifBlank { username.ifBlank { strings.userFallback(id) } }

private fun UserCacheEntity.initial(strings: VibeStrings): String =
    displayName(strings).trim().firstOrNull()?.uppercase() ?: "?"

/* ------------------------------------------------------------------ */
/*  Screen                                                            */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectPrivacyUsersScreen(
    selectedUserIds: Set<Int>,
    onUsersSelected: (Set<Int>) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    var searchQuery by remember { mutableStateOf("") }
    var currentSelected by remember(selectedUserIds) { mutableStateOf(selectedUserIds) }

    // Only users the current user actually has chats with.
    val chatsWithUsers by db.chatDao().getChatsWithUserInfo().collectAsState(initial = emptyList())
    val allUsers = remember(chatsWithUsers) {
        chatsWithUsers
            .map {
                UserCacheEntity(
                    id = it.chat.interlocutorId,
                    name = it.name ?: "",
                    username = it.username ?: "",
                    avatarUrl = it.avatarUrl,
                    isOnline = it.isOnline ?: false,
                    lastSeen = it.lastSeen,
                    isBot = it.isBot ?: false,
                    about = it.about,
                    isDeveloper = false,
                    isVerified = false,
                    registerDate = null
                )
            }
            .distinctBy { it.id }
            .sortedBy { (it.name.ifBlank { it.username }).lowercase() }
    }

    val filteredUsers = remember(allUsers, searchQuery) {
        val q = searchQuery.trim().removePrefix("@")
        if (q.isEmpty()) allUsers
        else allUsers.filter {
            it.name.contains(q, ignoreCase = true) || it.username.contains(q, ignoreCase = true)
        }
    }

    // Sticky alphabet sections, digits and symbols bucketed under "#".
    val sections = remember(filteredUsers, strings) {
        filteredUsers.groupBy { user ->
            val ch = user.displayName(strings).trim().firstOrNull()
            if (ch != null && ch.isLetter()) ch.uppercase() else "#"
        }
    }

    val selectedUsers = remember(allUsers, currentSelected) {
        allUsers.filter { currentSelected.contains(it.id) }
    }
    val dirty = currentSelected != selectedUserIds
    val allFilteredSelected = filteredUsers.isNotEmpty() &&
        filteredUsers.all { currentSelected.contains(it.id) }

    fun toggle(id: Int) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        currentSelected = currentSelected.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
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
        /* ---------------- Header ---------------- */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.backBtn,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.privacyExceptionsTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (currentSelected.isEmpty()) {
                        strings.privacyExceptionsHint
                    } else {
                        strings.privacyExceptionsSelected(currentSelected.size)
                    },
                    color = if (currentSelected.isEmpty()) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    } else {
                        VibePrimary
                    },
                    fontSize = 12.sp,
                    fontWeight = if (currentSelected.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            // Save turns into a solid pill only when there is something to save.
            Button(
                onClick = {
                    onUsersSelected(currentSelected)
                    onBack()
                },
                enabled = dirty,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier.height(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibePrimary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
                    disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                )
            ) {
                Text(text = strings.doneBtn, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        /* ---------------- Selected chips ---------------- */
        AnimatedVisibility(
            visible = selectedUsers.isNotEmpty(),
            enter = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(120))
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedUsers, key = { it.id }) { user ->
                    SelectedUserChip(
                        user = user,
                        strings = strings,
                        onRemove = { toggle(user.id) }
                    )
                }
            }
        }

        /* ---------------- Search ---------------- */
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(VibePrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = strings.searchPlaceholder,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.blockedClearSearch,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        )

        /* ---------------- Bulk action row ---------------- */
        if (filteredUsers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.privacyExceptionsSelected(currentSelected.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentSelected = if (allFilteredSelected) {
                            currentSelected - filteredUsers.map { it.id }.toSet()
                        } else {
                            currentSelected + filteredUsers.map { it.id }.toSet()
                        }
                    }
                ) {
                    Text(
                        text = if (allFilteredSelected) {
                            strings.privacyExceptionsClearAll
                        } else {
                            strings.privacyExceptionsSelectAll
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VibePrimary
                    )
                }
            }
        }

        /* ---------------- List ---------------- */
        if (filteredUsers.isEmpty()) {
            EmptyState(
                searching = searchQuery.isNotBlank(),
                query = searchQuery.trim(),
                strings = strings
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                sections.forEach { (letter, users) ->
                    stickyHeader(key = "header-$letter") {
                        Text(
                            text = letter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibePrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(start = 20.dp, top = 10.dp, bottom = 6.dp)
                        )
                    }
                    items(users, key = { it.id }) { user ->
                        UserSelectionItem(
                            user = user,
                            strings = strings,
                            isSelected = currentSelected.contains(user.id),
                            onClick = { toggle(user.id) }
                        )
                    }
                }
            }
        }
    }
}
}

/* ------------------------------------------------------------------ */
/*  Rows and pieces                                                   */
/* ------------------------------------------------------------------ */

@Composable
private fun UserSelectionItem(
    user: UserCacheEntity,
    strings: VibeStrings,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val rowBackground by animateColorAsState(
        targetValue = if (isSelected) VibePrimary.copy(alpha = 0.07f) else Color.Transparent,
        animationSpec = tween(180),
        label = "row-bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .semantics { contentDescription = strings.a11yExceptionToggle(user.displayName(strings)) }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user = user, strings = strings, isSelected = isSelected)

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.displayName(strings),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (user.isBot) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = null,
                        tint = VibePrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            val secondary = when {
                user.username.isNotBlank() -> "@${user.username}"
                user.isOnline -> strings.statusOnline
                else -> null
            }
            if (secondary != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = secondary,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        SelectionCheckbox(isSelected = isSelected)
    }
}

@Composable
private fun UserAvatar(
    user: UserCacheEntity,
    strings: VibeStrings,
    isSelected: Boolean
) {
    val ringWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(180),
        label = "avatar-ring"
    )

    Box(modifier = Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(width = ringWidth, color = VibePrimary, shape = CircleShape)
                .padding(ringWidth + 1.dp)
                .clip(CircleShape)
                .background(avatarBrush(user.id)),
            contentAlignment = Alignment.Center
        ) {
            if (!user.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = user.initial(strings),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (user.isOnline) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
    }
}

@Composable
private fun SelectionCheckbox(isSelected: Boolean) {
    val container by animateColorAsState(
        targetValue = if (isSelected) VibePrimary else Color.Transparent,
        animationSpec = tween(160),
        label = "check-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) VibePrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
        animationSpec = tween(160),
        label = "check-border"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(container)
            .border(width = 2.dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(tween(160)) + fadeIn(tween(120)),
            exit = scaleOut(tween(120)) + fadeOut(tween(100))
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun SelectedUserChip(
    user: UserCacheEntity,
    strings: VibeStrings,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(VibePrimary.copy(alpha = 0.10f))
            .clickable(onClick = onRemove)
            .semantics { contentDescription = strings.a11yExceptionRemove(user.displayName(strings)) }
            .padding(start = 4.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(avatarBrush(user.id)),
            contentAlignment = Alignment.Center
        ) {
            if (!user.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = user.initial(strings),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = user.displayName(strings),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun EmptyState(
    searching: Boolean,
    query: String,
    strings: VibeStrings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (searching) Icons.Rounded.SearchOff else Icons.Rounded.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (searching) strings.blockedSearchEmptyTitle else strings.privacyExceptionsEmptyTitle,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (searching) {
                strings.blockedSearchEmptyDesc(query)
            } else {
                strings.privacyExceptionsEmptyDesc
            },
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}