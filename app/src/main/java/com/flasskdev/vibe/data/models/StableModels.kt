package com.flasskdev.vibe.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/* ============================================================================
 *  СТАБИЛЬНОСТЬ ДЛЯ COMPOSE
 *  ---------------------------------------------------------------------------
 *  Compose считает нестабильным любой тип, у которого есть поле нестабильного
 *  типа — в частности List<T> из kotlin.collections. MessageEntity содержит
 *  List<String> и List<ReactionItem>, поэтому КАЖДЫЙ элемент списка сообщений
 *  перерисовывался при любой рекомпозиции родителя, даже если сам не менялся.
 *
 *  Два способа починить (нужны оба):
 *   1) пометить UI-модели @Immutable (ниже);
 *   2) добавить в build.gradle.kts stability-конфиг (см. compose_stability.conf).
 * ========================================================================== */

@Immutable
data class MessageUi(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean,
    val isMine: Boolean,
    val isEdited: Boolean,
    val isPinned: Boolean,
    val senderType: String,
    val replyToId: Int?,
    val replyToContent: String?,
    val replyToSenderName: String?,
    val forwardedFromId: Int?,
    val forwardedFromName: String?,
    val attachments: ImmutableList<String>,
    val reactions: ImmutableList<ReactionUi>,
    val replyMarkup: ReplyMarkupUi?,
    val uploadStatus: String?,
    val uploadProgress: Int?,
    val isEncrypted: Boolean,
    val decryptFailed: Boolean,
    val circleDurationMs: Long?,
    /** Ключ для LazyColumn: стабилен между локальным и серверным id. */
    val stableKey: String = if (id < 0) "local_$id" else "msg_$id"
)

@Immutable
data class ReactionUi(val emoji: String, val count: Int, val mine: Boolean)

@Immutable
data class ReplyMarkupUi(val rows: ImmutableList<ImmutableList<ButtonUi>>)

@Immutable
data class ButtonUi(
    val text: String,
    val callbackData: String?,
    val url: String?,
    val bgColor: String?,
    val textColor: String?
)

@Immutable
data class ChatUi(
    val interlocutorId: Int,
    val name: String,
    val username: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val isMuted: Boolean,
    val pinned: Boolean,
    val isVerified: Boolean,
    val isDeveloper: Boolean,
    val isBot: Boolean,
    val draft: String?,
    val isLastMessageMine: Boolean,
    val isLastMessageRead: Boolean,
    val lastMessageEncrypted: Boolean
)

/**
 * Минимальная неизменяемая обёртка над List, чтобы не тянуть
 * kotlinx-collections-immutable ради двух типов.
 */
@Immutable
class ImmutableList<out T>(private val delegate: List<T>) : List<T> by delegate {
    override fun equals(other: Any?): Boolean = other is ImmutableList<*> && delegate == other.delegate
    override fun hashCode(): Int = delegate.hashCode()
    companion object {
        private val EMPTY = ImmutableList<Nothing>(emptyList())
        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): ImmutableList<T> = EMPTY as ImmutableList<T>
    }
}

fun <T> List<T>.toImmutable(): ImmutableList<T> =
    if (isEmpty()) ImmutableList.empty() else ImmutableList(this)

@Stable
interface MessageActions {
    fun onReply(id: Int)
    fun onEdit(id: Int)
    fun onDelete(id: Int)
    fun onReact(id: Int, emoji: String)
    fun onCopy(id: Int)
    fun onForward(id: Int)
    fun onOpenAttachment(id: Int, index: Int)
}