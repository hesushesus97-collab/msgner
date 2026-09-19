package com.flasskdev.vibe.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.flasskdev.vibe.R
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class VibeFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "VibeFCM"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM registration token changed")
        // Токен будет отправлен на сервер при следующем auth_connect
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM data message received")

        val data = message.data
        val prefs = UserPreferences(this)
        if (!prefs.isLoggedIn || data["receiver_id"]?.toIntOrNull() != prefs.userId) return
        val action = data["action"] ?: "new"
        val senderName = data["sender_name"] ?: return
        val content = data["content"] ?: return
        val senderId = data["sender_id"]?.toIntOrNull() ?: return
        
        val attachmentsStr = data["attachments"]
        val attachments = if (!attachmentsStr.isNullOrEmpty() && attachmentsStr != "[]") {
            try {
                val arr = org.json.JSONArray(attachmentsStr)
                List(arr.length()) { arr.getString(it) }
            } catch (e: Exception) { null }
        } else null
        
        val formattedContent = com.flasskdev.vibe.utils.MessageUtils.formatMessagePreview(content, attachments)

        if (action == "edit") {
            NotificationHelper.editMessageNotification(
                context = this,
                senderName = senderName,
                messageContent = formattedContent,
                senderId = senderId
            )
        } else {
            NotificationHelper.showMessageNotification(
                context = this,
                senderName = senderName,
                messageContent = formattedContent,
                senderId = senderId
            )
        }
    }
}
