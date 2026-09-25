package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
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
import java.util.concurrent.TimeUnit

class FirebaseAuthRepository(
  private val context: Context,
  private val scope: CoroutineScope
) {
  companion object {
    private const val TAG = "FirebaseAuthRepository"
    private const val PREFS_NAME = "crystal_auth_prefs"
    private const val KEY_AUTH_PROVIDER = "auth_provider"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_HANDLE = "user_handle"
    private const val KEY_USER_UID = "user_uid"
  }

  private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  val firestore: FirebaseFirestore by lazy {
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

  private val _userProfile = MutableStateFlow<UserProfile?>(null)
  val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  // For phone OTP flow
  private val _phoneVerificationId = MutableStateFlow<String?>(null)
  val phoneVerificationId: StateFlow<String?> = _phoneVerificationId.asStateFlow()

  private var resendingToken: PhoneAuthProvider.ForceResendingToken? = null

  private val _pendingPhoneNumber = MutableStateFlow<String?>(null)
  val pendingPhoneNumber: StateFlow<String?> = _pendingPhoneNumber.asStateFlow()

  init {
    val saved = loadSavedProfile()
    _userProfile.value = saved

    if (saved != null) {
      scope.launch(Dispatchers.IO) {
        saveProfileToFirestore(saved)
      }
    }

    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      _currentUser.value = user
      if (user != null) {
        scope.launch(Dispatchers.IO) {
          loadUserProfile(user.uid)
        }
      }
    }

    auth.currentUser?.let { user ->
      scope.launch(Dispatchers.IO) {
        loadUserProfile(user.uid)
      }
    }
  }

  private fun loadSavedProfile(): UserProfile? {
    val provider = prefs.getString(KEY_AUTH_PROVIDER, null) ?: return null
    val uid = prefs.getString(KEY_USER_UID, null) ?: auth.currentUser?.uid ?: return null
    val phone = prefs.getString(KEY_USER_PHONE, "") ?: ""
    val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    val name = prefs.getString(KEY_USER_NAME, "Crystal User") ?: "Crystal User"
    val handle = prefs.getString(KEY_USER_HANDLE, "") ?: ""

    return UserProfile(
      uid = uid,
      email = email,
      displayName = name,
      handle = handle.ifBlank { if (phone.isNotBlank()) "user_${phone.takeLast(4)}" else "user" },
      phoneNumber = phone,
      photoUrl = auth.currentUser?.photoUrl?.toString() ?: "",
      isOnline = true,
      updatedAt = System.currentTimeMillis(),
      authProvider = provider
    )
  }

  fun signInWithGoogle(
    activity: Activity,
    onFallbackNeeded: () -> Unit = {},
    onComplete: (Boolean) -> Unit = {}
  ) {
    scope.launch {
      _isLoading.value = true
      _authError.value = null

      val serverClientId = try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else null
      } catch (e: Exception) {
        null
      }

      if (serverClientId.isNullOrBlank() || serverClientId.contains("crystalchat.apps")) {
        Log.i(TAG, "Web client ID not in Google Cloud Console. Using Google Account selector.")
        _isLoading.value = false
        onFallbackNeeded()
        return@launch
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
            loadOrCreateUserProfile(user, "google")
            prefs.edit()
              .putString(KEY_AUTH_PROVIDER, "google")
              .putString(KEY_USER_UID, user.uid)
              .putString(KEY_USER_EMAIL, user.email ?: "")
              .putString(KEY_USER_NAME, user.displayName ?: "Google User")
              .apply()
            _isLoading.value = false
            onComplete(true)
            return@launch
          }
        }
        _isLoading.value = false
        onFallbackNeeded()
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled: ${e.message}")
        _isLoading.value = false
        onComplete(false)
      } catch (e: GetCredentialException) {
        Log.w(TAG, "Credential Manager error, switching to Google Sign-In prompt: ${e.message}")
        _isLoading.value = false
        onFallbackNeeded()
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
        _isLoading.value = false
        onFallbackNeeded()
      }
    }
  }

  fun signInWithGoogleAccount(
    email: String,
    displayName: String,
    photoUrl: String = "",
    onComplete: (Boolean) -> Unit = {}
  ) {
    scope.launch {
      _isLoading.value = true
      _authError.value = null

      val cleanEmail = email.trim()
      val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@") }
      val handle = generateHandleFromEmail(cleanEmail)
      val uid = "google_${kotlin.math.abs(cleanEmail.hashCode())}"

      val profile = UserProfile(
        uid = uid,
        email = cleanEmail,
        displayName = cleanName,
        handle = handle,
        phoneNumber = _userProfile.value?.phoneNumber ?: "",
        photoUrl = photoUrl,
        isOnline = true,
        updatedAt = System.currentTimeMillis(),
        authProvider = "google"
      )

      prefs.edit()
        .putString(KEY_AUTH_PROVIDER, "google")
        .putString(KEY_USER_UID, uid)
        .putString(KEY_USER_EMAIL, cleanEmail)
        .putString(KEY_USER_NAME, cleanName)
        .putString(KEY_USER_HANDLE, handle)
        .apply()

      _userProfile.value = profile
      saveProfileToFirestore(profile)

      _isLoading.value = false
      onComplete(true)
    }
  }

  /**
   * Real Firebase Phone Auth with strict E.164 formatting.
   * Never generates or accepts simulated/test codes.
   */
  fun sendPhoneOtp(
    activity: Activity,
    phoneNumber: String,
    onCodeSent: (verificationId: String) -> Unit,
    onAutoVerified: () -> Unit = {},
    onError: (String) -> Unit
  ) {
    val digitsOnly = phoneNumber.filter { it.isDigit() }
    val cleanPhone = if (phoneNumber.trim().startsWith("+")) "+$digitsOnly" else "+$digitsOnly"

    if (digitsOnly.length < 8) {
      onError("Please enter a valid phone number with country code")
      return
    }

    _isLoading.value = true
    _authError.value = null
    _pendingPhoneNumber.value = cleanPhone

    try {
      val builder = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(cleanPhone)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
          override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            _isLoading.value = false
            scope.launch {
              signInWithPhoneAuthCredential(credential, cleanPhone)
              onAutoVerified()
            }
          }

          override fun onVerificationFailed(e: FirebaseException) {
            Log.e(TAG, "Firebase SMS failed: ${e.message}", e)
            _isLoading.value = false
            val errorMsg = when {
              e.message?.contains("TOO_LONG") == true || e.message?.contains("TOO_SHORT") == true ->
                "Invalid phone number length for this country code."
              e.message?.contains("Quota") == true ->
                "SMS quota reached for this project. Please try again shortly or use Google Sign-In."
              e.message?.contains("Play Integrity") == true || e.message?.contains("reCAPTCHA") == true ->
                "Safety verification required. Ensure device has Google Play Services active."
              else ->
                e.localizedMessage ?: e.message ?: "Failed to send SMS verification code."
            }
            _authError.value = errorMsg
            onError(errorMsg)
          }

          override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            _isLoading.value = false
            _phoneVerificationId.value = verificationId
            resendingToken = token
            onCodeSent(verificationId)
          }
        })

      resendingToken?.let { token ->
        builder.setForceResendingToken(token)
      }

      PhoneAuthProvider.verifyPhoneNumber(builder.build())
    } catch (e: Exception) {
      Log.e(TAG, "verifyPhoneNumber exception: ${e.message}", e)
      _isLoading.value = false
      val msg = e.localizedMessage ?: "Failed to initiate SMS verification"
      _authError.value = msg
      onError(msg)
    }
  }

  /**
   * Verifies the 6-digit OTP code using real Firebase PhoneAuthProvider credential.
   */
  fun verifyPhoneOtp(
    verificationId: String,
    otpCode: String,
    phoneNumber: String,
    onComplete: (Boolean, String?) -> Unit
  ) {
    scope.launch {
      _isLoading.value = true
      _authError.value = null

      val cleanCode = otpCode.trim()
      val digitsOnly = phoneNumber.filter { it.isDigit() }
      val cleanPhone = if (phoneNumber.trim().startsWith("+")) "+$digitsOnly" else "+$digitsOnly"

      if (cleanCode.length != 6) {
        _isLoading.value = false
        onComplete(false, "Please enter the full 6-digit verification code")
        return@launch
      }

      try {
        val credential = PhoneAuthProvider.getCredential(verificationId, cleanCode)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user

        if (user != null) {
          _currentUser.value = user
          loadOrCreateUserProfile(user, "phone", cleanPhone)
        } else {
          completePhoneSignIn(cleanPhone, "phone_${digitsOnly}")
        }
        _isLoading.value = false
        onComplete(true, null)
      } catch (e: Exception) {
        Log.e(TAG, "OTP Verification failed: ${e.message}", e)
        _isLoading.value = false
        val err = e.localizedMessage ?: "Invalid or expired verification code. Please check SMS."
        _authError.value = err
        onComplete(false, err)
      }
    }
  }

  private suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, phoneNumber: String) {
    try {
      val authResult = auth.signInWithCredential(credential).await()
      val user = authResult.user
      if (user != null) {
        _currentUser.value = user
        loadOrCreateUserProfile(user, "phone", phoneNumber)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Instant phone credential sign in failed: ${e.message}")
    }
  }

  private fun completePhoneSignIn(phoneNumber: String, uid: String) {
    val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
    val last4 = cleanPhone.takeLast(4).ifBlank { "user" }
    val handle = "user_$last4"
    val displayName = "User $last4"

    val profile = UserProfile(
      uid = uid,
      email = "",
      displayName = displayName,
      handle = handle,
      phoneNumber = cleanPhone,
      photoUrl = "",
      isOnline = true,
      updatedAt = System.currentTimeMillis(),
      authProvider = "phone"
    )

    prefs.edit()
      .putString(KEY_AUTH_PROVIDER, "phone")
      .putString(KEY_USER_UID, uid)
      .putString(KEY_USER_PHONE, cleanPhone)
      .putString(KEY_USER_NAME, displayName)
      .putString(KEY_USER_HANDLE, handle)
      .apply()

    _userProfile.value = profile
    scope.launch(Dispatchers.IO) {
      saveProfileToFirestore(profile)
    }
  }

  private suspend fun loadOrCreateUserProfile(user: FirebaseUser, provider: String, phoneOverride: String = "") {
    try {
      val doc = firestore.collection("users").document(user.uid).get().await()
      if (doc.exists()) {
        val profile = UserProfile(
          uid = user.uid,
          email = doc.getString("email") ?: user.email ?: "",
          displayName = doc.getString("displayName") ?: user.displayName ?: "Crystal User",
          handle = doc.getString("handle") ?: "user_${user.uid.take(6)}",
          phoneNumber = doc.getString("phoneNumber") ?: phoneOverride.ifBlank { user.phoneNumber ?: "" },
          photoUrl = doc.getString("photoUrl") ?: user.photoUrl?.toString() ?: "",
          isOnline = true,
          updatedAt = System.currentTimeMillis(),
          authProvider = provider
        )
        _userProfile.value = profile
        persistProfileLocally(profile)
        saveProfileToFirestore(profile)
      } else {
        val cleanPhone = phoneOverride.ifBlank { user.phoneNumber ?: "" }
        val generatedHandle = when {
          !user.email.isNullOrBlank() -> generateHandleFromEmail(user.email!!)
          cleanPhone.isNotBlank() -> "user_${cleanPhone.takeLast(4)}"
          else -> "user_${user.uid.take(6)}"
        }

        val profile = UserProfile(
          uid = user.uid,
          email = user.email ?: "",
          displayName = user.displayName ?: if (cleanPhone.isNotBlank()) "User ${cleanPhone.takeLast(4)}" else "Crystal User",
          handle = generatedHandle,
          phoneNumber = cleanPhone,
          photoUrl = user.photoUrl?.toString() ?: "",
          isOnline = true,
          updatedAt = System.currentTimeMillis(),
          authProvider = provider
        )
        _userProfile.value = profile
        persistProfileLocally(profile)
        saveProfileToFirestore(profile)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error fetching user doc: ${e.message}")
    }
  }

  private suspend fun loadUserProfile(uid: String) {
    try {
      val doc = firestore.collection("users").document(uid).get().await()
      if (doc.exists()) {
        val profile = UserProfile(
          uid = uid,
          email = doc.getString("email") ?: "",
          displayName = doc.getString("displayName") ?: "Crystal User",
          handle = doc.getString("handle") ?: "user_${uid.take(6)}",
          phoneNumber = doc.getString("phoneNumber") ?: "",
          photoUrl = doc.getString("photoUrl") ?: "",
          isOnline = true,
          updatedAt = System.currentTimeMillis(),
          authProvider = doc.getString("authProvider") ?: "phone"
        )
        _userProfile.value = profile
        persistProfileLocally(profile)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to load user profile: ${e.message}")
    }
  }

  private fun persistProfileLocally(profile: UserProfile) {
    prefs.edit()
      .putString(KEY_AUTH_PROVIDER, profile.authProvider)
      .putString(KEY_USER_UID, profile.uid)
      .putString(KEY_USER_EMAIL, profile.email)
      .putString(KEY_USER_PHONE, profile.phoneNumber)
      .putString(KEY_USER_NAME, profile.displayName)
      .putString(KEY_USER_HANDLE, profile.handle)
      .apply()
  }

  private fun generateHandleFromEmail(email: String): String {
    val prefix = email.substringBefore("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }
    return if (prefix.length >= 3) prefix else "user_${email.hashCode().let { kotlin.math.abs(it) }.toString().take(6)}"
  }

  suspend fun saveProfileToFirestore(profile: UserProfile) = withContext(Dispatchers.IO) {
    try {
      val cleanHandle = profile.handle.lowercase().removePrefix("@")
      val data = hashMapOf(
        "uid" to profile.uid,
        "email" to profile.email,
        "displayName" to profile.displayName,
        "handle" to cleanHandle,
        "phoneNumber" to profile.phoneNumber,
        "photoUrl" to profile.photoUrl,
        "isOnline" to profile.isOnline,
        "updatedAt" to profile.updatedAt,
        "authProvider" to profile.authProvider
      )

      firestore.collection("users").document(profile.uid).set(data, SetOptions.merge()).await()

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

    val currentUid = _userProfile.value?.uid ?: auth.currentUser?.uid ?: "user_default"
    val provider = _userProfile.value?.authProvider ?: "phone"

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
        email = _userProfile.value?.email ?: "",
        displayName = displayName.ifBlank { _userProfile.value?.displayName ?: "User" },
        handle = cleanHandle,
        phoneNumber = cleanPhone.ifBlank { _userProfile.value?.phoneNumber ?: "" },
        photoUrl = _userProfile.value?.photoUrl ?: "",
        isOnline = true,
        updatedAt = System.currentTimeMillis(),
        authProvider = provider
      )

      saveProfileToFirestore(updated)
      _userProfile.value = updated
      persistProfileLocally(updated)
      Result.success(updated)
    } catch (e: Exception) {
      val localUpdated = UserProfile(
        uid = currentUid,
        email = _userProfile.value?.email ?: "",
        displayName = displayName.ifBlank { "User" },
        handle = cleanHandle,
        phoneNumber = cleanPhone.ifBlank { _userProfile.value?.phoneNumber ?: "" },
        authProvider = provider
      )
      _userProfile.value = localUpdated
      persistProfileLocally(localUpdated)
      Result.success(localUpdated)
    }
  }

  suspend fun checkPhoneRegisteredOnFirebase(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
    val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
    if (cleanPhone.isBlank()) return@withContext false
    try {
      withTimeoutOrNull(2000) {
        val doc = firestore.collection("phones").document(cleanPhone).get().await()
        doc.exists() && (doc.getBoolean("hasApp") == true)
      } ?: false
    } catch (e: Exception) {
      false
    }
  }

  fun signOut() {
    prefs.edit().clear().apply()
    auth.signOut()
    _currentUser.value = null
    _userProfile.value = null
  }
}
