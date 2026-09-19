package com.flasskdev.vibe.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flasskdev.vibe.data.*
import com.flasskdev.vibe.data.local.AppDatabase
import com.flasskdev.vibe.data.local.ChatEntity
import com.flasskdev.vibe.data.local.ChatWithUser
import com.flasskdev.vibe.data.local.MessageEntity
import com.flasskdev.vibe.data.local.UserCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val userPrefs = UserPreferences(application)

    val chats: StateFlow<List<ChatWithUser>> = chatDao.getChatsWithUserInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var webSocket: VibeWebSocket? = null

    private val _typingUsers = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val typingUsers: StateFlow<Map<Int, Boolean>> = _typingUsers.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val searchResults: StateFlow<List<UserSearchResult>> = _searchResults.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ChatUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun hasInternet(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private val wsListener = object : VibeWebSocketListener {
        override fun onAuthResponse(message: VibeMessage) {}

        override fun onChatMessage(
            senderId: Int, receiverId: Int, senderType: String, content: String,
            timestamp: String, messageId: Int, senderName: String,
            replyToId: Int?, replyToContent: String?, replyToSenderName: String?,
            forwardedFromId: Int?, forwardedFromName: String?, isEdited: Boolean,
            attachments: List<String>?,
            replyMarkup: com.flasskdev.vibe.data.local.ReplyMarkup?
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                val myId = userPrefs.userId
                val partnerId = if (senderId == myId) receiverId else senderId

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

                val tsMillis = parseTimestamp(timestamp)
                chatDao.insertMessage(
                    MessageEntity(
                        id = messageId,
                        senderId = senderId,
                        receiverId = receiverId,
                        senderType = senderType,
                        content = content,
                        timestamp = tsMillis,
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

                if (senderId != myId) {
                    val isMuted = chatDao.isUserMuted(senderId) ?: false
                    if (!isMuted) {
                        val formattedContent = com.flasskdev.vibe.utils.MessageUtils.formatMessagePreview(content, attachments)
                        com.flasskdev.vibe.utils.NotificationHelper.showMessageNotification(
                            getApplication(),
                            senderName,
                            formattedContent,
                            senderId
                        )
                    }
                }

                webSocket?.loadChats(myId)
            }
        }

        override fun onChatListUpdate(chats: List<ChatInfo>) {
            viewModelScope.launch(Dispatchers.IO) {
                syncChatsToRoom(chats)
            }
        }
        // В начале класса ChatViewModel добавь:
        private val typingJobs = mutableMapOf<Int, kotlinx.coroutines.Job>()

        override fun onMessagesLoaded(withUserId: Int, messages: List<MessageInfo>, offset: Int) {}

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

        // Обнови методы в wsListener:
        override fun onTypingIndicator(senderId: Int, senderName: String) {
            _typingUsers.value = _typingUsers.value + (senderId to true)

            // Перезапускаем таймер на 4 секунды
            typingJobs[senderId]?.cancel()
            typingJobs[senderId] = viewModelScope.launch {
                kotlinx.coroutines.delay(4000)
                _typingUsers.value = _typingUsers.value + (senderId to false)
            }
        }

        override fun onTypingStop(senderId: Int) {
            _typingUsers.value = _typingUsers.value + (senderId to false)
            typingJobs[senderId]?.cancel()
        }

        override fun onUserInfo(
        userId: Int, isOnline: Boolean, lastSeen: Long?, isDeveloper: Boolean,
        isVerified: Boolean, registerDate: Long?, isBot: Boolean, about: String?,
        username: String?, name: String?, avatarUrl: String?, lastSeenStatus: String?,
        canMessage: Boolean, isBanned: Boolean, isFreezed: Boolean,
        isBlockedByMe: Boolean, isBlockedByUser: Boolean
    ) {
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

        override fun onMessagesReadByPartner(partnerId: Int, upToMessageId: Int?) {
            viewModelScope.launch(Dispatchers.IO) {
                // В ChatViewModel:
                chatDao.markMyMessagesAsRead(userPrefs.userId, partnerId)
                webSocket?.loadChats(userPrefs.userId)
            }
        }

        override fun onUsersSearchResult(users: List<UserSearchResult>) {
            _searchResults.value = users
        }


        override fun onConnected() {
            _isConnected.value = true
            val userId = userPrefs.userId
            if (userId > 0) {
                webSocket?.authConnect(userId, deviceId = userPrefs.deviceId, deviceName = userPrefs.deviceName)
                webSocket?.loadChats(userId)
                
                viewModelScope.launch(Dispatchers.IO) {
                    val pending = chatDao.getAllPendingMessages()
                    pending.forEach { msg ->
                        webSocket?.sendMessage(msg.senderId, msg.receiverId, msg.content, msg.replyToId, msg.forwardedFromId)
                    }
                }
            }
        }

        override fun onDisconnected() {
            _isConnected.value = false
        }
        override fun onError(error: String) {
            _isConnected.value = false
        }
        override fun onMessageEdited(messageId: Int, newContent: String) {
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.updateMessageContent(messageId, newContent)
                val myId = userPrefs.userId
                webSocket?.loadChats(myId) // To update last message in chat list if needed
            }
        }
        override fun onMessagesDeleted(messageIds: List<Int>) {
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.deleteMessagesByIds(messageIds)
                val myId = userPrefs.userId
                webSocket?.loadChats(myId)
            }
        }
        override fun onAvatarUploaded(userId: Int, avatarUrl: String) {
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.updateUserAvatar(userId, avatarUrl)
                val myId = userPrefs.userId
                webSocket?.loadChats(myId)
            }
        }
        
        override fun onReportError(error: String) {
            viewModelScope.launch { _uiEvents.emit(ChatUiEvent.ToastEvent(error)) }
        }
        
        override fun onReportSuccess(messageId: Int) {
            viewModelScope.launch { _uiEvents.emit(ChatUiEvent.ToastEvent(com.flasskdev.vibe.ui.theme.VibeStringsHolder.current.reportSentToast)) }
        }
        
        override fun onSendMessageError(error: String, message: String) {
            viewModelScope.launch {
                if (error == "spamblock_active") {
                    _uiEvents.emit(ChatUiEvent.SpamblockError(message))
                } else {
                    _uiEvents.emit(ChatUiEvent.ToastEvent(error))
                }
            }
        }
    }

    fun attachWebSocket(ws: VibeWebSocket) {
        webSocket = ws
        _isConnected.value = ws.isConnected
        ws.addListener(wsListener)
        val userId = userPrefs.userId
        if (userId > 0) {
            ws.authConnect(userId, deviceId = userPrefs.deviceId, deviceName = userPrefs.deviceName)
            ws.loadChats(userId)
        }
    }

    fun detachWebSocket() {
        webSocket?.removeListener(wsListener)
        webSocket = null
    }

    fun searchUsers(query: String) {
        val userId = userPrefs.userId
        if (userId > 0) {
            webSocket?.searchUsers(query, userId)
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun reportMessage(messageId: Int, theme: String, comment: String) {
        webSocket?.reportMessage(messageId, theme, comment)
    }

    fun muteUser(mutedId: Int) {
        val userId = userPrefs.userId
        if (userId > 0) {
            webSocket?.muteUser(userId, mutedId)
            // Оптимистичное обновление UI
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.updateMuteStatus(mutedId, true)
            }
        }
    }

    fun unmuteUser(mutedId: Int) {
        val userId = userPrefs.userId
        if (userId > 0) {
            webSocket?.updateMuteStatus(userId, mutedId, false)
            // Оптимистичное обновление UI
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.updateMuteStatus(mutedId, false)
            }
        }
    }

    fun pinChat(chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = chatDao.getPinnedChatsCount()
            if (count >= MAX_PINNED_CHATS) {
                _uiEvents.emit(ChatUiEvent.ToastEvent(com.flasskdev.vibe.ui.theme.VibeStringsHolder.current.maxPinnedChatsToast(MAX_PINNED_CHATS)))
            } else {
                chatDao.updatePinnedStatus(chatId, true)
            }
        }
    }

    fun unpinChat(chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updatePinnedStatus(chatId, false)
        }
    }

    fun deleteMessages(messageIds: List<Int>, forEveryone: Boolean) {
        val userId = userPrefs.userId
        if (userId > 0) {
            webSocket?.deleteMessages(userId, messageIds, forEveryone)
            // Оптимистичное локальное удаление
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.deleteMessagesByIds(messageIds)
            }
        }
    }

    private suspend fun syncChatsToRoom(chats: List<ChatInfo>) {
        if (chats.isEmpty()) {
            chatDao.syncChats(emptyList(), emptyList())
            return
        }

        val chatEntities = mutableListOf<ChatEntity>()
        val userEntities = mutableListOf<UserCacheEntity>()

        for (chat in chats) {
            val tsMillis = parseTimestamp(chat.lastTimestamp)
            chatEntities.add(
                ChatEntity(
                    interlocutorId = chat.interlocutorId,
                    lastMessage = chat.lastMessage,
                    timestamp = tsMillis,
                    unreadCount = chat.unreadCount,
                    isLastMessageMine = chat.isLastMessageMine,
                    isLastMessageRead = chat.isLastMessageRead,
                    isDeveloper = chat.isDeveloper,
                    isVerified = chat.isVerified,
                    isMuted = chat.isMuted,
                    canMessage = chat.canMessage,
                    lastMessageAttachments = chat.lastAttachments,
                    isBanned = chat.isBanned,
                    isFreezed = chat.isFreezed,
                    isBlockedByMe = chat.isBlockedByMe,
                    isBlockedByUser = chat.isBlockedByUser
                )
            )
            userEntities.add(
                UserCacheEntity(
                    id = chat.interlocutorId,
                    name = chat.name,
                    username = chat.username.takeIf { it.isNotBlank() } ?: chat.name,
                    avatarUrl = chat.avatarUrl,
                    isOnline = chat.isOnline,
                    lastSeen = chat.lastSeen,
                    isDeveloper = chat.isDeveloper,
                    isVerified = chat.isVerified,
                    registerDate = chat.registerDate,
                    isBot = chat.isBot,
                    about = chat.about,
                    lastSeenStatus = chat.lastSeenStatus,
                    canMessage = chat.canMessage,
                    isBanned = chat.isBanned,
                    isFreezed = chat.isFreezed,
                    isBlockedByMe = chat.isBlockedByMe,
                    isBlockedByUser = chat.isBlockedByUser
                )
            )
        }
        
        // Диффом, а не DELETE-ALL + INSERT-ALL: список чатов теперь не пересобирается
        // целиком на каждое входящее сообщение и на каждый реконнект.
        chatDao.syncChats(chatEntities, userEntities)
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

    fun getMessagesWithAttachments(partnerId: Int): kotlinx.coroutines.flow.Flow<List<MessageEntity>> {
        return chatDao.getMessagesWithAttachments(userPrefs.userId, partnerId)
    }

    override fun onCleared() {
        super.onCleared()
        detachWebSocket()
    }

    companion object {
        /** Лимит закреплённых чатов. Раньше 5 было зашито и в условии, и в тексте тоста. */
        const val MAX_PINNED_CHATS = 5

        private val timestampFormatter = ThreadLocal.withInitial {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("Europe/Paris")
            }
        }
    }
}

sealed class ChatUiEvent {
    data class ToastEvent(val message: String) : ChatUiEvent()
    data class SpamblockError(val message: String) : ChatUiEvent()
}