package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.*

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.input.pointer.pointerInput
import com.flasskdev.vibe.ui.theme.vibeOptionalBlur
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.zIndex
import androidx.compose.foundation.text.BasicTextField
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.github.fletchmckee.liquid.rememberLiquidState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.local.MessageEntity
import com.flasskdev.vibe.ui.components.VibeBackgroundMesh
import com.flasskdev.vibe.ui.components.VibeContextMenu
import com.flasskdev.vibe.ui.components.VibeMenuAction
import com.flasskdev.vibe.ui.components.rememberVibeMenuAnchor
import com.flasskdev.vibe.ui.theme.*
import com.flasskdev.vibe.ui.viewmodels.ChatScreenViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.produceState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import com.flasskdev.vibe.ui.components.TypingIndicator
import com.flasskdev.vibe.LocalGlobalAudioPlayer
import com.flasskdev.vibe.ui.viewmodels.AudioTrackInfo
import com.flasskdev.vibe.ui.components.VoiceMessageBubble
import com.flasskdev.vibe.utils.AudioRecorderHelper
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.graphics.asImageBitmap
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.FormatQuote
import com.flasskdev.vibe.ui.components.FormattedText
import com.flasskdev.vibe.utils.TextFormatting
import com.flasskdev.vibe.ui.theme.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/*
 * PERF-рефакторинг ChatScreen.
 *
 * Раньше это был один composable на ~2600 строк: чтение любого состояния (символ в поле ввода,
 * тик рекордера, высота панели эмодзи, индекс первого видимого элемента) инвалидировало весь
 * scope вместе с LazyColumn, поэтому пересобирались все видимые бабблы.
 *
 * Теперь экран собран из трёх независимых частей - ChatHeader, ChatMessageList, ChatInputBar,
 * а изменяемое состояние живёт в @Stable-холдерах из ChatUiState.kt. Сам ChatScreen эти холдеры
 * НЕ читает, только передаёт вниз, поэтому нажатие клавиши инвалидирует лишь ChatInputBar.
 */
/**
 * PERF: SimpleDateFormat дорогой в конструкторе, а создавался он на каждое сообщение
 * (внутри remember, но remember в баббле сбрасывается при любой смене id/времени, а сам
 * баббл не skippable). Один инстанс на локаль, вызывается только с main thread.
 */
private var bubbleTimeLocale: Locale? = null
private var bubbleTimeFormat: SimpleDateFormat? = null

private fun formatBubbleTime(timestamp: Long): String {
    val locale = Locale.getDefault()
    val cached = bubbleTimeFormat
    val format = if (cached != null && bubbleTimeLocale == locale) {
        cached
    } else {
        SimpleDateFormat("HH:mm", locale).also {
            bubbleTimeFormat = it
            bubbleTimeLocale = locale
        }
    }
    return format.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.FlowPreview::class)
@Composable
fun ChatScreen(
    interlocutorId: Int,
    interlocutorName: String,
    webSocket: VibeWebSocket,
    onBack: () -> Unit,
    onProfileClick: (Int, String) -> Unit,
    onNavigateToSpamInfo: (Int) -> Unit = {},
    scrollToMessageId: Int? = null,
    viewModel: ChatScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val db = remember { com.flasskdev.vibe.data.local.AppDatabase.getDatabase(context) }
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val scope = rememberCoroutineScope()

    // ViewModel и вебсокет-колбэки не видят CompositionLocal, но их тосты тоже нужно
    // локализовать. Страховочная синхронизация; в идеале это делается в VibeTheme.
    SideEffect { com.flasskdev.vibe.ui.theme.VibeStringsHolder.current = strings }

    // ---- Стабильные холдеры состояния. ChatScreen их не читает: только раздаёт детям. ----
    val inputState = remember(interlocutorId) { ChatInputState() }
    val layoutState = remember(interlocutorId) { ChatLayoutState() }

    /**
     * Пункт 7: открыто ли контекстное меню какого-нибудь сообщения.
     *
     * Читается ТОЛЬКО внутри graphicsLayer нижней панели, то есть на фазе draw:
     * иначе открытие меню рекомпозило бы весь экран вместе со списком сообщений.
     */
    val messageMenuOpen = remember(interlocutorId) { mutableStateOf(false) }

    val selection = remember(interlocutorId) { ChatSelectionState() }
    /**
     * Выделение, созданное одним пунктом меню («Переслать» / «Удалить») ради переиспользования
     * готовых диалогов. Если человек закрыл диалог, экран не должен остаться в режиме
     * мультивыделения с одним сообщением: такое выделение снимается автоматически.
     */
    var transientSelection by remember(interlocutorId) { mutableStateOf(false) }

    fun releaseTransientSelection() {
        if (transientSelection) {
            selection.ids.clear()
            transientSelection = false
        }
    }

    val toast = remember { ChatToastState() }

    // PERF: один снапшот вместо messages + groupedMessages. Обе карты (byId, lazyIndex)
    // приходят уже посчитанными из Dispatchers.Default, поэтому изменение сообщений
    // даёт ОДИН проход рекомпозиции, а не два с промежуточным пустым списком.
    val listSnapshot by viewModel.listSnapshot.collectAsState()
    val messages = listSnapshot.messages
    val partnerName by viewModel.partnerName.collectAsState()
    val partnerUser by viewModel.partnerUser.collectAsState()
    val pinnedMessages by viewModel.pinnedMessages.collectAsState()
    val pendingInlineCallbacks by viewModel.pendingInlineCallbacks.collectAsState()
    val highlightedMessageId by viewModel.highlightedMessageId.collectAsState()
    val myAvatarUrl by viewModel.myAvatarUrl.collectAsState()

    val effectiveUser = partnerUser
    val isBlockedByMe = effectiveUser?.isBlockedByMe == true
    val displayName = when {
        partnerUser?.isBanned == true -> strings.accountDeleted
        partnerUser?.isFreezed == true -> strings.accountFrozen
        else -> partnerName.ifEmpty { interlocutorName }
    }

    val pinnedIds = remember(pinnedMessages) { pinnedMessages.mapTo(HashSet()) { it.id } }
    // PERF: плейлист заворачивается в @Immutable data class. Раньше сюда уезжал новый
    // экземпляр List на каждое изменение списка сообщений: нестабильный параметр делал
    // MessageBubble принципиально не-skippable, то есть все видимые бабблы пересобирались.
    // Теперь equals сравнивает содержимое и Compose пропускает рекомпозицию.
    val audioFallbackTitle = strings.typeAudio
    val chatMusicPlaylist by produceState(
        ChatAudioPlaylist.Empty, messages, myAvatarUrl, partnerUser?.avatarUrl
    ) {
        value = withContext(Dispatchers.Default) {
            ChatAudioPlaylist(
                buildChatAudioPlaylist(messages, myAvatarUrl, partnerUser?.avatarUrl, viewModel.myUserId, audioFallbackTitle)
            )
        }
    }

    val listState = rememberLazyListState()
    val hazeState = remember { HazeState() }
    val liquidState = rememberLiquidState()

    // ---- Диалоги и оверлеи: живут здесь, потому что перекрывают весь экран. ----
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showForwardSheet by remember { mutableStateOf(false) }
    val forwardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showUnpinAllDialog by remember { mutableStateOf(false) }
    var showPinnedMessagesModal by remember { mutableStateOf(false) }
    var unpinForBoth by remember { mutableStateOf(false) }
    var messageToPin by remember { mutableStateOf<MessageEntity?>(null) }
    var messageToUnpin by remember { mutableStateOf<MessageEntity?>(null) }
    var reportMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var viewingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var viewingPhotoIndex by remember { mutableIntStateOf(0) }
    var botAlertText by remember { mutableStateOf<String?>(null) }
    var spamblockErrorMsg by remember { mutableStateOf<String?>(null) }
    var waitingForSpamInfo by remember { mutableStateOf(false) }

    // Черновик. Текст читается через snapshotFlow, а не в композиции, иначе ChatScreen снова
    // подписался бы на каждое нажатие клавиши.
    LaunchedEffect(interlocutorId) {
        val chat = withContext(Dispatchers.IO) { db.chatDao().getChatById(interlocutorId) }
        val draft = chat?.draft
        if (draft != null && inputState.value.text.isEmpty() && viewModel.editingMessage.value == null) {
            inputState.value = TextFieldValue(draft)
        }
        snapshotFlow { inputState.value.text }
            .debounce(300)
            .collect { text ->
                if (viewModel.editingMessage.value == null) {
                    db.chatDao().saveDraft(interlocutorId, text.ifBlank { null })
                }
            }
    }

    LaunchedEffect(Unit) {
        viewModel.botCallbackAlert.collect { (text, _) ->
            botAlertText = text
        }
    }
    LaunchedEffect(Unit) {
        viewModel.botCallbackToast.collect { text ->
            toast.show(text)
        }
    }

    DisposableEffect(webSocket) {
        val listener = object : com.flasskdev.vibe.data.VibeWebSocketListener {
            override fun onReportError(error: String) {
                toast.show(error)
            }
            override fun onReportSuccess(messageId: Int) {
                toast.show(strings.reportSentToast)
            }
            override fun onSendMessageError(error: String, message: String) {
                if (error == "spamblock_active") {
                    spamblockErrorMsg = message
                } else {
                    toast.show(message)
                }
            }
            override fun onUsersSearchResult(usersList: List<com.flasskdev.vibe.data.UserSearchResult>) {
                if (waitingForSpamInfo) {
                    val spamBot = usersList.find { it.username.equals("SpamInfo", ignoreCase = true) }
                    if (spamBot != null) {
                        waitingForSpamInfo = false
                        spamblockErrorMsg = null
                        onNavigateToSpamInfo(spamBot.id)
                    }
                }
            }
        }
        webSocket.addListener(listener)
        onDispose { webSocket.removeListener(listener) }
    }

    LaunchedEffect(interlocutorId) {
        viewModel.init(interlocutorId, interlocutorName, webSocket)
    }

    var hasJumpedToInitialMessage by remember(scrollToMessageId) { mutableStateOf(false) }
    LaunchedEffect(scrollToMessageId, messages.isNotEmpty()) {
        if (scrollToMessageId != null && scrollToMessageId > 0 && messages.isNotEmpty() && !hasJumpedToInitialMessage) {
            hasJumpedToInitialMessage = true
            viewModel.jumpToMessage(scrollToMessageId, messages)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUserInfo()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.activity.compose.BackHandler {
        // Читаем состояние в момент нажатия, а не в композиции.
        if (inputState.showEmojiPanel) {
            inputState.showEmojiPanel = false
        } else if (selection.ids.isNotEmpty()) {
            selection.ids.clear()
        } else if (viewModel.isSearchActive.value) {
            viewModel.closeSearch()
        } else {
            onBack()
        }
    }

    var previousLastMessageId by remember { mutableStateOf<Int?>(null) }
    var previousMessageCount by remember { mutableIntStateOf(0) }

    // PERF: было LaunchedEffect(listState.firstVisibleItemIndex) - чтение индекса в композиции,
    // то есть рекомпозиция всего ChatScreen на каждый проскролленный элемент.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index == 0) layoutState.newMessagesCount = 0
            }
    }

    // PERF: ключом был весь список, поэтому эффект перезапускался на каждую эмиссию Room,
    // даже когда содержимое не менялось.
    LaunchedEffect(messages.size, messages.lastOrNull()?.id) {
        val lastMsg = messages.lastOrNull()
        if (messages.size > 0 && previousMessageCount > 0) {
            if (messages.size < previousMessageCount) {
                // Сброс контекстного режима - скроллим в самый низ
                listState.scrollToItem(0)
            } else if (lastMsg != null && previousLastMessageId != null && lastMsg.id != previousLastMessageId) {
                // Новое сообщение пришло
                val added = maxOf(1, messages.size - previousMessageCount)
                val isMyMessage = lastMsg.senderId == viewModel.myUserId
                if (isMyMessage || listState.firstVisibleItemIndex <= 1) {
                    listState.animateScrollToItem(0)
                } else {
                    layoutState.newMessagesCount += added
                }
            }
        }
        previousMessageCount = messages.size
        previousLastMessageId = lastMsg?.id
    }

    // Подгрузка старых сообщений при скролле вверх (в reverseLayout это конец списка).
    //
    // PERF: раньше условие проверялось на КАЖДОЕ изменение layoutInfo и срабатывало сразу
    // при входе в чат, если вся первая страница влезала на экран (короткие сообщения). В
    // паре с безусловным ростом _messageLimit это давало каскад догрузок и многосекундный
    // фриз. Теперь:
    //  - canScrollForward: если контент вообще не скроллится, догружать нечего;
    //  - distinctUntilChanged по булеву флагу: один вызов на один вход в зону триггера.
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            Triple(lastVisible, total, listState.canScrollForward)
        }
            .map { (lastVisible, total, canScroll) ->
                total > 0 && canScroll && total - lastVisible <= 5
            }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                viewModel.loadMoreMessages()
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Int } }
            .distinctUntilChanged()
            // PERF: во время скролла этот блок выполнялся почти каждый кадр и грузил main thread
            // (updateCurrentPinnedIndex + onMessagesVisible + markRead по вебсокету).
            .debounce(120)
            .collect { visibleKeys ->
                if (visibleKeys.isNotEmpty()) {
                    viewModel.updateCurrentPinnedIndex(visibleKeys)
                    viewModel.onMessagesVisible(visibleKeys)
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {

        // Слой хрома над списком - и визуально, и в hit testing Compose.
        // PERF: imePadding() ЗДЕСЬ был главной причиной лагов при появлении клавиатуры.
        // Он менял размер контейнера каждый кадр анимации ime, а значит LazyColumn заново
        // измерял и раскладывал все видимые бабблы (с перерасчётом текста), плюс haze
        // перерисовывал и блюрил слой. Пустой чат этого не замечал - измерять нечего.
        // Теперь ime-инсет применяют только те части, которым он реально нужен.
        var isAttachMenuOpen by remember { mutableStateOf(false) }
        var isHeaderMenuOpen by remember { mutableStateOf(false) }

        val headerBlur by animateDpAsState(
            targetValue = 0.dp,
            animationSpec = tween(180),
            label = "headerBlur"
        )
        val messageListBlur by animateDpAsState(
            targetValue = 0.dp,
            animationSpec = tween(180),
            label = "messageListBlur"
        )
        val inputBarBlur by animateDpAsState(
            targetValue = 0.dp,
            animationSpec = tween(180),
            label = "inputBarBlur"
        )

        Box(modifier = Modifier.fillMaxSize().zIndex(20f)) {

            ChatHeader(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
                    .vibeOptionalBlur(headerBlur),
                viewModel = viewModel,
                strings = strings,
                hazeState = hazeState,
                liquidState = liquidState,
                interlocutorId = interlocutorId,
                displayName = displayName,
                inputState = inputState,
                selection = selection,
                toast = toast,
                onBack = onBack,
                onProfileClick = onProfileClick,
                onForwardSelected = { showForwardSheet = true },
                onDeleteSelected = { showDeleteDialog = true },
                onOpenDatePicker = { showDatePicker = true },
                onUnpinAll = { showUnpinAllDialog = true },
                onHeaderMenuOpenChange = { isHeaderMenuOpen = it }
            )

            ChatMessageList(
                // Список не пересчитывается под клавиатуру, а сдвигается на фазе draw.
                modifier = Modifier.chatImeSlide().vibeOptionalBlur(messageListBlur),
                viewModel = viewModel,
                strings = strings,
                listState = listState,
                hazeState = hazeState,
                liquidState = liquidState,
                layoutState = layoutState,
                selection = selection,
                toast = toast,
                messages = messages,
                groupedMessages = listSnapshot.grouped,
                messagesById = listSnapshot.byId,
                messageLazyIndex = listSnapshot.lazyIndex,
                pinnedIds = pinnedIds,
                pendingInlineCallbacks = pendingInlineCallbacks,
                highlightedMessageId = highlightedMessageId,
                myAvatarUrl = myAvatarUrl,
                partnerAvatarUrl = partnerUser?.avatarUrl,
                partnerName = partnerUser?.name,
                chatMusicPlaylist = chatMusicPlaylist,
                onProfileClick = onProfileClick,
                onPinRequest = { msg -> messageToPin = msg },
                onUnpinRequest = { msg -> messageToUnpin = msg },
                onReportRequest = { msg -> reportMessage = msg },
                // Переслать / удалить одно сообщение: переиспользуем те же диалоги,
                // что и мультивыделение, подставив в выделение ровно один id.
                onForwardRequest = { msg ->
                    selection.ids.clear()
                    selection.ids.add(msg.id)
                    transientSelection = true
                    showForwardSheet = true
                },
                onDeleteRequest = { msg ->
                    selection.ids.clear()
                    selection.ids.add(msg.id)
                    transientSelection = true
                    showDeleteDialog = true
                },
                onMessageMenuOpenChange = { open -> messageMenuOpen.value = open },
                onImageClick = { msg, idx ->
                    viewingMessage = msg
                    viewingPhotoIndex = idx
                },
                isMessageMenuOpen = messageMenuOpen.value
            )

            // Пункт 7: пока открыто меню сообщения, панель ввода уезжает под экран -
            // ровно так же, как в списке чатов прячется таббар. Сдвиг делается в
            // graphicsLayer, поэтому ни список, ни панель не перекомпозируются.
            val bottomBarShift by animateFloatAsState(
                targetValue = if (messageMenuOpen.value) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "chatBottomBarShift"
            )

            ChatInputBar(
                // Единственный участник иерархии, которому нужен реальный ime-инсет.
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .graphicsLayer {
                        if (bottomBarShift <= 0f) return@graphicsLayer
                        translationY = layoutState.bottomBarHeightPx * bottomBarShift
                        alpha = 1f - bottomBarShift
                    }
                    .vibeOptionalBlur(inputBarBlur),
                viewModel = viewModel,
                webSocket = webSocket,
                strings = strings,
                hazeState = hazeState,
                liquidState = liquidState,
                displayName = displayName,
                canMessage = effectiveUser?.canMessage != false,
                isBlockedByMe = isBlockedByMe,
                interlocutorId = interlocutorId,
                inputState = inputState,
                layoutState = layoutState,
                toast = toast,
                onAttachMenuOpenChange = { isAttachMenuOpen = it }
            )
        }

        if (showDeleteDialog) {
            val anyMine = selection.ids.any { id -> messages.find { it.id == id }?.senderId == viewModel.myUserId }
            val allMine = selection.ids.all { id -> messages.find { it.id == id }?.senderId == viewModel.myUserId }
            var deleteForEveryone by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    releaseTransientSelection()
                },
                title = { Text(text = strings.deleteMessagesTitle, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(text = strings.deleteMessagesText(selection.ids.size))
                        if (anyMine) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { deleteForEveryone = !deleteForEveryone }) {
                                Checkbox(checked = deleteForEveryone, onCheckedChange = { deleteForEveryone = it }, colors = CheckboxDefaults.colors(checkedColor = VibePrimary))
                                Spacer(modifier = Modifier.width(8.dp))
                                val textLabel = if (allMine) strings.deleteForEveryone(displayName) else strings.deleteForEveryoneAlsoMine(displayName)
                                Text(text = textLabel)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteMessages(selection.ids.toList(), deleteForEveryone)
                        selection.ids.clear()
                        transientSelection = false
                        showDeleteDialog = false
                    }) {
                        Text(strings.deleteBtn, color = VibeError, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        releaseTransientSelection()
                    }) {
                        Text(strings.cancelBtn, color = VibePrimary)
                    }
                },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                textContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        }

        var forwardSearchQuery by remember { mutableStateOf("") }

        if (showForwardSheet) {
            val recentChats by viewModel.recentChats.collectAsState()
            ModalBottomSheet(
                onDismissRequest = {
                    showForwardSheet = false
                    releaseTransientSelection()
                },
                sheetState = forwardSheetState,
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Text(
                        text = strings.forwardMessageTitle,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    androidx.compose.material3.OutlinedTextField(
                        value = forwardSearchQuery,
                        onValueChange = { forwardSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text(strings.searchPlaceholder) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibePrimary,
                            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredChats = if (forwardSearchQuery.isBlank()) {
                        recentChats
                    } else {
                        recentChats.filter {
                            val name = it.name ?: ""
                            val username = it.username ?: ""
                            name.contains(forwardSearchQuery, ignoreCase = true) ||
                                    username.contains(forwardSearchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredChats.isEmpty()) {
                        Text(
                            text = strings.noRecentChats,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(filteredChats) { chatUser ->
                                val chatDisplayName = when {
                                    chatUser.chat.isBanned -> strings.accountDeleted
                                    chatUser.chat.isFreezed -> strings.accountFrozen
                                    else -> chatUser.name ?: chatUser.username ?: strings.deletedAcc
                                }
                                val isBlocked = chatUser.chat.isBlockedByUser || chatUser.chat.isBanned || chatUser.chat.isFreezed
                                val isBlockedByMe = chatUser.chat.isBlockedByMe
                                val isOnline = chatUser.isOnline == true && !isBlocked && !isBlockedByMe && chatUser.isBot != true
                                val displayAvatarUrl = if (isBlocked) null else chatUser.avatarUrl

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.forwardMessages(chatUser.chat.interlocutorId, selection.ids.toList())
                                            selection.ids.clear()
                                            transientSelection = false
                                            showForwardSheet = false
                                            forwardSearchQuery = ""
                                            scope.launch { forwardSheetState.hide() }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!displayAvatarUrl.isNullOrBlank()) {
                                        coil.compose.AsyncImage(
                                            model = displayAvatarUrl,
                                            contentDescription = chatDisplayName,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(VibePrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (chatDisplayName.isNotEmpty()) chatDisplayName.take(1).uppercase() else "",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = chatDisplayName,
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            com.flasskdev.vibe.ui.components.UserBadgesRow(
                                                isVerified = chatUser.chat.isVerified,
                                                isDeveloper = chatUser.chat.isDeveloper,
                                                isBot = chatUser.isBot == true,
                                                isBanned = chatUser.chat.isBanned,
                                                isFreezed = chatUser.chat.isFreezed,
                                                badgeSize = 14.dp
                                            )
                                        }

                                        val statusText = if (isBlocked) {
                                            strings.lastSeenLongAgo
                                        } else if (isBlockedByMe) {
                                            strings.chatStatusBlockedByMe
                                        } else if (chatUser.isBot == true) {
                                            strings.statusBot
                                        } else if (isOnline) {
                                            strings.statusOnline
                                        } else {
                                            formatLastSeen(chatUser.lastSeen, strings)
                                        }

                                        Text(
                                            text = statusText,
                                            color = if (isBlockedByMe) com.flasskdev.vibe.ui.theme.VibeError
                                            else if (isOnline) com.flasskdev.vibe.ui.theme.VibeOnlineGreen
                                            else androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (messageToPin != null) {
            AlertDialog(
                onDismissRequest = { messageToPin = null },
                title = { Text(strings.pinMessage) },
                text = {
                    Column {
                        Text(strings.pinMessageConfirm)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { unpinForBoth = !unpinForBoth }) {
                            Checkbox(checked = unpinForBoth, onCheckedChange = { unpinForBoth = it })
                            Text(strings.forBoth(displayName))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.pinMessage(messageToPin!!.id, unpinForBoth)
                        messageToPin = null
                    }) {
                        Text(strings.pin)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToPin = null }) {
                        Text(strings.cancelBtn)
                    }
                }
            )
        }

        if (messageToUnpin != null) {
            AlertDialog(
                onDismissRequest = { messageToUnpin = null },
                title = { Text(strings.unpinMessage) },
                text = {
                    Column {
                        Text(strings.unpinMessageConfirm)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { unpinForBoth = !unpinForBoth }) {
                            Checkbox(checked = unpinForBoth, onCheckedChange = { unpinForBoth = it })
                            Text(strings.forBoth(displayName))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.unpinMessage(messageToUnpin!!.id, unpinForBoth)
                        messageToUnpin = null
                    }) {
                        Text(strings.unpin)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToUnpin = null }) {
                        Text(strings.cancelBtn)
                    }
                }
            )
        }

        if (showUnpinAllDialog) {
            AlertDialog(
                onDismissRequest = { showUnpinAllDialog = false },
                title = { Text(strings.unpinAll) },
                text = {
                    Column {
                        Text(strings.unpinAllConfirm)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { unpinForBoth = !unpinForBoth }) {
                            Checkbox(checked = unpinForBoth, onCheckedChange = { unpinForBoth = it })
                            Text(strings.forBoth(displayName))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.unpinAllMessages(unpinForBoth)
                        showUnpinAllDialog = false
                        showPinnedMessagesModal = false
                    }) {
                        Text(strings.unpin)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnpinAllDialog = false }) {
                        Text(strings.cancelBtn)
                    }
                }
            )
        }

        if (viewingMessage != null) {
            val attachments = viewingMessage!!.attachments ?: emptyList()
            if (attachments.isNotEmpty()) {
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    initialPage = viewingPhotoIndex,
                    pageCount = { attachments.size }
                )

                val msgTime = remember(viewingMessage!!.timestamp) {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(viewingMessage!!.timestamp))
                }
                val senderN = if (viewingMessage!!.senderId == viewModel.myUserId) strings.you else displayName

                var isZoomed by remember { mutableStateOf(false) }

                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { viewingMessage = null },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                        dismissOnBackPress = true
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier.fillMaxSize(),
                            pageSpacing = 16.dp,
                            userScrollEnabled = !isZoomed
                        ) { page ->
                            var targetZoomScale by remember { mutableFloatStateOf(1f) }
                            var targetZoomOffsetX by remember { mutableFloatStateOf(0f) }
                            var targetZoomOffsetY by remember { mutableFloatStateOf(0f) }
                            var isPinching by remember { mutableStateOf(false) }

                            val animatedZoomScale by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = targetZoomScale,
                                animationSpec = if (isPinching) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring()
                            )
                            val animatedZoomOffsetX by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = targetZoomOffsetX,
                                animationSpec = if (isPinching) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring()
                            )
                            val animatedZoomOffsetY by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = targetZoomOffsetY,
                                animationSpec = if (isPinching) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring()
                            )

                            LaunchedEffect(animatedZoomScale) {
                                if (page == pagerState.currentPage) {
                                    isZoomed = animatedZoomScale > 1f
                                }
                            }

                            LaunchedEffect(pagerState.currentPage) {
                                if (page != pagerState.currentPage) {
                                    targetZoomScale = 1f
                                    targetZoomOffsetX = 0f
                                    targetZoomOffsetY = 0f
                                }
                            }

                            val attachmentPath = attachments[page]
                            val isVideo = com.flasskdev.vibe.utils.AttachmentUtils.isPlayableVideo(attachmentPath)
                            val isLocal = attachmentPath.startsWith("/") || attachmentPath.startsWith("content://") || attachmentPath.contains("cacheDir")

                            val model = if (isLocal) {
                                java.io.File(attachmentPath)
                            } else if (attachmentPath.startsWith("http")) {
                                attachmentPath
                            } else {
                                "https://flasskdev.alwaysdata.net/api/upload/file/$attachmentPath"
                            }

                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val diff = 1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                            val baseAlpha = 0.3f + 0.7f * diff
                            val baseScale = 0.85f + 0.15f * diff

                            var componentSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { componentSize = it }
                                    .graphicsLayer {
                                        this.alpha = baseAlpha
                                        if (page == pagerState.currentPage) {
                                            scaleX = baseScale * animatedZoomScale
                                            scaleY = baseScale * animatedZoomScale
                                            translationX = animatedZoomOffsetX
                                            translationY = animatedZoomOffsetY
                                        } else {
                                            scaleX = baseScale
                                            scaleY = baseScale
                                        }
                                    }
                                    .pointerInput(page == pagerState.currentPage) {
                                        if (!isVideo && page == pagerState.currentPage) {
                                            awaitEachGesture {

                                                awaitFirstDown(requireUnconsumed = false)
                                                isPinching = true
                                                do {
                                                    val event = awaitPointerEvent()
                                                    val zoom = event.calculateZoom()
                                                    val pan = event.calculatePan()

                                                    if (targetZoomScale > 1f || event.changes.size > 1) {
                                                        event.changes.forEach { it.consume() }
                                                        targetZoomScale = (targetZoomScale * zoom).coerceIn(1f, 5f)

                                                        if (targetZoomScale > 1f) {
                                                            val maxX = (targetZoomScale - 1) * componentSize.width / 2
                                                            val maxY = (targetZoomScale - 1) * componentSize.height / 2
                                                            targetZoomOffsetX = (targetZoomOffsetX + pan.x * 2.5f).coerceIn(-maxX, maxX)
                                                            targetZoomOffsetY = (targetZoomOffsetY + pan.y * 2.5f).coerceIn(-maxY, maxY)
                                                        } else {
                                                            targetZoomOffsetX = 0f
                                                            targetZoomOffsetY = 0f
                                                        }
                                                    }
                                                } while (event.changes.any { it.pressed })
                                                isPinching = false
                                            }
                                        }
                                    }
                                    .pointerInput(page == pagerState.currentPage) {
                                        if (!isVideo && page == pagerState.currentPage) {
                                            detectTapGestures(

                                                onDoubleTap = {
                                                    isPinching = false
                                                    if (targetZoomScale > 1f) {
                                                        targetZoomScale = 1f
                                                        targetZoomOffsetX = 0f
                                                        targetZoomOffsetY = 0f
                                                    } else {
                                                        targetZoomScale = 2.5f
                                                    }
                                                }
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isVideo) {
                                    com.flasskdev.vibe.ui.components.InlineVideoPlayer(
                                        attachmentPath = attachmentPath,
                                        onClose = { viewingMessage = null },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.8f)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(model)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Photo view",
                                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                ))
                                .padding(top = 40.dp, bottom = 24.dp, start = 8.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewingMessage = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = senderN, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = msgTime, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }

                        if (viewingMessage!!.content.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    ))
                                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 48.dp)
                            ) {
                                if (com.flasskdev.vibe.utils.TextFormatting.hasFormatting(viewingMessage!!.content)) {
                                    FormattedText(
                                        text = viewingMessage!!.content,
                                        baseColor = Color.White,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp,
                                        isMine = true
                                    )
                                } else {
                                    Text(
                                        text = viewingMessage!!.content,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (reportMessage != null) {
        ReportDialog(
            onDismiss = { reportMessage = null },
            onSubmit = { reason, comment ->
                webSocket.reportMessage(
                    theme = reason,
                    fromUser = viewModel.myUserId,
                    toUser = reportMessage!!.senderId,
                    messageId = reportMessage!!.id,
                    comment = comment
                )
                reportMessage = null
            }
        )
    }

    if (spamblockErrorMsg != null) {
        AlertDialog(
            onDismissRequest = { spamblockErrorMsg = null },
            title = { Text(strings.restrictionTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(spamblockErrorMsg!!, color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(onClick = { spamblockErrorMsg = null }) {
                    Text(strings.restrictionUnderstood, color = VibePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    waitingForSpamInfo = true
                    webSocket.searchUsers("SpamInfo", viewModel.myUserId)
                }) {
                    Text(strings.restrictionWhy, color = VibePrimary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        viewModel.jumpToDate(
                            targetTimestamp = selectedMillis,
                            messageList = messages,
                            onNotFound = {
                                toast.show(strings.dateJumpNotFound)
                            }
                        )
                    }
                    showDatePicker = false
                }) {
                    Text(strings.okBtn, color = VibePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancelBtn, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = strings.datePickerTitle,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    subheadContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = VibePrimary,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = VibePrimary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = VibePrimary,
                    todayContentColor = VibePrimary,
                    todayDateBorderColor = VibePrimary
                )
            )
        }
    }

    val reactionSheetData by viewModel.reactionSheetState.collectAsState()
    if (reactionSheetData != null) {
        ReactionDetailsBottomSheet(
            sheetData = reactionSheetData!!,
            onSelectEmoji = { viewModel.selectReactionEmojiTab(it) },
            onLoadMore = { viewModel.loadMoreReactionUsers() },
            onDismiss = { viewModel.closeReactionDetails() },
            onProfileClick = onProfileClick
        )
    }
    ChatToastHost(state = toast)

    if (botAlertText != null) {
        AlertDialog(
            onDismissRequest = { botAlertText = null },
            title = {
                Text(
                    text = effectiveUser?.name ?: strings.botMessageTitle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = botAlertText!!,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { botAlertText = null }) {
                    Text(strings.okBtn, color = VibePrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
/** Держатель координат баббла: намеренно не state, см. комментарий в MessageBubble. */
private class BubbleCoordsHolder {
    var value: LayoutCoordinates? = null
}

@Immutable
data class MessageBubbleActions(
    val onReply: (MessageEntity) -> Unit = {},
    val onReplyClick: (Int) -> Unit = {},
    val onEditClick: (MessageEntity) -> Unit = {},
    val onPinRequest: (MessageEntity) -> Unit = {},
    val onUnpinRequest: (MessageEntity) -> Unit = {},
    val onForwardRequest: (MessageEntity) -> Unit = {},
    val onDeleteRequest: (MessageEntity) -> Unit = {},
    val onMenuOpenChange: (Boolean) -> Unit = {},
    val onProfileClick: (Int, String) -> Unit = { _, _ -> },
    val onShowCopyToast: () -> Unit = {},
    val onImageClick: (MessageEntity, Int) -> Unit = { _, _ -> },
    val onReportClick: (MessageEntity) -> Unit = {},
    val onRetryUpload: (Int) -> Unit = {},
    val onReactionToggle: (MessageEntity, String) -> Unit = { _, _ -> },
    val onReactionLongClick: (MessageEntity, String) -> Unit = { _, _ -> },
    val onInlineButtonClick: (MessageEntity, com.flasskdev.vibe.data.local.InlineKeyboardButton) -> Unit = { _, _ -> },
)

@Immutable
data class MessageBubbleUserData(
    val myUserId: Int = 0,
    val myDisplayName: String? = null,
    val myAvatarUrl: String? = null,
    val partnerName: String? = null,
    val partnerAvatarUrl: String? = null,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    message: MessageEntity,
    repliedMessage: MessageEntity? = null,
    isMine: Boolean,
    isNewerSameSender: Boolean = false,
    isOlderSameSender: Boolean = false,
    strings: com.flasskdev.vibe.ui.theme.VibeStrings,
    isHighlighted: Boolean = false,
    isPinned: Boolean = false,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    isAnyMessageMenuOpen: Boolean = false,
    onSelect: (() -> Unit)? = null,
    userData: MessageBubbleUserData = MessageBubbleUserData(),
    actions: MessageBubbleActions = MessageBubbleActions(),
    inlineKeyboardEnabled: Boolean = true,
    pendingInlineCallbackData: String? = null,
    chatPlaylist: ChatAudioPlaylist = ChatAudioPlaylist.Empty,
) {
    val onReply = actions.onReply
    val onReplyClick = actions.onReplyClick
    val onEditClick = actions.onEditClick
    val onPinRequest = actions.onPinRequest
    val onUnpinRequest = actions.onUnpinRequest
    val onForwardRequest = actions.onForwardRequest
    val onDeleteRequest = actions.onDeleteRequest
    val onMenuOpenChange = actions.onMenuOpenChange
    val onProfileClick = actions.onProfileClick
    val onShowCopyToast = actions.onShowCopyToast
    val onImageClick = actions.onImageClick
    val onReportClick = actions.onReportClick
    val onRetryUpload = actions.onRetryUpload
    val onReactionToggle = actions.onReactionToggle
    val onReactionLongClick = actions.onReactionLongClick
    val onInlineButtonClick = actions.onInlineButtonClick

    val myUserId = userData.myUserId
    val myDisplayName = userData.myDisplayName
    val myAvatarUrl = userData.myAvatarUrl
    val partnerName = userData.partnerName
    val partnerAvatarUrl = userData.partnerAvatarUrl

    val audioPlayer = LocalGlobalAudioPlayer.current
    // PERF: подписки на состояние плеера перенесены в аудио-баблы (VoiceBubbleHost /
    // MessageAudioList). Раньше каждый видимый баббл слушал progress/currentPosition,
    // и при проигрывании весь список рекомпозился десятки раз в секунду.

    val timeFormatted = remember(message.timestamp) { formatBubbleTime(message.timestamp) }

    val context = androidx.compose.ui.platform.LocalContext.current
    // PERF: раньше offsetX читался в композиции через animateFloatAsState + Modifier.offset,
    // поэтому баббл рекомпозился каждый кадр свайпа. Animatable + graphicsLayer держат
    // всё это на фазе draw.
    val swipeScope = rememberCoroutineScope()
    val swipeX = remember { Animatable(0f) }
    var showMenu by remember { mutableStateOf(false) }
    val menuAnchor = rememberVibeMenuAnchor()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val isTarget = showMenu || (isAnyMessageMenuOpen && isSelected)
    val targetScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMedium),
        label = "bubbleMenuScale"
    )
    val targetElevation by animateDpAsState(
        targetValue = if (isTarget) 12.dp else 0.dp,
        animationSpec = tween(180),
        label = "bubbleMenuElevation"
    )
    val bubbleBlur by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = tween(180),
        label = "bubbleBlur"
    )

    // PERF: координаты баббла держим в обычном холдере, не в snapshot-состоянии.
    // Запись состояния из onPlaced инвалидировала бы баббл на каждом кадре скролла,
    // а значение нужно ровно один раз - в момент открытия меню.
    val bubbleCoords = remember { BubbleCoordsHolder() }

    // Пункт 7: сообщаем наверх, открыто ли меню. onDispose нужен на случай, когда
    // строка уезжает из LazyColumn с открытым меню: иначе панель ввода осталась бы скрытой.
    DisposableEffect(showMenu) {
        onMenuOpenChange(showMenu)
        onDispose { if (showMenu) onMenuOpenChange(false) }
    }

    val highlightAnim = remember { Animatable(0f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            highlightAnim.snapTo(0f)
            highlightAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            kotlinx.coroutines.delay(500)
            highlightAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1100, easing = LinearOutSlowInEasing)
            )
        } else if (highlightAnim.value > 0f) {
            highlightAnim.snapTo(0f)
        }
    }

    val isVoiceMessage = message.content.startsWith("duration:")
    val isVideoMessage = message.content.startsWith("video_message:")
    val isSticker = message.content.startsWith("sticker:")
    val isGif = message.content.startsWith("gif:")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .vibeOptionalBlur(bubbleBlur)
            .graphicsLayer { translationX = swipeX.value * density },

        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background == VibeBackgroundDark
        val myBubbleColor = if (isDark) VibeBubbleMine else VibeBubbleMineLight
        val theirBubbleColor = if (isDark) VibeBubbleTheirsDark else VibeBubbleTheirs
        val bubbleBg = if (isMine) myBubbleColor else theirBubbleColor

        // PERF: 4 прохода фильтрации по вложениям выполнялись на каждой рекомпозиции баббла.
        val attachmentBuckets = remember(message.attachments) {
            val atts = message.attachments ?: emptyList()
            val imgs = atts.filter { com.flasskdev.vibe.utils.AttachmentUtils.isImage(it) }
            val vids = atts.filter { com.flasskdev.vibe.utils.AttachmentUtils.isPlayableVideo(it) }
            val auds = atts.filter { com.flasskdev.vibe.utils.AttachmentUtils.isPlayableAudio(it) }
            val other = atts.filter { it !in imgs && it !in vids && it !in auds }
            AttachmentBuckets(images = imgs, videos = vids, audios = auds, files = other)
        }
        val images = attachmentBuckets.images
        val videos = attachmentBuckets.videos
        val mediaAttachments = attachmentBuckets.mediaAttachments
        val files = attachmentBuckets.files
        val audios = attachmentBuckets.audios

        val isOnlyImagesAndText = mediaAttachments.isNotEmpty() && files.isEmpty() && audios.isEmpty() && !isVoiceMessage && !isVideoMessage && message.replyToId == null && message.forwardedFromId == null

        val actualBubbleBg = if (isOnlyImagesAndText || isVideoMessage || isSticker || isGif) Color.Transparent else bubbleBg
        val paddingX = if (isOnlyImagesAndText || isVideoMessage || isSticker || isGif) 0.dp else 14.dp
        val paddingY = if (isOnlyImagesAndText || isVideoMessage || isSticker || isGif) 0.dp else 8.dp

        val bubbleShape = RoundedCornerShape(
            topStart = if (isMine) 20.dp else (if (isOlderSameSender) 4.dp else 20.dp),
            topEnd = if (!isMine) 20.dp else (if (isOlderSameSender) 4.dp else 20.dp),
            bottomStart = if (isMine) 20.dp else (if (isNewerSameSender) 4.dp else 20.dp),
            bottomEnd = if (!isMine) 20.dp else (if (isNewerSameSender) 4.dp else 20.dp)
        )

        val rowHighlightBg = if (isSelected) {
            VibePrimary.copy(alpha = 0.12f)
        } else if (highlightAnim.value > 0.005f) {
            VibePrimary.copy(alpha = highlightAnim.value * 0.22f)
        } else {
            Color.Transparent
        }

        val scale = 1f + (highlightAnim.value * 0.025f)
        val highlightOverlayColor = if (highlightAnim.value > 0.005f) {
            if (isMine) Color.White.copy(alpha = highlightAnim.value * 0.22f)
            else VibePrimary.copy(alpha = highlightAnim.value * 0.20f)
        } else {
            Color.Transparent
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(rowHighlightBg)
                .fillMaxWidth()
                .padding(horizontal = if (isSelected) 6.dp else 2.dp, vertical = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(if (isMine) Alignment.CenterEnd else Alignment.CenterStart)
                    .widthIn(min = 180.dp, max = 290.dp),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        // Keep reply-swipe handling on the message body only. The old parent-level
                        // gesture detector consumed slight horizontal motion from inline button taps.
                        .pointerInput(message.id) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val released = swipeX.value
                                    if (released > 60f || released < -60f) onReply(message)
                                    swipeScope.launch { swipeX.animateTo(0f) }
                                },
                                onDragCancel = { swipeScope.launch { swipeX.animateTo(0f) } },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val target = (swipeX.value + dragAmount * 0.5f).coerceIn(-100f, 100f)
                                    swipeScope.launch { swipeX.snapTo(target) }
                                }
                            )
                        }
                        .zIndex(if (isTarget) 10f else 0f)
                        .graphicsLayer {
                            scaleX = scale * targetScale
                            scaleY = scale * targetScale
                            shadowElevation = targetElevation.toPx()
                            shape = bubbleShape
                            clip = false
                        }
                        .widthIn(max = 280.dp)
                        .clip(bubbleShape)
                        .background(actualBubbleBg)
                        .border(
                            width = 1.5.dp,
                            color = if (highlightAnim.value > 0.005f) VibePrimary.copy(alpha = highlightAnim.value * 0.85f) else Color.Transparent,
                            shape = bubbleShape
                        )
                        .drawWithContent {
                            drawContent()
                            if (highlightAnim.value > 0.005f) {
                                drawRect(highlightOverlayColor)
                            }
                        }
                        .onPlaced { bubbleCoords.value = it }
                        .combinedClickable(
                            onLongClick = {
                                if (!selectionMode) onSelect?.invoke()
                            },
                            onClick = {
                                if (selectionMode) {
                                    onSelect?.invoke()
                                } else {
                                    // Меню открывается у самого баббла, поэтому его границы
                                    // снимаются здесь: попап должен знать, от чего расти.
                                    val bounds = bubbleCoords.value?.boundsInWindow()
                                        ?: androidx.compose.ui.geometry.Rect.Zero
                                    menuAnchor.bounds = bounds
                                    menuAnchor.highlightBounds = bounds
                                    menuAnchor.cornerRadius = 18.dp
                                    keyboard?.hide()
                                    showMenu = true
                                }
                            }
                        )
                        .padding(horizontal = paddingX, vertical = paddingY)
                ) {
                    Column {
                        if (message.replyToId != null) {
                            val replyBg = if (isMine) Color.White.copy(alpha = 0.15f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            val barColor = if (isMine) Color.White else VibePrimary
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(replyBg)
                                    .clickable { onReplyClick(message.replyToId!!) }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(34.dp)
                                        .background(barColor, RoundedCornerShape(1.5.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val senderName = message.replyToSenderName
                                        ?: repliedMessage?.let { sourceMessage ->
                                            when {
                                                sourceMessage.senderId == myUserId ->
                                                    myDisplayName?.takeIf { it.isNotBlank() } ?: strings.you
                                                sourceMessage.senderType.equals("bot", ignoreCase = true) ->
                                                    partnerName?.takeIf { it.isNotBlank() } ?: strings.botLabel
                                                else -> partnerName?.takeIf { it.isNotBlank() }
                                                    ?: (strings.replyTo)
                                            }
                                        }
                                        ?: (strings.replyTo)

                                    Text(
                                        text = senderName,
                                        color = barColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (repliedMessage != null) {
                                        MessagePreviewBlock(
                                            message = repliedMessage,
                                            textColor = if (isMine) Color.White.copy(alpha = 0.95f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            isMine = isMine
                                        )
                                    } else if (message.replyToContent != null) {
                                        val repText = com.flasskdev.vibe.utils.MessageUtils.formatMessagePreview(message.replyToContent, null).replace("\n", " ")
                                        val repColor = if (isMine) Color.White.copy(alpha = 0.95f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                        if (com.flasskdev.vibe.utils.TextFormatting.hasFormatting(repText)) {
                                            FormattedText(
                                                text = repText,
                                                baseColor = repColor,
                                                fontSize = 13.sp,
                                                lineHeight = 16.sp,
                                                maxLines = 1,
                                                interactive = false,
                                                isMine = isMine
                                            )
                                        } else {
                                            Text(
                                                text = repText,
                                                color = repColor,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (message.forwardedFromId != null) {
                            val fwdId = message.forwardedFromId
                            val fwdName = message.forwardedFromName ?: strings.userLabel
                            val actualContent = message.content

                            val replyBg = if (isMine) Color.White.copy(alpha = 0.15f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            val barColor = if (isMine) Color.White else VibePrimary

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(replyBg)
                                    .clickable {
                                        if (fwdId == -1) {
                                            onShowCopyToast()
                                        } else {
                                            onProfileClick(fwdId, fwdName)
                                        }
                                    }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Forwarded",
                                    tint = barColor,
                                    modifier = Modifier.size(16.dp).scale(scaleX = -1f, scaleY = 1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.forwardedFrom(fwdName),
                                    color = barColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        val contentToRender = if (message.forwardedFromId != null) message.content else message.content
                        if (isSticker) {
                            com.flasskdev.vibe.ui.components.StickerMessage(
                                stickerId = com.flasskdev.vibe.ui.components.StickerRepository.idFromContent(message.content),
                                timeText = timeFormatted,
                                isMine = isMine,
                                isRead = message.isRead,
                                isPending = message.id < 0
                            )
                        } else if (isGif) {
                            com.flasskdev.vibe.ui.components.GifMessage(
                                url = message.attachments?.firstOrNull() ?: "",
                                meta = message.content,
                                timeText = timeFormatted,
                                isMine = isMine,
                                isRead = message.isRead,
                                isPending = message.id < 0,
                                onClick = { onImageClick(message, 0) }
                            )
                        } else if (isVideoMessage) {
                            // ПУНКТ 2. Раньше кружок рисовался прямоугольным
                            // VideoMessageBubble — то есть выглядел как обычное видео.
                            // CircleMessageBubble уже лежал в проекте, но не вызывался
                            // ниоткуда. Активный кружок ровно один: см. ActiveCircle.
                            val videoUrl = message.attachments?.firstOrNull() ?: ""
                            val ms = message.content.substringAfter("video_message:").toLongOrNull() ?: 0L
                            com.flasskdev.vibe.ui.circles.CircleMessageBubble(
                                videoUrl = videoUrl,
                                thumbUrl = videoUrl.takeIf { it.isNotBlank() },
                                durationMs = ms,
                                isMine = isMine,
                                isActive = com.flasskdev.vibe.ui.circles.ActiveCircle.activeMessageId == message.id,
                                onActivate = { com.flasskdev.vibe.ui.circles.ActiveCircle.toggle(message.id) },
                                uploadStatus = message.uploadStatus,
                                uploadProgress = message.uploadProgress,
                                onRetryUpload = { onRetryUpload(message.id) }
                            )
                        } else if (isVoiceMessage) {
                            VoiceBubbleHost(
                                message = message,
                                isMine = isMine,
                                myAvatarUrl = myAvatarUrl,
                                partnerAvatarUrl = partnerAvatarUrl
                            )
                        } else {
                            if (mediaAttachments.isNotEmpty()) {
                                MessageAttachmentsGrid(attachments = mediaAttachments, onImageClick = { idx -> onImageClick(message, idx) })
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (audios.isNotEmpty()) {
                                MessageAudioList(
                                    audios = audios,
                                    message = message,
                                    audioPlayer = audioPlayer,
                                    isMine = isMine,
                                    bubbleBg = bubbleBg,
                                    myAvatarUrl = myAvatarUrl,
                                    partnerAvatarUrl = partnerAvatarUrl,
                                    chatPlaylist = chatPlaylist
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (files.isNotEmpty()) {
                                MessageFilesList(files = files, context = context, isMine = isMine, bubbleBg = bubbleBg)
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            val innerModifier = if (isOnlyImagesAndText) {
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bubbleBg)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            } else {
                                Modifier
                            }

                            Box(modifier = innerModifier) {
                                Column {
                                    val plainContent = if (message.content.startsWith("duration:") || message.content.startsWith("video_message:")) "" else contentToRender
                                    if (plainContent.isNotEmpty()) {
                                        if (TextFormatting.hasFormatting(plainContent)) {
                                            FormattedText(
                                                text = plainContent,
                                                baseColor = if (isMine || isOnlyImagesAndText) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                                fontSize = 15.sp,
                                                lineHeight = 20.sp,
                                                onMentionClick = { username ->
                                                    // Could navigate to user profile by username
                                                },
                                                onProfileClick = onProfileClick,
                                                isMine = isMine
                                            )
                                        } else {
                                            Text(
                                                text = plainContent,
                                                color = if (isMine || isOnlyImagesAndText) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                                fontSize = 15.sp,
                                                lineHeight = 20.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = timeFormatted + if (message.isEdited) strings.editedLabel else "",
                                            color = if (isMine || isOnlyImagesAndText) Color.White.copy(alpha = 0.7f) else androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                            fontSize = 11.sp
                                        )
                                        if (isPinned) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Filled.PushPin,
                                                contentDescription = "Pinned",
                                                tint = if (isMine || isOnlyImagesAndText) Color.White.copy(alpha = 0.8f) else androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        if (isMine) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            if (message.uploadStatus == "UPLOADING") {
                                                Text(
                                                    text = "${message.uploadProgress ?: 0}%",
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    progress = { (message.uploadProgress ?: 0) / 100f },
                                                    modifier = Modifier.size(12.dp),
                                                    color = Color.White,
                                                    trackColor = Color.White.copy(alpha = 0.3f),
                                                    strokeWidth = 2.dp
                                                )
                                            } else if (message.uploadStatus == "FAILED") {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Retry",
                                                    tint = VibeError,
                                                    modifier = Modifier.size(14.dp).clickable { onRetryUpload(message.id) }
                                                )
                                            } else if (message.id < 0) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = "Pending",
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Check,
                                                    contentDescription = "Read status",
                                                    tint = if (message.isRead) Color(0xFF81D4FA) else Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!message.reactions.isNullOrEmpty()) {
                            MessageReactionsRow(
                                reactions = message.reactions!!,
                                myUserId = myUserId,
                                isMine = isMine,
                                onReactionClick = { emoji -> onReactionToggle(message, emoji) },
                                onReactionLongClick = { emoji -> onReactionLongClick(message, emoji) }
                            )
                        }
                    }
                }

                if (message.replyMarkup != null && message.replyMarkup.inlineKeyboard.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    com.flasskdev.vibe.ui.components.InlineKeyboard(
                        replyMarkup = message.replyMarkup,
                        onButtonClick = { btn -> onInlineButtonClick(message, btn) },
                        isInteractionEnabled = inlineKeyboardEnabled,
                        pendingCallbackData = pendingInlineCallbackData,
                        modifier = Modifier.fillMaxWidth()

                    )
                }
            }
        }
    }

// ========== ПУНКТ 7: КОНТЕКСТНОЕ МЕНЮ СООБЩЕНИЯ ==========
// Раньше здесь был ModalBottomSheet: меню приезжало снизу через полэкрана,
// перекрывало само сообщение, к которому относится, и выглядело как отдельная
// подсистема на фоне меню в списке чатов. Теперь то же якорное стекло, что и там,
// плюс быстрые реакции в шапке.
    val menuActions = buildList {
        add(
            VibeMenuAction(
                label = strings.replyTo,
                icon = Icons.AutoMirrored.Filled.Reply,
                onClick = {
                    showMenu = false
                    onReply(message)
                }
            )
        )

        val hasTextContent = !isVoiceMessage && !isVideoMessage &&
                !isSticker && !isGif &&
                message.content.isNotBlank()

        if (hasTextContent) {
            add(
                VibeMenuAction(
                    label = strings.actionCopy,
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        showMenu = false
                        val plainText = com.flasskdev.vibe.utils.TextFormatting.stripFormatting(message.content)
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("message", plainText))
                        onShowCopyToast()
                    }
                )
            )
        }

        // Правка доступна сутки и только для своего текста: серверный лимит совпадает.
        val canEdit = isMine && hasTextContent &&
                (System.currentTimeMillis() - message.timestamp <= 24 * 60 * 60 * 1000)
        if (canEdit) {
            add(
                VibeMenuAction(
                    label = strings.edit,
                    icon = Icons.Default.Edit,
                    onClick = {
                        showMenu = false
                        onEditClick(message)
                    }
                )
            )
        }

        add(
            VibeMenuAction(
                label = strings.actionForward,
                icon = Icons.AutoMirrored.Filled.Send,
                startsGroup = true,
                onClick = {
                    showMenu = false
                    onForwardRequest(message)
                }
            )
        )

        // Пункт 8: выбрать сообщение можно было только длинным нажатием, о котором
        // в интерфейсе ничего не говорило. Теперь это обычный пункт меню.
        if (onSelect != null) {
            add(
                VibeMenuAction(
                    label = strings.actionSelectMessage,
                    icon = Icons.Outlined.CheckCircle,
                    onClick = {
                        showMenu = false
                        onSelect.invoke()
                    }
                )
            )
        }

        add(
            VibeMenuAction(
                label = if (isPinned) strings.unpinMessage else strings.pinMessage,
                icon = Icons.Default.PushPin,
                rotateIcon = isPinned,
                selected = isPinned,
                onClick = {
                    showMenu = false
                    if (isPinned) onUnpinRequest(message) else onPinRequest(message)
                }
            )
        )

        if (!message.attachments.isNullOrEmpty()) {
            val attachmentCount = message.attachments!!.size
            add(
                VibeMenuAction(
                    label = if (attachmentCount > 1) strings.actionDownloadSelected else strings.actionDownload,
                    icon = Icons.Outlined.Download,
                    onClick = {
                        showMenu = false
                        val filesToDownload = message.attachments!!.map { att ->
                            com.flasskdev.vibe.utils.DownloadHelper.resolveUrl(att) to
                                    com.flasskdev.vibe.utils.AttachmentUtils.getFilename(att)
                        }
                        if (filesToDownload.size == 1) {
                            com.flasskdev.vibe.utils.DownloadHelper.downloadFile(
                                context, filesToDownload[0].first, filesToDownload[0].second
                            )
                        } else {
                            com.flasskdev.vibe.utils.DownloadHelper.downloadFiles(context, filesToDownload)
                        }
                    }
                )
            )
        }

        // Пункт 8: удалить одно сообщение было нельзя вообще - только через
        // мультивыделение в хедере.
        add(
            VibeMenuAction(
                label = strings.deleteBtn,
                icon = Icons.Default.Delete,
                destructive = true,
                startsGroup = true,
                onClick = {
                    showMenu = false
                    onDeleteRequest(message)
                }
            )
        )

        if (!isMine) {
            add(
                VibeMenuAction(
                    label = strings.actionReport,
                    icon = Icons.Outlined.Warning,
                    destructive = true,
                    onClick = {
                        showMenu = false
                        onReportClick(message)
                    }
                )
            )
        }
    }

    VibeContextMenu(
        expanded = showMenu,
        anchor = menuAnchor,
        onDismiss = { showMenu = false },
        actions = menuActions,
        header = {
            QuickReactionsBar(
                currentReactions = message.reactions ?: emptyList(),
                myUserId = myUserId,
                onSelectEmoji = { emoji ->
                    showMenu = false
                    onReactionToggle(message, emoji)
                }
            )
        }
    )
}

@Composable
fun DateSeparator(
    modifier: Modifier = Modifier,
    dateMillis: Long,
    strings: com.flasskdev.vibe.ui.theme.VibeStrings
) {
    val dateText = remember(dateMillis) {
        val now = Calendar.getInstance()
        val msgDate = Calendar.getInstance().apply { timeInMillis = dateMillis }

        when {
            now.get(Calendar.YEAR) == msgDate.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == msgDate.get(Calendar.DAY_OF_YEAR) -> strings.dateToday

            now.get(Calendar.YEAR) == msgDate.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) - msgDate.get(Calendar.DAY_OF_YEAR) == 1 -> strings.dateYesterday

            else -> {
                val pattern = if (now.get(Calendar.YEAR) == msgDate.get(Calendar.YEAR)) {
                    "d MMMM"
                } else {
                    "d MMMM yyyy"
                }
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.format(Date(dateMillis))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 0.dp
        ) {
            Text(
                text = dateText,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}



internal fun formatLastSeen(lastSeenTimestamp: Long?, strings: com.flasskdev.vibe.ui.theme.VibeStrings): String {
    if (lastSeenTimestamp == null) return strings.lastSeenRecently

    val now = Calendar.getInstance()
    val lastSeen = Calendar.getInstance().apply { timeInMillis = lastSeenTimestamp }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(lastSeenTimestamp))

    return when {
        // Сегодня
        now.get(Calendar.YEAR) == lastSeen.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == lastSeen.get(Calendar.DAY_OF_YEAR) -> {
            strings.lastSeenToday(timeStr)
        }
        // Вчера
        now.get(Calendar.YEAR) == lastSeen.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - lastSeen.get(Calendar.DAY_OF_YEAR) == 1 -> {
            strings.lastSeenYesterday(timeStr)
        }
        // Больше года назад
        now.get(Calendar.YEAR) > lastSeen.get(Calendar.YEAR) -> {
            strings.lastSeenLongAgo
        }
        // В остальных случаях (позавчера и далее в пределах года)
        else -> {
            val dateFormat = SimpleDateFormat("d MMMM", Locale.getDefault())
            strings.lastSeenDate(dateFormat.format(Date(lastSeenTimestamp)), timeStr)
        }
    }
}

@Composable
fun MessageAttachmentsGrid(attachments: List<String>, onImageClick: (Int) -> Unit) {
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val itemsPerRow = if (attachments.size == 1) 1 else if (attachments.size in 2..4) 2 else 3
    val spacing = 4.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    ) {
        val rows = attachments.chunked(itemsPerRow)
        var globalIndex = 0
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { attachmentPath ->
                    val currentIndex = globalIndex++
                    val isLocal = attachmentPath.startsWith("/") || attachmentPath.startsWith("content://") || attachmentPath.contains("cacheDir")
                    val model = if (isLocal) {
                        java.io.File(attachmentPath)
                    } else if (attachmentPath.startsWith("http")) {
                        attachmentPath
                    } else {
                        "https://flasskdev.alwaysdata.net/api/upload/file/$attachmentPath"
                    }
                    val isVideo = com.flasskdev.vibe.utils.AttachmentUtils.isPlayableVideo(attachmentPath)
                    val context = androidx.compose.ui.platform.LocalContext.current
                    // PERF: ImageRequest пересобирался на каждой рекомпозиции сетки.
                    val imageRequest = remember(model, context) {
                        coil.request.ImageRequest.Builder(context)
                            .data(model)
                            .crossfade(true)
                            .build()
                    }

                    Box(modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isVideo) androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .clickable { onImageClick(currentIndex) }
                    ) {
                        if (isVideo) {
                            com.flasskdev.vibe.ui.components.VideoCover(
                                source = model,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = strings.attachmentLabel,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                    }
                }
                // Fill empty slots in row
                val emptySlots = itemsPerRow - rowItems.size
                for (i in 0 until emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MessageAudioList(
    audios: List<String>,
    message: MessageEntity,
    audioPlayer: com.flasskdev.vibe.ui.viewmodels.GlobalAudioPlayerViewModel,
    isMine: Boolean,
    bubbleBg: Color,
    myAvatarUrl: String?,
    partnerAvatarUrl: String?,
    chatPlaylist: ChatAudioPlaylist = ChatAudioPlaylist.Empty
) {
    // PERF: состояние плеера читается здесь, а не в каждом MessageBubble.
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val currentPlayingTrack by audioPlayer.currentTrack.collectAsState()
    val isPlayingAudio by audioPlayer.isPlaying.collectAsState()
    val audioProgress by audioPlayer.progress.collectAsState()
    val audioCurrentPos by audioPlayer.currentPosition.collectAsState()
    val audioDuration by audioPlayer.duration.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        audios.forEach { attachmentPath ->
            val isLocal = attachmentPath.startsWith("/") || attachmentPath.startsWith("content://") || attachmentPath.contains("cacheDir")
            val audioUrl = if (isLocal) attachmentPath else if (attachmentPath.startsWith("http")) attachmentPath else "https://flasskdev.alwaysdata.net/api/upload/file/$attachmentPath"
            val trackId = "${message.id}_${attachmentPath.hashCode()}"

            val initialMeta = androidx.compose.runtime.remember(audioUrl) {
                com.flasskdev.vibe.utils.AudioMetadataHelper.getCachedMetadata(audioUrl)
            }
            var audioMeta by androidx.compose.runtime.remember(audioUrl) {
                androidx.compose.runtime.mutableStateOf(initialMeta)
            }
            androidx.compose.runtime.LaunchedEffect(audioUrl) {
                // PERF: LaunchedEffect стартует на MAIN. MediaMetadataRetriever по сети
                // блокировал UI-поток на секунды при входе в чат с аудио.
                val meta = withContext(Dispatchers.IO) {
                    com.flasskdev.vibe.utils.AudioMetadataHelper.getMetadata(audioUrl)
                }
                audioMeta = meta
                val tTitle = if (meta.displayArtist.isNotBlank()) "${meta.displayArtist} — ${meta.displayTitle}" else meta.displayTitle
                audioPlayer.updateTrackTitle(trackId, tTitle)
            }

            val displayTitle = audioMeta?.displayTitle ?: strings.typeAudio
            val displayArtist = audioMeta?.displayArtist?.takeIf { it.isNotBlank() } ?: ""
            val displayDuration = audioMeta?.durationMs ?: 0L

            val isThisPlaying = currentPlayingTrack?.id == trackId
            val textColor = if (isMine) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            val subColor = textColor.copy(alpha = 0.55f)

            // Play/Pause row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isThisPlaying) {
                            if (isPlayingAudio) audioPlayer.pause() else audioPlayer.resume()
                        } else {
                            val trackTitle = if (displayArtist.isNotBlank()) "$displayArtist — $displayTitle" else displayTitle
                            val currentTrackInfo = com.flasskdev.vibe.ui.viewmodels.AudioTrackInfo(
                                id = trackId,
                                url = audioUrl,
                                title = trackTitle,
                                avatarUrl = if (isMine) myAvatarUrl else partnerAvatarUrl
                            )
                            val finalPlaylist = if (chatPlaylist.tracks.isNotEmpty()) {
                                chatPlaylist.tracks.map { if (it.id == trackId) currentTrackInfo else it }
                            } else {
                                listOf(currentTrackInfo)
                            }
                            audioPlayer.playAudio(currentTrackInfo, finalPlaylist)
                        }
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art / Play icon
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val coverArtBytes = audioMeta?.coverArt
                    // PERF: BitmapFactory.decodeByteArray выполнялся внутри remember, то есть
                    // прямо в композиции на UI-потоке, для каждого аудио-сообщения в чате.
                    // Теперь декод уходит в фон, а до его завершения рисуется обычная плашка.
                    var cover by androidx.compose.runtime.remember(coverArtBytes) {
                        androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null)
                    }
                    androidx.compose.runtime.LaunchedEffect(coverArtBytes) {
                        cover = if (coverArtBytes == null) null else withContext(Dispatchers.Default) {
                            runCatching {
                                android.graphics.BitmapFactory.decodeByteArray(
                                    coverArtBytes, 0, coverArtBytes.size
                                )
                            }.getOrNull()
                        }
                    }

                    val decoded = cover
                    if (decoded != null) {
                        androidx.compose.foundation.Image(
                            bitmap = decoded.asImageBitmap(),
                            contentDescription = "Cover",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                        )
                        // Overlay play/pause
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying && isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(VibePrimary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying && isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    if (isThisPlaying && isPlayingAudio) {
                        // Progress bar while playing
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(subColor.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = audioProgress.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(VibePrimary)
                            )
                        }
                    } else {
                        Text(
                            text = displayArtist,
                            color = subColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    // Timer
                    val posStr = if (isThisPlaying) {
                        val sec = audioCurrentPos / 1000
                        String.format("%d:%02d", sec / 60, sec % 60)
                    } else "0:00"

                    val durToUse = if (audioDuration > 0) audioDuration else displayDuration
                    val durStr = if (durToUse > 0) {
                        val sec = durToUse / 1000
                        String.format("%d:%02d", sec / 60, sec % 60)
                    } else {
                        "--:--"
                    }
                    Text(
                        text = if (isThisPlaying) "$posStr / $durStr" else durStr,
                        color = subColor,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MessageFilesList(files: List<String>, context: android.content.Context, isMine: Boolean, bubbleBg: androidx.compose.ui.graphics.Color) {
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val textColor = if (isMine) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onBackground
    val subColor = textColor.copy(alpha = 0.5f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        files.forEach { attachmentPath ->
            val isLocal = attachmentPath.startsWith("/") || attachmentPath.startsWith("content://") || attachmentPath.contains("cacheDir")
            val fullFilename = if (isLocal) java.io.File(attachmentPath).name else attachmentPath.substringAfterLast("/")
            val nameWithoutExt = fullFilename.substringBeforeLast(".")
            val extension = "." + fullFilename.substringAfterLast(".", "")
            val downloadUrl = if (isLocal) attachmentPath else if (attachmentPath.startsWith("http")) attachmentPath else "https://flasskdev.alwaysdata.net/api/upload/file/$attachmentPath"

            var fileSizeText by androidx.compose.runtime.remember(downloadUrl) { androidx.compose.runtime.mutableStateOf(strings.fileSizeLoading) }
            androidx.compose.runtime.LaunchedEffect(downloadUrl) {
                fileSizeText = withContext(Dispatchers.IO) {
                    com.flasskdev.vibe.utils.AttachmentUtils.getFileSizeAsync(downloadUrl)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isMine) Color.White.copy(alpha = 0.12f) else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File icon
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(VibePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nameWithoutExt.ifBlank { "File" },
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = extension,
                            color = subColor,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier.size(3.dp).background(subColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = fileSizeText,
                            color = subColor,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Native download button
                IconButton(onClick = {
                    com.flasskdev.vibe.utils.DownloadHelper.downloadFile(context, downloadUrl, fullFilename)
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = VibePrimary)
                }
            }
        }
    }
}


@Composable
fun MessagePreviewBlock(
    message: com.flasskdev.vibe.data.local.MessageEntity,
    textColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    isMine: Boolean = false
) {
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val rawPreview = com.flasskdev.vibe.utils.MessageUtils.formatMessagePreview(message.content, message.attachments)
    val isAudio = message.attachments?.any { com.flasskdev.vibe.utils.AttachmentUtils.isPlayableAudio(it) } == true
    val isVideo = message.attachments?.any { com.flasskdev.vibe.utils.AttachmentUtils.isPlayableVideo(it) } == true
    val isImage = message.attachments?.any { com.flasskdev.vibe.utils.AttachmentUtils.isImage(it) } == true
    val isFile = message.attachments?.any { !com.flasskdev.vibe.utils.AttachmentUtils.isImage(it) && !com.flasskdev.vibe.utils.AttachmentUtils.isPlayableVideo(it) && !com.flasskdev.vibe.utils.AttachmentUtils.isPlayableAudio(it) } == true
    val isVoice = message.content.startsWith("duration:")
    val isVideoMsg = message.content.startsWith("video_message:")

    val icon = when {
        isVideoMsg -> androidx.compose.material.icons.Icons.Default.Videocam
        isVoice -> androidx.compose.material.icons.Icons.Default.Mic
        isImage -> androidx.compose.material.icons.Icons.Default.Image
        isVideo -> androidx.compose.material.icons.Icons.Default.Videocam
        isAudio -> androidx.compose.material.icons.Icons.Default.MusicNote
        isFile -> androidx.compose.material.icons.Icons.Default.InsertDriveFile
        else -> null
    }

    var dynamicAudioTitle by androidx.compose.runtime.remember(message.attachments) { androidx.compose.runtime.mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(message.attachments) {
        if (isAudio && !message.attachments.isNullOrEmpty()) {
            val firstAtt = message.attachments.first { com.flasskdev.vibe.utils.AttachmentUtils.isPlayableAudio(it) }
            val isLocal = firstAtt.startsWith("/") || firstAtt.startsWith("content://") || firstAtt.contains("cacheDir")
            val url = if (isLocal) firstAtt else if (firstAtt.startsWith("http")) firstAtt else "https://flasskdev.alwaysdata.net/api/upload/file/$firstAtt"
            val meta = withContext(Dispatchers.IO) {
                com.flasskdev.vibe.utils.AudioMetadataHelper.getMetadata(url)
            }
            dynamicAudioTitle = if (meta.displayArtist != "Unknown Artist") "${meta.displayArtist} — ${meta.displayTitle}" else meta.displayTitle
        }
    }

    val cleanText = rawPreview.removePrefix("\uD83D\uDCF9 ").removePrefix("\uD83C\uDFA4 ").removePrefix("\uD83D\uDDBC ").removePrefix("\uD83C\uDFAC ").removePrefix("\uD83C\uDFB5 ").removePrefix("\uD83D\uDCCE ").removePrefix("+")

    val displayText = when {
        isAudio && (message.content.isBlank() || message.content.startsWith("Музыка")) -> dynamicAudioTitle ?: strings.previewAudioLoading
        else -> cleanText.replace("\n", " ")
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VibePrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (com.flasskdev.vibe.utils.TextFormatting.hasFormatting(displayText)) {
            FormattedText(
                text = displayText,
                baseColor = textColor,
                fontSize = fontSize,
                lineHeight = (fontSize.value + 4).sp,
                maxLines = 1,
                interactive = false,
                isMine = isMine
            )
        } else {
            Text(
                text = displayText,
                color = textColor,
                fontSize = fontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
fun QuickReactionsBar(
    currentReactions: List<com.flasskdev.vibe.data.local.ReactionItem>,
    myUserId: Int,
    onSelectEmoji: (String) -> Unit
) {
    val emojis = remember {
        listOf(
            "❤️", "👍", "👎", "🔥", "😂", "🥰", "😮", "😢", "😭", "🙏",
            "👏", "🎉", "🤩", "🤔", "🤬", "🤯", "💩", "👀", "💯", "😍",
            "🥳", "😱", "🤝", "😴", "😎", "🫡", "💔", "⚡", "✨", "🕊️",
            "🍓", "🍾", "🏆", "😇", "🤡", "👾"
        )
    }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val myReactionEmoji = remember(currentReactions, myUserId) {
        currentReactions.find { it.userIds.contains(myUserId) }?.emoji
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEach { emoji ->
            val isSelected = emoji == myReactionEmoji
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.22f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "emoji_scale"
            )

            Box(
                modifier = Modifier
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (isSelected) VibePrimary.copy(alpha = 0.22f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) VibePrimary else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onSelectEmoji(emoji)
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageReactionsRow(
    reactions: List<com.flasskdev.vibe.data.local.ReactionItem>,
    myUserId: Int,
    isMine: Boolean,
    onReactionClick: (String) -> Unit,
    onReactionLongClick: (String) -> Unit = {}
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        reactions.forEach { reaction ->
            val isSelected = reaction.userIds.contains(myUserId)
            val chipBg = if (isSelected) {
                if (isMine) Color.White.copy(alpha = 0.28f) else VibePrimary.copy(alpha = 0.18f)
            } else {
                if (isMine) Color.White.copy(alpha = 0.14f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            }

            val borderColor = if (isSelected) {
                if (isMine) Color.White.copy(alpha = 0.6f) else VibePrimary.copy(alpha = 0.5f)
            } else Color.Transparent

            val textColor = if (isMine) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onReactionClick(reaction.emoji)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onReactionLongClick(reaction.emoji)
                        }
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = reaction.emoji, fontSize = 13.sp)
                if (reaction.count > 1) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${reaction.count}",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReactionDetailsBottomSheet(
    sheetData: com.flasskdev.vibe.ui.viewmodels.ReactionSheetData,
    onSelectEmoji: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    onProfileClick: (Int, String) -> Unit
) {
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && sheetData.hasMore && !sheetData.isLoading) {
            onLoadMore()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalSheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.reactionsTitle,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.actionClose,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Emoji Filter Tabs
            val allReactions = sheetData.message.reactions ?: emptyList()
            val totalCount = allReactions.sumOf { it.count }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSelected = sheetData.selectedEmoji == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isAllSelected) VibePrimary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { onSelectEmoji(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = strings.reactionsAllTab(totalCount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAllSelected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                }

                allReactions.forEach { r ->
                    val isSelected = sheetData.selectedEmoji == r.emoji
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) VibePrimary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { onSelectEmoji(r.emoji) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = r.emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${r.count}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Stable height container to prevent bottom sheet jumping
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (sheetData.users.isEmpty() && sheetData.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = VibePrimary,
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (sheetData.users.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.reactionsEmpty,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sheetData.users, key = { "${it.userId}_${it.emoji}" }) { user ->
                            ReactionUserItem(
                                user = user,
                                onProfileClick = onProfileClick
                            )
                        }

                        if (sheetData.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = VibePrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReactionUserItem(
    user: com.flasskdev.vibe.data.ReactionUserDetail,
    onProfileClick: (Int, String) -> Unit
) {
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val timeFormatted = remember(user.timestamp) {
        if (user.timestamp <= 0L) "" else {
            val sdf = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
            sdf.format(Date(user.timestamp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick(user.userId, user.username ?: "") }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(VibePrimary),
            contentAlignment = Alignment.Center
        ) {
            if (!user.avatarUrl.isNullOrEmpty()) {
                coil.compose.AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initial = (user.name?.take(1) ?: user.username?.take(1) ?: "?").uppercase()
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and username
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.name ?: user.username ?: strings.userLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Verified",
                        tint = VibePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (!user.username.isNullOrEmpty()) {
                Text(
                    text = "@${user.username}",
                    fontSize = 13.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right side: Emoji + Date
        Column(horizontalAlignment = Alignment.End) {
            Text(text = user.emoji, fontSize = 20.sp)
            if (timeFormatted.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}

/**
 * Стабильный контейнер плейлиста чата. Нужен именно data class: Compose сравнивает
 * параметры через equals, а у голого List<T> в качестве типа параметра стабильности нет.
 */
@androidx.compose.runtime.Immutable
data class ChatAudioPlaylist(
    val tracks: List<com.flasskdev.vibe.ui.viewmodels.AudioTrackInfo>
) {
    companion object {
        val Empty = ChatAudioPlaylist(emptyList())
    }
}

/** contentType для LazyColumn: бабблы одного вида переиспользуют слоты друг друга. */
fun messageContentType(message: com.flasskdev.vibe.data.local.MessageEntity): String = when {
    message.content.startsWith("\$\$SYSTEM\$\$") -> "system"
    message.content.startsWith("duration:") -> "voice"
    message.content.startsWith("video_message:") -> "videoNote"
    message.content.startsWith("sticker:") -> "sticker"
    message.content.startsWith("gif:") -> "gif"
    message.attachments.isNullOrEmpty() -> "text"
    else -> "attachments"
}

fun buildChatAudioPlaylist(
    messages: List<com.flasskdev.vibe.data.local.MessageEntity>,
    myAvatarUrl: String?,
    partnerAvatarUrl: String?,
    myUserId: Int,
    audioFallbackTitle: String
): List<com.flasskdev.vibe.ui.viewmodels.AudioTrackInfo> {
    val list = mutableListOf<com.flasskdev.vibe.ui.viewmodels.AudioTrackInfo>()
    // Oldest to newest
    messages.sortedBy { it.timestamp }.forEach { msg ->
        val isVoice = msg.content.startsWith("duration:")
        if (!isVoice) {
            val atts = msg.attachments ?: emptyList()
            val audios = atts.filter {
                com.flasskdev.vibe.utils.AttachmentUtils.getType(it, msg.content) == com.flasskdev.vibe.utils.AttachmentType.AUDIO
            }
            audios.forEach { attPath ->
                val isLocal = attPath.startsWith("/") || attPath.startsWith("content://") || attPath.contains("cacheDir")
                val audioUrl = if (isLocal) attPath else if (attPath.startsWith("http")) attPath else "https://flasskdev.alwaysdata.net/api/upload/file/$attPath"
                val meta = com.flasskdev.vibe.utils.AudioMetadataHelper.getCachedMetadata(audioUrl)
                val trackId = "${msg.id}_${attPath.hashCode()}"
                val trackTitle = if (meta != null && meta.displayArtist.isNotBlank()) {
                    "${meta.displayArtist} — ${meta.displayTitle}"
                } else meta?.displayTitle ?: audioFallbackTitle

                list.add(
                    com.flasskdev.vibe.ui.viewmodels.AudioTrackInfo(
                        id = trackId,
                        url = audioUrl,
                        title = trackTitle,
                        avatarUrl = if (msg.senderId == myUserId) myAvatarUrl else partnerAvatarUrl
                    )
                )
            }
        }
    }
    return list
}




/** Разобранные вложения сообщения. Считается один раз на сообщение (см. remember в MessageBubble). */
@androidx.compose.runtime.Immutable
data class AttachmentBuckets(
    val images: List<String>,
    val videos: List<String>,
    val audios: List<String>,
    val files: List<String>
) {
    val mediaAttachments: List<String> = images + videos
}

/**
 * Голосовое сообщение вынесено в отдельный composable, чтобы высокочастотные обновления
 * плеера (progress / currentPosition) рекомпозили только сам голосовой баббл,
 * а не каждое видимое сообщение в списке.
 */
@Composable
private fun VoiceBubbleHost(
    message: MessageEntity,
    isMine: Boolean,
    myAvatarUrl: String?,
    partnerAvatarUrl: String?
) {
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val audioPlayer = LocalGlobalAudioPlayer.current
    val currentPlayingTrack by audioPlayer.currentTrack.collectAsState()
    val isPlayingAudio by audioPlayer.isPlaying.collectAsState()
    val audioProgress by audioPlayer.progress.collectAsState()
    val audioCurrentPos by audioPlayer.currentPosition.collectAsState()

    val rawAudio = message.attachments?.firstOrNull().orEmpty()
    val audioUrl = when {
        rawAudio.isBlank() -> ""
        rawAudio.startsWith("http") || rawAudio.startsWith("content://") || rawAudio.startsWith("/") -> rawAudio
        else -> "https://flasskdev.alwaysdata.net/api/upload/file/$rawAudio"
    }
    val isThisPlaying = currentPlayingTrack?.id == message.id.toString()
    val voiceFallbackLabel = strings.typeVoice
    val durationString = remember(message.content, voiceFallbackLabel) {
        if (message.content.startsWith("duration:")) {
            val ms = message.content.substringAfter("duration:").toLongOrNull() ?: 0L
            val totalSec = ms / 1000
            String.format("%d:%02d", totalSec / 60, totalSec % 60)
        } else {
            voiceFallbackLabel
        }
    }
    val formattedPos = if (isThisPlaying && isPlayingAudio) {
        val sec = audioCurrentPos / 1000
        String.format("%d:%02d", sec / 60, sec % 60)
    } else {
        durationString
    }

    VoiceMessageBubble(
        isPlaying = isThisPlaying && isPlayingAudio,
        progress = if (isThisPlaying) audioProgress else 0f,
        isMine = isMine,
        messageId = message.id,
        durationFormatted = formattedPos,
        onPlayClick = {
            if (audioUrl.isNotBlank()) {
                audioPlayer.playAudio(
                    AudioTrackInfo(
                        id = message.id.toString(),
                        url = audioUrl,
                        title = if (isMine) strings.voiceTrackTitleMine else strings.typeVoice,
                        avatarUrl = if (isMine) myAvatarUrl else partnerAvatarUrl
                    )
                )
            }
        }
    )
}
