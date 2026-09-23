package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.model.ContactEntity
import com.example.data.local.model.ConversationEntity
import com.example.data.local.model.DeviceSessionEntity
import com.example.data.local.model.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
  @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTime DESC")
  fun getAllConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE isGroup = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
  fun getDirectChats(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE isGroup = 1 ORDER BY isPinned DESC, lastMessageTime DESC")
  fun getGroups(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  fun getConversationById(id: String): Flow<ConversationEntity?>

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  suspend fun getConversationDirect(id: String): ConversationEntity?

  @Query("SELECT * FROM conversations WHERE phoneNumber = :phone LIMIT 1")
  suspend fun getConversationByPhone(phone: String): ConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(conversation: ConversationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(conversations: List<ConversationEntity>)

  @Update
  suspend fun update(conversation: ConversationEntity)

  @Query("UPDATE conversations SET lastMessageText = :text, lastMessageTime = :time WHERE id = :id")
  suspend fun updateLastMessage(id: String, text: String, time: Long)

  @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
  suspend fun clearUnread(id: String)

  @Query("UPDATE conversations SET isVerified = :verified WHERE id = :id")
  suspend fun updateSafetyVerification(id: String, verified: Boolean)

  @Query("UPDATE conversations SET disappearingSeconds = :seconds WHERE id = :id")
  suspend fun updateDisappearingTimer(id: String, seconds: Int)

  @Query("DELETE FROM conversations WHERE id = :id")
  suspend fun deleteById(id: String)
}

@Dao
interface MessageDao {
  @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
  fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE status = 'QUEUED_OFFLINE' ORDER BY timestamp ASC")
  suspend fun getQueuedOfflineMessages(): List<MessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(message: MessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(messages: List<MessageEntity>)

  @Query("UPDATE messages SET status = :newStatus WHERE id = :id")
  suspend fun updateStatus(id: String, newStatus: String)

  @Query("DELETE FROM messages WHERE expiresAt > 0 AND expiresAt <= :currentTime")
  suspend fun deleteExpired(currentTime: Long): Int

  @Query("DELETE FROM messages WHERE conversationId = :convId")
  suspend fun clearMessagesForConversation(convId: String)

  @Query("SELECT * FROM messages ORDER BY timestamp ASC")
  suspend fun getAllMessages(): List<MessageEntity>

  @Query("SELECT COUNT(*) FROM messages")
  fun getTotalMessageCount(): Flow<Int>
}

@Dao
interface DeviceSessionDao {
  @Query("SELECT * FROM device_sessions ORDER BY isCurrentDevice DESC, id ASC")
  fun getAllSessions(): Flow<List<DeviceSessionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: DeviceSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(sessions: List<DeviceSessionEntity>)

  @Query("DELETE FROM device_sessions WHERE id = :id")
  suspend fun delete(id: String)
}

@Dao
interface ContactDao {
  @Query("SELECT * FROM contacts ORDER BY name ASC")
  fun getAllContacts(): Flow<List<ContactEntity>>

  @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
  suspend fun getContactById(id: String): ContactEntity?

  @Query("SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
  suspend fun getContactByPhone(phone: String): ContactEntity?

  @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR handle LIKE '%' || :query || '%'")
  fun searchContacts(query: String): Flow<List<ContactEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(contact: ContactEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(contacts: List<ContactEntity>)

  @Update
  suspend fun update(contact: ContactEntity)

  @Query("UPDATE contacts SET hasApp = :hasApp WHERE id = :id")
  suspend fun updateHasApp(id: String, hasApp: Boolean)

  @Query("DELETE FROM contacts WHERE id = :id")
  suspend fun deleteById(id: String)
}
