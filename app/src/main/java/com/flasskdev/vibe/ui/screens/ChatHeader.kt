package com.flasskdev.vibe.ui.screens

import com.flasskdev.vibe.ui.components.VibeContextMenu
import com.flasskdev.vibe.ui.components.VibeMenuAction
import com.flasskdev.vibe.ui.components.rememberVibeMenuAnchor
import com.flasskdev.vibe.ui.components.vibeMenuAnchor
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.LocalGlobalAudioPlayer
import com.flasskdev.vibe.ui.components.TypingIndicator
import com.flasskdev.vibe.ui.theme.*
import com.flasskdev.vibe.ui.viewmodels.ChatScreenViewModel
import dev.chrisbanes.haze.HazeState
import io.github.fletchmckee.liquid.LiquidState

/**
 * Верх чата: навбар (назад / профиль / меню, либо поиск, либо мультивыбор), баннер
 * закреплённых сообщений и инлайновый мини-плеер.
 *
 * "Мигающее" состояние (печатает…, поиск, пины, статус собеседника) читается здесь,
 * а не в ChatScreen, поэтому его изменения не пересобирают список сообщений.
 *
 * ДИЗАЙН (iOS / Apple): edge-to-edge полупрозрачный навбар без скруглений, тени,
 * градиентов и бордеров. Фон уходит под статус-бар, снизу hairline 0.5dp.
 * Имя и статус центрированы, аватар справа, иконки плоские и монохромные,
 * отмена действий — текстовой кнопкой, как в системных приложениях iOS.
 * Все тексты берутся из VibeStrings (ru/en).
 */

private enum class ChatHeaderMode { SELECTION, SEARCH, PROFILE }

/**
 * ЛЕТАЮЩАЯ ПАНЕЛЬ (в паре с ChatInputBar).
 *
 * Хедер больше не edge-to-edge полоса под статус-баром: это отдельная карточка со
 * отступами по краям, скруглением и мягкой тенью. Сообщения проходят ПОД ней,
 * нижний hairline заменён обводкой по всему контуру.
 */
private val HeaderSideMargin = 10.dp
private val HeaderTopMargin = 6.dp
private val HeaderCorner = 24.dp
private val HeaderElevation = 14.dp

/** Компактная геометрия: ряд 44dp, кнопки 34dp, поле поиска 32dp. */
private val BarMinHeight = 44.dp
private val ActionButtonSize = 34.dp
private val SearchFieldHeight = 32.dp
private val Hairline = 0.5.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHeader(
    modifier: Modifier = Modifier,
    viewModel: ChatScreenViewModel,
    strings: com.flasskdev.vibe.ui.theme.VibeStrings,
    hazeState: HazeState,
    liquidState: LiquidState? = null,
    interlocutorId: Int,
    displayName: String,
    inputState: ChatInputState,
    selection: ChatSelectionState,
    toast: ChatToastState,
    onBack: () -> Unit,
    onProfileClick: (Int, String) -> Unit,
    onForwardSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onUnpinAll: () -> Unit,
    onHeaderMenuOpenChange: (Boolean) -> Unit = {}
) {
    val partnerUser by viewModel.partnerUser.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val currentSearchIndex by viewModel.currentSearchIndex.collectAsState()
    val pinnedMessages by viewModel.pinnedMessages.collectAsState()
    val currentPinnedIndex by viewModel.currentPinnedIndex.collectAsState()
    val audioPlayer = LocalGlobalAudioPlayer.current

    var showHeaderMenu by remember { mutableStateOf(false) }
    var userInfoBounds by remember { mutableStateOf(Rect.Zero) }
    LaunchedEffect(showHeaderMenu) { onHeaderMenuOpenChange(showHeaderMenu) }

    val userInfoScale by animateFloatAsState(
        targetValue = if (showHeaderMenu) 1.025f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "userInfoScale"
    )

    val colors = MaterialTheme.colorScheme
    val secondary = colors.onSurface.copy(alpha = 0.5f)
    val separator = colors.onSurface.copy(alpha = 0.12f)

    val isBlockedByMe = partnerUser?.isBlockedByMe == true
    val isRestricted = partnerUser?.isBlockedByUser == true ||
            partnerUser?.isBanned == true ||
            partnerUser?.isFreezed == true
    val isOnline = partnerUser?.isOnline == true && !isRestricted && !isBlockedByMe && partnerUser?.isBot != true

    val mode = when {
        selection.ids.isNotEmpty() -> ChatHeaderMode.SELECTION
        isSearchActive -> ChatHeaderMode.SEARCH
        else -> ChatHeaderMode.PROFILE
    }

    // Полупрозрачный "материал" панели: рефракция liquid + блюр Haze.
    // Панель — самостоятельная плавающая карточка, а не полоса во всю ширину.
    val glassActive = liquidState != null && VibeEffects.blurSupportedByDevice
    val panelAlpha = VibeEffects.chatPanelAlpha(hasLiquid = glassActive)
    val headerShape = RoundedCornerShape(HeaderCorner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(21f)
            .statusBarsPadding()
            .padding(
                start = HeaderSideMargin,
                end = HeaderSideMargin,
                top = HeaderTopMargin
            )
            .graphicsLayer {
                shadowElevation = HeaderElevation.toPx()
                shape = headerShape
                clip = true
            }
            // Форма отдаётся в стекло: иначе liquid считает край по CircleShape
            // и на высокой панели (пины / поиск в две строки) дуга уезжает.
            .vibeChatGlass(hazeState, liquidState, shape = headerShape)
            .background(colors.surface.copy(alpha = panelAlpha))
            .border(width = Hairline, color = separator, shape = headerShape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Crossfade(
                targetState = mode,
                animationSpec = tween(durationMillis = 150)
            ) { currentMode ->
                when (currentMode) {

                    // ---------- МУЛЬТИВЫБОР ----------
                    ChatHeaderMode.SELECTION -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = BarMinHeight)
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { selection.ids.clear() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.cancelBtn,
                                color = VibePrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Crossfade(
                            targetState = selection.ids.size,
                            animationSpec = tween(150),
                            modifier = Modifier.weight(1f)
                        ) { count ->
                            Text(
                                text = strings.selectedMessagesCount(count),
                                color = colors.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        IconButton(onClick = onForwardSelected, modifier = Modifier.size(ActionButtonSize)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = strings.actionForward,
                                tint = VibePrimary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(scaleX = -1f, scaleY = 1f)
                            )
                        }
                        IconButton(onClick = onDeleteSelected, modifier = Modifier.size(ActionButtonSize)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = strings.deleteBtn,
                                tint = VibeError,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // ---------- ПОИСК ----------
                    ChatHeaderMode.SEARCH -> Column(modifier = Modifier.fillMaxWidth()) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = BarMinHeight)
                                .padding(start = 12.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Поле поиска без заливки: «материалом» служит стекло
                            // самой летающей панели, плашка убрана.
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(SearchFieldHeight)
                                    .padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Лупа 18dp: одинаковая оптическая масса с иконками навбара.
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = secondary,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    textStyle = TextStyle(
                                        color = colors.onSurface,
                                        fontSize = 16.sp
                                    ),
                                    cursorBrush = SolidColor(VibePrimary),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onSearch = {
                                            val results = searchResults
                                            if (results.isNotEmpty()) {
                                                val target = results.getOrNull(currentSearchIndex) ?: results.first()
                                                viewModel.jumpToMessage(target.id, viewModel.messages.value)
                                            }
                                        }
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = strings.chatSearchPlaceholder,
                                                    color = secondary,
                                                    fontSize = 16.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.setSearchQuery("") },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = strings.chatSearchClear,
                                            tint = colors.onSurface.copy(alpha = 0.35f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            TextButton(
                                onClick = { viewModel.closeSearch() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = strings.cancelBtn,
                                    color = VibePrimary,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Вторая строка появляется только при непустом запросе:
                        // счётчик слева, дата и навигация справа (как find-on-page в Safari).
                        if (searchQuery.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .padding(start = 14.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (searchResults.isNotEmpty())
                                        strings.chatSearchCounter(currentSearchIndex + 1, searchResults.size)
                                    else
                                        strings.chatSearchNoResults,
                                    color = secondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { onOpenDatePicker() },
                                    modifier = Modifier.size(ActionButtonSize)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = strings.chatSearchByDate,
                                        tint = VibePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                val hasResults = searchResults.isNotEmpty()
                                IconButton(
                                    onClick = { viewModel.nextSearchResult(viewModel.messages.value) },
                                    enabled = hasResults,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = strings.chatSearchNext,
                                        tint = if (hasResults) VibePrimary else colors.onSurface.copy(alpha = 0.25f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.prevSearchResult(viewModel.messages.value) },
                                    enabled = hasResults,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = strings.chatSearchPrev,
                                        tint = if (hasResults) VibePrimary else colors.onSurface.copy(alpha = 0.25f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ---------- ПРОФИЛЬ ----------
                    ChatHeaderMode.PROFILE -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = BarMinHeight)
                            .padding(start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Тонкий шеврон "назад", как в iOS
                        IconButton(onClick = onBack, modifier = Modifier.size(ActionButtonSize)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                                contentDescription = strings.backBtn,
                                tint = VibePrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Центр: имя + статус, оптически по центру навбара
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { userInfoBounds = it.boundsInWindow() }
                                .graphicsLayer {
                                    scaleX = userInfoScale
                                    scaleY = userInfoScale
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onProfileClick(interlocutorId, displayName) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                com.flasskdev.vibe.ui.components.UserBadgesRow(
                                    isVerified = partnerUser?.isVerified == true,
                                    isDeveloper = partnerUser?.isDeveloper == true,
                                    isBot = partnerUser?.isBot == true,
                                    isBanned = partnerUser?.isBanned == true,
                                    isFreezed = partnerUser?.isFreezed == true,
                                    badgeSize = 13.dp
                                )
                            }

                            Crossfade(
                                targetState = isPartnerTyping,
                                animationSpec = tween(150)
                            ) { typing ->
                                if (typing) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = strings.typing,
                                            color = VibePrimary,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        TypingIndicator()
                                    }
                                } else {
                                    val statusText = when {
                                        isRestricted -> strings.lastSeenLongAgo
                                        isBlockedByMe -> strings.chatStatusBlockedByMe
                                        partnerUser?.isBot == true -> strings.statusBot
                                        isOnline -> strings.statusOnline
                                        partnerUser?.lastSeenStatus == "hidden" ||
                                                partnerUser?.lastSeenStatus == "approximate" ||
                                                partnerUser?.lastSeenStatus == "recently" -> strings.lastSeenRecently
                                        partnerUser?.lastSeenStatus == "long_ago" -> strings.lastSeenLongAgo
                                        partnerUser?.lastSeenStatus == "this_week" -> strings.lastSeenInWeek
                                        partnerUser?.lastSeenStatus == "this_month" -> strings.lastSeenInMonth
                                        else -> formatLastSeen(partnerUser?.lastSeen, strings)
                                    }

                                    Text(
                                        text = statusText,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        color = when {
                                            isBlockedByMe -> VibeError
                                            isOnline -> VibePrimary
                                            else -> secondary
                                        }
                                    )
                                }
                            }
                        }

                        // Аватар справа — открывает профиль
                        val showAvatar = !partnerUser?.avatarUrl.isNullOrEmpty() &&
                                partnerUser?.isBanned != true &&
                                partnerUser?.isFreezed != true &&
                                partnerUser?.isBlockedByUser != true

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VibePrimary.copy(alpha = 0.85f))
                                .clickable { onProfileClick(interlocutorId, displayName) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (showAvatar) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(partnerUser?.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = strings.chatAvatar,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = if (displayName.isNotEmpty())
                                        displayName.take(1).uppercase() else "",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Меню действий "…"
                        val headerMenuAnchor = rememberVibeMenuAnchor()
                        headerMenuAnchor.highlightBounds = userInfoBounds
                        headerMenuAnchor.cornerRadius = 16.dp

                        // Пункт 7: меню собирается как ДАННЫЕ, а не как разметка.
                        // Раньше условная логика (бот / заблокирован) жила прямо
                        // внутри DropdownMenu вперемешку с иконками, из-за чего
                        // добавление пункта требовало копипасты 18 строк.
                        val headerActions = buildList {
                            add(
                                VibeMenuAction(
                                    label = strings.chatActionSearch,
                                    icon = Icons.Default.Search,
                                    onClick = { viewModel.openSearch() }
                                )
                            )
                            add(
                                VibeMenuAction(
                                    label = strings.chatSearchByDate,
                                    icon = Icons.Default.CalendarMonth,
                                    onClick = { onOpenDatePicker() }
                                )
                            )
                            add(
                                VibeMenuAction(
                                    label = strings.formatFormat,
                                    icon = Icons.Default.FormatBold,
                                    selected = inputState.showFormattingBar,
                                    onClick = {
                                        inputState.showFormattingBar = !inputState.showFormattingBar
                                        if (!inputState.showFormattingBar) {
                                            inputState.showPreviewMode = false
                                        }
                                    }
                                )
                            )
                            if (partnerUser?.isBot != true) {
                                if (isBlockedByMe) {
                                    add(
                                        VibeMenuAction(
                                            label = strings.chatUnblockUser,
                                            icon = Icons.Outlined.LockOpen,
                                            startsGroup = true,
                                            onClick = {
                                                viewModel.unblockUser(interlocutorId)
                                                toast.show(strings.blockedUnblockedToast)
                                            }
                                        )
                                    )
                                } else {
                                    add(
                                        VibeMenuAction(
                                            label = strings.chatBlockUser,
                                            icon = Icons.Outlined.Block,
                                            destructive = true,
                                            startsGroup = true,
                                            onClick = {
                                                viewModel.blockUser(interlocutorId)
                                                toast.show(strings.chatUserBlockedToast)
                                            }
                                        )
                                    )
                                }
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showHeaderMenu = true },
                                modifier = Modifier
                                    .size(ActionButtonSize)
                                    .vibeMenuAnchor(headerMenuAnchor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = strings.chatMenu,
                                    tint = VibePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            VibeContextMenu(
                                expanded = showHeaderMenu,
                                anchor = headerMenuAnchor,
                                onDismiss = { showHeaderMenu = false },
                                actions = headerActions
                            )
                        }
                    }
                }
            }

            // ========== PINNED MESSAGES ==========
            if (pinnedMessages.isNotEmpty()) {
                val currentMsg = pinnedMessages.getOrNull(currentPinnedIndex) ?: pinnedMessages.first()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Внутри скруглённой карточки разделитель не должен
                        // упираться в обводку — поджимаем его по бокам.
                        .padding(horizontal = 14.dp)
                        .height(Hairline)
                        .background(separator)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .clickable { viewModel.jumpToMessage(currentMsg.id, viewModel.messages.value) }
                        .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VibePinnedIndicator(
                        count = pinnedMessages.size,
                        activeIndex = currentPinnedIndex.coerceIn(0, pinnedMessages.size - 1)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = VibePrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (pinnedMessages.size > 1)
                                    strings.pinnedMessages else strings.pinnedMessage,
                                color = VibePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (pinnedMessages.size > 1) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.chatPinnedCounter(
                                        currentPinnedIndex + 1,
                                        pinnedMessages.size
                                    ),
                                    color = VibePrimary.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        Crossfade(
                            targetState = currentMsg,
                            animationSpec = tween(150)
                        ) { msg ->
                            MessagePreviewBlock(
                                message = msg,
                                textColor = colors.onSurface.copy(alpha = 0.75f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { onUnpinAll() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.chatUnpinAllHint,
                            tint = colors.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ========== MINI PLAYER ==========
            val chatHazeState = remember { dev.chrisbanes.haze.HazeState() }
            var showChatExpandedPlayer by remember { mutableStateOf(false) }
            com.flasskdev.vibe.ui.components.GlobalMiniPlayer(
                viewModel = audioPlayer,
                hazeState = chatHazeState,
                isInline = true,
                onExpand = { showChatExpandedPlayer = true }
            )
            if (showChatExpandedPlayer) {
                com.flasskdev.vibe.ui.components.ExpandedAudioPlayerSheet(
                    viewModel = audioPlayer,
                    hazeState = chatHazeState,
                    onDismiss = { showChatExpandedPlayer = false }
                )
            }

            // Нижний hairline больше не нужен: контур карточки рисует .border выше.
        }
    }
}

/**
 * Вертикальный сегментированный индикатор закреплённых сообщений (тонкий, 2dp).
 * Показывает максимум 4 сегмента, окно скользит вокруг активного пина.
 */
@Composable
private fun VibePinnedIndicator(
    count: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val maxSegments = 4
    val visible = count.coerceIn(1, maxSegments)
    val start = when {
        count <= maxSegments -> 0
        activeIndex <= 1 -> 0
        activeIndex >= count - 2 -> count - maxSegments
        else -> activeIndex - 1
    }

    Column(
        modifier = modifier
            .width(2.dp)
            .height(30.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until visible) {
            val isActive = (start + i) == activeIndex
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.28f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(1.dp))
                    .background(VibePrimary.copy(alpha = alpha))
            )
        }
    }
}