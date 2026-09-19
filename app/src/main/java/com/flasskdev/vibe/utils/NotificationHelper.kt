package com.flasskdev.vibe.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.flasskdev.vibe.MainActivity
import com.flasskdev.vibe.R
import com.flasskdev.vibe.ui.theme.VibeStringsHolder

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val senderId = intent.getIntExtra("sender_id", -1)
        if (senderId != -1) {
            NotificationHelper.clearNotificationState(senderId)
        }
    }
}

object NotificationHelper {
    private const val CHANNEL_ID = "vibe_messages_channel"
    private const val CHANNEL_NAME = "Messages"

    private val activeMessagingStyles = mutableMapOf<Int, NotificationCompat.MessagingStyle>()

    var activeChatId: Int? = null
        set(value) {
            field = value
            if (value != null) {
                clearNotificationState(value)
            }
        }

    fun resetForLogout() {
        activeMessagingStyles.clear()
        activeChatId = null
    }
    fun clearNotificationState(senderId: Int) {
        activeMessagingStyles.remove(senderId)
    }

    private fun soundUri(context: Context): android.net.Uri? = when (val sound = com.flasskdev.vibe.data.UserPreferences(context).notificationSound) {
        "silent" -> null
        "default" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        else -> android.net.Uri.parse(sound)
    }
    private fun channelId(context: Context): String {
        val sound = com.flasskdev.vibe.data.UserPreferences(context).notificationSound
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(sound.toByteArray()).take(8).joinToString("") { "%02x".format(it) }
        return "${CHANNEL_ID}_$digest"
    }
    private fun suppressed(context: Context, senderId: Int): Boolean {
        val prefs = com.flasskdev.vibe.data.UserPreferences(context)
        return !prefs.isLoggedIn || prefs.notificationMuteAll || senderId.toString() in prefs.mutedNotificationPeers
    }
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId(context), CHANNEL_NAME, importance).apply {
                description = "Notifications for new messages"
                setSound(soundUri(context), android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build())
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMessageNotification(context: Context, senderName: String, messageContent: String, senderId: Int) {
        if (senderId == activeChatId || suppressed(context, senderId)) return

        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chat_partner_id", senderId)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            senderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            putExtra("sender_id", senderId)
        }
        val deletePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            senderId,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bitmap = createAvatarBitmap(context, senderName)
        val icon = IconCompat.createWithBitmap(bitmap)

        val senderPerson = Person.Builder()
            .setName(senderName)
            .setIcon(icon)
            .setKey(senderId.toString())
            .build()

        val shortcut = ShortcutInfoCompat.Builder(context, senderId.toString())
            .setShortLabel(senderName)
            .setLongLabel(senderName)
            .setIcon(icon)
            .setPerson(senderPerson)
            .setIntent(intent)
            .setLongLived(true)
            .setCategories(setOf("androidx.core.content.pm.shortcut_conversation"))
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        val mePerson = Person.Builder()
            .setName(VibeStringsHolder.current.notifMe)
            .build()

        val messagingStyle = activeMessagingStyles.getOrPut(senderId) {
            NotificationCompat.MessagingStyle(mePerson)
                .setConversationTitle(senderName)
                .setGroupConversation(false)
        }

        messagingStyle.addMessage(messageContent, System.currentTimeMillis(), senderPerson)

        val builder = NotificationCompat.Builder(context, channelId(context))
            .setSound(soundUri(context))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(bitmap)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(senderId.toString())
            .setNumber(messagingStyle.messages.size)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)

        with(NotificationManagerCompat.from(context)) {
            try { notify(senderId, builder.build()) } catch (_: SecurityException) {}
        }
    }

    fun editMessageNotification(context: Context, senderName: String, messageContent: String, senderId: Int) {
        if (senderId == activeChatId || suppressed(context, senderId)) return
        createNotificationChannel(context)

        val existingStyle = activeMessagingStyles[senderId]
        if (existingStyle == null) {
            // Если уведомления нет, просто показываем новое
            showMessageNotification(context, senderName, messageContent, senderId)
            return
        }

        val messages = existingStyle.messages
        if (messages.isEmpty()) {
            showMessageNotification(context, senderName, messageContent, senderId)
            return
        }

        val mePerson = Person.Builder().setName(VibeStringsHolder.current.notifMe).build()
        val newStyle = NotificationCompat.MessagingStyle(mePerson)
            .setConversationTitle(senderName)
            .setGroupConversation(false)

        // Копируем все сообщения, кроме последнего от этого отправителя
        var lastSenderMsgIndex = -1
        for (i in messages.indices.reversed()) {
            if (messages[i].person?.name == senderName) {
                lastSenderMsgIndex = i
                break
            }
        }

        if (lastSenderMsgIndex != -1) {
            for (i in messages.indices) {
                if (i == lastSenderMsgIndex) {
                    val oldMsg = messages[i]
                    // Добавляем пометку "(ред.)"
                    val editedText = "$messageContent (${VibeStringsHolder.current.editedLabel})"
                    newStyle.addMessage(editedText, oldMsg.timestamp, oldMsg.person)
                } else {
                    newStyle.addMessage(messages[i])
                }
            }
        } else {
            // Если сообщений от него нет, просто заменяем последнее
            for (i in 0 until messages.size - 1) {
                newStyle.addMessage(messages[i])
            }
            val last = messages.last()
            newStyle.addMessage("$messageContent (${VibeStringsHolder.current.editedLabel})", last.timestamp, last.person)
        }

        activeMessagingStyles[senderId] = newStyle

        val bitmap = createAvatarBitmap(context, senderName)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chat_partner_id", senderId)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, senderId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deleteIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            putExtra("sender_id", senderId)
        }
        val deletePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, senderId, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId(context))
            .setSound(soundUri(context))
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(bitmap)
            .setStyle(newStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(senderId.toString())
            .setOnlyAlertOnce(true)
            .setNumber(newStyle.messages.size)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(senderId, builder.build())
        } catch (_: SecurityException) {
            // The user can revoke notification permission after the explicit check.
        }
    }

    private fun createAvatarBitmap(context: Context, name: String): android.graphics.Bitmap {
        val size = 128
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        paint.color = android.graphics.Color.parseColor("#81D4FA")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = size / 2f
        paint.textAlign = android.graphics.Paint.Align.CENTER

        val initial = if (name.isNotBlank()) name.take(1).uppercase() else "?"
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(initial, 0, initial.length, textBounds)

        val x = size / 2f
        val y = (size / 2f) - textBounds.exactCenterY()

        canvas.drawText(initial, x, y, paint)
        return bitmap
    }
}