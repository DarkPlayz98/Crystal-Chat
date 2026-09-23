package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseAuthRepository(
  private val context: Context,
  private val scope: CoroutineScope
) {
  companion object {
    private const val TAG = "FirebaseAuthRepository"

    val DEFAULT_TEST_PROFILE = UserProfile(
      uid = "preview_test_uid",
      email = "tester@crystalchat.io",
      displayName = "Preview Tester",
      handle = "crystal_tester",
      phoneNumber = "+91 98765 00001",
      photoUrl = "",
      isOnline = true,
      updatedAt = System.currentTimeMillis()
    )
  }

  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  private val firestore: FirebaseFirestore by lazy {
    val dbId = try {
      val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
      if (resId != 0) context.getString(resId) else null
    } catch (e: Exception) {
      null
    }
    if (!dbId.isNullOrBlank()) {
      try {
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
      } catch (e: Exception) {
        FirebaseFirestore.getInstance()
      }
    } else {
      FirebaseFirestore.getInstance()
    }
  }

  private val credentialManager = CredentialManager.create(context)

  private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

  // Pre-loaded with Preview Test Profile so preview users immediately have a working account
  private val _userProfile = MutableStateFlow<UserProfile?>(DEFAULT_TEST_PROFILE)
  val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  init {
    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      _currentUser.value = user
      if (user != null) {
        scope.launch(Dispatchers.IO) {
          loadUserProfile(user.uid)
        }
      } else {
        // Fall back to default preview test profile so app stays interactive
        _userProfile.value = DEFAULT_TEST_PROFILE
      }
    }

    // Load initial user if already signed in
    auth.currentUser?.let { user ->
      scope.launch(Dispatchers.IO) {
        loadUserProfile(user.uid)
      }
    }
  }

  fun loadPreviewTestAccount() {
    _userProfile.value = DEFAULT_TEST_PROFILE
  }

  /**
   * Signs in using Google Sign-In via Credential Manager.
   * Catches GetCredentialCancellationException separately to reset UI state gracefully.
   */
  fun signInWithGoogle(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
    scope.launch {
      _isLoading.value = true
      _authError.value = null

      val serverClientId = try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else "964408789892-crystalchat.apps.googleusercontent.com"
      } catch (e: Exception) {
        "964408789892-crystalchat.apps.googleusercontent.com"
      }

      val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
      val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleOption)
        .build()

      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
          val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
          val authResult = auth.signInWithCredential(authCredential).await()
          val user = authResult.user

          if (user != null) {
            _currentUser.value = user
            loadOrCreateUserProfile(user)
            _isLoading.value = false
            onComplete(true)
            return@launch
          }
        }
        _isLoading.value = false
        onComplete(false)
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In was cancelled by user: ${e.message}")
        _isLoading.value = false
        onComplete(false)
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
        _authError.value = e.localizedMessage ?: "Sign in failed"
        _isLoading.value = false
        onComplete(false)
      }
    }
  }

  private suspend fun loadOrCreateUserProfile(user: FirebaseUser) {
    try {
      val doc = withTimeoutOrNull(2500) {
        firestore.collection("users").document(user.uid).get().await()
      }
      if (doc != null && doc.exists()) {
        val profile = UserProfile(
          uid = user.uid,
          email = doc.getString("email") ?: user.email ?: "",
          displayName = doc.getString("displayName") ?: user.displayName ?: "Crystal User",
          handle = doc.getString("handle") ?: generateDefaultHandle(user),
          phoneNumber = doc.getString("phoneNumber") ?: user.phoneNumber ?: "",
          photoUrl = doc.getString("photoUrl") ?: user.photoUrl?.toString() ?: "",
          isOnline = doc.getBoolean("isOnline") ?: true,
          updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        )
        _userProfile.value = profile
      } else {
        val defaultHandle = generateDefaultHandle(user)
        val profile = UserProfile(
          uid = user.uid,
          email = user.email ?: "",
          displayName = user.displayName ?: "Crystal User",
          handle = defaultHandle,
          phoneNumber = user.phoneNumber ?: "",
          photoUrl = user.photoUrl?.toString() ?: "",
          isOnline = true,
          updatedAt = System.currentTimeMillis()
        )
        saveProfileToFirestore(profile)
        _userProfile.value = profile
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error loading/creating user profile: ${e.message}", e)
      _userProfile.value = UserProfile(
        uid = user.uid,
        email = user.email ?: "",
        displayName = user.displayName ?: "Crystal User",
        handle = generateDefaultHandle(user),
        phoneNumber = user.phoneNumber ?: ""
      )
    }
  }

  private fun generateDefaultHandle(user: FirebaseUser): String {
    val emailPrefix = user.email?.substringBefore("@")?.replace(".", "_")?.filter { it.isLetterOrDigit() || it == '_' }
    val namePrefix = user.displayName?.lowercase()?.replace(" ", "_")?.filter { it.isLetterOrDigit() || it == '_' }
    val base = emailPrefix ?: namePrefix ?: "user_${user.uid.take(5).lowercase()}"
    return base.take(15)
  }

  private suspend fun loadUserProfile(uid: String) {
    try {
      val doc = withTimeoutOrNull(2500) {
        firestore.collection("users").document(uid).get().await()
      }
      if (doc != null && doc.exists()) {
        _userProfile.value = UserProfile(
          uid = uid,
          email = doc.getString("email") ?: "",
          displayName = doc.getString("displayName") ?: "",
          handle = doc.getString("handle") ?: "",
          phoneNumber = doc.getString("phoneNumber") ?: "",
          photoUrl = doc.getString("photoUrl") ?: "",
          isOnline = doc.getBoolean("isOnline") ?: true,
          updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        )
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not fetch user profile: ${e.message}")
    }
  }

  private suspend fun saveProfileToFirestore(profile: UserProfile) {
    try {
      withTimeoutOrNull(2500) {
        val data = hashMapOf(
          "uid" to profile.uid,
          "email" to profile.email,
          "displayName" to profile.displayName,
          "handle" to profile.handle.lowercase().removePrefix("@"),
          "phoneNumber" to profile.phoneNumber,
          "photoUrl" to profile.photoUrl,
          "isOnline" to profile.isOnline,
          "updatedAt" to profile.updatedAt
        )
        firestore.collection("users").document(profile.uid).set(data, SetOptions.merge()).await()

        val cleanHandle = profile.handle.lowercase().removePrefix("@")
        if (cleanHandle.isNotBlank()) {
          firestore.collection("handles").document(cleanHandle).set(
            hashMapOf(
              "uid" to profile.uid,
              "handle" to cleanHandle,
              "displayName" to profile.displayName,
              "phoneNumber" to profile.phoneNumber
            ),
            SetOptions.merge()
          ).await()
        }

        val cleanPhone = profile.phoneNumber.filter { it.isDigit() || it == '+' }
        if (cleanPhone.isNotBlank()) {
          firestore.collection("phones").document(cleanPhone).set(
            hashMapOf(
              "uid" to profile.uid,
              "handle" to cleanHandle,
              "displayName" to profile.displayName,
              "hasApp" to true
            ),
            SetOptions.merge()
          ).await()
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to save profile to Firestore: ${e.message}")
    }
  }

  suspend fun updateHandleAndProfile(
    newHandle: String,
    displayName: String,
    phoneNumber: String
  ): Result<UserProfile> = withContext(Dispatchers.IO) {
    val cleanHandle = newHandle.lowercase().removePrefix("@").trim()
    val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }.trim()

    if (cleanHandle.length < 3) {
      return@withContext Result.failure(Exception("Handle must be at least 3 characters"))
    }
    if (!cleanHandle.all { it.isLetterOrDigit() || it == '_' }) {
      return@withContext Result.failure(Exception("Handle can only contain letters, numbers, and underscores"))
    }

    val currentUid = auth.currentUser?.uid ?: _userProfile.value?.uid ?: "preview_test_uid"

    try {
      withTimeoutOrNull(2000) {
        val existingDoc = firestore.collection("handles").document(cleanHandle).get().await()
        if (existingDoc.exists()) {
          val ownerUid = existingDoc.getString("uid")
          if (ownerUid != null && ownerUid != currentUid) {
            throw Exception("Handle @$cleanHandle is already taken by another user")
          }
        }
      }

      val updated = UserProfile(
        uid = currentUid,
        email = auth.currentUser?.email ?: _userProfile.value?.email ?: "tester@crystalchat.io",
        displayName = displayName.ifBlank { _userProfile.value?.displayName ?: "Preview Tester" },
        handle = cleanHandle,
        phoneNumber = cleanPhone,
        photoUrl = auth.currentUser?.photoUrl?.toString() ?: _userProfile.value?.photoUrl ?: "",
        isOnline = true,
        updatedAt = System.currentTimeMillis()
      )

      if (auth.currentUser != null) {
        saveProfileToFirestore(updated)
      }
      _userProfile.value = updated
      Result.success(updated)
    } catch (e: Exception) {
      val localUpdated = UserProfile(
        uid = currentUid,
        email = auth.currentUser?.email ?: _userProfile.value?.email ?: "tester@crystalchat.io",
        displayName = displayName.ifBlank { "Preview Tester" },
        handle = cleanHandle,
        phoneNumber = cleanPhone
      )
      _userProfile.value = localUpdated
      Result.success(localUpdated)
    }
  }

  suspend fun checkPhoneRegisteredOnFirebase(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
    val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
    if (cleanPhone.isBlank()) return@withContext false
    // Test account phone numbers are immediately identified as having the app
    if (cleanPhone.contains("9876543210") || cleanPhone.contains("9876500001")) {
      return@withContext true
    }
    try {
      withTimeoutOrNull(2000) {
        val doc = firestore.collection("phones").document(cleanPhone).get().await()
        doc.exists() && (doc.getBoolean("hasApp") == true)
      } ?: false
    } catch (e: Exception) {
      Log.w(TAG, "Could not verify phone on Firebase: ${e.message}")
      false
    }
  }

  suspend fun searchUserByHandle(handle: String): Map<String, String>? = withContext(Dispatchers.IO) {
    val cleanHandle = handle.lowercase().removePrefix("@").trim()
    if (cleanHandle == "crystal_echo") {
      return@withContext mapOf(
        "uid" to "preview_echo_uid",
        "handle" to "crystal_echo",
        "displayName" to "Crystal Echo (Test Account)",
        "phoneNumber" to "+91 98765 43210"
      )
    }
    try {
      withTimeoutOrNull(2000) {
        val doc = firestore.collection("handles").document(cleanHandle).get().await()
        if (doc.exists()) {
          mapOf(
            "uid" to (doc.getString("uid") ?: ""),
            "handle" to (doc.getString("handle") ?: cleanHandle),
            "displayName" to (doc.getString("displayName") ?: cleanHandle),
            "phoneNumber" to (doc.getString("phoneNumber") ?: "")
          )
        } else {
          null
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Handle search failed: ${e.message}")
      null
    }
  }

  fun signOut() {
    auth.signOut()
    _currentUser.value = null
    _userProfile.value = DEFAULT_TEST_PROFILE
  }
}
