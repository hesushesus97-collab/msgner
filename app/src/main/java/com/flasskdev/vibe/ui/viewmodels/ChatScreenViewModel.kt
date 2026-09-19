package com.flasskdev.vibe.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flasskdev.vibe.data.*
import com.flasskdev.vibe.data.local.AppDatabase
import com.flasskdev.vibe.data.local.MessageEntity
import com.flasskdev.vibe.data.local.UserCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Immutable

/**
 * Единый снапшот списка сообщений для UI.
 *
 * PERF: раньше messages и groupedMessages были двумя независимыми StateFlow с разным
 * таймингом (groupedMessages ехал через flowOn(Default)). Каждое изменение сообщений
 * давало два прохода рекомпозиции ChatScreen, плюс существовал кадр, в котором
 * messages уже непустой, а grouped ещё пустой: LazyColumn собирался с нуля дважды.
 * Карты byId и lazyIndex вдобавок строились на main thread прямо в композиции.
 * Теперь всё считается один раз в Dispatchers.Default и приезжает одной эмиссией.
 */
@Immutable
data class ChatListSnapshot(
    val messages: List<MessageEntity>,
    val grouped: Map<Long, List<MessageEntity>>,
    val byId: Map<Int, MessageEntity>,
    /** id сообщения -> его позиция в LazyColumn (с учётом разделителей дат). */
    val lazyIndex: Map<Int, Int>
) {
    companion object {
        val Empty = ChatListSnapshot(emptyList(), emptyMap(), emptyMap(), emptyMap())
    }
}

private fun buildChatListSnapshot(list: List<MessageEntity>): ChatListSnapshot {
    if (list.isEmpty()) return ChatListSnapshot.Empty

    val tzOffset = java.util.TimeZone.getDefault().rawOffset.toLong()
    val dayMillis = 86_400_000L
    val grouped = list.asReversed().groupBy { message ->
        ((message.timestamp + tzOffset) / dayMillis) * dayMillis - tzOffset
    }

    val byId = HashMap<Int, MessageEntity>(list.size)
    for (message in list) byId[message.id] = message

    val lazyIndex = HashMap<Int, Int>(list.size)
    var index = 0
    for ((_, messagesInDay) in grouped) {
        for (message in messagesInDay) {
            lazyIndex[message.id] = index
            index++
        }
        index++ // разделитель даты
    }

    return ChatListSnapshot(
        messages = list,
        grouped = grouped,
        byId = byId,
        lazyIndex = lazyIndex
    )
}

data class ReactionSheetData(
    val message: MessageEntity,
    val selectedEmoji: String? = null,
    val users: List<ReactionUserDetail> = emptyList(),
    val offset: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false
)

/** One in-flight callback at a time prevents duplicate bot actions from the chat UI. */
data class PendingInlineCallback(
    val messageId: Int,
    val callbackData: String,
    val buttonText: String,
    val expiresAtMillis: Long,
    val callbackId: Int? = null
)

/** Keeps short-lived callback waits while a chat destination is recreated during navigation. */
private object PendingInlineCallbackStore {
    private val pendingByChat = mutableMapOf<Int, MutableMap<Int, PendingInlineCallback>>()

    fun put(chatId: Int, pending: PendingInlineCallback) = synchronized(this) {
        pendingByChat.getOrPut(chatId) { mutableMapOf() }[pending.messageId] = pending
    }

    fun remove(chatId: Int, messageId: Int) = synchronized(this) {
        pendingByChat[chatId]?.remove(messageId)
        if (pendingByChat[chatId].isNullOrEmpty()) pendingByChat.remove(chatId)
    }

    fun activeForChat(chatId: Int, now: Long): Map<Int, PendingInlineCallback> = synchronized(this) {
        val active = pendingByChat[chatId]
            ?.filterValues { it.expiresAtMillis > now }
            .orEmpty()
        if (active.isEmpty()) pendingByChat.remove(chatId)
        else pendingByChat[chatId] = active.toMutableMap()
        active
    }
}

class ChatScreenViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /** Размер окна сообщений. Первый экран должен быть маленьким: чем меньше бабблов
         *  композится при входе, тем короче фриз на старте. */
        // Совпадает с серверной страницей. Раньше здесь было 20 при 50 на сервере: localCount
        // всегда оказывался больше нового лимита, из-за чего окно раздувалось без запросов.
        private const val MESSAGES_PAGE_SIZE = 50

        /**
         * PERF: сколько сообщений уходит в UI на ПЕРВЫЙ кадр.
         *
         * Первая композиция чата — самая дорогая в процессе: под каждый баббл грузятся
         * и JIT-компилируются классы, парсится форматирование текста, считается раскладка.
         * 50 бабблов на первом кадре на холодном процессе и дают тот самый фриз, из-за
         * которого клавиатура не может выехать. 15 хватает, чтобы заполнить экран; окно
         * расширяется до полной страницы сразу после первого кадра.
         */
        private const val FIRST_PAGE_SIZE = 15

        /** Пауза перед расширением окна до полной страницы — один кадр с запасом. */
        private const val FIRST_PAGE_EXPAND_DELAY_MS = 120L

        /**
         * Пауза перед сетевым запросом истории при входе в чат. Локальный кэш Room
         * рисуется мгновенно; ответ сервера почти всегда совпадает с кэшем, но его
         * разбор и запись в Room раньше приходились ровно на анимацию открытия экрана.
         */
        private const val INITIAL_NETWORK_LOAD_DELAY_MS = 250L

        private val timestampFormatter = ThreadLocal.withInitial {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("Europe/Paris")
            }
        }
    }

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val userPrefs = UserPreferences(application)

    val myUserId: Int get() = userPrefs.userId
    val myDisplayName: String
        get() = userPrefs.name.trim().ifBlank { userPrefs.username.trim() }

    private val _partnerId = MutableStateFlow(0)
    private val _partnerName = MutableStateFlow("")
    val partnerName: StateFlow<String> = _partnerName.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private val _typingUsers = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val typingUsers: StateFlow<Map<Int, Boolean>> = _typingUsers.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    /**
     * Единственный источник истины для списка: сообщения, группировка по дням,
     * карта id -> сообщение и карта id -> индекс в LazyColumn. Одна эмиссия = одна
     * рекомпозиция. Заменяет прежние groupedMessages / messageFlatIndex и ручную
     * сборку messagesById / messageLazyIndex в композиции.
     */
    val listSnapshot: StateFlow<ChatListSnapshot> = _messages
        .map { list -> buildChatListSnapshot(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, ChatListSnapshot.Empty)

    var isContextMode by mutableStateOf(false)
        private set
    private var isResettingToBottom = false

    private val _messageLimit = MutableStateFlow(FIRST_PAGE_SIZE)
    private var isReachedEnd = false
    private var isLoadingMore = false

    val partnerUser = MutableStateFlow<UserCacheEntity?>(null)

    private val _replyingToMessage = MutableStateFlow<MessageEntity?>(null)
    val replyingToMessage = _replyingToMessage.asStateFlow()

    private val _myAvatarUrl = MutableStateFlow<String?>(null)
    val myAvatarUrl = _myAvatarUrl.asStateFlow()

    private val _editingMessage = MutableStateFlow<MessageEntity?>(null)
    val editingMessage = _editingMessage.asStateFlow()

    private var webSocket: VibeWebSocket? = null
    private var typingJob: Job? = null
    private var typingTimeoutJob: Job? = null
    private var isCurrentlyTyping = false
    private var messagesFlowJob: Job? = null
    private var initialLoadJob: Job? = null
    private var firstPageExpandJob: Job? = null
    private var loadMoreWatchdogJob: Job? = null
    private var partnerUserJob: Job? = null
    private val pendingInlineCallbackTimeoutJobs = mutableMapOf<Int, Job>()

    private var lastTypingTime = 0L

    // Tracks IDs of messages already sent to the server to prevent duplicates
    private val processedUploadIds = mutableSetOf<Int>()

    private val _highlightedMessageId = MutableStateFlow<Int?>(null)
    val highlightedMessageId = _highlightedMessageId.asStateFlow()

    private val _pinnedMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val pinnedMessages = _pinnedMessages.asStateFlow()

    private val _currentPinnedIndex = MutableStateFlow(0)
    val currentPinnedIndex = _currentPinnedIndex.asStateFlow()

    /** Keeps the most recently sent pinned message first, independently of server delivery order. */
    private fun normalizedPinned(messages: Collection<MessageEntity>): List<MessageEntity> =
        messages.distinctBy { it.id }.sortedByDescending { it.timestamp }

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _currentSearchIndex = MutableStateFlow(0)
    val currentSearchIndex = _currentSearchIndex.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val searchResults: StateFlow<List<MessageEntity>> = combine(_partnerId, _searchQuery) { partnerId, query ->
        Pair(partnerId, query)
    }
        .debounce(250)
        .flatMapLatest { (partnerId, query) ->
            if (query.isBlank() || partnerId <= 0) {
                flowOf(emptyList())
            } else {
                chatDao.searchMessagesInChat(myUserId, partnerId, query.trim())
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Полный список чатов с JOIN нужен только для шторки "переслать". Держать его
    // подписанным всё время жизни ViewModel (Lazily) значило пересобирать JOIN при
    // любой записи в chats/users_cache, даже когда шторка закрыта.
    val recentChats: StateFlow<List<com.flasskdev.vibe.data.local.ChatWithUser>> = chatDao.getChatsWithUserInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val botCallbackAlert = kotlinx.coroutines.flow.MutableSharedFlow<Pair<String, Boolean>>(
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val botCallbackToast = kotlinx.coroutines.flow.MutableSharedFlow<String>(
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    private val _pendingInlineCallbacks = MutableStateFlow<Map<Int, PendingInlineCallback>>(emptyMap())
    val pendingInlineCallbacks = _pendingInlineCallbacks.asStateFlow()

    private val wsListener = object : VibeWebSocketListener {
        override fun onAuthResponse(message: VibeMessage) {}

        override fun onChatMessage(
            senderId: Int, receiverId: Int, senderType: String, content: String,
            timestamp: String, messageId: Int, senderName: String, replyToId: Int?,
            replyToContent: String?, replyToSenderName: String?, forwardedFromId: Int?,
            forwardedFromName: String?, isEdited: Boolean, attachments: List<String>?,
            replyMarkup: com.flasskdev.vibe.data.local.ReplyMarkup?
        ) {
            val partnerId = _partnerId.value
            val myId = myUserId

            viewModelScope.launch(Dispatchers.IO) {
                if (senderId != myId) {
                    val senderUser = chatDao.getUserByIdSync(senderId)
                    if (senderUser?.isBlockedByMe == true) {
                        return@launch
                    }
                    val senderChat = chatDao.getChatById(senderId)
                    if (senderChat?.isBlockedByMe == true) {
                        return@launch
                    }
                }

                if (senderId == myId) {
                    chatDao.deleteOldestPendingMessage(content, receiverId)
                }
                chatDao.insertMessage(
                    MessageEntity(
                        id = messageId,
                        senderId = senderId,
                        receiverId = receiverId,
                        senderType = senderType,
                        content = content,
                        timestamp = parseTimestamp(timestamp),
                        isRead = false,
                        replyToId = replyToId,
                        replyToContent = replyToContent,
                        replyToSenderName = replyToSenderName,
                        forwardedFromId = forwardedFromId,
                        forwardedFromName = forwardedFromName,
                        isEdited = isEdited,
                        attachments = attachments,
                        replyMarkup = replyMarkup
                    )
                )
            }
        }

        override fun onChatListUpdate(chats: List<ChatInfo>) {}



        override fun onMessageEdited(messageId: Int, newContent: String) {
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.updateMessageContent(messageId, newContent)
            }
        }

        override fun onMessagesDeleted(messageIds: List<Int>) {
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.deleteMessagesByIds(messageIds)

                _pinnedMessages.update { current ->
                    val currentList = current.toMutableList()
                    val initialSize = currentList.size
                    currentList.removeAll { it.id in messageIds }
                    if (currentList.size != initialSize) {
                        if (_currentPinnedIndex.value >= currentList.size) {
                            _currentPinnedIndex.value = maxOf(0, currentList.size - 1)
                        }
                    }
                    currentList
                }
            }
        }

        override fun onMessagePinned(messageId: Int, withUserId: Int) {
            if (withUserId == _partnerId.value) {
                viewModelScope.launch(Dispatchers.IO) {
                    delay(300)
                    val msgs = chatDao.getMessagesByIds(listOf(messageId))
                    if (msgs.isNotEmpty()) {
                        _pinnedMessages.update { current ->
                            normalizedPinned(current + msgs.first())
                        }
                        _currentPinnedIndex.value = 0
                    }
                }
            }
        }

        override fun onMessageUnpinned(messageId: Int, withUserId: Int) {
            if (withUserId == _partnerId.value) {
                _pinnedMessages.update { current ->
                    val currentList = current.toMutableList()
                    currentList.removeAll { it.id == messageId }
                    if (_currentPinnedIndex.value >= currentList.size) {
                        _currentPinnedIndex.value = maxOf(0, currentList.size - 1)
                    }
                    currentList
                }
            }
        }

        override fun onAllMessagesUnpinned(withUserId: Int) {
            if (withUserId == _partnerId.value) {
                _pinnedMessages.value = emptyList()
                _currentPinnedIndex.value = 0
            }
        }

        override fun onPinnedMessagesLoaded(withUserId: Int, messageIds: List<Int>) {
            if (withUserId != _partnerId.value) return
            viewModelScope.launch(Dispatchers.IO) {
                val msgs = if (messageIds.isEmpty()) emptyList() else chatDao.getMessagesByIds(messageIds)
                _pinnedMessages.value = normalizedPinned(msgs)
                _currentPinnedIndex.value = 0
            }
        }

        override fun onMessagesLoaded(withUserId: Int, messages: List<MessageInfo>, offset: Int) {
            if (withUserId != _partnerId.value) return
            if (messages.isEmpty()) {
                isReachedEnd = true
            }
            isLoadingMore = false
            loadMoreWatchdogJob?.cancel()
            viewModelScope.launch(Dispatchers.IO) {
                val entities = messages.map { msg ->
                    MessageEntity(
                        id = msg.id,
                        senderId = msg.senderId,
                        receiverId = msg.receiverId,
                        senderType = msg.senderType,
                        content = msg.content,
                        timestamp = parseTimestamp(msg.timestamp),
                        isRead = msg.isRead,
                        replyToId = msg.replyToId,
                        replyToContent = msg.replyToContent,
                        replyToSenderName = msg.replyToSenderName,
                        forwardedFromId = msg.forwardedFromId,
                        forwardedFromName = msg.forwardedFromName,
                        isEdited = msg.isEdited,
                        attachments = msg.attachments,
                        reactions = msg.reactions,
                        replyMarkup = msg.replyMarkup
                    )
                }
                if (isResettingToBottom && offset == 0) {
                    chatDao.replaceMessagesByPartner(myUserId, withUserId, entities)
                    isResettingToBottom = false
                } else {
                    // Сервер почти всегда отдаёт то, что уже лежит в кеше. Пишем только
                    // отличия, иначе таблица messages инвалидируется целиком и весь
                    // список сообщений пересобирается — это и был лаг при входе в чат.
                    chatDao.upsertMessagesIfChanged(entities)
                }
            }
        }

        override fun onMessagesLoadedAround(withUserId: Int, messages: List<MessageInfo>) {
            if (withUserId != _partnerId.value) return
            _messageLimit.value = messages.size
            viewModelScope.launch(Dispatchers.IO) {
                val entities = messages.map { msg ->
                    MessageEntity(
                        id = msg.id,
                        senderId = msg.senderId,
                        receiverId = msg.receiverId,
                        senderType = msg.senderType,
                        content = msg.content,
                        timestamp = parseTimestamp(msg.timestamp),
                        isRead = msg.isRead,
                        replyToId = msg.replyToId,
                        replyToContent = msg.replyToContent,
                        replyToSenderName = msg.replyToSenderName,
                        forwardedFromId = msg.forwardedFromId,
                        forwardedFromName = msg.forwardedFromName,
                        isEdited = msg.isEdited,
                        attachments = msg.attachments,
                        reactions = msg.reactions,
                        replyMarkup = msg.replyMarkup
                    )
                }
                chatDao.replaceMessagesByPartner(myUserId, withUserId, entities)
            }
        }

        override fun onBotCallbackAnswer(callbackId: Int?, text: String?, showAlert: Boolean) {
            // The server first confirms receipt with bot_callback_result. Only the bot's
            // answer_callback_query ends the pending state for the matching message.
            _pendingInlineCallbacks.value.values
                .firstOrNull { pending -> callbackId == null || pending.callbackId == null || pending.callbackId == callbackId }
                ?.let { completePendingInlineCallback(it.messageId) }
            if (!text.isNullOrBlank()) {
                if (showAlert) {
                    botCallbackAlert.tryEmit(text to true)
                } else {
                    botCallbackToast.tryEmit(text)
                }
            }
        }

        override fun onBotCallbackError(error: String, message: String) {
            // The protocol error has no message ID. Unlock only the oldest pending request so
            // independent keyboards remain usable.
            _pendingInlineCallbacks.value.values.minByOrNull { it.messageId }
                ?.let { completePendingInlineCallback(it.messageId) }
            botCallbackToast.tryEmit(message)
        }

        override fun onBotCallbackResult(callbackId: Int?, messageId: Int, data: String) {
            val pending = _pendingInlineCallbacks.value[messageId]
            // This confirms server receipt and binds the future answer_callback_query to the
            // exact message keyboard. It must not unlock the button by itself.
            if (pending?.callbackData == data) {
                val updated = pending.copy(callbackId = callbackId)
                _pendingInlineCallbacks.update { current -> current + (messageId to updated) }
                PendingInlineCallbackStore.put(_partnerId.value, updated)
            }
        }

        override fun onTypingIndicator(senderId: Int, senderName: String) {
            if (senderId == _partnerId.value) {
                _isPartnerTyping.value = true
                typingTimeoutJob?.cancel()
                typingTimeoutJob = viewModelScope.launch {
                    delay(4000)
                    _isPartnerTyping.value = false
                }
            }
        }

        override fun onTypingStop(senderId: Int) {
            if (senderId == _partnerId.value) {
                _isPartnerTyping.value = false
                typingTimeoutJob?.cancel()
            }
        }

        override fun onMessagesReadByPartner(partnerId: Int, upToMessageId: Int?) {
            viewModelScope.launch(Dispatchers.IO) {
                if (partnerId == _partnerId.value) {
                    if (upToMessageId != null) {
                        chatDao.markMyMessagesAsReadUpTo(myUserId, partnerId, upToMessageId)
                    } else {
                        chatDao.markMyMessagesAsRead(myUserId, partnerId)
                    }
                }
            }
        }

        override fun onMarkReadResult(fromUserId: Int, upToMessageId: Int?) {
            viewModelScope.launch(Dispatchers.IO) {
                if (fromUserId == _partnerId.value) {
                    if (upToMessageId != null) {
                        chatDao.markMessagesAsReadUpTo(myUserId, fromUserId, upToMessageId)
                    } else {
                        chatDao.markMessagesAsRead(myUserId, fromUserId)
                    }
                }
            }
        }

        override fun onUserInfo(userId: Int, isOnline: Boolean, lastSeen: Long?, isDeveloper: Boolean, isVerified: Boolean, registerDate: Long?, isBot: Boolean, about: String?, username: String?, name: String?, avatarUrl: String?, lastSeenStatus: String?, canMessage: Boolean, isBanned: Boolean, isFreezed: Boolean, isBlockedByMe: Boolean, isBlockedByUser: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                val existing = chatDao.getUserByIdSync(userId)
                val newName = name ?: existing?.name ?: "User $userId"
                val newUsername = username ?: existing?.username ?: "user$userId"
                val userToSave = com.flasskdev.vibe.data.local.UserCacheEntity(
                    id = userId,
                    name = newName,
                    username = newUsername,
                    avatarUrl = avatarUrl,
                    isOnline = isOnline,
                    lastSeen = lastSeen,
                    isDeveloper = isDeveloper,
                    isVerified = isVerified,
                    isBanned = isBanned,
                    isFreezed = isFreezed,
                    registerDate = registerDate,
                    isBot = isBot,
                    about = about,
                    lastSeenStatus = lastSeenStatus,
                    canMessage = canMessage,
                    isBlockedByMe = isBlockedByMe,
                    isBlockedByUser = isBlockedByUser
                )
                if (existing != userToSave) {
                    chatDao.insertUser(userToSave)
                }
                val chat = chatDao.getChatById(userId)
                if (chat != null) {
                    if (chat.isBlockedByMe != isBlockedByMe) {
                        chatDao.updateChatBlockedByMe(userId, isBlockedByMe)
                    }
                    if (chat.isBlockedByUser != isBlockedByUser) {
                        chatDao.updateChatBlockedByUser(userId, isBlockedByUser)
                    }
                }
            }
        }

        override fun onBlockUserSuccess(blockedId: Int) {
            viewModelScope.launch(Dispatchers.IO) {
                val u = chatDao.getUserByIdSync(blockedId)
                if (u != null && !u.isBlockedByMe) {
                    chatDao.updateUserBlockedByMe(blockedId, true)
                }
                val c = chatDao.getChatById(blockedId)
                if (c != null && !c.isBlockedByMe) {
                    chatDao.updateChatBlockedByMe(blockedId, true)
                }
            }
        }

        override fun onUnblockUserSuccess(blockedId: Int) {
            viewModelScope.launch(Dispatchers.IO) {
                val u = chatDao.getUserByIdSync(blockedId)
                if (u != null && u.isBlockedByMe) {
                    chatDao.updateUserBlockedByMe(blockedId, false)
                }
                val c = chatDao.getChatById(blockedId)
                if (c != null && c.isBlockedByMe) {
                    chatDao.updateChatBlockedByMe(blockedId, false)
                }
            }
        }

        override fun onUsersSearchResult(users: List<UserSearchResult>) {}
        override fun onConnected() {
            refreshUserInfo()
        }
        override fun onDisconnected() {}
        override fun onError(error: String) {}

        override fun onMessageReaction(
            messageId: Int,
            userId: Int,
            emoji: String,
            reactions: List<com.flasskdev.vibe.data.local.ReactionItem>
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.updateMessageReactions(messageId, if (reactions.isEmpty()) null else reactions)
            }
        }

        override fun onReactionUsersResult(
            messageId: Int,
            emoji: String?,
            offset: Int,
            hasMore: Boolean,
            users: List<com.flasskdev.vibe.data.ReactionUserDetail>
        ) {
            val current = _reactionSheetState.value ?: return
            if (current.message.id != messageId || current.selectedEmoji.orEmpty() != emoji.orEmpty()) return

            val updatedUsers = if (offset == 0) users else current.users + users
            _reactionSheetState.value = current.copy(
                users = updatedUsers,
                offset = offset + users.size,
                hasMore = hasMore,
                isLoading = false
            )
        }
    }

    private val _reactionSheetState = MutableStateFlow<ReactionSheetData?>(null)
    val reactionSheetState = _reactionSheetState.asStateFlow()

    private suspend fun buildLocalReactionUsers(message: MessageEntity, emoji: String?): List<com.flasskdev.vibe.data.ReactionUserDetail> {
        val localReactions = message.reactions ?: emptyList()
        val matchingReactions = if (emoji.isNullOrBlank()) localReactions else localReactions.filter { it.emoji == emoji }
        val list = mutableListOf<com.flasskdev.vibe.data.ReactionUserDetail>()
        for (r in matchingReactions) {
            val tsMap = r.users.associate { it.userId to it.timestamp }
            for (uid in r.userIds) {
                val u = chatDao.getUserByIdSync(uid)
                list.add(
                    com.flasskdev.vibe.data.ReactionUserDetail(
                        userId = uid,
                        name = u?.name ?: run {
                            val s = com.flasskdev.vibe.ui.theme.VibeStringsHolder.current
                            if (uid == myUserId) s.you else s.userFallback(uid)
                        },
                        username = u?.username,
                        avatarUrl = u?.avatarUrl,
                        emoji = r.emoji,
                        timestamp = tsMap[uid] ?: message.timestamp,
                        isVerified = u?.isVerified ?: false,
                        isDeveloper = u?.isDeveloper ?: false,
                        isBot = u?.isBot ?: false
                    )
                )
            }
        }
        list.sortByDescending { it.timestamp }
        return list
    }

    fun openReactionDetails(message: MessageEntity, emoji: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val initialUsers = buildLocalReactionUsers(message, emoji)
            withContext(Dispatchers.Main) {
                _reactionSheetState.value = ReactionSheetData(
                    message = message,
                    selectedEmoji = emoji,
                    users = initialUsers,
                    offset = 0,
                    hasMore = false,
                    isLoading = initialUsers.isEmpty()
                )
            }
            webSocket?.getReactionUsers(message.id, emoji, offset = 0, limit = 20)
        }
    }

    fun selectReactionEmojiTab(emoji: String?) {
        val current = _reactionSheetState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val localUsers = buildLocalReactionUsers(current.message, emoji)
            withContext(Dispatchers.Main) {
                val cur = _reactionSheetState.value ?: return@withContext
                _reactionSheetState.value = cur.copy(
                    selectedEmoji = emoji,
                    users = localUsers,
                    offset = 0,
                    hasMore = false,
                    isLoading = localUsers.isEmpty()
                )
            }
            webSocket?.getReactionUsers(current.message.id, emoji, offset = 0, limit = 20)
        }
    }

    fun loadMoreReactionUsers() {
        val current = _reactionSheetState.value ?: return
        if (current.isLoading || !current.hasMore) return
        _reactionSheetState.value = current.copy(isLoading = true)
        webSocket?.getReactionUsers(current.message.id, current.selectedEmoji, offset = current.offset, limit = 20)
    }

    fun closeReactionDetails() {
        _reactionSheetState.value = null
    }

    fun toggleReaction(message: MessageEntity, emoji: String) {
        val current = message.reactions ?: emptyList()
        val userPrevEmoji = current.find { it.userIds.contains(myUserId) }?.emoji

        val now = System.currentTimeMillis()
        val updated = mutableListOf<com.flasskdev.vibe.data.local.ReactionItem>()
        for (r in current) {
            val uIds = r.userIds.filter { it != myUserId }
            val rUsers = r.users.filter { it.userId != myUserId }
            if (r.emoji == emoji && userPrevEmoji != emoji) {
                val newIds = uIds + myUserId
                val newUsers = rUsers + com.flasskdev.vibe.data.local.ReactionUser(myUserId, now)
                updated.add(r.copy(count = newIds.size, userIds = newIds, users = newUsers))
            } else if (uIds.isNotEmpty()) {
                updated.add(r.copy(count = uIds.size, userIds = uIds, users = rUsers))
            }
        }
        if (userPrevEmoji != emoji && updated.none { it.emoji == emoji }) {
            updated.add(com.flasskdev.vibe.data.local.ReactionItem(
                emoji = emoji,
                count = 1,
                userIds = listOf(myUserId),
                users = listOf(com.flasskdev.vibe.data.local.ReactionUser(myUserId, now))
            ))
        }

        val result = if (updated.isEmpty()) null else updated
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateMessageReactions(message.id, result)
            webSocket?.sendReaction(message.id, myUserId, emoji)
        }
    }

    private var lastUserInfoRequestTime = 0L

    fun refreshUserInfo(force: Boolean = false) {
        val partnerId = _partnerId.value
        if (partnerId <= 0) return
        val now = System.currentTimeMillis()
        // UI already shows the cached user from Room. Only hit the network on the initial
        // open (force) or at most once a minute for passive resume/reconnect refreshes,
        // instead of firing on every ON_RESUME and churning recompositions.
        val window = if (force) 3000L else 60_000L
        if (now - lastUserInfoRequestTime > window) {
            lastUserInfoRequestTime = now
            webSocket?.getUserInfo(partnerId)
        }
    }

    fun init(partnerId: Int, partnerName: String, ws: VibeWebSocket) {
        _partnerId.value = partnerId
        _partnerName.value = partnerName
        com.flasskdev.vibe.utils.NotificationHelper.activeChatId = partnerId
        webSocket = ws
        ws.addListener(wsListener)
        refreshUserInfo(force = true)

        viewModelScope.launch(Dispatchers.IO) {
            val me = chatDao.getUserByIdSync(myUserId)
            if (me != null && _myAvatarUrl.value != me.avatarUrl) {
                _myAvatarUrl.value = me.avatarUrl
            }
        }

        // PERF: стартуем с маленького окна, расширяем после первого кадра (см. ниже).
        _messageLimit.value = FIRST_PAGE_SIZE
        isReachedEnd = false
        isLoadingMore = false
        loadMoreWatchdogJob?.cancel()
        lastMarkedReadId = -1
        // Keep an unexpired callback wait when this destination is recreated for the same chat.
        restorePendingInlineCallbacks(partnerId)
        // The screen ViewModel can outlive a navigation transition; never leak pin state into another chat.
        _pinnedMessages.value = emptyList()
        _currentPinnedIndex.value = 0
        _highlightedMessageId.value = null

        // PERF: сеть — после того, как локальный кэш отрисован. Раньше ответ сервера
        // (разбор JSON + запись в Room + инвалидация Flow + пересборка LazyColumn)
        // прилетал прямо во время анимации открытия чата.
        initialLoadJob?.cancel()
        initialLoadJob = viewModelScope.launch {
            delay(INITIAL_NETWORK_LOAD_DELAY_MS)
            ws.loadMessages(myUserId, partnerId, offset = 0)
        }

        // Подписываемся на локальные сообщения из Room
        messagesFlowJob?.cancel()
        messagesFlowJob = viewModelScope.launch {
            @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
            _messageLimit
                .flatMapLatest { limit ->
                    chatDao.getMessagesByPartner(myUserId, partnerId, limit)
                }
                .flowOn(Dispatchers.IO) // Обработка БД в IO потоке
                .collect { msgs ->
                    _messages.value = msgs

                    val pendingUploads = msgs.filter { it.uploadStatus == "SUCCESS" && it.senderId == myUserId && !processedUploadIds.contains(it.id) }
                    if (pendingUploads.isNotEmpty()) {
                        pendingUploads.forEach { processedUploadIds.add(it.id) }
                        viewModelScope.launch(Dispatchers.IO) {
                            pendingUploads.forEach { msg ->
                                webSocket?.sendMessage(
                                    senderId = myUserId,
                                    receiverId = partnerId,
                                    content = msg.content,
                                    replyToId = msg.replyToId,
                                    attachments = msg.attachments
                                )
                                chatDao.updateUploadStatus(msg.id, "SENT", msg.attachments)
                            }
                        }
                    }
                }
        }

        // PERF: после первого кадра окно расширяется до полной страницы. Один лишний
        // SELECT с LIMIT 50 стоит копейки, а первая (самая дорогая) композиция чата
        // получает 15 бабблов вместо 50.
        firstPageExpandJob?.cancel()
        firstPageExpandJob = viewModelScope.launch {
            delay(FIRST_PAGE_EXPAND_DELAY_MS)
            if (_messageLimit.value == FIRST_PAGE_SIZE) {
                _messageLimit.value = MESSAGES_PAGE_SIZE
            }
        }

        // Подписываемся на юзера, чтобы получить статус онлайна и статус блокировки
        partnerUserJob?.cancel()
        partnerUserJob = viewModelScope.launch(Dispatchers.IO) {
            val cached = chatDao.getUserByIdSync(partnerId)
            if (cached != null && partnerUser.value != cached) {
                partnerUser.value = cached
            }
            chatDao.getUserById(partnerId)
                .distinctUntilChanged()
                .collect { user ->
                    if (user != null && partnerUser.value != user) {
                        partnerUser.value = user
                    }
                }
        }
    }

    /**
     * PERF: главный источник фриза при входе в чат.
     *
     * Раньше `_messageLimit` увеличивался БЕЗУСЛОВНО, ещё до проверки локального
     * количества, а ранний `return` не выставлял `isLoadingMore`. Смена лимита дёргает
     * flatMapLatest -> отмена старого Room-Flow -> новый SELECT с новым LIMIT -> новый
     * инстанс списка -> пересборка LazyColumn -> новый layoutInfo -> snapshotFlow в
     * ChatScreen снова вызывал loadMoreMessages(). Получался каскад 20 -> 40 -> 60 -> ...
     * до конца истории: десятки запросов в Room и в сокет за пару секунд, main thread
     * забит, клавиатура не может выехать. На пустом чате каскад не стартовал вообще.
     *
     * Теперь окно расширяется только когда локальные сообщения реально исчерпаны, и
     * ровно один запрос за раз.
     */
    fun loadMoreMessages() {
        if (isLoadingMore || isReachedEnd) return

        val localCount = _messages.value.size
        // Пока на экран не отдано всё, что уже лежит в Room, лимит не трогаем:
        // иначе меняется ключ flatMapLatest и список пересобирается впустую.
        if (localCount < _messageLimit.value) return

        isLoadingMore = true
        _messageLimit.value = localCount + MESSAGES_PAGE_SIZE
        webSocket?.loadMessages(myUserId, _partnerId.value, offset = localCount)

        // Без ответа сервера (обрыв сокета) isLoadingMore иначе залипает навсегда
        // и подгрузка старых сообщений умирает до перезахода в чат.
        loadMoreWatchdogJob?.cancel()
        loadMoreWatchdogJob = viewModelScope.launch {
            delay(10_000)
            isLoadingMore = false
        }
    }

    fun replyToMessage(msg: MessageEntity) {
        _replyingToMessage.value = msg
    }

    fun cancelReply() {
        _replyingToMessage.value = null
    }

    fun startEditing(message: MessageEntity) {
        val now = System.currentTimeMillis()
        if (now - message.timestamp <= 24 * 60 * 60 * 1000) {
            _editingMessage.value = message
            cancelReply()
        }
    }

    fun cancelEditing() {
        _editingMessage.value = null
    }

    private var lastMarkedReadId = -1

    fun onMessagesVisible(visibleIds: List<Int>) {
        val partnerId = _partnerId.value
        if (partnerId <= 0 || visibleIds.isEmpty()) return

        // PERF: было O(видимые * все сообщения) через find{} на каждый видимый элемент,
        // причём вызывалось это почти на каждый кадр скролла. Теперь один проход по списку.
        val visible = HashSet(visibleIds)
        var maxUnreadId = -1
        for (msg in _messages.value) {
            if (msg.id in visible && msg.senderId == partnerId && !msg.isRead && msg.id > maxUnreadId) {
                maxUnreadId = msg.id
            }
        }

        // Не спамим вебсокет одним и тем же markRead при каждом мелком сдвиге списка.
        if (maxUnreadId != -1 && maxUnreadId > lastMarkedReadId) {
            lastMarkedReadId = maxUnreadId
            webSocket?.markRead(myUserId, partnerId, maxUnreadId)
        }
    }

    fun deleteMessages(messageIds: List<Int>, forEveryone: Boolean) {
        val userId = userPrefs.userId
        if (userId > 0) {
            webSocket?.deleteMessages(userId, messageIds, forEveryone)
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.deleteMessagesByIds(messageIds)
            }
        }
    }

    fun forwardMessages(targetUserId: Int, messageIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            val messagesToForward = _messages.value.filter { it.id in messageIds }.sortedBy { it.timestamp }
            for (msg in messagesToForward) {
                val origId = if (msg.senderId == myUserId) myUserId else _partnerId.value
                webSocket?.sendMessage(myUserId, targetUserId, msg.content, null, origId, msg.attachments)
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun blockUser(targetId: Int) {
        if (myUserId > 0 && targetId > 0) {
            webSocket?.blockUser(myUserId, targetId)
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.updateUserBlockedByMe(targetId, true)
                chatDao.updateChatBlockedByMe(targetId, true)
            }
        }
    }

    fun unblockUser(targetId: Int) {
        if (myUserId > 0 && targetId > 0) {
            webSocket?.unblockUser(myUserId, targetId)
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.updateUserBlockedByMe(targetId, false)
                chatDao.updateChatBlockedByMe(targetId, false)
            }
        }
    }

    fun submitEditMessage(newContent: String) {
        val trimmed = newContent.trim().take(2048)
        val msg = _editingMessage.value
        if (trimmed.isEmpty() || msg == null) return

        // Отправляем измененный текст через вебсокет
        webSocket?.editMessage(msg.id, trimmed)

        // Локально сразу обновляем
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateMessageContent(msg.id, trimmed)
        }

        cancelEditing()
    }

    fun sendMessage(content: String) {
        val trimmed = content.trim().take(2048)
        if (trimmed.isEmpty()) return

        if (_editingMessage.value != null) {
            submitEditMessage(content)
            return
        }

        val replyId = _replyingToMessage.value?.id

        val replyContent = _replyingToMessage.value?.content
        val replySenderName = if (replyId != null) (if (_replyingToMessage.value?.senderId == myUserId) com.flasskdev.vibe.ui.theme.VibeStringsHolder.current.you else partnerName.value) else null
        val tempId = -(System.currentTimeMillis() % 1000000000).toInt()
        val tempMsg = com.flasskdev.vibe.data.local.MessageEntity(
            id = tempId,
            senderId = myUserId,
            receiverId = _partnerId.value,
            content = trimmed,
            timestamp = System.currentTimeMillis(),
            replyToId = replyId,
            replyToContent = replyContent,
            replyToSenderName = replySenderName
        )
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            chatDao.insertMessage(tempMsg)
            webSocket?.sendMessage(myUserId, _partnerId.value, trimmed, replyId)
        }

        cancelReply()
        if (isCurrentlyTyping) {
            isCurrentlyTyping = false
            webSocket?.sendStopTyping(myUserId, _partnerId.value)
        }
        typingJob?.cancel()
    }

    /**
     * Sends a sticker as its own message. Stickers are encoded in the content field as
     * "sticker:<pack>/<file>" (same "prefix" convention as voice/video messages) and carry
     * no attachment: the image itself lives in each client's bundled assets.
     */
    fun sendSticker(stickerId: String) {
        if (stickerId.isBlank()) return
        val content = "sticker:$stickerId"

        val replyId = _replyingToMessage.value?.id
        val replyContent = _replyingToMessage.value?.content
        val replySenderName = if (replyId != null) {
            if (_replyingToMessage.value?.senderId == myUserId) com.flasskdev.vibe.ui.theme.VibeStringsHolder.current.you else partnerName.value
        } else null

        val tempId = -(System.currentTimeMillis() % 1000000000).toInt()
        val tempMsg = com.flasskdev.vibe.data.local.MessageEntity(
            id = tempId,
            senderId = myUserId,
            receiverId = _partnerId.value,
            content = content,
            timestamp = System.currentTimeMillis(),
            replyToId = replyId,
            replyToContent = replyContent,
            replyToSenderName = replySenderName
        )
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            chatDao.insertMessage(tempMsg)
            webSocket?.sendMessage(myUserId, _partnerId.value, content, replyId)
        }
        cancelReply()
    }

    /**
     * Sends a GIF (e.g. from GIPHY) as its own message. The remote URL is passed directly as
     * an attachment (no upload needed), while the content carries a "gif:<w>x<h>" marker so the
     * bubble can reserve the right aspect ratio before the image loads.
     */
    fun sendGif(url: String, width: Int = 0, height: Int = 0) {
        if (url.isBlank()) return
        val content = "gif:${width}x${height}"
        val attachments = listOf(url)

        val replyId = _replyingToMessage.value?.id
        val replyContent = _replyingToMessage.value?.content
        val replySenderName = if (replyId != null) {
            if (_replyingToMessage.value?.senderId == myUserId) com.flasskdev.vibe.ui.theme.VibeStringsHolder.current.you else partnerName.value
        } else null

        val tempId = -(System.currentTimeMillis() % 1000000000).toInt()
        val tempMsg = com.flasskdev.vibe.data.local.MessageEntity(
            id = tempId,
            senderId = myUserId,
            receiverId = _partnerId.value,
            content = content,
            timestamp = System.currentTimeMillis(),
            replyToId = replyId,
            replyToContent = replyContent,
            replyToSenderName = replySenderName,
            attachments = attachments
        )
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            chatDao.insertMessage(tempMsg)
            webSocket?.sendMessage(myUserId, _partnerId.value, content, replyId, null, attachments)
        }
        cancelReply()
    }

    fun onTextChanged(text: String) {
        if (text.isNotEmpty()) {
            val now = System.currentTimeMillis()
            if (!isCurrentlyTyping || (now - lastTypingTime > 1500)) {
                isCurrentlyTyping = true
                lastTypingTime = now
                webSocket?.sendTyping(myUserId, _partnerId.value)
            }
        }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            if (isCurrentlyTyping) {
                isCurrentlyTyping = false
                webSocket?.sendStopTyping(myUserId, _partnerId.value)
            }
        }
    }

    private fun parseTimestamp(ts: String): Long {
        if (ts.isEmpty()) return System.currentTimeMillis()

        val tsLong = ts.toLongOrNull()
        if (tsLong != null) {
            return if (tsLong < 10000000000L) tsLong * 1000 else tsLong
        }

        try {
            if (ts.contains("T")) {
                val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                return isoFormat.parse(ts)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {}

        return try {
            val sdf = timestampFormatter.get()!!
            sdf.parse(ts)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    override fun onCleared() {
        super.onCleared()
        com.flasskdev.vibe.utils.NotificationHelper.activeChatId = null
        webSocket?.removeListener(wsListener)
        if (isCurrentlyTyping) {
            webSocket?.sendStopTyping(myUserId, _partnerId.value)
        }
        messagesFlowJob?.cancel()
        partnerUserJob?.cancel()
        typingJob?.cancel()
        typingTimeoutJob?.cancel()
        initialLoadJob?.cancel()
        firstPageExpandJob?.cancel()
        loadMoreWatchdogJob?.cancel()
    }

    fun jumpToMessage(messageId: Int, messageList: List<MessageEntity>) {
        val existsLocal = messageList.any { it.id == messageId }
        if (existsLocal) {
            triggerHighlight(messageId)
        } else {
            // Need to fetch from server
            viewModelScope.launch {
                isContextMode = true
                webSocket?.loadMessagesAround(myUserId, _partnerId.value, messageId)
                triggerHighlight(messageId)
            }
        }
    }

    private var highlightJob: Job? = null

    private fun triggerHighlight(messageId: Int) {
        highlightJob?.cancel()
        highlightJob = viewModelScope.launch {
            _highlightedMessageId.value = null
            delay(10)
            _highlightedMessageId.value = messageId
            delay(2200)
            if (_highlightedMessageId.value == messageId) {
                _highlightedMessageId.value = null
            }
        }
    }

    fun highlightMessage(messageId: Int) {
        triggerHighlight(messageId)
    }

    fun openSearch() {
        _isSearchActive.value = true
    }

    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
        _currentSearchIndex.value = 0
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _currentSearchIndex.value = 0
    }

    fun nextSearchResult(messageList: List<MessageEntity>) {
        val results = searchResults.value
        if (results.isEmpty()) return
        val nextIdx = (_currentSearchIndex.value + 1) % results.size
        _currentSearchIndex.value = nextIdx
        val targetMsg = results[nextIdx]
        jumpToMessage(targetMsg.id, messageList)
    }

    fun prevSearchResult(messageList: List<MessageEntity>) {
        val results = searchResults.value
        if (results.isEmpty()) return
        val prevIdx = if (_currentSearchIndex.value - 1 < 0) results.size - 1 else _currentSearchIndex.value - 1
        _currentSearchIndex.value = prevIdx
        val targetMsg = results[prevIdx]
        jumpToMessage(targetMsg.id, messageList)
    }

    fun jumpToDate(targetTimestamp: Long, messageList: List<MessageEntity>, onNotFound: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val partnerId = _partnerId.value
            if (partnerId <= 0) return@launch

            var targetMessage = chatDao.getFirstMessageOnOrAfterDate(myUserId, partnerId, targetTimestamp)
            if (targetMessage == null) {
                targetMessage = chatDao.getMessageClosestToTimestamp(myUserId, partnerId, targetTimestamp)
            }

            if (targetMessage != null) {
                jumpToMessage(targetMessage.id, messageList)
            } else {
                withContext(Dispatchers.Main) {
                    onNotFound()
                }
            }
        }
    }

    fun sendPhotos(context: android.content.Context, uris: List<android.net.Uri>, caption: String = "") {
        if (uris.isEmpty()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val attachments = mutableListOf<String>()
            val imageUris = mutableListOf<android.net.Uri>()

            for (uri in uris) {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                if (mimeType.startsWith("image/") && !mimeType.contains("gif")) {
                    imageUris.add(uri)
                } else {
                    val documentName = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)?.name.orEmpty()
                    val inferredExtension = when {
                        mimeType.startsWith("video/") -> when (mimeType) {
                            "video/webm" -> "webm"
                            "video/quicktime" -> "mov"
                            "video/3gpp" -> "3gp"
                            else -> "mp4"
                        }
                        mimeType.startsWith("audio/") -> "m4a"
                        else -> "bin"
                    }
                    val sourceExtension = documentName.substringAfterLast('.', "").lowercase()
                    val filename = if (sourceExtension.isNotBlank()) {
                        documentName
                    } else {
                        "media_${System.currentTimeMillis()}.$inferredExtension"
                    }
                    // A unique cache file prevents stale or incomplete media from being reused.
                    val cachedFile = java.io.File(context.cacheDir, "vibe_${System.nanoTime()}_$filename")
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            cachedFile.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw java.io.IOException(com.flasskdev.vibe.ui.theme.VibeStringsHolder.current.fileOpenFailed)
                        attachments.add(cachedFile.absolutePath)
                    } catch (e: Exception) {
                        cachedFile.delete()
                        e.printStackTrace()
                    }
                }
            }

            if (imageUris.isNotEmpty()) {
                val processed = com.flasskdev.vibe.utils.ImageProcessor.processAndCacheImages(context, imageUris)
                attachments.addAll(processed.map { it.cachedFilePath })
            }

            if (attachments.isEmpty()) return@launch

            val persisted = attachments.map { com.flasskdev.vibe.utils.OutboxFiles.persist(context, it) }
            attachments.clear()
            attachments.addAll(persisted)

            val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()

            val replyId = _replyingToMessage.value?.id
            val replyContent = _replyingToMessage.value?.content
            val replySenderName = if (_replyingToMessage.value?.senderId == myUserId) null else _partnerName.value

            val tempMsg = MessageEntity(
                id = tempId,
                senderId = myUserId,
                receiverId = _partnerId.value,
                content = caption.trim(),
                timestamp = System.currentTimeMillis(),
                replyToId = replyId,
                replyToContent = replyContent,
                replyToSenderName = replySenderName,
                uploadStatus = "UPLOADING",
                uploadProgress = 0,
                attachments = attachments
            )
            chatDao.insertMessage(tempMsg)

            enqueueUpload(context, tempId)
        }
        cancelReply()
    }

    private fun enqueueUpload(context: android.content.Context, messageId: Int) {
        val request = androidx.work.OneTimeWorkRequestBuilder<com.flasskdev.vibe.data.network.FileUploadWorker>()
            .setInputData(androidx.work.Data.Builder().putInt("messageId", messageId).build())
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10, java.util.concurrent.TimeUnit.SECONDS
            )
            .build()

        // Уникальная работа на сообщение: повтор не плодит параллельные загрузки,
        // REPLACE снимает зависший прошлый заход.
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "upload-$messageId",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun retryUpload(context: android.content.Context, messageId: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val message = chatDao.getMessageById(messageId)
            val attachments = message?.attachments
            if (attachments.isNullOrEmpty()) {
                // Повторять нечего: не оставляем сообщение в UPLOADING.
                chatDao.updateUploadStatusOnly(messageId, "FAILED")
                return@launch
            }
            // updateUploadStatus(..., null) затирал attachments, из-за чего сообщение
            // навсегда зависало на "0%", а кружок становился "не удалось отправить".
            chatDao.updateUploadStatusOnly(messageId, "UPLOADING")
            chatDao.updateUploadProgress(messageId, 0)
            enqueueUpload(context, messageId)
        }
    }

    fun sendVoiceMessage(context: android.content.Context, audioFile: java.io.File, durationMs: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val attachments = listOf(
                com.flasskdev.vibe.utils.OutboxFiles.persist(context, audioFile.absolutePath)
            )
            val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()

            val replyId = _replyingToMessage.value?.id
            val replyContent = _replyingToMessage.value?.content
            val replySenderName = if (_replyingToMessage.value?.senderId == myUserId) null else _partnerName.value

            val tempMsg = com.flasskdev.vibe.data.local.MessageEntity(
                id = tempId,
                senderId = myUserId,
                receiverId = _partnerId.value,
                content = "duration:$durationMs",
                timestamp = System.currentTimeMillis(),
                replyToId = replyId,
                replyToContent = replyContent,
                replyToSenderName = replySenderName,
                uploadStatus = "UPLOADING",
                uploadProgress = 0,
                attachments = attachments
            )
            chatDao.insertMessage(tempMsg)

            enqueueUpload(context, tempId)
        }
        cancelReply()
    }


    /**
     * ПУНКТ 2 — ОТПРАВКА КРУЖКА.
     *
     * Отдельный метод понадобился, потому что CircleSendUseCase выдумывал свой
     * протокол (`videomsg:<url>?d=...&t=...`), которого не знает ни ChatScreen,
     * ни ChatListScreen, ни MessageUtils — они все разбирают
     * `video_message:<durationMs>` + файл во вложениях. Из-за расхождения кружок
     * не отрисовался бы даже при удачной записи.
     *
     * Реализация зеркалит sendVoiceMessage: тот же оптимистичный MessageEntity с
     * отрицательным id и тот же FileUploadWorker, поэтому прогресс загрузки,
     * ретраи и офлайн-очередь работают из коробки.
     */
    fun sendVideoNote(context: android.content.Context, videoFile: java.io.File, durationMs: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (!videoFile.exists() || videoFile.length() == 0L) return@launch

            val attachments = listOf(
                com.flasskdev.vibe.utils.OutboxFiles.persist(context, videoFile.absolutePath)
            )
            val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()

            val replyId = _replyingToMessage.value?.id
            val replyContent = _replyingToMessage.value?.content
            val replySenderName =
                if (_replyingToMessage.value?.senderId == myUserId) null else _partnerName.value

            val tempMsg = com.flasskdev.vibe.data.local.MessageEntity(
                id = tempId,
                senderId = myUserId,
                receiverId = _partnerId.value,
                content = "video_message:$durationMs",
                timestamp = System.currentTimeMillis(),
                replyToId = replyId,
                replyToContent = replyContent,
                replyToSenderName = replySenderName,
                uploadStatus = "UPLOADING",
                uploadProgress = 0,
                attachments = attachments
            )
            chatDao.insertMessage(tempMsg)

            enqueueUpload(context, tempId)
        }
        cancelReply()
    }

    fun jumpToBottom() {
        viewModelScope.launch(Dispatchers.IO) {
            isResettingToBottom = true
            isContextMode = false
            isReachedEnd = false
            _messageLimit.value = 15
            webSocket?.loadMessages(myUserId, _partnerId.value, 0)
        }
    }

    fun pinMessage(messageId: Int, forBoth: Boolean) {
        webSocket?.pinMessage(myUserId, messageId, forBoth)
    }

    fun unpinMessage(messageId: Int, forBoth: Boolean) {
        webSocket?.unpinMessage(myUserId, messageId, forBoth)
    }

    fun unpinAllMessages(forBoth: Boolean) {
        webSocket?.unpinAllMessages(myUserId, _partnerId.value, forBoth)
    }

    fun updateCurrentPinnedIndex(visibleItemsList: List<Int>) {
        if (_highlightedMessageId.value != null || visibleItemsList.isEmpty()) return

        val pinned = _pinnedMessages.value
        if (pinned.isEmpty()) return

        // Message IDs are not a reliable chronological order after migrations or temporary local IDs.
        // Use timestamps from the current window and keep the newest pin first.
        // PERF: `id in List` — линейный поиск для каждого сообщения, на скролле это заметно.
        val visibleIdSet = HashSet(visibleItemsList)
        val visibleMessages = _messages.value.filter { it.id in visibleIdSet }
        if (visibleMessages.isEmpty()) return

        val visiblePinned = pinned.filter { pin -> visibleMessages.any { it.id == pin.id } }
        val targetIndex = if (visiblePinned.isNotEmpty()) {
            pinned.indexOf(visiblePinned.maxBy { it.timestamp })
        } else {
            val newestVisibleTimestamp = visibleMessages.maxOf { it.timestamp }
            pinned.indexOfFirst { it.timestamp <= newestVisibleTimestamp }
                .takeIf { it >= 0 }
                ?: pinned.lastIndex
        }
        _currentPinnedIndex.value = targetIndex.coerceIn(0, pinned.lastIndex)
    }

    fun nextPinnedIndex() {
        val size = _pinnedMessages.value.size
        if (size > 0) {
            _currentPinnedIndex.value = (_currentPinnedIndex.value + 1) % size
        }
    }

    private fun completePendingInlineCallback(messageId: Int) {
        pendingInlineCallbackTimeoutJobs.remove(messageId)?.cancel()
        _pendingInlineCallbacks.update { current -> current - messageId }
        PendingInlineCallbackStore.remove(_partnerId.value, messageId)
    }

    private fun schedulePendingInlineCallbackTimeout(pending: PendingInlineCallback) {
        pendingInlineCallbackTimeoutJobs.remove(pending.messageId)?.cancel()
        val remainingMillis = (pending.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        pendingInlineCallbackTimeoutJobs[pending.messageId] = viewModelScope.launch {
            delay(remainingMillis)
            if (_pendingInlineCallbacks.value[pending.messageId] == pending) {
                completePendingInlineCallback(pending.messageId)
                botCallbackToast.emit(com.flasskdev.vibe.ui.theme.VibeStringsHolder.current.botCallbackTimeout)
            }
        }
    }

    private fun restorePendingInlineCallbacks(chatId: Int) {
        pendingInlineCallbackTimeoutJobs.values.forEach { it.cancel() }
        pendingInlineCallbackTimeoutJobs.clear()
        val restored = PendingInlineCallbackStore.activeForChat(chatId, System.currentTimeMillis())
        _pendingInlineCallbacks.value = restored
        restored.values.forEach(::schedulePendingInlineCallbackTimeout)
    }

    fun onInlineButtonClicked(
        message: MessageEntity,
        button: com.flasskdev.vibe.data.local.InlineKeyboardButton,
        onOpenUrl: (String) -> Unit
    ) {
        if (!button.url.isNullOrBlank()) {
            onOpenUrl(button.url)
            return
        }

        val callbackData = button.callbackData ?: return
        // Lock only this message keyboard. Buttons in other bot messages remain independent.
        if (_pendingInlineCallbacks.value.containsKey(message.id)) return

        val socket = webSocket
        if (socket == null) {
            botCallbackToast.tryEmit(com.flasskdev.vibe.ui.theme.VibeStringsHolder.current.connectionLostToast)
            return
        }

        val pending = PendingInlineCallback(
            messageId = message.id,
            callbackData = callbackData,
            buttonText = button.text,
            expiresAtMillis = System.currentTimeMillis() + 10_000L
        )
        _pendingInlineCallbacks.update { current -> current + (message.id to pending) }
        PendingInlineCallbackStore.put(_partnerId.value, pending)
        schedulePendingInlineCallbackTimeout(pending)

        val botId = if (message.senderId != myUserId && message.senderId > 0) message.senderId else _partnerId.value
        socket.sendBotCallback(
            userId = myUserId,
            botId = botId,
            messageId = message.id,
            data = callbackData
        )
    }
}