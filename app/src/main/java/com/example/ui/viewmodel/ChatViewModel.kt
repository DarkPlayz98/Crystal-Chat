package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.ContactEntity
import com.example.data.local.model.ConversationEntity
import com.example.data.local.model.DeviceSessionEntity
import com.example.data.local.model.MessageEntity
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.FirebaseAuthRepository
import com.example.util.ImageStorageHelper
import com.example.util.NotificationPrivacyMode
import com.example.util.SmsHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {
  val repository = ChatRepository(application, viewModelScope)
  val authRepository = FirebaseAuthRepository(application, viewModelScope)

  val isOnline: StateFlow<Boolean> = repository.isOnline

  // Firebase Auth & Handle States
  val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser
  val userProfile: StateFlow<UserProfile?> = authRepository.userProfile
  val authLoading: StateFlow<Boolean> = authRepository.isLoading
  val authError: StateFlow<String?> = authRepository.authError

  // Conversations
  val allConversations: StateFlow<List<ConversationEntity>> = repository.allConversations
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val directChats: StateFlow<List<ConversationEntity>> = repository.directChats
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val groupChats: StateFlow<List<ConversationEntity>> = repository.groupChats
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val linkedDevices: StateFlow<List<DeviceSessionEntity>> = repository.linkedDevices
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Contacts
  val allContacts: StateFlow<List<ContactEntity>> = repository.allContacts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _selectedConversationId = MutableStateFlow<String?>(null)
  val selectedConversationId = _selectedConversationId.asStateFlow()

  val activeConversation: StateFlow<ConversationEntity?> = _selectedConversationId
    .flatMapLatest { id ->
      if (id != null) repository.getConversation(id) else flowOf(null)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val activeMessages: StateFlow<List<MessageEntity>> = _selectedConversationId
    .flatMapLatest { id ->
      if (id != null) repository.getMessages(id) else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val notificationPrivacyMode: StateFlow<NotificationPrivacyMode> = repository.notificationPrivacyMode
  val screenSecurityEnabled: StateFlow<Boolean> = repository.screenSecurityEnabled
  val biometricLockEnabled: StateFlow<Boolean> = repository.biometricLockEnabled
  val isAppLocked: StateFlow<Boolean> = repository.isAppLocked

  // Search filter query
  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectConversation(id: String?) {
    _selectedConversationId.value = id
    repository.activeChatId.value = id
    if (id != null) {
      repository.clearUnread(id)
    }
  }

  fun toggleOnline() {
    repository.toggleOnline()
  }

  fun sendMessage(
    text: String,
    mediaType: String = "TEXT",
    mediaUri: String? = null,
    mediaMeta: String? = null
  ) {
    val convId = _selectedConversationId.value ?: return
    if (text.isBlank() && mediaUri == null) return
    repository.sendMessage(convId, text.trim(), mediaType, mediaUri, mediaMeta)
  }

  /**
   * Fix for image sending: receives the real Uri selected from PhotoPicker,
   * copies image to local private storage, and sends the real image file.
   */
  fun sendRealImage(uri: Uri, caption: String = "") {
    val convId = _selectedConversationId.value ?: return
    viewModelScope.launch {
      val result = ImageStorageHelper.saveImageFromUri(getApplication(), uri) ?: return@launch
      val file = result.first
      val fileSize = result.second
      val sizeFormatted = ImageStorageHelper.formatFileSize(fileSize)

      val displayText = caption.ifBlank { "Sent an image" }
      repository.sendMessage(
        conversationId = convId,
        text = displayText,
        mediaType = "IMAGE",
        mediaUri = file.absolutePath,
        mediaMeta = "$sizeFormatted • Photo"
      )
    }
  }

  /**
   * Directly routes to default messaging app for a given phone and text.
   */
  fun openInDefaultMessagingApp(phoneNumber: String, text: String) {
    SmsHelper.openDefaultMessagingApp(getApplication(), phoneNumber, text)
  }

  /**
   * Adds a phone number based contact and checks whether they are on Firebase.
   */
  fun addContact(
    name: String,
    phoneNumber: String,
    handle: String? = null,
    onSuccess: (ContactEntity) -> Unit = {}
  ) {
    viewModelScope.launch {
      val hasApp = authRepository.checkPhoneRegisteredOnFirebase(phoneNumber)
      val contact = repository.addContact(name, phoneNumber, handle, hasApp)
      onSuccess(contact)
    }
  }

  fun deleteContact(id: String) {
    viewModelScope.launch {
      repository.deleteContact(id)
    }
  }

  fun startChatWithContact(contact: ContactEntity) {
    viewModelScope.launch {
      val chatId = repository.startChatWithContact(contact)
      selectConversation(chatId)
    }
  }

  // Firebase Auth & Handle Actions
  fun loadPreviewTestAccount() {
    authRepository.loadPreviewTestAccount()
  }

  fun signInWithGoogle(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
    authRepository.signInWithGoogle(activity, onComplete)
  }

  fun updateHandleAndProfile(
    newHandle: String,
    displayName: String,
    phoneNumber: String,
    onResult: (Result<UserProfile>) -> Unit
  ) {
    viewModelScope.launch {
      val res = authRepository.updateHandleAndProfile(newHandle, displayName, phoneNumber)
      onResult(res)
    }
  }

  fun signOut() {
    authRepository.signOut()
  }

  fun toggleSafetyVerification(conversationId: String, currentVerified: Boolean) {
    repository.toggleSafetyVerification(conversationId, currentVerified)
  }

  fun updateDisappearingTimer(conversationId: String, seconds: Int) {
    repository.updateDisappearingTimer(conversationId, seconds)
  }

  fun clearChat(conversationId: String) {
    repository.clearChat(conversationId)
  }

  fun createGroup(title: String, members: List<String>, colorHex: Long) {
    repository.createGroup(title, members, colorHex)
  }

  fun createDirectChat(name: String, handle: String, colorHex: Long) {
    repository.createDirectChat(name, handle, colorHex)
  }

  fun linkDevice(name: String, platform: String) {
    repository.linkDevice(name, platform)
  }

  fun revokeDevice(id: String) {
    repository.revokeDevice(id)
  }

  fun setNotificationPrivacyMode(mode: NotificationPrivacyMode) {
    repository.notificationPrivacyMode.value = mode
  }

  fun setScreenSecurity(enabled: Boolean) {
    repository.screenSecurityEnabled.value = enabled
  }

  fun setBiometricLock(enabled: Boolean) {
    repository.biometricLockEnabled.value = enabled
    if (!enabled) {
      repository.isAppLocked.value = false
    }
  }

  fun unlockApp() {
    repository.unlockApp()
  }

  fun lockApp() {
    repository.lockApp()
  }

  fun exportBackup(passphrase: String, onResult: (String) -> Unit) {
    viewModelScope.launch {
      val backup = repository.exportEncryptedCloudBackup(passphrase)
      onResult(backup)
    }
  }

  fun restoreBackup(encryptedJson: String, passphrase: String, onResult: (Boolean) -> Unit) {
    viewModelScope.launch {
      val success = repository.restoreEncryptedCloudBackup(encryptedJson, passphrase)
      onResult(success)
    }
  }
}
