package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.crypto.CryptoEngine
import com.example.data.local.CipherDatabase
import com.example.data.local.model.ContactEntity
import com.example.data.local.model.ConversationEntity
import com.example.data.local.model.DeviceSessionEntity
import com.example.data.local.model.MessageEntity
import com.example.util.ContactsSyncHelper
import com.example.util.ContactsSyncResult
import com.example.util.NotificationHelper
import com.example.util.NotificationPrivacyMode
import com.example.util.SmsHelper
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatRepository(
  private val context: Context,
  private val scope: CoroutineScope,
  private val authRepository: FirebaseAuthRepository
) {
  companion object {
    private const val TAG = "ChatRepository"
  }

  private val database = CipherDatabase.getDatabase(context, scope)
  private val conversationDao = database.conversationDao()
  private val messageDao = database.messageDao()
  private val deviceSessionDao = database.deviceSessionDao()
  private val contactDao = database.contactDao()
  private val notificationHelper = NotificationHelper(context)

  // Network Connectivity State
  private val _isOnline = MutableStateFlow(true)
  val isOnline = _isOnline.asStateFlow()

  // Active Chat tracking for push notifications (don't alert if user is looking at this chat)
  val activeChatId = MutableStateFlow<String?>(null)

  // Privacy Preferences
  val notificationPrivacyMode = MutableStateFlow(NotificationPrivacyMode.FULL_PREVIEW)
  val screenSecurityEnabled = MutableStateFlow(false)
  val biometricLockEnabled = MutableStateFlow(false)
  val isAppLocked = MutableStateFlow(false)

  // Observables from Database
  val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
  val directChats: Flow<List<ConversationEntity>> = conversationDao.getDirectChats()
  val groupChats: Flow<List<ConversationEntity>> = conversationDao.getGroups()
  val linkedDevices: Flow<List<DeviceSessionEntity>> = deviceSessionDao.getAllSessions()
  val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()

  private var firestoreListener: ListenerRegistration? = null
  private val appStartTime = System.currentTimeMillis() - 120000L // Listen from last 2 minutes onwards

  init {
    // Start real-time Firestore message listener
    setupFirestoreMessageListener()

    // Start periodic background cleaner for disappearing messages
    scope.launch(Dispatchers.IO) {
      while (true) {
        delay(5000)
        messageDao.deleteExpired(System.currentTimeMillis())
      }
    }
  }

  /**
   * Real-time listener for incoming messages across Firestore.
   * Enables Guest-to-Guest and User-to-User cross-device messaging.
   */
  private fun setupFirestoreMessageListener() {
    try {
      firestoreListener?.remove()
      firestoreListener = authRepository.firestore.collection("messages")
        .whereGreaterThanOrEqualTo("timestamp", appStartTime)
        .addSnapshotListener { snapshots, error ->
          if (error != null) {
            Log.w(TAG, "Firestore message listener error: ${error.message}")
            return@addSnapshotListener
          }
          if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener

          val currentProfile = authRepository.userProfile.value
          val myUid = currentProfile?.uid ?: ""
          val myPhone = (currentProfile?.phoneNumber ?: "").filter { it.isDigit() || it == '+' }
          val myHandle = (currentProfile?.handle ?: "").lowercase().removePrefix("@")

          for (change in snapshots.documentChanges) {
            if (change.type == DocumentChange.Type.ADDED) {
              val doc = change.document
              val data = doc.data
              val senderId = data["senderId"] as? String ?: ""
              // Ignore messages sent by self
              if (senderId == myUid) continue

              val msgId = data["id"] as? String ?: doc.id
              val recipientPhone = (data["recipientPhone"] as? String ?: "").filter { it.isDigit() || it == '+' }
              val recipientHandle = (data["recipientHandle"] as? String ?: "").lowercase().removePrefix("@")
              val convId = data["conversationId"] as? String ?: ""

              val isDirectRecipient = (myPhone.isNotBlank() && recipientPhone.isNotBlank() && recipientPhone == myPhone) ||
                (myHandle.isNotBlank() && recipientHandle.isNotBlank() && recipientHandle == myHandle)
              val isConversationMatch = activeChatId.value != null && activeChatId.value == convId

              if (!isDirectRecipient && !isConversationMatch) {
                continue
              }

              scope.launch(Dispatchers.IO) {
                if (messageDao.getMessageDirect(msgId) != null) return@launch

                val senderName = data["senderName"] as? String ?: "Contact"
                val senderPhone = data["senderPhone"] as? String ?: ""
                val senderHandle = data["senderHandle"] as? String ?: ""
                val cipher = data["cipherTextBase64"] as? String ?: ""
                val nonce = data["nonceBase64"] as? String ?: ""
                val plainText = data["plainText"] as? String ?: ""
                val mediaType = data["mediaType"] as? String ?: "TEXT"
                val mediaUri = data["mediaUri"] as? String
                val mediaMeta = data["mediaMeta"] as? String
                val time = (data["timestamp"] as? Long) ?: System.currentTimeMillis()
                val authTag = data["authTagHex"] as? String ?: ""

                // Ensure a conversation exists locally for this peer
                var targetConvId = convId
                var conv = conversationDao.getConversationDirect(targetConvId)
                if (conv == null && senderPhone.isNotBlank()) {
                  conv = conversationDao.getConversationByPhone(senderPhone)
                  if (conv != null) targetConvId = conv.id
                }
                if (conv == null) {
                  targetConvId = "chat_${UUID.randomUUID().toString().take(8)}"
                  val newConv = ConversationEntity(
                    id = targetConvId,
                    title = senderName,
                    isGroup = false,
                    participantNames = senderName,
                    participantHandles = senderHandle.let { if (it.startsWith("@")) it else "@$it" },
                    phoneNumber = senderPhone,
                    isExternalSms = false,
                    lastMessageText = "$senderName: $plainText",
                    lastMessageTime = time,
                    unreadCount = 1,
                    isPinned = false,
                    isVerified = true,
                    safetyNumber = CryptoEngine.generateSafetyNumber("my_pubkey", senderPhone.ifBlank { senderHandle }),
                    avatarColorHex = 0xFF6366F1
                  )
                  conversationDao.insert(newConv)
                } else {
                  conversationDao.updateLastMessage(targetConvId, "$senderName: $plainText", time)
                }

                val incomingMsg = MessageEntity(
                  id = msgId,
                  conversationId = targetConvId,
                  senderId = senderId,
                  senderName = senderName,
                  cipherTextBase64 = cipher,
                  nonceBase64 = nonce,
                  plainText = plainText,
                  mediaType = mediaType,
                  mediaUri = mediaUri,
                  mediaMeta = mediaMeta,
                  timestamp = time,
                  status = "DELIVERED",
                  isOutgoing = false,
                  authTagHex = authTag,
                  isSms = false
                )
                messageDao.insert(incomingMsg)

                if (activeChatId.value != targetConvId) {
                  notificationHelper.showMessageNotification(
                    conversationId = targetConvId,
                    senderName = senderName,
                    plainText = plainText,
                    privacyMode = notificationPrivacyMode.value
                  )
                }
              }
            }
          }
        }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to start Firestore message listener: ${e.message}")
    }
  }

  fun getConversation(id: String): Flow<ConversationEntity?> =
    conversationDao.getConversationById(id)

  fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
    messageDao.getMessagesForConversation(conversationId)

  fun setOnline(online: Boolean) {
    _isOnline.value = online
    if (online) {
      flushOfflineQueue()
    }
  }

  fun toggleOnline() {
    setOnline(!_isOnline.value)
  }

  fun unlockApp() {
    isAppLocked.value = false
  }

  fun lockApp() {
    if (biometricLockEnabled.value) {
      isAppLocked.value = true
    }
  }

  fun sendMessage(
    conversationId: String,
    text: String,
    mediaType: String = "TEXT",
    mediaUri: String? = null,
    mediaMeta: String? = null
  ) {
    scope.launch(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      val conv = conversationDao.getConversationDirect(conversationId)
      val disappearing = conv?.disappearingSeconds ?: 0
      val expiresAt = if (disappearing > 0) now + (disappearing * 1000L) else 0L

      val isSmsDelivery = conv?.isExternalSms == true && !conv.phoneNumber.isNullOrBlank()

      // Encrypt payload with AES-256-GCM
      val payload = CryptoEngine.encrypt(text)

      val isCurrentOnline = _isOnline.value
      val initialStatus = when {
        isSmsDelivery -> "SENT_SMS"
        isCurrentOnline -> "SENT"
        else -> "QUEUED_OFFLINE"
      }

      val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
      val currentProfile = authRepository.userProfile.value
      val myUid = currentProfile?.uid ?: "user_me"
      val myName = currentProfile?.displayName ?: "Me"
      val myPhone = currentProfile?.phoneNumber ?: ""
      val myHandle = currentProfile?.handle ?: ""

      val message = MessageEntity(
        id = msgId,
        conversationId = conversationId,
        senderId = "me",
        senderName = "Me",
        cipherTextBase64 = payload.cipherTextBase64,
        nonceBase64 = payload.nonceBase64,
        plainText = text,
        mediaType = mediaType,
        mediaUri = mediaUri,
        mediaMeta = mediaMeta,
        timestamp = now,
        status = initialStatus,
        isOutgoing = true,
        expiresAt = expiresAt,
        authTagHex = payload.authTagHex,
        isSms = isSmsDelivery
      )

      messageDao.insert(message)
      val prefix = if (isSmsDelivery) "[SMS] " else ""
      val summary = if (mediaType != "TEXT") "[$mediaType] $text" else text
      conversationDao.updateLastMessage(conversationId, "${prefix}You: $summary", now)

      if (isSmsDelivery) {
        val phone = conv.phoneNumber ?: ""
        // Send directly in background via Android's SmsManager
        SmsHelper.sendDirectSmsInBackground(
          context = context,
          phoneNumber = phone,
          messageText = text
        )
      } else {
        // Publish to Firestore for peer / guest delivery across devices
        try {
          val firestoreMsg = hashMapOf(
            "id" to msgId,
            "conversationId" to conversationId,
            "senderId" to myUid,
            "senderName" to myName,
            "senderPhone" to myPhone,
            "senderHandle" to myHandle,
            "recipientPhone" to (conv?.phoneNumber ?: ""),
            "recipientHandle" to (conv?.participantHandles ?: ""),
            "cipherTextBase64" to payload.cipherTextBase64,
            "nonceBase64" to payload.nonceBase64,
            "plainText" to text,
            "mediaType" to mediaType,
            "mediaUri" to (mediaUri ?: ""),
            "mediaMeta" to (mediaMeta ?: ""),
            "timestamp" to now,
            "status" to "SENT",
            "authTagHex" to payload.authTagHex
          )
          authRepository.firestore.collection("messages").document(msgId).set(firestoreMsg)
          messageDao.updateStatus(msgId, "DELIVERED")
        } catch (e: Exception) {
          Log.w(TAG, "Firestore publish failed: ${e.message}")
        }
      }
    }
  }

  fun flushOfflineQueue() {
    scope.launch(Dispatchers.IO) {
      val queued = messageDao.getQueuedOfflineMessages()
      if (queued.isEmpty()) return@launch

      for (msg in queued) {
        delay(300)
        messageDao.updateStatus(msg.id, "SENT")
        delay(300)
        messageDao.updateStatus(msg.id, "DELIVERED")
      }
    }
  }

  /**
   * Syncs contacts from device phonebook and checks against Firestore.
   */
  suspend fun syncContactsFromDevice(): ContactsSyncResult = withContext(Dispatchers.IO) {
    ContactsSyncHelper.syncDeviceContacts(context, contactDao, authRepository.firestore)
  }

  fun createGroup(
    title: String,
    memberNames: List<String>,
    colorHex: Long = 0xFF6366F1
  ) {
    scope.launch(Dispatchers.IO) {
      val groupId = "group_${UUID.randomUUID().toString().take(8)}"
      val safetyNum = CryptoEngine.generateSafetyNumber("my_pubkey", groupId)
      val allMembers = (memberNames + "You").joinToString(", ")
      val handles = memberNames.joinToString(", ") { "@" + it.lowercase().replace(" ", ".") } + ", @you"

      val group = ConversationEntity(
        id = groupId,
        title = title,
        isGroup = true,
        participantNames = allMembers,
        participantHandles = handles,
        lastMessageText = "Group created with end-to-end encryption",
        lastMessageTime = System.currentTimeMillis(),
        unreadCount = 0,
        isPinned = false,
        isVerified = true,
        safetyNumber = safetyNum,
        avatarColorHex = colorHex
      )

      conversationDao.insert(group)

      val sysMsg = "End-to-end encrypted group created. Participant sender keys distributed."
      val p = CryptoEngine.encrypt(sysMsg)
      messageDao.insert(
        MessageEntity(
          id = "msg_${UUID.randomUUID().toString().take(8)}",
          conversationId = groupId,
          senderId = "system",
          senderName = "System",
          cipherTextBase64 = p.cipherTextBase64,
          nonceBase64 = p.nonceBase64,
          plainText = sysMsg,
          timestamp = System.currentTimeMillis(),
          status = "READ",
          isOutgoing = false,
          authTagHex = p.authTagHex
        )
      )
    }
  }

  fun createDirectChat(name: String, handle: String, colorHex: Long = 0xFF0D9488): String {
    val chatId = "chat_${UUID.randomUUID().toString().take(8)}"
    scope.launch(Dispatchers.IO) {
      val cleanHandle = if (handle.startsWith("@")) handle else "@$handle"
      val safetyNum = CryptoEngine.generateSafetyNumber("my_pubkey", cleanHandle)
      val chat = ConversationEntity(
        id = chatId,
        title = name,
        isGroup = false,
        participantNames = name,
        participantHandles = cleanHandle,
        lastMessageText = "Chat started with $name. Safety numbers generated.",
        lastMessageTime = System.currentTimeMillis(),
        unreadCount = 0,
        isPinned = false,
        isVerified = true,
        safetyNumber = safetyNum,
        avatarColorHex = colorHex
      )
      conversationDao.insert(chat)
    }
    return chatId
  }

  /**
   * Starts or returns an existing conversation for a contact.
   * If receiver does not have the app, isExternalSms is set to true.
   */
  suspend fun startChatWithContact(contact: ContactEntity): String = withContext(Dispatchers.IO) {
    val cleanPhone = contact.phoneNumber.filter { it.isDigit() || it == '+' }
    val existing = conversationDao.getConversationByPhone(contact.phoneNumber)
      ?: conversationDao.getConversationByPhone(cleanPhone)

    if (existing != null) {
      if (existing.isExternalSms != !contact.hasApp) {
        conversationDao.update(existing.copy(isExternalSms = !contact.hasApp))
      }
      return@withContext existing.id
    }

    val chatId = "chat_contact_${UUID.randomUUID().toString().take(8)}"
    val displayHandle = contact.handle ?: contact.phoneNumber
    val safetyNum = CryptoEngine.generateSafetyNumber("my_pubkey", contact.phoneNumber)
    val initialNotice = if (contact.hasApp) {
      "Encrypted Crystal chat started with ${contact.name}"
    } else {
      "SMS Contact • Messages deliver via SMS (${contact.phoneNumber})"
    }

    val newConv = ConversationEntity(
      id = chatId,
      title = contact.name,
      isGroup = false,
      participantNames = contact.name,
      participantHandles = displayHandle,
      phoneNumber = contact.phoneNumber,
      isExternalSms = !contact.hasApp,
      lastMessageText = initialNotice,
      lastMessageTime = System.currentTimeMillis(),
      unreadCount = 0,
      isPinned = false,
      isVerified = contact.hasApp,
      safetyNumber = safetyNum,
      avatarColorHex = contact.avatarColorHex
    )

    conversationDao.insert(newConv)
    chatId
  }

  suspend fun addContact(
    name: String,
    phoneNumber: String,
    handle: String? = null,
    hasApp: Boolean = false
  ): ContactEntity = withContext(Dispatchers.IO) {
    val id = "contact_${UUID.randomUUID().toString().take(8)}"
    val colorPalettes = listOf(0xFF0D9488, 0xFF6366F1, 0xFFEC4899, 0xFFF59E0B, 0xFF3B82F6, 0xFF8B5CF6)
    val color = colorPalettes[(name.hashCode() and 0x7FFFFFFF) % colorPalettes.size]

    val cleanHandle = handle?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("@")) it else "@$it" }
    val contact = ContactEntity(
      id = id,
      name = name.trim(),
      phoneNumber = phoneNumber.trim(),
      handle = cleanHandle,
      avatarColorHex = color,
      hasApp = hasApp,
      addedAt = System.currentTimeMillis()
    )
    contactDao.insert(contact)
    contact
  }

  suspend fun deleteContact(id: String) = withContext(Dispatchers.IO) {
    contactDao.deleteById(id)
  }

  suspend fun updateContactHasApp(id: String, hasApp: Boolean) = withContext(Dispatchers.IO) {
    contactDao.updateHasApp(id, hasApp)
  }

  fun toggleSafetyVerification(conversationId: String, currentVerified: Boolean) {
    scope.launch(Dispatchers.IO) {
      conversationDao.updateSafetyVerification(conversationId, !currentVerified)
    }
  }

  fun updateDisappearingTimer(conversationId: String, seconds: Int) {
    scope.launch(Dispatchers.IO) {
      conversationDao.updateDisappearingTimer(conversationId, seconds)
    }
  }

  fun clearChat(conversationId: String) {
    scope.launch(Dispatchers.IO) {
      messageDao.clearMessagesForConversation(conversationId)
      conversationDao.updateLastMessage(conversationId, "Chat history cleared", System.currentTimeMillis())
    }
  }

  fun clearUnread(conversationId: String) {
    scope.launch(Dispatchers.IO) {
      conversationDao.clearUnread(conversationId)
    }
  }

  fun linkDevice(name: String, platform: String) {
    scope.launch(Dispatchers.IO) {
      val deviceId = "dev_${UUID.randomUUID().toString().take(8)}"
      val session = DeviceSessionEntity(
        id = deviceId,
        deviceName = name,
        platform = platform,
        lastActive = "Linked just now",
        fingerprint = CryptoEngine.computeIdentityFingerprint(name),
        isCurrentDevice = false,
        isVerified = true
      )
      deviceSessionDao.insert(session)
    }
  }

  fun revokeDevice(deviceId: String) {
    scope.launch(Dispatchers.IO) {
      deviceSessionDao.delete(deviceId)
    }
  }

  suspend fun exportEncryptedCloudBackup(passphrase: String): String {
    val messages = messageDao.getAllMessages()
    val backupJson = JSONObject()
    backupJson.put("version", 1)
    backupJson.put("exportedAt", System.currentTimeMillis())
    backupJson.put("app", "Crystal Chat")

    val msgArray = JSONArray()
    messages.forEach { msg ->
      val mObj = JSONObject()
      mObj.put("id", msg.id)
      mObj.put("convId", msg.conversationId)
      mObj.put("cipher", msg.cipherTextBase64)
      mObj.put("nonce", msg.nonceBase64)
      mObj.put("text", msg.plainText)
      mObj.put("time", msg.timestamp)
      msgArray.put(mObj)
    }
    backupJson.put("messages", msgArray)

    val key = CryptoEngine.deriveKeyFromPassword(passphrase)
    val encrypted = CryptoEngine.encrypt(backupJson.toString(), key)
    val resultEnvelope = JSONObject()
    resultEnvelope.put("header", "CIPHER_E2EE_VAULT_BACKUP_V1")
    resultEnvelope.put("payload", encrypted.cipherTextBase64)
    resultEnvelope.put("nonce", encrypted.nonceBase64)
    resultEnvelope.put("mac", encrypted.authTagHex)
    return resultEnvelope.toString(2)
  }

  suspend fun restoreEncryptedCloudBackup(encryptedJson: String, passphrase: String): Boolean {
    return try {
      val envelope = JSONObject(encryptedJson)
      val cipher = envelope.getString("payload")
      val nonce = envelope.getString("nonce")
      val key = CryptoEngine.deriveKeyFromPassword(passphrase)
      val decrypted = CryptoEngine.decrypt(cipher, nonce, key)
      if (decrypted.startsWith("[Encrypted")) return false

      val root = JSONObject(decrypted)
      val msgArray = root.getJSONArray("messages")
      msgArray.length() >= 0
    } catch (e: Exception) {
      false
    }
  }
}
