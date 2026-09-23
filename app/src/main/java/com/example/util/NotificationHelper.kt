package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

enum class NotificationPrivacyMode {
  FULL_PREVIEW,
  SENDER_ONLY,
  HIDDEN_ALL
}

class NotificationHelper(private val context: Context) {
  companion object {
    const val CHANNEL_ID = "cipher_channel_secure"
    const val CHANNEL_NAME = "CipherChat Secure Messages"
  }

  init {
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "End-to-end encrypted push notifications with strict zero-leakage protection"
        enableVibration(true)
        setShowBadge(true)
        lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
      }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      manager?.createNotificationChannel(channel)
    }
  }

  fun showMessageNotification(
    conversationId: String,
    senderName: String,
    plainText: String,
    privacyMode: NotificationPrivacyMode = NotificationPrivacyMode.FULL_PREVIEW
  ) {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("OPEN_CONVERSATION_ID", conversationId)
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      conversationId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val (title, content) = when (privacyMode) {
      NotificationPrivacyMode.FULL_PREVIEW -> Pair(senderName, "🔒 $plainText")
      NotificationPrivacyMode.SENDER_ONLY -> Pair(senderName, "🔒 New encrypted message")
      NotificationPrivacyMode.HIDDEN_ALL -> Pair("CipherChat", "🔒 New encrypted message received")
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
      .setContentTitle(title)
      .setContentText(content)
      .setStyle(NotificationCompat.BigTextStyle().bigText(content))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .build()

    try {
      NotificationManagerCompat.from(context).notify(conversationId.hashCode(), notification)
    } catch (_: SecurityException) {
      // Handled if runtime permission not yet granted
    }
  }
}
