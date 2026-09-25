package com.example.util

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import com.example.data.local.dao.ContactDao
import com.example.data.local.model.ContactEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

data class ContactsSyncResult(
  val totalFound: Int,
  val addedCount: Int,
  val registeredAppUsers: Int,
  val errorMessage: String? = null
)

object ContactsSyncHelper {
  private const val TAG = "ContactsSyncHelper"

  private val COLOR_PALETTES = listOf(
    0xFF0D9488, 0xFF6366F1, 0xFFEC4899, 0xFFF59E0B, 0xFF3B82F6, 0xFF8B5CF6, 0xFF10B981, 0xFFEF4444
  )

  /**
   * Reads contacts from device phonebook, checks if each phone is registered
   * on Crystal Chat via Firebase Firestore, and updates local Room ContactDao.
   */
  suspend fun syncDeviceContacts(
    context: Context,
    contactDao: ContactDao,
    firestore: FirebaseFirestore
  ): ContactsSyncResult = withContext(Dispatchers.IO) {
    var totalFound = 0
    var addedCount = 0
    var registeredAppUsers = 0

    try {
      val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
      )

      val cursor: Cursor? = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
      )

      val rawContacts = mutableListOf<Pair<String, String>>() // Name, Phone
      cursor?.use { c ->
        val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (c.moveToNext()) {
          val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""
          val number = if (numIdx >= 0) c.getString(numIdx) ?: "" else ""
          if (name.isNotBlank() && number.isNotBlank()) {
            rawContacts.add(name.trim() to number.trim())
          }
        }
      }

      totalFound = rawContacts.size
      if (rawContacts.isEmpty()) {
        return@withContext ContactsSyncResult(0, 0, 0)
      }

      // Group by normalized phone number to avoid duplicates
      val uniqueContacts = mutableMapOf<String, String>() // CleanPhone -> Name
      for ((name, phone) in rawContacts) {
        val clean = phone.filter { it.isDigit() || it == '+' }
        if (clean.length >= 7 && !uniqueContacts.containsKey(clean)) {
          uniqueContacts[clean] = name
        }
      }

      // Query registered phones in Firestore in batch / parallel
      for ((cleanPhone, name) in uniqueContacts) {
        var hasApp = false
        var registeredHandle: String? = null

        try {
          val doc = withTimeoutOrNull(1500) {
            firestore.collection("phones").document(cleanPhone).get().await()
          }
          if (doc != null && doc.exists()) {
            hasApp = doc.getBoolean("hasApp") ?: true
            registeredHandle = doc.getString("handle")
          }
        } catch (e: Exception) {
          Log.w(TAG, "Failed phone check in Firestore for $cleanPhone: ${e.message}")
        }

        if (hasApp) {
          registeredAppUsers++
        }

        val existing = contactDao.getContactByPhone(cleanPhone)
        val color = COLOR_PALETTES[(name.hashCode() and 0x7FFFFFFF) % COLOR_PALETTES.size]

        if (existing != null) {
          contactDao.update(
            existing.copy(
              name = name,
              handle = registeredHandle?.let { if (it.startsWith("@")) it else "@$it" } ?: existing.handle,
              hasApp = hasApp || existing.hasApp
            )
          )
        } else {
          val id = "contact_synced_${UUID.randomUUID().toString().take(8)}"
          val handle = registeredHandle?.let { if (it.startsWith("@")) it else "@$it" }
          val newContact = ContactEntity(
            id = id,
            name = name,
            phoneNumber = cleanPhone,
            handle = handle,
            avatarColorHex = color,
            hasApp = hasApp,
            addedAt = System.currentTimeMillis()
          )
          contactDao.insert(newContact)
          addedCount++
        }
      }

      ContactsSyncResult(totalFound, addedCount, registeredAppUsers)
    } catch (e: SecurityException) {
      Log.e(TAG, "Contacts permission not granted: ${e.message}")
      ContactsSyncResult(0, 0, 0, "Contacts permission not granted")
    } catch (e: Exception) {
      Log.e(TAG, "Error syncing contacts: ${e.message}", e)
      ContactsSyncResult(totalFound, addedCount, registeredAppUsers, e.localizedMessage)
    }
  }
}
