package com.smsread.app.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "sms_events", indices = [Index(value = ["receivedAtMillis"])])
data class SmsEvent(
    @PrimaryKey val eventId: String,
    val sender: String,
    val rawText: String,
    val receivedAtMillis: Long,
    val bankCode: String?,
    val direction: String?,
    val amountIrr: Long?,
    val balanceIrr: Long?,
    val accountHint: String?,
    val dateText: String?,
    val parseStatus: String,
    val syncStatus: String = "PENDING",
    val attemptCount: Int = 0
)

@Dao
interface SmsEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: SmsEvent)

    @Query("SELECT * FROM sms_events WHERE syncStatus = 'PENDING' ORDER BY receivedAtMillis ASC LIMIT :limit")
    suspend fun pending(limit: Int = 50): List<SmsEvent>

    @Query("UPDATE sms_events SET syncStatus = 'ACKED' WHERE eventId IN (:ids) AND syncStatus = 'PENDING'")
    suspend fun acknowledge(ids: List<String>)

    @Query("UPDATE sms_events SET attemptCount = attemptCount + 1 WHERE eventId IN (:ids)")
    suspend fun recordAttempt(ids: List<String>)
}

@Database(entities = [SmsEvent::class], version = 1, exportSchema = true)
abstract class SmsDatabase : RoomDatabase() {
    abstract fun events(): SmsEventDao
    companion object {
        @Volatile private var instance: SmsDatabase? = null
        fun get(context: Context): SmsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, SmsDatabase::class.java, "sms_read.db"
                ).build().also { instance = it }
            }
    }
}
