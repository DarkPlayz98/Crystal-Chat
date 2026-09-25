package com.example.data.model

data class UserProfile(
  val uid: String = "",
  val email: String = "",
  val displayName: String = "",
  val handle: String = "",
  val phoneNumber: String = "",
  val photoUrl: String = "",
  val isOnline: Boolean = true,
  val updatedAt: Long = System.currentTimeMillis(),
  val authProvider: String = "guest" // "google", "phone", "guest"
) {
  val isGuest: Boolean
    get() = authProvider == "guest" || uid.startsWith("guest_") || uid == "preview_test_uid"

  val isGoogleAuth: Boolean
    get() = authProvider == "google"

  val isPhoneAuth: Boolean
    get() = authProvider == "phone"
}
