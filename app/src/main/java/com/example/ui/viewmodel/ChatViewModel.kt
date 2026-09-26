package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CipherDatabase
import com.example.data.local.model.CallEntity
import com.example.data.local.model.ContactEntity
import com.example.data.local.model.ConversationEntity
import com.example.data.local.model.DeviceSessionEntity
import com.example.data.local.model.MessageEntity
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.FirebaseAuthRepository
import com.example.util.ActiveCallSession
import com.example.util.CallManager
import com.example.util.ContactsSyncResult
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
  val authRepository = FirebaseAuthRepository(application, viewModelScope)
  val repository = ChatRepository(application, viewModelScope, authRepository)
  private val database = CipherDatabase.getDatabase(application, viewModelScope)

  // HD+ Voice Call Manager
  private val callManager = CallManager(application, database.callDao(), viewModelScope)
  val activeCall: StateFlow<ActiveCallSession?> = callManager.activeCall
  val waveformHeights: StateFlow<List<Float>> = callManager.waveformHeights
  val callLogs: StateFlow<List<CallEntity>> = database.callDao().getAllCalls()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val isOnline: StateFlow<Boolean> = repository.isOnline

  // Firebase Auth & Handle States
  val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser
  val userProfile: StateFlow<UserProfile?> = authRepository.userProfile
  val authLoading: StateFlow<Boolean> = authRepository.isLoading
  val authError: StateFlow<String?> = authRepository.authError

  // Phone OTP States
  val phoneVerificationId: StateFlow<String?> = authRepository.phoneVerificationId
  val pendingPhoneNumber: StateFlow<String?> = authRepository.pendingPhoneNumber
  val isApiKeyRestricted: StateFlow<Boolean> = authRepository.isApiKeyRestricted
  val generatedSecurityCode: StateFlow<String?> = authRepository.generatedSecurityCode

  // Contact Syncing State
  private val _isSyncingContacts = MutableStateFlow(false)
  val isSyncingContacts: StateFlow<Boolean> = _isSyncingContacts.asStateFlow()

  private val _lastSyncResult = MutableStateFlow<ContactsSyncResult?>(null)
  val lastSyncResult: StateFlow<ContactsSyncResult?> = _lastSyncResult.asStateFlow()

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
    if (id != null) {
      repository.setActiveChat(id)
      repository.markAsRead(id)
    } else {
      repository.setActiveChat(null)
    }
  }

  fun sendMessage(text: String) {
    val id = _selectedConversationId.value ?: return
    repository.sendMessage(id, text)
  }

  fun sendRealImage(uri: Uri, caption: String = "") {
    val id = _selectedConversationId.value ?: return
    viewModelScope.launch {
      val saved = ImageStorageHelper.saveImageFromUri(getApplication(), uri)
      if (saved != null) {
        val fileUri = Uri.fromFile(saved.first).toString()
        val sizeFormatted = ImageStorageHelper.formatFileSize(saved.second)
        val text = if (caption.isNotBlank()) caption else "Photo ($sizeFormatted)"
        repository.sendMessage(
          conversationId = id,
          text = text,
          mediaType = "IMAGE",
          mediaUri = fileUri,
          mediaMeta = sizeFormatted
        )
      }
    }
  }

  fun sendImageMessage(uri: Uri) {
    sendRealImage(uri)
  }

  fun sendVoiceMessage(localPath: String, durationSecs: Int) {
    val id = _selectedConversationId.value ?: return
    repository.sendMessage(
      conversationId = id,
      text = "Voice message (${durationSecs}s)",
      mediaType = "VOICE",
      mediaUri = localPath,
      mediaMeta = "$durationSecs"
    )
  }

  fun openExternalSms(phoneNumber: String, text: String = "") {
    SmsHelper.openDefaultMessagingApp(getApplication(), phoneNumber, text)
  }

  fun createGroup(title: String, memberNames: List<String>, colorHex: Long) {
    repository.createGroup(title, memberNames, colorHex)
  }

  fun createDirectChat(name: String, handle: String, colorHex: Long): String {
    val id = repository.createDirectChat(name, handle, colorHex)
    selectConversation(id)
    return id
  }

  fun startChatWithContact(contact: ContactEntity) {
    viewModelScope.launch {
      val chatId = repository.startChatWithContact(contact)
      selectConversation(chatId)
    }
  }

  // HD+ Voice Calling Actions
  fun startVoiceCall(
    contactName: String,
    phoneNumber: String,
    handle: String? = null,
    avatarColorHex: Long = 0xFF0D9488,
    recipientHasApp: Boolean = true
  ) {
    callManager.startOutgoingCall(contactName, phoneNumber, handle, avatarColorHex, recipientHasApp = recipientHasApp)
  }

  fun changeCallerTune(tune: com.example.util.CallerTuneStyle) {
    callManager.changeCallerTune(tune)
  }

  fun dialWithDefaultCallerApp(phoneNumber: String) {
    CallManager.dialWithDefaultCallerApp(getApplication(), phoneNumber)
  }

  fun openDefaultSms(phoneNumber: String, text: String = "") {
    SmsHelper.openDefaultMessagingApp(getApplication(), phoneNumber, text)
  }

  fun playDtmfTone(digit: Char) {
    callManager.playDtmfTone(digit)
  }

  fun answerCall() {
    callManager.answerIncomingCall()
  }

  fun endCall() {
    callManager.endCall()
  }

  fun toggleCallMute() {
    callManager.toggleMute()
  }

  fun toggleCallSpeaker() {
    callManager.toggleSpeaker()
  }

  fun toggleCallMinimize(minimize: Boolean) {
    callManager.toggleMinimize(minimize)
  }

  fun deleteCallLog(id: String) {
    viewModelScope.launch {
      database.callDao().deleteById(id)
    }
  }

  fun clearAllCalls() {
    viewModelScope.launch {
      database.callDao().clearAll()
    }
  }

  // Google Sign-In with Fallback Dialog
  fun signInWithGoogle(
    activity: Activity,
    onFallbackNeeded: () -> Unit = {},
    onComplete: (Boolean) -> Unit = {}
  ) {
    authRepository.signInWithGoogle(activity, onFallbackNeeded) { success ->
      if (success) {
        syncContacts()
      }
      onComplete(success)
    }
  }

  fun signInWithGoogleAccount(
    email: String,
    displayName: String,
    photoUrl: String = "",
    onComplete: (Boolean) -> Unit = {}
  ) {
    authRepository.signInWithGoogleAccount(email, displayName, photoUrl) { success ->
      if (success) {
        syncContacts()
      }
      onComplete(success)
    }
  }

  // Real Phone Number OTP Sign-In (No simulated or test codes)
  fun sendPhoneOtp(
    activity: Activity,
    phoneNumber: String,
    onCodeSent: (verificationId: String) -> Unit,
    onAutoVerified: () -> Unit = {},
    onError: (String) -> Unit
  ) {
    authRepository.sendPhoneOtp(activity, phoneNumber, onCodeSent, onAutoVerified, onError)
  }

  fun verifyPhoneOtp(
    verificationId: String,
    otpCode: String,
    phoneNumber: String,
    onComplete: (Boolean, String?) -> Unit
  ) {
    authRepository.verifyPhoneOtp(verificationId, otpCode, phoneNumber) { success, err ->
      if (success) {
        syncContacts()
      }
      onComplete(success, err)
    }
  }

  fun sendDeviceSecurityOtp(
    phoneNumber: String,
    onCodeSent: (verificationId: String, codeSent: String) -> Unit
  ) {
    authRepository.sendDeviceSecurityOtp(phoneNumber, onCodeSent)
  }

  fun setCustomFirebaseApiKey(apiKey: String, projectId: String? = null): Boolean {
    return authRepository.setCustomFirebaseApiKey(apiKey, projectId)
  }

  fun getGoogleSystemPickerIntent(): android.content.Intent {
    return authRepository.getGoogleSystemPickerIntent()
  }

  fun handleGoogleAccountPicked(accountEmail: String, onComplete: (Boolean) -> Unit) {
    authRepository.handleGoogleAccountPicked(accountEmail) { success ->
      if (success) {
        syncContacts()
      }
      onComplete(success)
    }
  }

  fun signOut() {
    authRepository.signOut()
  }

  // Sync Contacts from Device
  fun syncContacts(onResult: (ContactsSyncResult) -> Unit = {}) {
    viewModelScope.launch {
      _isSyncingContacts.value = true
      val res = repository.syncContactsFromDevice()
      _lastSyncResult.value = res
      _isSyncingContacts.value = false
      onResult(res)
    }
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

  fun toggleOnline() {
    repository.toggleOnline()
  }

  fun updateDisappearingTimer(conversationId: String, seconds: Int) {
    repository.updateDisappearingTimer(conversationId, seconds)
  }

  fun clearChat(conversationId: String) {
    repository.clearChat(conversationId)
  }

  fun togglePin(conversationId: String, isPinned: Boolean) {
    repository.togglePin(conversationId, isPinned)
  }

  fun toggleSafetyVerification(conversationId: String, currentVerified: Boolean) {
    repository.toggleSafetyVerification(conversationId, currentVerified)
  }

  fun addContact(name: String, phoneNumber: String, handle: String? = null, onComplete: (ContactEntity) -> Unit = {}) {
    viewModelScope.launch {
      val contact = repository.addContact(name, phoneNumber, handle)
      onComplete(contact)
    }
  }

  fun deleteContact(contactId: String) {
    viewModelScope.launch {
      repository.deleteContact(contactId)
    }
  }

  fun toggleContactHasApp(contactId: String, currentHasApp: Boolean) {
    viewModelScope.launch {
      repository.updateContactHasApp(contactId, !currentHasApp)
    }
  }

  fun deleteConversation(id: String) {
    repository.deleteConversation(id)
    if (_selectedConversationId.value == id) {
      _selectedConversationId.value = null
    }
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
