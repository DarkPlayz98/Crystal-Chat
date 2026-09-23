package com.example.data.model

data class UserProfile(
  val uid: String = "",
  val email: String = "",
  val displayName: String = "",
  val handle: String = "",
  val phoneNumber: String = "",
  val photoUrl: String = "",
  val isOnline: Boolean = true,
  val updatedAt: Long = System.currentTimeMillis()
)
