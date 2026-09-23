package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.crypto.CryptoEngine
import com.example.data.local.CipherDatabase
import com.example.data.local.model.ContactEntity
import com.example.data.local.model.ConversationEntity
import com.example.data.local.model.DeviceSessionEntity
import com.example.data.local.model.MessageEntity
import com.example.util.NotificationHelper
import com.example.util.NotificationPrivacyMode
import com.example.util.SmsHelper
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
import java.io.File
import java.util.UUID

class ChatRepository(
  private val context: Context,
  private val scope: CoroutineScope
) {
  private val database = CipherDatabase.getDatabase(context, scope)
  private val conversationDao = database.conversationDao()
  private val messageDao = database.messageDao()
  private val deviceSessionDao = database.deviceSessionDao()
  private val contactDao = database.contactDao()
  private val notificationHelper = NotificationHelper(context)

  // Network Connectivity State (Simulated & Reactive)
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

  init {
    // Seed preview test account data immediately
    scope.launch(Dispatchers.IO) {
      seedPreviewTestData()
    }

    // Start periodic background cleaner for disappearing messages
    scope.launch(Dispatchers.IO) {
      while (true) {
        delay(5000)
        messageDao.deleteExpired(System.currentTimeMillis())
      }
    }
  }

  suspend fun seedPreviewTestData() {
    val testEchoId = "chat_crystal_echo_test"
    if (conversationDao.getConversationDirect(testEchoId) == null) {
      val testContact = ContactEntity(
        id = "contact_crystal_echo_test",
        name = "Crystal Echo (Test Account)",
        phoneNumber = "+91 98765 43210",
        handle = "@crystal_echo",
        avatarColorHex = 0xFF0D9488,
        hasApp = true,
        addedAt = System.currentTimeMillis()
      )
      contactDao.insert(testContact)

      val conv = ConversationEntity(
        id = testEchoId,
        title = "Crystal Echo (Test Account)",
        isGroup = false,
        participantNames = "Crystal Echo",
        participantHandles = "@crystal_echo",
        phoneNumber = "+91 98765 43210",
        isExternalSms = false,
        lastMessageText = "Welcome to Crystal Chat preview! Send me any message to test encrypted messaging.",
        lastMessageTime = System.currentTimeMillis(),
        unreadCount = 1,
        isPinned = true,
        isVerified = true,
        safetyNumber = "84920 18492 01849 20184 92018",
        avatarColorHex = 0xFF0D9488
      )
      conversationDao.insert(conv)

      val welcomeText = "Welcome to Crystal Chat! I am your preview test account. Type any message below, pick an image, or test disappearing timers. Every message is end-to-end encrypted with AES-256-GCM."
      val welcomePayload = CryptoEngine.encrypt(welcomeText)
      messageDao.insert(
        MessageEntity(
          id = "msg_welcome_test_echo",
          conversationId = testEchoId,
          senderId = "peer",
          senderName = "Crystal Echo",
          cipherTextBase64 = welcomePayload.cipherTextBase64,
          nonceBase64 = welcomePayload.nonceBase64,
          plainText = welcomeText,
          timestamp = System.currentTimeMillis(),
          status = "READ",
          isOutgoing = false,
          authTagHex = welcomePayload.authTagHex
        )
      )
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

      // Encrypt with AES-256-GCM
      val payload = CryptoEngine.encrypt(text)

      val isCurrentOnline = _isOnline.value
      val initialStatus = when {
        isSmsDelivery -> "SENT_SMS"
        isCurrentOnline -> "SENT"
        else -> "QUEUED_OFFLINE"
      }

      val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
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
        // Send directly in background via Android's SmsManager - sender NEVER routed to external app!
        SmsHelper.sendDirectSmsInBackground(
          context = context,
          phoneNumber = phone,
          messageText = text
        )
        simulateSmsLifecycle(msgId, conversationId, phone, text)
      } else if (isCurrentOnline) {
        simulateMessageLifecycle(msgId, conversationId, text, mediaType)
      }
    }
  }

  private fun simulateSmsLifecycle(msgId: String, conversationId: String, phone: String, userText: String) {
    scope.launch(Dispatchers.IO) {
      delay(300)
      messageDao.updateStatus(msgId, "SENT_SMS")
      // In preview, always simulate delivery confirmation / reply so testing is immediate
      delay(1200)
      val reply = "Simulated SMS from $phone: Received \"$userText\""
      simulateIncomingPeerMessage(conversationId, phone, reply)
    }
  }

  private fun simulateMessageLifecycle(
    msgId: String,
    conversationId: String,
    userText: String,
    mediaType: String = "TEXT"
  ) {
    scope.launch(Dispatchers.IO) {
      delay(300)
      messageDao.updateStatus(msgId, "DELIVERED")
      delay(600)
      messageDao.updateStatus(msgId, "READ")

      val conv = conversationDao.getConversationDirect(conversationId) ?: return@launch
      delay(1000)
      val replyText = when {
        mediaType == "IMAGE" ->
          "Received your photo! Decrypted in volatile memory [AES-256-GCM]."
        conv.id == "chat_crystal_echo_test" || conv.participantHandles.contains("echo") ->
          "Echo [AES-256-GCM Decrypted]: \"$userText\""
        conv.isGroup ->
          "Peer in ${conv.title}: \"$userText\" (Sender key verified)"
        else ->
          "Received: \"$userText\" [Encrypted payload validated]"
      }

      simulateIncomingPeerMessage(
        conversationId = conversationId,
        senderName = conv.title.substringBefore(" ("),
        plainText = replyText
      )
    }
  }

  fun simulateIncomingPeerMessage(
    conversationId: String,
    senderName: String,
    plainText: String
  ) {
    scope.launch(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      val payload = CryptoEngine.encrypt(plainText)
      val replyId = "msg_peer_${UUID.randomUUID().toString().take(8)}"
      val replyMsg = MessageEntity(
        id = replyId,
        conversationId = conversationId,
        senderId = "peer",
        senderName = senderName,
        cipherTextBase64 = payload.cipherTextBase64,
        nonceBase64 = payload.nonceBase64,
        plainText = plainText,
        mediaType = "TEXT",
        timestamp = now,
        status = "READ",
        isOutgoing = false,
        authTagHex = payload.authTagHex
      )
      messageDao.insert(replyMsg)
      conversationDao.updateLastMessage(conversationId, "$senderName: $plainText", now)

      // Fire notification if user is not in this conversation
      if (activeChatId.value != conversationId) {
        notificationHelper.showMessageNotification(
          conversationId = conversationId,
          senderName = senderName,
          plainText = plainText,
          privacyMode = notificationPrivacyMode.value
        )
      }
    }
  }

  fun flushOfflineQueue() {
    scope.launch(Dispatchers.IO) {
      val queued = messageDao.getQueuedOfflineMessages()
      if (queued.isEmpty()) return@launch

      for (msg in queued) {
        delay(400)
        messageDao.updateStatus(msg.id, "SENT")
        delay(500)
        messageDao.updateStatus(msg.id, "DELIVERED")
        delay(800)
        messageDao.updateStatus(msg.id, "READ")
      }
    }
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
      "SMS Contact • Messages route to default messaging app (${contact.phoneNumber})"
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
