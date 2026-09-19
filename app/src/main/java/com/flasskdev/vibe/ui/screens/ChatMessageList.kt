package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.flasskdev.vibe.data.local.InlineKeyboardButton
import com.flasskdev.vibe.data.local.MessageEntity
// Wildcard on purpose: VibePrimary, VibeStrings, а также модификаторы-эффекты
// (vibeChatGlassSource и т.п.) живут в этом пакете.
import com.flasskdev.vibe.ui.theme.*
import com.flasskdev.vibe.ui.viewmodels.ChatScreenViewModel
import com.flasskdev.vibe.ui.viewmodels.PendingInlineCallback
import dev.chrisbanes.haze.HazeState
import io.github.fletchmckee.liquid.LiquidState
import kotlinx.coroutines.launch

/** Постоянный нижний отступ списка. Высота нижней панели добавляется на фазе layout. */
private val LIST_BOTTOM_PADDING = 120.dp

/** Отступ сверху под хедер + панель закреплённых. */
private val LIST_TOP_PADDING = 160.dp

/** Зазор между бабблами: плотнее внутри серии сообщений одного автора. */
private val GAP_SAME_SENDER = 2.dp
private val GAP_NEW_SENDER = 12.dp

private const val SYSTEM_PINNED_PREFIX = "\$\$SYSTEM\$\$PINNED_MESSAGE|"

/**
 * Единый стабильный набор колбэков для бабблов.
 *
 * PERF: раньше ~15 лямбд создавались заново внутри каждого item на каждой рекомпозиции
 * списка (а список рекомпозится на каждый символ в поле ввода, т.к. List/Map нестабильны).
 * Из-за этого ни один MessageBubble не мог быть пропущен: каждый кадр перекомпозировались
 * все видимые сообщения вместе с медиа-контентом. Отсюда и «клавиатура выезжает рывками».
 * Теперь инстансы колбэков создаются один раз и переиспользуются.
 */
@Stable
private class MessageListActions(
    private val viewModel: ChatScreenViewModel,
    private val context: android.content.Context,
    private val selection: ChatSelectionState,
    private val toast: ChatToastState,
    private val copiedToastText: String,
    private val messagesRef: State<List<MessageEntity>>,
    private val pinRequestRef: State<(MessageEntity) -> Unit>,
    private val unpinRequestRef: State<(MessageEntity) -> Unit>,
    private val reportRequestRef: State<(MessageEntity) -> Unit>,
    private val forwardRequestRef: State<(MessageEntity) -> Unit>,
    private val deleteRequestRef: State<(MessageEntity) -> Unit>,
    private val menuOpenRef: State<(Boolean) -> Unit>,
    private val imageClickRef: State<(MessageEntity, Int) -> Unit>,
    private val profileClickRef: State<(Int, String) -> Unit>
) {
    val onReply: (MessageEntity) -> Unit = { viewModel.replyToMessage(it) }
    val onEditClick: (MessageEntity) -> Unit = { viewModel.startEditing(it) }
    val onReplyClick: (Int) -> Unit = { replyId -> viewModel.jumpToMessage(replyId, messagesRef.value) }
    val onRetryUpload: (Int) -> Unit = { msgId -> viewModel.retryUpload(context, msgId) }
    val onReactionToggle: (MessageEntity, String) -> Unit = { msg, emoji -> viewModel.toggleReaction(msg, emoji) }
    val onReactionLongClick: (MessageEntity, String) -> Unit = { msg, emoji -> viewModel.openReactionDetails(msg, emoji) }
    val onShowCopyToast: () -> Unit = { toast.show(copiedToastText) }
    val onPinRequest: (MessageEntity) -> Unit = { pinRequestRef.value(it) }
    val onUnpinRequest: (MessageEntity) -> Unit = { unpinRequestRef.value(it) }
    val onReportClick: (MessageEntity) -> Unit = { reportRequestRef.value(it) }
    val onForwardRequest: (MessageEntity) -> Unit = { forwardRequestRef.value(it) }
    val onDeleteRequest: (MessageEntity) -> Unit = { deleteRequestRef.value(it) }
    val onMenuOpenChange: (Boolean) -> Unit = { menuOpenRef.value(it) }
    val onImageClick: (MessageEntity, Int) -> Unit = { msg, index -> imageClickRef.value(msg, index) }
    val onProfileClick: (Int, String) -> Unit = { id, name -> profileClickRef.value(id, name) }

    /** Кэш per-message лямбд выделения: инстанс на id, а не на кадр. */
    private val selectToggles = HashMap<Int, () -> Unit>()

    fun selectToggle(messageId: Int): () -> Unit = selectToggles.getOrPut(messageId) {
        {
            if (selection.ids.contains(messageId)) {
                selection.ids.remove(messageId)
            } else if (selection.ids.size < MAX_SELECTION) {
                selection.ids.add(messageId)
            }
        }
    }

    /**
     * Кэш жил всё время существования экрана и рос по одному замыканию на каждое
     * когда-либо отрисованное сообщение. Чистится при выходе из режима выделения:
     * в этот момент лишняя рекомпозиция бабблов ничего не стоит.
     */
    fun pruneSelectToggles() {
        if (selectToggles.size > 128) selectToggles.clear()
    }

    private companion object {
        const val MAX_SELECTION = 10
    }
}

/**
 * Скроллящийся список сообщений. Вынесен из ChatScreen, чтобы ввод текста, тики плеера
 * и анимация нижней панели не пересобирали LazyColumn и видимые бабблы.
 */
@Composable
fun ChatMessageList(
    modifier: Modifier = Modifier,
    viewModel: ChatScreenViewModel,
    strings: VibeStrings,
    listState: LazyListState,
    hazeState: HazeState,
    liquidState: LiquidState? = null,
    layoutState: ChatLayoutState,
    selection: ChatSelectionState,
    toast: ChatToastState,
    messages: List<MessageEntity>,
    groupedMessages: Map<Long, List<MessageEntity>>,
    messagesById: Map<Int, MessageEntity>,
    /** id -> позиция в LazyColumn. Считается в ViewModel вместе с группировкой. */
    messageLazyIndex: Map<Int, Int>,
    pinnedIds: Set<Int>,
    pendingInlineCallbacks: Map<Int, PendingInlineCallback>,
    highlightedMessageId: Int?,
    myAvatarUrl: String?,
    partnerAvatarUrl: String?,
    partnerName: String?,
    chatMusicPlaylist: ChatAudioPlaylist,
    onProfileClick: (Int, String) -> Unit,
    onPinRequest: (MessageEntity) -> Unit,
    onUnpinRequest: (MessageEntity) -> Unit,
    onReportRequest: (MessageEntity) -> Unit,
    onForwardRequest: (MessageEntity) -> Unit,
    onDeleteRequest: (MessageEntity) -> Unit,
    /** Пункт 7: открытое меню сообщения прячет нижнюю панель. */
    onMessageMenuOpenChange: (Boolean) -> Unit,
    onImageClick: (MessageEntity, Int) -> Unit,
    isMessageMenuOpen: Boolean = false
) {
    val context = LocalContext.current

    // Колбэки родителя пересоздаются на каждой рекомпозиции ChatScreen, поэтому они
    // проксируются через State: сам холдер остаётся тем же инстансом.
    val messagesRef = rememberUpdatedState(messages)
    val pinRequestRef = rememberUpdatedState(onPinRequest)
    val unpinRequestRef = rememberUpdatedState(onUnpinRequest)
    val reportRequestRef = rememberUpdatedState(onReportRequest)
    val forwardRequestRef = rememberUpdatedState(onForwardRequest)
    val deleteRequestRef = rememberUpdatedState(onDeleteRequest)
    val menuOpenRef = rememberUpdatedState(onMessageMenuOpenChange)
    val imageClickRef = rememberUpdatedState(onImageClick)
    val profileClickRef = rememberUpdatedState(onProfileClick)

    val copiedToastText = strings.formatCopied
    val actions = remember(viewModel, context, selection, toast, copiedToastText) {
        MessageListActions(
            viewModel = viewModel,
            context = context,
            selection = selection,
            toast = toast,
            copiedToastText = copiedToastText,
            messagesRef = messagesRef,
            pinRequestRef = pinRequestRef,
            unpinRequestRef = unpinRequestRef,
            reportRequestRef = reportRequestRef,
            forwardRequestRef = forwardRequestRef,
            deleteRequestRef = deleteRequestRef,
            menuOpenRef = menuOpenRef,
            imageClickRef = imageClickRef,
            profileClickRef = profileClickRef
        )
    }

    val linkErrorText = strings.linkOpenFailed
    // Открытие ссылок из inline-кнопок. Мемоизируется, чтобы не ломать skipping бабблов.
    val openUrl: (String) -> Unit = remember(context, toast, linkErrorText) {
        { url ->
            try {
                val uri = android.net.Uri.parse(url)
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                )
            } catch (e: Exception) {
                toast.show(linkErrorText)
            }
        }
    }

    val onInlineButtonClick: (MessageEntity, InlineKeyboardButton) -> Unit =
        remember(viewModel, openUrl) {
            { msg, btn -> viewModel.onInlineButtonClicked(msg, btn, openUrl) }
        }

    val bubbleActions = remember(actions, onInlineButtonClick) {
        MessageBubbleActions(
            onReply = actions.onReply,
            onReplyClick = actions.onReplyClick,
            onEditClick = actions.onEditClick,
            onPinRequest = actions.onPinRequest,
            onUnpinRequest = actions.onUnpinRequest,
            onForwardRequest = actions.onForwardRequest,
            onDeleteRequest = actions.onDeleteRequest,
            onMenuOpenChange = actions.onMenuOpenChange,
            onProfileClick = actions.onProfileClick,
            onShowCopyToast = actions.onShowCopyToast,
            onImageClick = actions.onImageClick,
            onReportClick = actions.onReportClick,
            onRetryUpload = actions.onRetryUpload,
            onReactionToggle = actions.onReactionToggle,
            onReactionLongClick = actions.onReactionLongClick,
            onInlineButtonClick = onInlineButtonClick
        )
    }

    val bubbleUserData = remember(viewModel.myUserId, viewModel.myDisplayName, myAvatarUrl, partnerName, partnerAvatarUrl) {
        MessageBubbleUserData(
            myUserId = viewModel.myUserId,
            myDisplayName = viewModel.myDisplayName,
            myAvatarUrl = myAvatarUrl,
            partnerName = partnerName,
            partnerAvatarUrl = partnerAvatarUrl
        )
    }

    // Режим выделения читается один раз наверху: иначе каждый баббл подписывался на весь
    // список selection.ids и любое выделение инвалидировало все видимые сообщения.
    val selectionMode by remember(selection) {
        derivedStateOf { selection.ids.isNotEmpty() }
    }

    LaunchedEffect(selectionMode) {
        if (!selectionMode) actions.pruneSelectToggles()
    }

    // PERF: карта индексов приходит из ChatListSnapshot и строится в фоне вместе с
    // группировкой, а не на main thread в композиции. Она гарантированно соответствует
    // тому же снапшоту, что уходит в LazyColumn, поэтому прыжки по reply/пину не врут.
    val lazyIndexRef = rememberUpdatedState(messageLazyIndex)

    // PERF: ключом раньше была сама карта, то есть эффект перезапускался на КАЖДОЕ
    // изменение списка и, пока подсветка активна, повторно дёргал scrollToItem, воюя
    // с пользовательским скроллом и с автоскроллом новых сообщений.
    LaunchedEffect(highlightedMessageId) {
        val target = highlightedMessageId ?: return@LaunchedEffect
        val targetIndex = lazyIndexRef.value[target] ?: return@LaunchedEffect
        // scrollToItem performs a deterministic jump and supersedes the prior scroll mutation.
        listState.scrollToItem(targetIndex)
    }

    // Keep the scrolling content below the top chrome. This is a sibling-level layer boundary.
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(0f)
            .clipToBounds()
            // PERF: слой появляется только если VibeEffects.chatBlur = true или передан liquidState.
            .vibeChatGlassSource(hazeState, liquidState)
    ) {
        if (messages.isEmpty()) {
            ChatEmptyState(
                strings = strings,
                modifier = Modifier.blur(if (isMessageMenuOpen) 16.dp else 0.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    // Место под нижнюю панель резервируется на фазе layout (см. ChatUiState.kt),
                    // поэтому её рост больше не рекомпозит список.
                    .reserveBottomBarSpace(layoutState, LIST_BOTTOM_PADDING)
                    .padding(horizontal = 12.dp)
                    .semantics { contentDescription = strings.a11yMessageList },
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(
                    top = LIST_TOP_PADDING,
                    bottom = LIST_BOTTOM_PADDING
                )
            ) {
                groupedMessages.forEach { (dateMillis, messagesInDay) ->
                    itemsIndexed(
                        messagesInDay,
                        key = { _, it -> it.id },
                        // PERF: LazyColumn переиспользует слоты только внутри одного contentType.
                        // Без него при быстром скролле каждый баббл композился с нуля.
                        contentType = { _, it -> messageContentType(it) }
                    ) { index, message ->
                        val isNewerSame = messagesInDay.getOrNull(index - 1)?.senderId == message.senderId
                        val isOlderSame = messagesInDay.getOrNull(index + 1)?.senderId == message.senderId

                        if (message.content.startsWith(SYSTEM_PINNED_PREFIX)) {
                            val parts = message.content.substringAfter(SYSTEM_PINNED_PREFIX).split("|")
                            val senderName = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                                ?: strings.someoneLabel
                            val msgContent = parts.getOrNull(1) ?: ""
                            SystemMessageChip(
                                text = strings.pinnedMessageSystemText(senderName, msgContent),
                                label = strings.chatSystemMessageLabel,
                                modifier = Modifier.blur(if (isMessageMenuOpen) 16.dp else 0.dp)
                            )
                        } else {
                            // PERF: точечная подписка вместо чтения всего selection.ids в баббле.
                            val isSelected by remember(message.id, selection) {
                                derivedStateOf { selection.ids.contains(message.id) }
                            }

                            MessageBubble(
                                modifier = Modifier.padding(
                                    bottom = if (isNewerSame) GAP_SAME_SENDER else GAP_NEW_SENDER
                                ),
                                message = message,
                                repliedMessage = message.replyToId?.let { messagesById[it] },
                                isMine = message.senderId == viewModel.myUserId,
                                isNewerSameSender = isNewerSame,
                                isOlderSameSender = isOlderSame,
                                strings = strings,
                                isHighlighted = message.id == highlightedMessageId,
                                isPinned = message.id in pinnedIds,
                                isSelected = isSelected,
                                selectionMode = selectionMode,
                                onSelect = actions.selectToggle(message.id),
                                userData = bubbleUserData,
                                actions = bubbleActions,
                                inlineKeyboardEnabled = !pendingInlineCallbacks.containsKey(message.id),
                                pendingInlineCallbackData = pendingInlineCallbacks[message.id]?.callbackData,
                                chatPlaylist = chatMusicPlaylist,
                                isAnyMessageMenuOpen = isMessageMenuOpen
                            )
                        }
                    }

                    item(key = "date_$dateMillis") {
                        DateSeparator(
                            modifier = Modifier.blur(if (isMessageMenuOpen) 16.dp else 0.dp),
                            dateMillis = dateMillis,
                            strings = strings
                        )
                    }
                }
            }
        }

        ScrollToBottomFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .blur(if (isMessageMenuOpen) 16.dp else 0.dp),
            viewModel = viewModel,
            listState = listState,
            layoutState = layoutState,
            strings = strings,
            hasMessages = messages.isNotEmpty()
        )
    }
}

/* ------------------------------------------------------------------------------------------ */
/* Пустой чат                                                                                  */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun ChatEmptyState(
    strings: VibeStrings,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    // Мягкое «дыхание» подсветки. Один animateFloat, никаких перерисовок текста.
    val infinite = rememberInfiniteTransition(label = "emptyChatGlow")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyChatPulse"
    )

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val appear by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "emptyChatAppear"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = appear
                translationY = (1f - appear) * 24.dp.toPx()
            }
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Радиальное свечение под иконкой.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    VibePrimary.copy(alpha = 0.26f),
                                    VibePrimary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = colors.surface.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, VibePrimary.copy(alpha = 0.22f)),
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = VibePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = strings.emptyChat,
                color = colors.onBackground,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.emptyChatSubtitle,
                color = colors.onBackground.copy(alpha = 0.58f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                shape = CircleShape,
                color = VibePrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, VibePrimary.copy(alpha = 0.20f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = VibePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.emptyChatHint,
                        color = VibePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/* Системное сообщение                                                                         */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun SystemMessageChip(
    text: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = colors.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.07f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = label,
                    tint = colors.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = text,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = colors.onBackground.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/* Кнопка «вниз» + счётчик непрочитанных                                                       */
/* ------------------------------------------------------------------------------------------ */

@Composable
private fun ScrollToBottomFab(
    modifier: Modifier = Modifier,
    viewModel: ChatScreenViewModel,
    listState: LazyListState,
    layoutState: ChatLayoutState,
    strings: VibeStrings,
    hasMessages: Boolean
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme

    // Высота панели и счётчик непрочитанных читаются здесь, а не в списке.
    val bottomBarHeight = with(density) { layoutState.bottomBarHeightPx.toDp() }

    val showFab by remember {
        derivedStateOf {
            (listState.canScrollBackward || viewModel.isContextMode) && hasMessages
        }
    }

    val onClick: () -> Unit = remember(viewModel, listState, scope) {
        {
            if (!viewModel.isContextMode && listState.canScrollBackward) {
                scope.launch {
                    // Длинный путь схлопывается: сначала телепорт ближе к низу, потом анимация.
                    val current = listState.firstVisibleItemIndex
                    if (current > 15) {
                        listState.scrollToItem(15)
                    }
                    listState.animateScrollToItem(0)
                }
            } else {
                viewModel.jumpToBottom()
            }
            Unit
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = showFab,
            enter = scaleIn(
                initialScale = 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(160)),
            exit = scaleOut(targetScale = 0.7f, animationSpec = tween(140)) +
                    fadeOut(animationSpec = tween(120)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = maxOf(88.dp, bottomBarHeight + 8.dp))
        ) {
            Box {
                Surface(
                    onClick = onClick,
                    shape = CircleShape,
                    color = colors.surface.copy(alpha = 0.94f),
                    contentColor = VibePrimary,
                    border = BorderStroke(1.dp, VibePrimary.copy(alpha = 0.18f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .semantics { contentDescription = strings.a11yScrollToBottom }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                val unread = layoutState.newMessagesCount
                AnimatedVisibility(
                    visible = unread > 0,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(140)),
                    exit = scaleOut(animationSpec = tween(120)) + fadeOut(animationSpec = tween(100)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                ) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(VibePrimary, VibePrimary.copy(alpha = 0.82f))
                                )
                            )
                            .padding(horizontal = 6.dp)
                            .semantics { contentDescription = strings.a11yUnreadCount(unread) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unread > 99) strings.unreadCountOverflow else unread.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */
/* ime-сдвиг                                                                                   */
/* ------------------------------------------------------------------------------------------ */

/**
 * Сдвигает контент вверх на высоту клавиатуры БЕЗ повторного измерения.
 *
 * `Modifier.imePadding()` меняет размер узла, поэтому LazyColumn на каждом кадре анимации
 * клавиатуры заново измеряет и раскладывает видимые сообщения (а вместе с ними и текст),
 * плюс haze перерисовывает свой offscreen-слой. Здесь чтение инсета живёт внутри
 * graphicsLayer, то есть меняется только трансформация слоя: ни рекомпозиции, ни layout.
 *
 * Список с reverseLayout прижат к низу, так что сдвиг вверх - именно то поведение,
 * которое давал imePadding, только дешевле. Хит-тестинг учитывает трансформацию слоя.
 */
@Composable
fun Modifier.chatImeSlide(): Modifier {
    val imeInsets = WindowInsets.ime
    return this.graphicsLayer {
        translationY = -imeInsets.getBottom(this).toFloat()
    }
}