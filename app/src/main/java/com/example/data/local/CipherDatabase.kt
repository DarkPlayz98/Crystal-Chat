package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.crypto.CryptoEngine
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.DeviceSessionDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.model.ContactEntity
import com.example.data.local.model.ConversationEntity
import com.example.data.local.model.DeviceSessionEntity
import com.example.data.local.model.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [ConversationEntity::class, MessageEntity::class, DeviceSessionEntity::class, ContactEntity::class],
  version = 2,
  exportSchema = false
)
abstract class CipherDatabase : RoomDatabase() {
  abstract fun conversationDao(): ConversationDao
  abstract fun messageDao(): MessageDao
  abstract fun deviceSessionDao(): DeviceSessionDao
  abstract fun contactDao(): ContactDao

  companion object {
    @Volatile
    private var INSTANCE: CipherDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): CipherDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          CipherDatabase::class.java,
          "crystal_chat_database"
        )
          .fallbackToDestructiveMigration()
          .addCallback(CipherDatabaseCallback(scope))
          .build()
        INSTANCE = instance
        instance
      }
    }
  }

  private class CipherDatabaseCallback(
    private val scope: CoroutineScope
  ) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
      super.onCreate(db)
      INSTANCE?.let { database ->
        scope.launch(Dispatchers.IO) {
          populateInitialData(database)
        }
      }
    }

    private suspend fun populateInitialData(database: CipherDatabase) {
      val deviceDao = database.deviceSessionDao()

      val currentDevice = DeviceSessionEntity(
        id = "dev_current_primary",
        deviceName = "This Device (Primary)",
        platform = "Android Mobile",
        lastActive = "Active now",
        fingerprint = CryptoEngine.computeIdentityFingerprint("Crystal_Primary_Device_ECDH"),
        isCurrentDevice = true,
        isVerified = true
      )

      deviceDao.insert(currentDevice)
    }
  }
}
