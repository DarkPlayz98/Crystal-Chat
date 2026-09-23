package com.example.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey val id: String,
  val title: String,
  val isGroup: Boolean,
  val participantNames: String,
  val participantHandles: String,
  val lastMessageText: String,
  val lastMessageTime: Long,
  val unreadCount: Int = 0,
  val isPinned: Boolean = false,
  val isVerified: Boolean = true,
  val safetyNumber: String = "",
  val disappearingSeconds: Int = 0, // 0 means off
  val avatarColorHex: Long = 0xFF0D9488,
  val isEncrypted: Boolean = true,
  val phoneNumber: String? = null,
  val isExternalSms: Boolean = false
)

@Entity(
  tableName = "messages",
  indices = [Index(value = ["conversationId"]), Index(value = ["timestamp"])]
)
data class MessageEntity(
  @PrimaryKey val id: String,
  val conversationId: String,
  val senderId: String,
  val senderName: String,
  val cipherTextBase64: String,
  val nonceBase64: String,
  val plainText: String,
  val mediaType: String = "TEXT", // "TEXT", "IMAGE", "VOICE", "FILE"
  val mediaUri: String? = null,
  val mediaMeta: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val status: String = "READ", // "QUEUED_OFFLINE", "SENT", "DELIVERED", "READ", "SENT_SMS"
  val isOutgoing: Boolean = false,
  val expiresAt: Long = 0L,
  val authTagHex: String = "",
  val isSms: Boolean = false
)

@Entity(tableName = "device_sessions")
data class DeviceSessionEntity(
  @PrimaryKey val id: String,
  val deviceName: String,
  val platform: String,
  val lastActive: String,
  val fingerprint: String,
  val isCurrentDevice: Boolean = false,
  val isVerified: Boolean = true
)

@Entity(tableName = "contacts")
data class ContactEntity(
  @PrimaryKey val id: String,
  val name: String,
  val phoneNumber: String,
  val handle: String? = null,
  val avatarColorHex: Long = 0xFF0D9488,
  val hasApp: Boolean = false,
  val addedAt: Long = System.currentTimeMillis()
)
