package com.flasskdev.vibe.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import com.flasskdev.vibe.ui.theme.VibeStringsHolder

@Serializable
data class VibeMessage(
    val type: String,
    val session_token: String? = null,
    val challenge_token: String? = null,
    val requires_two_factor: Boolean = false,
    val challenge_expired: Boolean = false,
    val hint: String? = null,
    val email: String? = null,
    val username: String? = null,
    val code: String? = null,
    val nickname: String? = null,
    val user_id: Int? = null,
    val success: Boolean? = null,
    val message: String? = null,
    val email_taken: Boolean? = null,
    val username_taken: Boolean? = null,
    val chat_id: String? = null,
    val content: String? = null,
    val sender_name: String? = null,
    val timestamp: String? = null,
    val sender_id: Int? = null,
    val receiver_id: Int? = null,
    val message_id: Int? = null,
    val reply_to_id: Int? = null,
    val reply_to_content: String? = null,
    val reply_to_sender_name: String? = null,
    val forwarded_from_id: Int? = null,
    val forwarded_from_name: String? = null,
    val with_user_id: Int? = null,
    val around_message_id: Int? = null,
    val offset: Int? = null,
    val from_user_id: Int? = null,
    val up_to_message_id: Int? = null,
    val query: String? = null,
    val is_new_user: Boolean? = null,
    val theme: String? = null,
    val comment: String? = null,
    val target_user_id: Int? = null,
    val punishments: List<String>? = null,
    val times: Map<String, Long>? = null,
    val status: String? = null,
    val target_id: Int? = null,
    val is_edited: Boolean? = null,
    val fcm_token: String? = null,
    val muted_id: Int? = null,
    val message_ids: List<Int>? = null,
    val for_everyone: Boolean? = null,
    val for_both: Boolean? = null,
    val base64_data: String? = null,
    val avatar_url: String? = null,
    val device_id: String? = null,
    val device_name: String? = null,
    val os_version: String? = null,
    val from_user: Int? = null,
    val to_user: Int? = null,
    val can_message: Boolean? = null,
    val attachments: List<String>? = null,
    val emoji: String? = null,
    val reactions: List<com.flasskdev.vibe.data.local.ReactionItem>? = null,
    /** login_result: куда ушёл код — "app" (чат с Vibe cat на другом устройстве) или "email". */
    val delivery: String? = null,
    /** Язык интерфейса (RU/EN): на нём сервер пишет системные сообщения Vibe cat. */
    val lang: String? = null,
    /** two_factor_reset_result / recovery_email_result: этап (code_sent, confirmed, removed). */
    val step: String? = null,
    /** two_factor_reset_result: маскированная резервная почта, куда ушёл код сброса. */
    val email_masked: String? = null,
    /** Машиночитаемый код ошибки (rate_limited, no_recovery_email, wrong_password …). */
    val error: String? = null
)

// ОНБОВЛЕННАЯ МОДЕЛЬ
data class ChatInfo(
    val interlocutorId: Int,
    val name: String,
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int,
    val isLastMessageMine: Boolean,
    val isLastMessageRead: Boolean,
    val isOnline: Boolean,
    val lastSeen: Long?,
    val isDeveloper: Boolean,
    val isVerified: Boolean,
    val username: String,
    val avatarUrl: String?,
    val registerDate: Long?,
    val isBot: Boolean,
    val about: String?,
    val lastSeenStatus: String?,
    val isMuted: Boolean = false,
    val canMessage: Boolean = true,
    val lastAttachments: List<String>? = null,
    val isBanned: Boolean = false,
    val isFreezed: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isBlockedByUser: Boolean = false
)

data class MessageInfo(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val senderType: String = "user", // "user" или "bot"
    val content: String,
    val timestamp: String,
    val isRead: Boolean,
    val replyToId: Int? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val forwardedFromId: Int? = null,
    val forwardedFromName: String? = null,
    val isEdited: Boolean = false,
    val attachments: List<String>? = null,
    val reactions: List<com.flasskdev.vibe.data.local.ReactionItem>? = null,
    val isBanned: Boolean,
    val isFreezed: Boolean,
    val replyMarkup: com.flasskdev.vibe.data.local.ReplyMarkup? = null
)

data class UserSearchResult(
    val id: Int,
    val name: String?,
    val username: String?,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val isDeveloper: Boolean = false,
    val isBot: Boolean = false,
    val isBanned: Boolean = false,
    val isFreezed: Boolean = false
)

data class BlockedUserItem(
    val id: Int,
    val name: String?,
    val username: String?,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val isDeveloper: Boolean = false,
    val isBot: Boolean = false,
    val isFreezed: Boolean = false,
    val isBanned: Boolean = false
)

data class ReactionUserDetail(
    val userId: Int,
    val name: String?,
    val username: String?,
    val avatarUrl: String?,
    val emoji: String,
    val timestamp: Long,
    val isVerified: Boolean = false,
    val isDeveloper: Boolean = false,
    val isBot: Boolean = false
)

interface VibeWebSocketListener {
    fun onAuthResponse(message: VibeMessage) {}
    fun onSettingsResponse(message: JSONObject) {}
    fun onChatMessage(senderId: Int, receiverId: Int, senderType: String, content: String, timestamp: String, messageId: Int, senderName: String, replyToId: Int?, replyToContent: String?, replyToSenderName: String?, forwardedFromId: Int?, forwardedFromName: String?, isEdited: Boolean = false, attachments: List<String>? = null, replyMarkup: com.flasskdev.vibe.data.local.ReplyMarkup? = null) {}
    fun onChatListUpdate(chats: List<ChatInfo>) {}
    fun onMessagesLoaded(withUserId: Int, messages: List<MessageInfo>, offset: Int) {}
    fun onMessagesLoadedAround(withUserId: Int, messages: List<MessageInfo>) {}
    fun onTypingIndicator(senderId: Int, senderName: String) {}
    fun onTypingStop(senderId: Int) {}
    fun onUsersSearchResult(users: List<UserSearchResult>) {}
    fun onMessagesReadByPartner(partnerId: Int, upToMessageId: Int? = null) {}
    fun onMarkReadResult(fromUserId: Int, upToMessageId: Int? = null) {}
    fun onConnected() {}
    fun onDisconnected() {}
    fun onError(error: String) {}
    fun onBotCallbackAnswer(callbackId: Int?, text: String?, showAlert: Boolean) {}
    fun onBotCallbackError(error: String, message: String) {}
    fun onBotCallbackResult(callbackId: Int?, messageId: Int, data: String) {}
    fun onUserInfo(userId: Int, isOnline: Boolean, lastSeen: Long?, isDeveloper: Boolean, isVerified: Boolean, registerDate: Long?, isBot: Boolean, about: String?, username: String?, name: String?, avatarUrl: String?, lastSeenStatus: String? = null, canMessage: Boolean = true, isBanned: Boolean = false, isFreezed: Boolean = false, isBlockedByMe: Boolean = false, isBlockedByUser: Boolean = false) {}
    fun onBlockUserSuccess(blockedId: Int) {}
    fun onUnblockUserSuccess(blockedId: Int) {}
    fun onBlockedUsersResult(users: List<BlockedUserItem>, totalCount: Int, page: Int, hasMore: Boolean) {}
    fun onBlockedCountResult(count: Int) {}
    fun onMessageEdited(messageId: Int, newContent: String) {}
    fun onMessagesDeleted(messageIds: List<Int>) {}
    fun onMessagePinned(messageId: Int, withUserId: Int) {}
    fun onMessageUnpinned(messageId: Int, withUserId: Int) {}
    fun onAllMessagesUnpinned(withUserId: Int) {}
    fun onPinnedMessagesLoaded(withUserId: Int, messageIds: List<Int>) {}
    fun onAvatarUploaded(userId: Int, avatarUrl: String) {}
    fun onSessionsResult(sessions: JSONArray) {}
    fun onSessionTerminated(deviceId: String) {}
    fun onForceLogout(reason: String) {}
    fun onPrivacySettingsResult(settings: JSONObject) {}
    fun onReportError(error: String) {}
    fun onReportSuccess(messageId: Int) {}
    fun onSendMessageError(error: String, message: String) {}
    fun onMessageReaction(messageId: Int, userId: Int, emoji: String, reactions: List<com.flasskdev.vibe.data.local.ReactionItem>) {}
    fun onReactionUsersResult(messageId: Int, emoji: String?, offset: Int, hasMore: Boolean, users: List<ReactionUserDetail>) {}
    fun onStickerPacksResult(packsJson: JSONArray) {}
    fun onStickerPacksSearchResult(query: String, packsJson: JSONArray) {}
    fun onStickerPackAdded(packId: Int) {}
    fun onStickerPackRemoved(packId: Int) {}
    fun onStickerPackError(error: String, message: String) {}
}

class VibeWebSocket {
    private var preferences: UserPreferences? = null
    @Volatile private var authenticated = false
    private var lastAuth: VibeMessage? = null
    private var heartbeat: Job? = null
    fun configure(preferences: UserPreferences) { this.preferences = preferences }
    private val publicTypes = setOf("auth_connect", "register", "login", "check_availability", "verify_code", "verify_two_factor", "request_two_factor_reset", "confirm_two_factor_reset")
    private fun flushPending() {
        val typed = synchronized(pendingMessages) { pendingMessages.toList().also { pendingMessages.clear() } }
        typed.forEach(::sendJson)
        val raw = synchronized(pendingRawMessages) { pendingRawMessages.toList().also { pendingRawMessages.clear() } }
        raw.forEach(::sendRawJson)
    }
    fun verifyTwoFactor(challenge: String, password: String) {
        sendRawJson(JSONObject().put("type", "verify_two_factor").put("challenge_token", challenge).put("password", password).toString())
    }

    /* ---------------- Резервная почта и сброс второго фактора ---------------- */

    /**
     * set_recovery_email / request: сервер шлёт 6-значный код на [email].
     * При включённой защите сервер требует её пароль — иначе подмена адреса
     * позволила бы снять защиту «сбросом» с разблокированного телефона.
     */
    fun requestRecoveryEmail(email: String, currentPassword: String = "") {
        sendRawJson(
            JSONObject()
                .put("type", "set_recovery_email")
                .put("operation", "request")
                .put("email", email)
                .put("current_password", currentPassword)
                .toString()
        )
    }

    /** set_recovery_email / confirm: код из письма подтверждает адрес. */
    fun confirmRecoveryEmail(code: String) {
        sendRawJson(
            JSONObject()
                .put("type", "set_recovery_email")
                .put("operation", "confirm")
                .put("code", code)
                .toString()
        )
    }

    /** set_recovery_email / remove. */
    fun removeRecoveryEmail(currentPassword: String = "") {
        sendRawJson(
            JSONObject()
                .put("type", "set_recovery_email")
                .put("operation", "remove")
                .put("current_password", currentPassword)
                .toString()
        )
    }

    /**
     * request_two_factor_reset. С экрана входа передаём [challenge] (сессии ещё нет),
     * из настроек — null: там пользователя опознаёт текущая сессия.
     */
    fun requestTwoFactorReset(challenge: String? = null) {
        val json = JSONObject().put("type", "request_two_factor_reset")
        challenge?.let { json.put("challenge_token", it) }
        sendRawJson(json.toString())
    }

    /**
     * confirm_two_factor_reset: верный код отключает защиту. С экрана входа сервер
     * сразу отвечает verify_code_result с сессией, из настроек — two_factor_result.
     */
    fun confirmTwoFactorReset(code: String, challenge: String? = null) {
        val json = JSONObject().put("type", "confirm_two_factor_reset").put("code", code)
        challenge?.let { json.put("challenge_token", it) }
        sendRawJson(json.toString())
    }

    /** Токен, который мы предъявляем в auth_connect, совпадает с актуальным в prefs. */
    private fun presentsCurrentToken(): Boolean {
        val presented = lastAuth?.session_token ?: return false
        val current = preferences?.sessionToken ?: return false
        return presented == current
    }

    /**
     * force_logout исполняем, только если он действительно про нас:
     *  - бан / заморозка аккаунта — всегда;
     *  - соединение уже авторизовано — сервер отзывает именно эту сессию;
     *  - не авторизовано, но отвергнут тот самый токен, который мы сейчас предъявляем.
     * Всё остальное (старый токен, случайный кадр до авторизации) — шум, от которого
     * раньше свежий логин тут же стирался.
     */
    private fun shouldHonorForceLogout(reason: String): Boolean {
        if (reason == "banned" || reason == "freezed") return true
        if (authenticated) return true
        return presentsCurrentToken()
    }
    fun logout() {
        com.flasskdev.vibe.utils.NotificationHelper.resetForLogout()
        if (authenticated) webSocket?.send("{\"type\":\"logout\"}")
        disconnect()
        lastAuth = null
        preferences?.logout()
        synchronized(pendingMessages) { pendingMessages.clear() }
        synchronized(pendingRawMessages) { pendingRawMessages.clear() }
    }
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val TAG = "VibeWS"

    private val _isActuallyConnected = AtomicBoolean(false)
    val isConnected: Boolean get() = _isActuallyConnected.get()


    private val listeners = CopyOnWriteArrayList<VibeWebSocketListener>()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var isConnecting = false
    private var reconnectJob: Job? = null
    private var scopeJob = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.IO + scopeJob)
    private val pendingMessages = mutableListOf<VibeMessage>()
    private val pendingRawMessages = mutableListOf<String>()


    fun addListener(listener: VibeWebSocketListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: VibeWebSocketListener) {
        listeners.remove(listener)
    }

    fun connect() {
        if (isConnecting || isConnected) return
        isConnecting = true

        // Recreate scope if it was cancelled
        if (scopeJob.isCancelled) {
            scopeJob = SupervisorJob()
            scope = CoroutineScope(Dispatchers.IO + scopeJob)
        }

        val request = Request.Builder()
            .url("wss://flasskdev.alwaysdata.net/wss")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                if (webSocket != ws) return
                Log.d(TAG, "CONNECTED")
                _isActuallyConnected.set(true)
                isConnecting = false
                reconnectJob?.cancel()
                authenticated = false
                lastAuth?.let { sendJson(it) }
                listeners.forEach { it.onConnected() }
                flushPending()

            }

            override fun onMessage(ws: WebSocket, text: String) {
                if (webSocket != ws) return
                // Do not log message payloads: they can contain private chat content and credentials.
                Log.d(TAG, "INCOMING websocket frame")
                try {
                    val obj = JSONObject(text)
                    val type = obj.optString("type", "")

                    if (type == "verify_code_result" && obj.optBoolean("success")) {
                        val token = obj.optString("session_token")
                        if (token.isBlank()) throw IllegalStateException("Server update required")
                        preferences?.sessionToken = token
                    }
                    if (type == "verify_code_result" && obj.optBoolean("requires_two_factor")) {
                        // Сервер спрашивает второй фактор только если он включён. Отражаем это
                        // локально сразу: logout() стирает префы, и иначе «Приватность» показывала
                        // «Выключена» до первого открытия экрана 2FA.
                        preferences?.let {
                            it.twoFactorEnabled = true
                            it.twoFactorHint = obj.optString("hint").takeUnless { h -> h == "null" || h.isBlank() }
                        }
                    }
                    when (type) {
                        "auth_connect_result" -> {
                            if (obj.optBoolean("success")) {
                                authenticated = true
                                heartbeat?.cancel()
                                heartbeat = scope.launch {
                                    while (isActive && authenticated) { delay(15_000); sendRawJson("{\"type\":\"session_ping\"}") }
                                }
                                flushPending()
                                preferences?.let {
                                    getUserInfo(it.userId)
                                    sendRawJson("{\"type\":\"get_notification_settings\"}")
                                    // Состояние 2FA живёт на сервере; после входа подтягиваем его сразу,
                                    // а не ждём, пока пользователь зайдёт в настройки безопасности.
                                    sendRawJson("{\"type\":\"get_two_factor\"}")
                                }
                            } else {
                                when (obj.optString("error")) {
                                    // Анти-флуд на сервере: повторяем auth_connect позже, не разлогиниваясь.
                                    "rate_limited" -> {
                                        val retry = obj.optInt("retry_after", 5).coerceIn(1, 900)
                                        Log.w(TAG, "auth_connect rate limited, retrying in ${retry}s")
                                        scope.launch {
                                            delay(retry * 1000L)
                                            if (isConnected && !authenticated) lastAuth?.let(::sendJson)
                                        }
                                    }
                                    "session_invalid" -> {
                                        if (presentsCurrentToken()) {
                                            logout()
                                            listeners.forEach { it.onForceLogout("session_invalid") }
                                        } else {
                                            Log.w(TAG, "auth_connect rejected for a stale token; ignoring")
                                        }
                                    }
                                    else -> Log.w(TAG, "auth_connect failed: ${obj.optString("error")}")
                                }
                            }
                        }
                        "auth_required" -> {
                            // Команда добралась до сервера раньше auth_connect. Очередь не трогаем,
                            // просто повторяем авторизацию — очередь уйдёт после auth_connect_result.
                            authenticated = false
                            lastAuth?.let(::sendJson)
                        }
                        "session_pong" -> Unit
                        "notification_settings_result" -> {
                            if (obj.optBoolean("success")) preferences?.let {
                                it.notificationMuteAll = obj.optBoolean("mute_all")
                                it.autoMuteNewChats = obj.optBoolean("auto_mute_new")
                            }
                            listeners.forEach { it.onSettingsResponse(obj) }
                        }
                        "two_factor_result" -> {
                            if (obj.optBoolean("success")) preferences?.let { p ->
                                p.twoFactorEnabled = obj.optBoolean("enabled")
                                p.twoFactorHint = obj.optString("hint").takeUnless { h -> h == "null" || h.isBlank() }
                                p.twoFactorPassword = null
                                // settings() и сброс через резервную почту всегда возвращают адрес.
                                if (obj.has("recovery_email")) {
                                    p.twoFactorRecoveryEmail = obj.optString("recovery_email")
                                        .takeUnless { e -> e == "null" || e.isBlank() }
                                }
                            }
                            listeners.forEach { it.onSettingsResponse(obj) }
                        }
                        "recovery_email_result" -> {
                            if (obj.optBoolean("success") && obj.has("recovery_email")) {
                                preferences?.twoFactorRecoveryEmail = obj.optString("recovery_email")
                                    .takeUnless { e -> e == "null" || e.isBlank() }
                            }
                            listeners.forEach { it.onSettingsResponse(obj) }
                        }
                        // code_sent / ошибки сброса. Успех приходит как two_factor_result (настройки)
                        // или verify_code_result (экран входа) и обрабатывается выше.
                        "two_factor_reset_result" -> listeners.forEach { it.onSettingsResponse(obj) }
                        "chat_message" -> {
                            val senderId = obj.optInt("sender_id", 0)
                            preferences?.let { prefs ->
                                if (obj.optInt("receiver_id") == prefs.userId) {
                                    prefs.mutedNotificationPeers = if (obj.optBoolean("suppress_notification")) prefs.mutedNotificationPeers + senderId.toString() else prefs.mutedNotificationPeers - senderId.toString()
                                }
                            }
                            val receiverId = obj.optInt("receiver_id", 0)
                            val senderType = obj.optString("sender_type", "user")
                            val content = obj.optString("content", "")
                            val timestamp = obj.optString("timestamp", "")
                            val messageId = obj.optInt("message_id", 0)
                            val senderName = obj.optString("sender_name", "")
                            val replyToId = if (obj.has("reply_to_id") && !obj.isNull("reply_to_id")) obj.optInt("reply_to_id") else null
                            val replyToContent = if (obj.has("reply_to_content") && !obj.isNull("reply_to_content")) obj.optString("reply_to_content") else null
                            val replyToSenderName = if (obj.has("reply_to_sender_name") && !obj.isNull("reply_to_sender_name")) obj.optString("reply_to_sender_name") else null
                            val forwardedFromId = if (obj.has("forwarded_from_id") && !obj.isNull("forwarded_from_id")) obj.optInt("forwarded_from_id") else null
                            val forwardedFromName = if (obj.has("forwarded_from_name") && !obj.isNull("forwarded_from_name")) obj.optString("forwarded_from_name") else null
                            val isEdited = obj.optBoolean("is_edited", false)
                            val isFreezed = obj.optBoolean("is_edited", false)
                            
                            val attachmentsList = mutableListOf<String>()
                            if (obj.has("attachments") && !obj.isNull("attachments")) {
                                val atts = obj.optJSONArray("attachments")
                                if (atts != null) {
                                    for (i in 0 until atts.length()) {
                                        attachmentsList.add(atts.getString(i))
                                    }
                                }
                            }
                            val attachments = if (attachmentsList.isEmpty()) null else attachmentsList
                            val replyMarkup = parseReplyMarkup(if (obj.has("reply_markup")) obj.opt("reply_markup") else obj.opt("replyMarkup"))
                            
                            listeners.forEach { it.onChatMessage(senderId, receiverId, senderType, content, timestamp, messageId, senderName, replyToId, replyToContent, replyToSenderName, forwardedFromId, forwardedFromName, isEdited, attachments, replyMarkup) }
                        }
                        "message_edited" -> {
                            val messageId = obj.optInt("message_id", 0)
                            val content = obj.optString("content", "")
                            listeners.forEach { it.onMessageEdited(messageId, content) }
                        }
                        "messages_deleted" -> {
                            val msgIdsArray = obj.optJSONArray("message_ids") ?: JSONArray()
                            val msgIds = mutableListOf<Int>()
                            for (i in 0 until msgIdsArray.length()) {
                                msgIds.add(msgIdsArray.getInt(i))
                            }
                            listeners.forEach { it.onMessagesDeleted(msgIds) }
                        }
                        "message_pinned" -> {
                            val messageId = obj.optInt("message_id", 0)
                            val withUserId = obj.optInt("with_user_id", 0)
                            listeners.forEach { it.onMessagePinned(messageId, withUserId) }
                        }
                        "message_unpinned" -> {
                            val messageId = obj.optInt("message_id", 0)
                            val withUserId = obj.optInt("with_user_id", 0)
                            listeners.forEach { it.onMessageUnpinned(messageId, withUserId) }
                        }
                        "all_messages_unpinned" -> {
                            val withUserId = obj.optInt("with_user_id", 0)
                            listeners.forEach { it.onAllMessagesUnpinned(withUserId) }
                        }
                        "pinned_messages_loaded" -> {
                            val withUserId = obj.optInt("with_user_id", 0)
                            val idsArray = obj.optJSONArray("pinned_message_ids") ?: JSONArray()
                            val ids = mutableListOf<Int>()
                            for (i in 0 until idsArray.length()) {
                                ids.add(idsArray.getInt(i))
                            }
                            listeners.forEach { it.onPinnedMessagesLoaded(withUserId, ids) }
                        }
                        "chat_list_update", "load_chats_result" -> {
                            val chatsArray = obj.optJSONArray("chats") ?: JSONArray()
                            val chats = parseChatList(chatsArray)
                            listeners.forEach { it.onChatListUpdate(chats) }
                        }
                        "load_messages_result", "load_messages_around_result" -> {
                            val withUserId = obj.optInt("with_user_id", 0)
                            val offset = obj.optInt("offset", 0)
                            val messagesArray = obj.optJSONArray("messages") ?: JSONArray()
                            val messages = parseMessageList(messagesArray)
                            if (type == "load_messages_around_result") {
                                listeners.forEach { it.onMessagesLoadedAround(withUserId, messages) }
                            } else {
                                listeners.forEach { it.onMessagesLoaded(withUserId, messages, offset) }
                            }
                        }
                        "typing_indicator" -> {
                            val senderId = obj.optInt("sender_id", 0)
                            val senderName = obj.optString("sender_name", "")
                            listeners.forEach { it.onTypingIndicator(senderId, senderName) }
                        }
                        "typing_indicator_stop" -> {
                            val senderId = obj.optInt("sender_id", 0)
                            listeners.forEach { it.onTypingStop(senderId) }
                        }
                        "search_users_result" -> {
                            val usersArray = obj.optJSONArray("users") ?: JSONArray()
                            val users = parseUserList(usersArray)
                            listeners.forEach { it.onUsersSearchResult(users) }
                        }
                        "messages_read_by_partner" -> {
                            val partnerId = obj.optInt("partner_id", 0)
                            val upToMessageId = if (obj.has("up_to_message_id") && !obj.isNull("up_to_message_id")) obj.optInt("up_to_message_id") else null
                            listeners.forEach { it.onMessagesReadByPartner(partnerId, upToMessageId) }
                        }
                        "mark_read_result" -> {
                            val success = obj.optBoolean("success", false)
                            val fromUserId = obj.optInt("from_user_id", 0)
                            val upToMessageId = if (obj.has("up_to_message_id") && !obj.isNull("up_to_message_id")) obj.optInt("up_to_message_id") else null
                            if (success) {
                                listeners.forEach { it.onMarkReadResult(fromUserId, upToMessageId) }
                            }
                        }
                        "user_info_result" -> {
                            val uid = obj.optInt("user_id", 0)
                            val isOnline = obj.optBoolean("is_online", false)
                            val lastSeen = if (obj.has("last_seen") && !obj.isNull("last_seen")) obj.optLong("last_seen") else null
                            val isDeveloper = obj.optBoolean("is_developer", false) || obj.optInt("is_developer", 0) == 1
                            val isVerified = obj.optBoolean("is_verified", false) || obj.optInt("is_verified", 0) == 1
                            val registerDate = if (obj.has("register_date") && !obj.isNull("register_date")) obj.optLong("register_date") else null
                            val isBot = obj.optBoolean("is_bot", false)
                            val about = if (obj.has("about") && !obj.isNull("about")) obj.optString("about") else if (obj.has("content") && !obj.isNull("content")) obj.optString("content") else null

                            val username = if (obj.has("username") && !obj.isNull("username")) obj.optString("username") else null
                            val name = if (obj.has("name") && !obj.isNull("name")) obj.optString("name") else null
                            val avatarUrl = if (obj.has("avatar_url") && !obj.isNull("avatar_url")) obj.optString("avatar_url") else null
                            val lastSeenStatus = if (obj.has("last_seen_status") && !obj.isNull("last_seen_status")) obj.optString("last_seen_status") else null
                            val canMessage = obj.optBoolean("can_message", true)
                            val isBanned = obj.optBoolean("is_banned", false)
                            val isFreezed = obj.optBoolean("is_freezed", false)
                            val isBlockedByMe = obj.optBoolean("is_blocked_by_me", false) || obj.optInt("is_blocked_by_me", 0) == 1
                            val isBlockedByUser = obj.optBoolean("is_blocked_by_user", false) || obj.optInt("is_blocked_by_user", 0) == 1
                            listeners.forEach { it.onUserInfo(uid, isOnline, lastSeen, isDeveloper, isVerified, registerDate, isBot, about, username, name, avatarUrl, lastSeenStatus, canMessage, isBanned, isFreezed, isBlockedByMe, isBlockedByUser) }
                        }
                        "block_user_success" -> {
                            val blockedId = obj.optInt("blocked_id", 0)
                            listeners.forEach { it.onBlockUserSuccess(blockedId) }
                        }
                        "unblock_user_success" -> {
                            val blockedId = obj.optInt("blocked_id", 0)
                            listeners.forEach { it.onUnblockUserSuccess(blockedId) }
                        }
                        "blocked_users_result" -> {
                            val usersArr = obj.optJSONArray("users") ?: JSONArray()
                            val totalCount = obj.optInt("total_count", 0)
                            val page = obj.optInt("page", 1)
                            val hasMore = obj.optBoolean("has_more", false)
                            val list = mutableListOf<BlockedUserItem>()
                            for (i in 0 until usersArr.length()) {
                                val u = usersArr.getJSONObject(i)
                                list.add(
                                    BlockedUserItem(
                                        id = u.getInt("id"),
                                        name = if (u.has("name") && !u.isNull("name")) u.optString("name") else null,
                                        username = if (u.has("username") && !u.isNull("username")) u.optString("username") else null,
                                        avatarUrl = if (u.has("avatar_url") && !u.isNull("avatar_url")) u.optString("avatar_url") else null,
                                        isVerified = u.optBoolean("is_verified", false),
                                        isDeveloper = u.optBoolean("is_developer", false),
                                        isBot = u.optBoolean("is_bot", false),
                                        isFreezed = u.optBoolean("is_freezed", false),
                                        isBanned = u.optBoolean("is_banned", false)
                                    )
                                )
                            }
                            listeners.forEach { it.onBlockedUsersResult(list, totalCount, page, hasMore) }
                        }
                        "blocked_count_result" -> {
                            val count = obj.optInt("count", 0)
                            listeners.forEach { it.onBlockedCountResult(count) }
                        }
                        "sticker_packs_result" -> {
                            val packs = obj.optJSONArray("packs") ?: JSONArray()
                            listeners.forEach { it.onStickerPacksResult(packs) }
                        }
                        "sticker_packs_search_result" -> {
                            val query = obj.optString("query", "")
                            val packs = obj.optJSONArray("packs") ?: JSONArray()
                            listeners.forEach { it.onStickerPacksSearchResult(query, packs) }
                        }
                        "sticker_pack_added" -> {
                            val packId = obj.optInt("pack_id", 0)
                            listeners.forEach { it.onStickerPackAdded(packId) }
                        }
                        "sticker_pack_removed" -> {
                            val packId = obj.optInt("pack_id", 0)
                            listeners.forEach { it.onStickerPackRemoved(packId) }
                        }
                        "sticker_pack_error" -> {
                            val error = obj.optString("error", "")
                            val message = obj.optString("message", "")
                            listeners.forEach { it.onStickerPackError(error, message) }
                        }
                        "avatar_uploaded" -> {
                            val uid = obj.optInt("user_id", 0)
                            val url = obj.optString("avatar_url", "")
                            listeners.forEach { it.onAvatarUploaded(uid, url) }
                        }
                        "sessions_result" -> {
                            val sessionsArray = obj.optJSONArray("sessions") ?: JSONArray()
                            listeners.forEach { it.onSessionsResult(sessionsArray) }
                        }
                        "session_terminated" -> {
                            val deviceId = obj.optString("device_id", "")
                            listeners.forEach { it.onSessionTerminated(deviceId) }
                        }
                        "report_error" -> {
                            val msg = obj.optString("message", "Error")
                            listeners.forEach { it.onReportError(msg) }
                        }
                        "report_success" -> {
                            val msgId = obj.optInt("message_id", 0)
                            listeners.forEach { it.onReportSuccess(msgId) }
                        }
                        "send_message_error" -> {
                            val error = obj.optString("error", "")
                            val msg = obj.optString("message", "")
                            listeners.forEach { it.onSendMessageError(error, msg) }
                        }
                        "force_logout" -> {
                            val reason = obj.optString("reason", "")
                            if (shouldHonorForceLogout(reason)) {
                                logout()
                                listeners.forEach { it.onForceLogout(reason) }
                            } else {
                                // Сервер отверг не нашу текущую сессию (запоздавший ответ на старый токен
                                // или кадр, ушедший раньше auth_connect). Стирать креды из-за этого нельзя.
                                Log.w(TAG, "Ignoring force_logout($reason): not our active session")
                            }
                        }

                        "privacy_settings_result" -> {
                            val settings = obj.optJSONObject("settings") ?: JSONObject()
                            listeners.forEach { it.onPrivacySettingsResult(settings) }
                        }

                        "message_reaction" -> {
                            val msgId = obj.optInt("message_id", 0)
                            val uid = obj.optInt("user_id", 0)
                            val emoji = obj.optString("emoji", "")
                            val rArr = obj.optJSONArray("reactions")
                            val rList = mutableListOf<com.flasskdev.vibe.data.local.ReactionItem>()
                            if (rArr != null) {
                                for (k in 0 until rArr.length()) {
                                    val rObj = rArr.getJSONObject(k)
                                    val uArr = rObj.optJSONArray("userIds")
                                    val uList = mutableListOf<Int>()
                                    if (uArr != null) {
                                        for (u in 0 until uArr.length()) {
                                            uList.add(uArr.getInt(u))
                                        }
                                    }
                                    val usersArr = rObj.optJSONArray("users")
                                    val rUsers = mutableListOf<com.flasskdev.vibe.data.local.ReactionUser>()
                                    if (usersArr != null) {
                                        for (u in 0 until usersArr.length()) {
                                            val uo = usersArr.getJSONObject(u)
                                            rUsers.add(com.flasskdev.vibe.data.local.ReactionUser(
                                                userId = uo.optInt("userId", uo.optInt("user_id", 0)),
                                                timestamp = uo.optLong("timestamp", System.currentTimeMillis())
                                            ))
                                        }
                                    }
                                    rList.add(com.flasskdev.vibe.data.local.ReactionItem(
                                        emoji = rObj.optString("emoji", ""),
                                        count = rObj.optInt("count", uList.size),
                                        userIds = uList,
                                        users = rUsers
                                    ))
                                }
                            }
                            listeners.forEach { it.onMessageReaction(msgId, uid, emoji, rList) }
                        }

                        "reaction_users_result" -> {
                            val msgId = obj.optInt("message_id", 0)
                            val emoji = if (obj.has("emoji") && !obj.isNull("emoji")) obj.optString("emoji") else null
                            val offset = obj.optInt("offset", 0)
                            val hasMore = obj.optBoolean("has_more", false)
                            val uArr = obj.optJSONArray("users") ?: JSONArray()
                            val uList = mutableListOf<ReactionUserDetail>()
                            for (i in 0 until uArr.length()) {
                                val uObj = uArr.getJSONObject(i)
                                uList.add(ReactionUserDetail(
                                    userId = uObj.optInt("userId", 0),
                                    name = if (uObj.has("name") && !uObj.isNull("name")) uObj.optString("name") else null,
                                    username = if (uObj.has("username") && !uObj.isNull("username")) uObj.optString("username") else null,
                                    avatarUrl = if (uObj.has("avatarUrl") && !uObj.isNull("avatarUrl")) uObj.optString("avatarUrl") else null,
                                    emoji = uObj.optString("emoji", ""),
                                    timestamp = uObj.optLong("timestamp", 0L),
                                    isVerified = uObj.optBoolean("isVerified", false),
                                    isDeveloper = uObj.optBoolean("isDeveloper", false),
                                    isBot = uObj.optBoolean("isBot", false)
                                ))
                            }
                            listeners.forEach { it.onReactionUsersResult(msgId, emoji, offset, hasMore, uList) }
                        }

                        "bot_callback_answer" -> {
                            val callbackId = if (obj.has("callback_id") && !obj.isNull("callback_id")) obj.optInt("callback_id") else null
                            val text = if (obj.has("text") && !obj.isNull("text")) obj.optString("text") else null
                            val showAlert = obj.optBoolean("show_alert", false)
                            listeners.forEach { it.onBotCallbackAnswer(callbackId, text, showAlert) }
                        }

                        "bot_callback_error" -> {
                            val error = obj.optString("error", "error")
                            val message = obj.optString("message", VibeStringsHolder.current.errorGeneric)
                            listeners.forEach { it.onBotCallbackError(error, message) }
                        }

                        "bot_callback_result" -> {
                            val callbackId = if (obj.has("callback_query_id") && !obj.isNull("callback_query_id")) obj.optInt("callback_query_id") else null
                            val messageId = obj.optInt("message_id", 0)
                            val data = obj.optString("data", "")
                            listeners.forEach { it.onBotCallbackResult(callbackId, messageId, data) }
                        }

                        else -> {
                            val msg = json.decodeFromString<VibeMessage>(text)
                            listeners.forEach { it.onAuthResponse(msg) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "JSON Error: ${e.message}")
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                if (webSocket == ws) {
                    handleDisconnect()
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                if (webSocket == ws) {
                    handleDisconnect()
                }
            }
        })
    }

    private fun parseChatList(array: JSONArray): List<ChatInfo> {
        val result = mutableListOf<ChatInfo>()
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                result.add(ChatInfo(
                    interlocutorId = obj.optInt("interlocutor_id", 0),
                    name = obj.optString("name", ""),
                    lastMessage = obj.optString("last_message", ""),
                    lastTimestamp = obj.optString("last_timestamp", ""),
                    unreadCount = obj.optInt("unread_count", 0),
                    isLastMessageMine = obj.optInt("is_last_message_mine", 0) == 1,
                    isLastMessageRead = obj.optInt("is_last_message_read", 0) == 1,
                    isOnline = obj.optBoolean("is_online", false) || obj.optInt("is_online", 0) == 1,
                    lastSeen = if (obj.has("last_seen") && !obj.isNull("last_seen")) obj.optLong("last_seen") else null,
                    isDeveloper = obj.optBoolean("is_developer", false) || obj.optInt("is_developer", 0) == 1,
                    isVerified = obj.optBoolean("is_verified", false) || obj.optInt("is_verified", 0) == 1,
                    username = obj.optString("username", ""),
                    avatarUrl = if (obj.has("avatar_url") && !obj.isNull("avatar_url")) obj.optString("avatar_url") else null,
                    registerDate = if (obj.has("register_date") && !obj.isNull("register_date")) obj.optLong("register_date") else null,
                    isBot = obj.optBoolean("is_bot", false),
                    about = if (obj.has("about") && !obj.isNull("about")) obj.optString("about") else null,
                    lastSeenStatus = if (obj.has("last_seen_status") && !obj.isNull("last_seen_status")) obj.optString("last_seen_status") else null,
                    isMuted = obj.optBoolean("is_muted", false) || obj.optInt("is_muted", 0) == 1,
                    canMessage = if (obj.has("can_message") && !obj.isNull("can_message")) obj.optBoolean("can_message", true) else true,
                    lastAttachments = if (!obj.isNull("last_attachments")) {
                        val atts = obj.optJSONArray("last_attachments")
                        val list = mutableListOf<String>()
                        if (atts != null) {
                            for (j in 0 until atts.length()) {
                                list.add(atts.getString(j))
                            }
                        }
                        if (list.isEmpty()) null else list
                    } else null,
                    isBanned = obj.optBoolean("is_banned", false) || obj.optInt("is_banned", 0) == 1,
                    isFreezed = obj.optBoolean("is_freezed", false) || obj.optInt("is_freezed", 0) == 1,
                    isBlockedByMe = obj.optBoolean("is_blocked_by_me", false) || obj.optInt("is_blocked_by_me", 0) == 1,
                    isBlockedByUser = obj.optBoolean("is_blocked_by_user", false) || obj.optInt("is_blocked_by_user", 0) == 1
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Parse chat error: ${e.message}")
            }
        }
        return result
    }

    private fun parseMessageList(array: JSONArray): List<MessageInfo> {
        val result = mutableListOf<MessageInfo>()
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                result.add(MessageInfo(
                    id = obj.optInt("id", 0),
                    senderId = obj.optInt("sender_id", 0),
                    receiverId = obj.optInt("receiver_id", 0),
                    senderType = obj.optString("sender_type", "user"),
                    content = obj.optString("content", ""),
                    timestamp = obj.optString("timestamp", ""),
                    isRead = obj.optBoolean("is_read", false),
                    replyToId = if (!obj.isNull("reply_to_id")) obj.optInt("reply_to_id") else null,
                    replyToContent = if (!obj.isNull("reply_to_content")) obj.optString("reply_to_content") else null,
                    replyToSenderName = if (!obj.isNull("reply_to_sender_name")) obj.optString("reply_to_sender_name") else null,
                    forwardedFromId = if (!obj.isNull("forwarded_from_id")) obj.optInt("forwarded_from_id") else null,
                    forwardedFromName = if (!obj.isNull("forwarded_from_name")) obj.optString("forwarded_from_name") else null,
                    isEdited = obj.optBoolean("is_edited", false),
                    attachments = if (!obj.isNull("attachments")) {
                        val atts = obj.optJSONArray("attachments")
                        val list = mutableListOf<String>()
                        if (atts != null) {
                            for (j in 0 until atts.length()) {
                                list.add(atts.getString(j))
                            }
                        }
                        if (list.isEmpty()) null else list
                    } else null,
                    reactions = if (!obj.isNull("reactions")) {
                        val rArr = obj.optJSONArray("reactions")
                        val rList = mutableListOf<com.flasskdev.vibe.data.local.ReactionItem>()
                        if (rArr != null) {
                            for (k in 0 until rArr.length()) {
                                val rObj = rArr.getJSONObject(k)
                                val uArr = rObj.optJSONArray("userIds")
                                val uList = mutableListOf<Int>()
                                if (uArr != null) {
                                    for (u in 0 until uArr.length()) {
                                        uList.add(uArr.getInt(u))
                                    }
                                }
                                val usersArr = rObj.optJSONArray("users")
                                val rUsers = mutableListOf<com.flasskdev.vibe.data.local.ReactionUser>()
                                if (usersArr != null) {
                                    for (u in 0 until usersArr.length()) {
                                        val uo = usersArr.getJSONObject(u)
                                        rUsers.add(com.flasskdev.vibe.data.local.ReactionUser(
                                            userId = uo.optInt("userId", uo.optInt("user_id", 0)),
                                            timestamp = uo.optLong("timestamp", System.currentTimeMillis())
                                        ))
                                    }
                                }
                                rList.add(com.flasskdev.vibe.data.local.ReactionItem(
                                    emoji = rObj.optString("emoji", ""),
                                    count = rObj.optInt("count", uList.size),
                                    userIds = uList,
                                    users = rUsers
                                ))
                            }
                        }
                        if (rList.isEmpty()) null else rList
                    } else null,
                    isBanned = obj.optBoolean("is_banned", false) || obj.optInt("is_banned", 0) == 1,
                    isFreezed = obj.optBoolean("is_freezed", false) || obj.optInt("is_freezed", 0) == 1,
                    replyMarkup = parseReplyMarkup(if (obj.has("reply_markup")) obj.opt("reply_markup") else obj.opt("replyMarkup"))
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Parse message error: ${e.message}")
            }
        }
        return result
    }

    private fun parseReplyMarkup(raw: Any?): com.flasskdev.vibe.data.local.ReplyMarkup? {
        if (raw == null || raw == JSONObject.NULL) return null
        return try {
            val rowsArray = when (raw) {
                is JSONObject -> raw.optJSONArray("inline_keyboard")
                is JSONArray -> raw
                is String -> {
                    val trimmed = raw.trim()
                    if (trimmed.startsWith("{")) {
                        JSONObject(trimmed).optJSONArray("inline_keyboard")
                    } else if (trimmed.startsWith("[")) {
                        JSONArray(trimmed)
                    } else null
                }
                else -> null
            } ?: return null

            val keyboard = mutableListOf<List<com.flasskdev.vibe.data.local.InlineKeyboardButton>>()
            val maxRows = 10
            for (r in 0 until minOf(rowsArray.length(), maxRows)) {
                val rowObj = rowsArray.optJSONArray(r) ?: continue
                val rowList = mutableListOf<com.flasskdev.vibe.data.local.InlineKeyboardButton>()
                val maxBtns = 5
                for (c in 0 until minOf(rowObj.length(), maxBtns)) {
                    val btnObj = rowObj.optJSONObject(c) ?: continue
                    val text = btnObj.optString("text", "")
                    if (text.isEmpty()) continue
                    val cbData = if (btnObj.has("callback_data") && !btnObj.isNull("callback_data")) btnObj.optString("callback_data") else null
                    val url = if (btnObj.has("url") && !btnObj.isNull("url")) btnObj.optString("url") else null
                    val bgColor = if (btnObj.has("bg_color") && !btnObj.isNull("bg_color")) btnObj.optString("bg_color") else if (btnObj.has("bgColor") && !btnObj.isNull("bgColor")) btnObj.optString("bgColor") else null
                    val textColor = if (btnObj.has("text_color") && !btnObj.isNull("text_color")) btnObj.optString("text_color") else if (btnObj.has("textColor") && !btnObj.isNull("textColor")) btnObj.optString("textColor") else null
                    rowList.add(
                        com.flasskdev.vibe.data.local.InlineKeyboardButton(
                            text = text,
                            callbackData = cbData,
                            url = url,
                            bgColor = bgColor,
                            textColor = textColor
                        )
                    )
                }
                if (rowList.isNotEmpty()) {
                    keyboard.add(rowList)
                }
            }
            if (keyboard.isNotEmpty()) com.flasskdev.vibe.data.local.ReplyMarkup(keyboard) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseUserList(array: JSONArray): List<UserSearchResult> {
        val result = mutableListOf<UserSearchResult>()
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                result.add(UserSearchResult(
                    id = obj.optInt("id", 0),
                    name = if (obj.has("name")) obj.optString("name") else null,
                    username = if (obj.has("username")) obj.optString("username") else null,
                    avatarUrl = if (obj.has("avatar_url") && !obj.isNull("avatar_url")) obj.optString("avatar_url") else null,
                    isVerified = obj.optBoolean("is_verified", false) || obj.optInt("is_verified", 0) == 1,
                    isDeveloper = obj.optBoolean("is_developer", false) || obj.optInt("is_developer", 0) == 1,
                    isBot = obj.optBoolean("is_bot", false) || obj.optInt("is_bot", 0) == 1,
                    isBanned = obj.optBoolean("is_banned", false) || obj.optInt("is_banned", 0) == 1,
                    isFreezed = obj.optBoolean("is_freezed", false) || obj.optInt("is_freezed", 0) == 1
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Parse user error: ${e.message}")
            }
        }
        return result
    }

    private fun handleDisconnect() {
        authenticated = false
        heartbeat?.cancel()
        _isActuallyConnected.set(false)
        isConnecting = false
        webSocket = null
        listeners.forEach { it.onDisconnected() }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            connect()
        }
    }

    fun authConnect(userId: Int, fcmToken: String? = null, deviceId: String, deviceName: String) {
        val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
        val auth = VibeMessage(
            session_token = preferences?.sessionToken,
            type = "auth_connect", 
            user_id = userId, 
            fcm_token = fcmToken,
            device_id = deviceId,
            device_name = deviceName,
            os_version = osVersion,
            // Сервер запоминает язык за аккаунтом и пишет на нём коды входа / уведомления о входе.
            lang = preferences?.language
        )
        lastAuth = auth
        sendJson(auth)
    }

    fun getSessions(userId: Int) {
        sendJson(VibeMessage(type = "get_sessions", user_id = userId))
    }

    fun terminateSession(userId: Int, deviceId: String) {
        sendJson(VibeMessage(type = "terminate_session", user_id = userId, device_id = deviceId))
    }

    fun checkAvailability(email: String, username: String) {
        sendJson(VibeMessage(type = "check_availability", email = email, username = username))
    }

    fun requestRegistration(email: String, username: String) {
        sendJson(VibeMessage(type = "register", email = email, username = username))
    }

    /**
     * Запрос кода входа. Если у аккаунта есть другая активная сессия, сервер пришлёт код
     * сообщением от Vibe cat на то устройство (login_result.delivery = "app"), иначе — на почту.
     * device_id нужен, чтобы сервер не считал сессию этого же телефона «другим устройством».
     */
    fun requestLogin(email: String) {
        sendJson(
            VibeMessage(
                type = "login",
                email = email,
                device_id = preferences?.deviceId,
                lang = preferences?.language
            )
        )
    }

    fun verifyCode(email: String, code: String) {
        sendJson(VibeMessage(type = "verify_code", email = email, code = code, device_id = preferences?.deviceId))
    }

    fun setNickname(email: String, nickname: String, userId: Int? = null) {
        sendJson(VibeMessage(type = "set_nickname", email = email, nickname = nickname, user_id = userId))
    }

    fun updateProfile(userId: Int, username: String? = null, nickname: String? = null, about: String? = null) {
        sendJson(VibeMessage(type = "update_profile", user_id = userId, username = username, nickname = nickname, content = about))
    }

    fun sendMessage(senderId: Int, receiverId: Int, content: String, replyToId: Int? = null, forwardedFromId: Int? = null, attachments: List<String>? = null) {
        sendJson(VibeMessage(type = "send_message", sender_id = senderId, receiver_id = receiverId, content = content, reply_to_id = replyToId, forwarded_from_id = forwardedFromId, attachments = attachments))
    }

    fun sendReaction(messageId: Int, userId: Int, emoji: String) {
        sendJson(VibeMessage(type = "send_reaction", message_id = messageId, user_id = userId, emoji = emoji))
    }

    fun getReactionUsers(messageId: Int, emoji: String? = null, offset: Int = 0, limit: Int = 20) {
        sendJson(VibeMessage(type = "get_reaction_users", message_id = messageId, emoji = emoji, offset = offset))
    }

    fun reportMessage(messageId: Int, theme: String, comment: String) {
        sendJson(VibeMessage(type = "report_message", message_id = messageId, theme = theme, comment = comment))
    }



    fun editMessage(messageId: Int, content: String) {
        sendJson(VibeMessage(type = "edit_message", message_id = messageId, content = content))
    }

    fun loadChats(userId: Int) {
        sendJson(VibeMessage(type = "load_chats", user_id = userId))
    }

    fun loadMessages(userId: Int, withUserId: Int, offset: Int = 0) {
        sendJson(VibeMessage(type = "load_messages", user_id = userId, with_user_id = withUserId, offset = offset))
    }

    fun loadMessagesAround(userId: Int, withUserId: Int, aroundMessageId: Int) {
        sendJson(VibeMessage(type = "load_messages_around", user_id = userId, with_user_id = withUserId, around_message_id = aroundMessageId))
    }

    fun sendTyping(senderId: Int, receiverId: Int) {
        sendJson(VibeMessage(type = "typing", sender_id = senderId, receiver_id = receiverId))
    }

    fun sendStopTyping(senderId: Int, receiverId: Int) {
        sendJson(VibeMessage(type = "stop_typing", sender_id = senderId, receiver_id = receiverId))
    }

    fun markRead(userId: Int, fromUserId: Int, upToMessageId: Int? = null) {
        sendJson(VibeMessage(type = "mark_read", user_id = userId, from_user_id = fromUserId, up_to_message_id = upToMessageId))
    }

    fun searchUsers(query: String, userId: Int) {
        sendJson(VibeMessage(type = "search_users", query = query, user_id = userId))
    }

    fun getUserInfo(targetId: Int) {
        sendJson(VibeMessage(type = "get_user_info", target_id = targetId))
    }

    fun muteUser(userId: Int, mutedId: Int) {
        sendJson(VibeMessage(type = "mute_user", user_id = userId, muted_id = mutedId))
    }

    fun updateMuteStatus(userId: Int, mutedId: Int, isMuted: Boolean) {
        val msg = VibeMessage(
            type = if (isMuted) "mute_user" else "unmute_user",
            user_id = userId,
            muted_id = mutedId
        )
        sendJson(msg)
    }

    fun deleteMessages(userId: Int, messageIds: List<Int>, forEveryone: Boolean) {
        val msg = VibeMessage(
            type = "delete_messages",
            user_id = userId,
            message_ids = messageIds,
            for_everyone = forEveryone
        )
        sendJson(msg)
    }

    fun pinMessage(userId: Int, messageId: Int, forBoth: Boolean) {
        val msg = VibeMessage(
            type = "pin_message",
            user_id = userId,
            message_id = messageId,
            for_both = forBoth
        )
        sendJson(msg)
    }

    fun unpinMessage(userId: Int, messageId: Int, forBoth: Boolean) {
        val msg = VibeMessage(
            type = "unpin_message",
            user_id = userId,
            message_id = messageId,
            for_both = forBoth
        )
        sendJson(msg)
    }

    fun unpinAllMessages(userId: Int, withUserId: Int, forBoth: Boolean) {
        val msg = VibeMessage(
            type = "unpin_all_messages",
            user_id = userId,
            with_user_id = withUserId,
            for_both = forBoth
        )
        sendJson(msg)
    }

    fun uploadAvatar(userId: Int, base64Data: String) {
        val msg = VibeMessage(
            type = "upload_avatar",
            user_id = userId,
            base64_data = base64Data
        )
        sendJson(msg)
    }

    private fun sendJson(message: VibeMessage) {
        if (message.type == "auth_connect" && !isConnected) { connect(); return }
        if (message.type !in publicTypes && !authenticated) {
            synchronized(pendingMessages) { if (pendingMessages.size < 100) pendingMessages.add(message) }
            connect(); return
        }
        try {
            val text = json.encodeToString(message)
            // Keep diagnostics metadata-only; message bodies must not reach logcat.
            Log.d(TAG, "OUTGOING type=${message.type}")
            val success = webSocket?.send(text) ?: false
            if (!success) {
                synchronized(pendingMessages) {
                    pendingMessages.add(message)
                }
                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON Encode Error: ${e.message}")
        }
    }

    fun sendRawJson(jsonString: String) {
        try {
            val type = JSONObject(jsonString).optString("type")
            if (type !in publicTypes && !authenticated) {
                synchronized(pendingRawMessages) { if (pendingRawMessages.size < 100) pendingRawMessages.add(jsonString) }
                connect(); return
            }
            // Raw commands include callbacks and administration requests. Queue them across reconnects
            // just like typed protocol messages so a tap is not silently lost while the socket reconnects.
            Log.d(TAG, "OUTGOING raw command")
            val success = webSocket?.send(jsonString) ?: false
            if (!success) {
                synchronized(pendingRawMessages) {
                    if (pendingRawMessages.size >= 100) pendingRawMessages.removeAt(0)
                    pendingRawMessages.add(jsonString)
                }
                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Raw JSON send error", e)
        }
    }

    fun getPrivacySettings(userId: Int) {
        sendJson(VibeMessage(type = "get_privacy_settings", user_id = userId))
    }

    fun updatePrivacySettings(userId: Int, settings: JSONObject) {
        val msg = JSONObject().apply {
            put("type", "update_privacy_settings")
            put("user_id", userId)
            put("settings", settings)
        }
        sendRawJson(msg.toString())
    }

    fun reportMessage(theme: String, fromUser: Int, toUser: Int, messageId: Int, comment: String) {
        val msg = JSONObject().apply {
            put("type", "report_message")
            put("theme", theme)
            put("from_user", fromUser)
            put("to_user", toUser)
            put("message_id", messageId)
            put("comment", comment)
        }
        sendRawJson(msg.toString())
    }

    fun blockUser(userId: Int, blockedId: Int) {
        val msg = JSONObject().apply {
            put("type", "block_user")
            put("user_id", userId)
            put("blocked_id", blockedId)
        }
        sendRawJson(msg.toString())
    }

    fun unblockUser(userId: Int, blockedId: Int) {
        val msg = JSONObject().apply {
            put("type", "unblock_user")
            put("user_id", userId)
            put("blocked_id", blockedId)
        }
        sendRawJson(msg.toString())
    }

    fun getBlockedUsers(userId: Int, page: Int = 1, limit: Int = 30, query: String = "") {
        val msg = JSONObject().apply {
            put("type", "get_blocked_users")
            put("user_id", userId)
            put("page", page)
            put("limit", limit)
            if (query.isNotBlank()) {
                put("query", query)
            }
        }
        sendRawJson(msg.toString())
    }

    fun getBlockedCount(userId: Int) {
        val msg = JSONObject().apply {
            put("type", "get_blocked_count")
            put("user_id", userId)
        }
        sendRawJson(msg.toString())
    }

    fun getStickerPacks(userId: Int) {
        val msg = JSONObject().apply {
            put("type", "get_sticker_packs")
            put("user_id", userId)
        }
        sendRawJson(msg.toString())
    }

    fun searchStickerPacks(query: String, userId: Int) {
        val msg = JSONObject().apply {
            put("type", "search_sticker_packs")
            put("user_id", userId)
            put("query", query)
        }
        sendRawJson(msg.toString())
    }

    fun addStickerPack(userId: Int, packId: Int) {
        val msg = JSONObject().apply {
            put("type", "add_sticker_pack")
            put("user_id", userId)
            put("pack_id", packId)
        }
        sendRawJson(msg.toString())
    }

    fun removeStickerPack(userId: Int, packId: Int) {
        val msg = JSONObject().apply {
            put("type", "remove_sticker_pack")
            put("user_id", userId)
            put("pack_id", packId)
        }
        sendRawJson(msg.toString())
    }

    fun sendBotCallback(userId: Int, botId: Int, messageId: Int, data: String) {
        val msg = JSONObject().apply {
            put("type", "bot_callback")
            put("user_id", userId)
            put("bot_id", botId)
            put("message_id", messageId)
            put("data", data)
        }
        sendRawJson(msg.toString())
    }

    fun disconnect() {
        authenticated = false
        heartbeat?.cancel()
        _isActuallyConnected.set(false)
        isConnecting = false
        reconnectJob?.cancel()
        scopeJob.cancel()
        val old = webSocket
        webSocket = null
        old?.close(1000, "App closing")
    }
}